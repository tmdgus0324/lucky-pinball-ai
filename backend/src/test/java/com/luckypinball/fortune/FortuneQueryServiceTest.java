package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.common.ApiException;
import com.luckypinball.common.ErrorLogStore;
import com.luckypinball.fortune.FortuneQueryService.FortuneQueryResult;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

/**
 * AI 호출 실패 시 임시 점수(FALLBACK)로 대체하는 동작과, 그것이 정체성 캐시를 오염시키지 않는지를
 * 실제 DB 쿼리(메모리 H2)까지 포함해 검증한다. Claude 쪽은 성공/실패를 마음대로 바꿀 수 있는 가짜로 교체한다.
 */
@SpringBootTest
class FortuneQueryServiceTest {

    /** 성공/실패를 바꿔 끼우고 호출 횟수를 세는 가짜 FortuneService. */
    static class ControllableFortuneService implements FortuneService {
        volatile RuntimeException failure;
        volatile int score = 90;
        volatile long delayMillis = 0;
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public FortuneResult analyze(String name, LocalDate birthDate) {
            calls.incrementAndGet();
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (failure != null) {
                throw failure;
            }
            return new FortuneResult(score, 7, "AI가 만든 운세");
        }
    }

    @TestConfiguration
    static class FakeAiConfig {
        @Bean
        @Primary
        ControllableFortuneService controllableFortuneService() {
            return new ControllableFortuneService();
        }
    }

    @Autowired FortuneQueryService service;
    @Autowired ControllableFortuneService ai;
    @Autowired PlayerService playerService;
    @Autowired FortuneResultJpaRepository repository;
    @Autowired ErrorLogStore errorLogStore;

    private static final LocalDate BIRTH = LocalDate.of(1990, 1, 1);

    @BeforeEach
    void resetFake() {
        ai.failure = null;
        ai.score = 90;
        ai.delayMillis = 0;
        ai.calls.set(0);
    }

    /** 테스트끼리 이름이 겹치면 이전 테스트가 남긴 캐시를 만나므로, 테스트마다 다른 이름을 쓴다. */
    private PlayerEntity newPlayer(String name) {
        return playerService.register(name, BIRTH);
    }

    private static ApiException upstreamDown() {
        return ApiException.upstreamFailure("AI 운세 서비스에 일시적인 문제가 있습니다.", new RuntimeException("401 invalid key"));
    }

    @Test
    void successfulAiCallIsReportedAsAi() {
        PlayerEntity p = newPlayer("성공");

        FortuneQueryResult result = service.getTodayFortune(p.getId());

        assertEquals("AI", result.source());
        assertEquals(90, result.fortuneScore());
        assertEquals(1, ai.calls.get());
    }

    @Test
    void whenAiFailsItFallsBackToATemporaryScoreInsteadOfFailing() {
        PlayerEntity p = newPlayer("실패폴백");
        ai.failure = upstreamDown();

        FortuneQueryResult result = service.getTodayFortune(p.getId());

        assertEquals("FALLBACK", result.source());
        assertEquals(FortuneQueryService.FALLBACK_NOTICE, result.fortuneMessage(), "규칙 기반 문구를 운세처럼 보여주면 안 된다");
        int expected = new MockFortuneGenerator().analyze("실패폴백", BIRTH).fortuneScore();
        assertEquals(expected, result.fortuneScore(), "같은 사람은 같은 임시 점수를 받는다(결정적)");
        assertTrue(errorLogStore.recent().stream().anyMatch(e -> "AI 호출".equals(e.path()) && e.message().contains("401 invalid key")),
                "폴백으로 가려진 AI 실패가 관리자 오류 로그에 남아야 한다");
    }

    @Test
    void aFallbackDoesNotPoisonTheIdentityCache() {
        PlayerEntity first = newPlayer("캐시오염");
        ai.failure = upstreamDown();
        assertEquals("FALLBACK", service.getTodayFortune(first.getId()).source());

        // AI가 복구된 뒤, 같은 이름+생년월일의 다른 참가자는 임시 점수가 아니라 진짜 AI 결과를 받아야 한다.
        ai.failure = null;
        ai.score = 55;
        PlayerEntity second = newPlayer("캐시오염");
        FortuneQueryResult result = service.getTodayFortune(second.getId());

        assertEquals("AI", result.source(), "FALLBACK 행이 캐시로 재사용되면 안 된다");
        assertEquals(55, result.fortuneScore());
    }

    @Test
    void pressingCheckAgainRetriesTheAiAfterAFallback() {
        PlayerEntity p = newPlayer("재시도");
        ai.failure = upstreamDown();
        assertEquals("FALLBACK", service.getTodayFortune(p.getId()).source());

        ai.failure = null;
        ai.score = 70;
        FortuneQueryResult retried = service.getTodayFortune(p.getId());

        assertEquals("AI", retried.source());
        assertEquals(70, retried.fortuneScore());
    }

    @Test
    void gameCreationReusesTheTemporaryScoreWithoutCallingTheAiAgain() {
        PlayerEntity p = newPlayer("게임재사용");
        ai.failure = upstreamDown();
        FortuneQueryResult shown = service.getTodayFortune(p.getId());
        int callsBefore = ai.calls.get();

        // 게임 생성 시점에 AI가 복구돼 있어도, 카드로 보여준 점수를 그대로 쓰고 AI를 다시 부르지 않는다.
        ai.failure = null;
        ai.score = 10;
        FortuneQueryResult forGame = service.getFortuneForGame(p.getId());

        assertEquals("FALLBACK", forGame.source());
        assertEquals(shown.fortuneScore(), forGame.fortuneScore());
        assertEquals(callsBefore, ai.calls.get(), "참가자마다 다시 호출하면 AI가 죽은 상태에서 게임 시작이 몇 분씩 걸린다");
    }

    @Test
    void fallbackCanBeTurnedOffAndThenTheFailureSurfaces() {
        FortuneQueryService noFallback = new FortuneQueryService(playerService, ai, repository, errorLogStore, false);
        PlayerEntity p = newPlayer("폴백끔");
        ApiException failure = upstreamDown();
        ai.failure = failure;

        ApiException thrown = assertThrows(ApiException.class, () -> noFallback.getTodayFortune(p.getId()));

        assertSame(failure, thrown);
        assertEquals(HttpStatus.BAD_GATEWAY, thrown.getStatus());
    }

    @Test
    void ourOwnBugsAndClientErrorsAreNotMaskedByTheFallback() {
        PlayerEntity bug = newPlayer("버그");
        ai.failure = new IllegalStateException("우리 코드의 버그");
        assertThrows(IllegalStateException.class, () -> service.getTodayFortune(bug.getId()));

        PlayerEntity bad = newPlayer("잘못된요청");
        ai.failure = ApiException.badRequest("4xx는 외부 장애가 아니다");
        assertThrows(ApiException.class, () -> service.getTodayFortune(bad.getId()));

        assertFalse(repository.findByPlayerIdOrderByCreatedDateAsc(bug.getId()).stream()
                .anyMatch(r -> "FALLBACK".equals(r.getSource())), "버그를 임시 점수로 덮으면 안 된다");
    }

    /** 같은 신원(이름+생년월일)으로 등록된 참가자 n명이 동시에 운세를 확인한다. */
    private List<FortuneQueryResult> checkAllAtOnce(List<PlayerEntity> players) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(players.size());
        try {
            CountDownLatch ready = new CountDownLatch(players.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<FortuneQueryResult>> futures = new ArrayList<>();
            for (PlayerEntity player : players) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();   // 모두 준비된 뒤 한꺼번에 출발
                    return service.getTodayFortune(player.getId());
                }));
            }
            ready.await();
            go.countDown();
            List<FortuneQueryResult> results = new ArrayList<>();
            for (Future<FortuneQueryResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void simultaneousRequestsForTheSameIdentityCallTheAiOnlyOnce() throws Exception {
        // AI가 느린 동안 같은 신원의 요청이 겹치면, 모두 "캐시 없음"을 보고 각자 AI를 부르는 문제(비용이 N배)를 재현한다.
        ai.delayMillis = 400;
        List<PlayerEntity> players = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            players.add(newPlayer("동시신원"));
        }

        List<FortuneQueryResult> results = checkAllAtOnce(players);

        assertEquals(1, ai.calls.get(), "같은 이름+생년월일이면 AI는 한 번만 호출되어야 한다");
        assertEquals(1, results.stream().filter(r -> "AI".equals(r.source())).count(), "한 명만 AI 결과를 받고");
        assertEquals(5, results.stream().filter(r -> "CACHE".equals(r.source())).count(), "나머지는 그 결과를 재사용한다");
        assertEquals(1, results.stream().map(FortuneQueryResult::fortuneScore).distinct().count(), "점수도 모두 같아야 한다");
    }

    @Test
    void differentIdentitiesAreNotSerializedByTheLock() throws Exception {
        // 잠금이 너무 넓으면(전체 잠금) 서로 다른 사람의 요청까지 줄을 선다. 신원별로만 잠겨야 한다.
        ai.delayMillis = 500;
        List<PlayerEntity> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(newPlayer("병렬신원" + i));
        }

        long startedAt = System.currentTimeMillis();
        checkAllAtOnce(players);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertEquals(4, ai.calls.get(), "서로 다른 신원은 각자 AI를 호출한다");
        assertTrue(elapsed < 1500, "4명이 줄을 서면 약 2000ms다. 병렬이면 500ms 근처여야 한다: " + elapsed + "ms");
    }
}
