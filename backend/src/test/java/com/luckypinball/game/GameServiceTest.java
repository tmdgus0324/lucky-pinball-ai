package com.luckypinball.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luckypinball.common.ApiException;
import com.luckypinball.fortune.BuffCalculator;
import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.fortune.FortuneQueryService;
import com.luckypinball.fortune.FortuneQueryService.FortuneQueryResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * GameService의 규칙(인원 제한, 상대평가 시작 높이, 상태 전이, 결과 검증)을 DB·AI 없이 확인한다.
 * 저장소는 메모리 Map, 운세는 정해 둔 점수를 돌려주는 대역으로 바꿔 끼운다.
 */
class GameServiceTest {

    /** 메모리 저장소. save 횟수를 세서 "상태가 바뀔 때 저장했는지"도 본다. */
    static class InMemoryGameRepository implements GameRepository {
        final Map<String, GameSession> store = new HashMap<>();
        int saves;

        @Override
        public GameSession save(GameSession session) {
            saves++;
            store.put(session.getGameId(), session);
            return session;
        }

        @Override
        public Optional<GameSession> findById(String gameId) {
            return Optional.ofNullable(store.get(gameId));
        }

        @Override
        public List<GameSession> findAll() {
            return new ArrayList<>(store.values());
        }
    }

    /** playerId → 점수(null이면 생년월일 미입력)를 돌려주는 운세 대역. 어떤 경로로 불렸는지 기록한다. */
    static class StubFortunes extends FortuneQueryService {
        final Map<Long, Integer> scores = new HashMap<>();
        final List<String> calls = new ArrayList<>();

        StubFortunes() {
            super(null, null, null, null, true);
        }

        @Override
        public FortuneQueryResult getFortuneForGame(Long playerId) {
            calls.add("game:" + playerId);
            Integer score = scores.get(playerId);
            if (score == null) {
                return new FortuneQueryResult(playerId, "P" + playerId, null, null, null, new Buff(0, 0), "NONE");
            }
            return new FortuneQueryResult(playerId, "P" + playerId, score, 7, "msg", BuffCalculator.fromScore(score), "AI");
        }

        @Override
        public FortuneQueryResult getTodayFortune(Long playerId) {
            calls.add("today:" + playerId);
            throw new AssertionError("게임 생성은 getFortuneForGame을 써야 한다(임시 점수 재사용 경로)");
        }
    }

    private final InMemoryGameRepository repository = new InMemoryGameRepository();
    private final StubFortunes fortunes = new StubFortunes();
    private final GameService service = new GameService(repository, fortunes);

    private GameSession newGame(Long... playerIds) {
        for (Long id : playerIds) {
            fortunes.scores.putIfAbsent(id, 50);
        }
        return service.createGame(List.of(playerIds));
    }

    private static void assertStatus(HttpStatus expected, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(expected, e.getStatus(), e.getMessage());
    }

    // ---------- 게임 생성 ----------

    @Test
    void createGameNeedsTwoToEightPlayers() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createGame(null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createGame(List.of(1L)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.createGame(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)));

        assertEquals(2, newGame(1L, 2L).getParticipants().size());
        assertEquals(8, newGame(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L).getParticipants().size());
    }

    @Test
    void createGameUsesTheGamePathSoTemporaryScoresAreReused() {
        newGame(1L, 2L);

        assertEquals(List.of("game:1", "game:2"), fortunes.calls);
    }

    @Test
    void startHeightIsRelativeToTheBestScoreInThisGame() {
        fortunes.scores.put(1L, 90);
        fortunes.scores.put(2L, 85);
        fortunes.scores.put(3L, 60);

        GameSession game = service.createGame(List.of(1L, 2L, 3L));
        Map<Long, GameParticipant> byId = new HashMap<>();
        game.getParticipants().forEach(p -> byId.put(p.playerId(), p));

        assertEquals(0, byId.get(1L).buff().startY(), "최고점자는 맨 위(0)에서 출발한다");
        assertEquals(BuffCalculator.relativeStartY(85, 90), byId.get(2L).buff().startY());
        assertEquals(BuffCalculator.relativeStartY(60, 90), byId.get(3L).buff().startY());
        assertTrue(byId.get(3L).buff().startY() > byId.get(2L).buff().startY(), "점수가 낮을수록 결승선에 가깝게 출발한다");
        assertEquals(BuffCalculator.fromScore(60).tier(), byId.get(3L).buff().tier(), "티어는 절대 점수 기준 그대로");
    }

    @Test
    void playersWithoutBirthDateAreLeftOutOfTheRelativeComparison() {
        fortunes.scores.put(1L, 70);
        fortunes.scores.put(2L, null);   // 생년월일 미입력

        GameSession game = service.createGame(List.of(1L, 2L));
        GameParticipant noBirth = game.getParticipants().stream().filter(p -> p.playerId() == 2L).findFirst().orElseThrow();

        assertEquals(new Buff(0, 0), noBirth.buff());
    }

    @Test
    void createdGameIsSavedAndStartsInCreatedState() {
        GameSession game = newGame(1L, 2L);

        assertEquals(GameStatus.CREATED, game.getStatus());
        assertSame(game, repository.store.get(game.getGameId()));
    }

    // ---------- 시작 ----------

    @Test
    void startMovesTheGameToStartedAndSavesIt() {
        GameSession game = newGame(1L, 2L);
        int savesBefore = repository.saves;

        GameSession started = service.start(game.getGameId());

        assertEquals(GameStatus.STARTED, started.getStatus());
        assertNotNull(started.getStartedAt());
        assertEquals(savesBefore + 1, repository.saves);
    }

    @Test
    void startingTwiceKeepsTheOriginalStartTime() throws InterruptedException {
        GameSession game = newGame(1L, 2L);
        Instant firstStart = service.start(game.getGameId()).getStartedAt();
        Thread.sleep(5);

        GameSession again = service.start(game.getGameId());

        assertEquals(GameStatus.STARTED, again.getStatus());
        assertEquals(firstStart, again.getStartedAt(), "재시도된 시작 요청이 시작 시각을 덮어쓰면 안 된다");
    }

    @Test
    void aFinishedGameCannotBeRestarted() {
        GameSession game = newGame(1L, 2L);
        service.start(game.getGameId());
        service.reportResult(game.getGameId(), List.of(1L, 2L));

        assertStatus(HttpStatus.CONFLICT, () -> service.start(game.getGameId()));
        assertEquals(GameStatus.FINISHED, repository.store.get(game.getGameId()).getStatus(), "상태가 STARTED로 되돌아가면 안 된다");
    }

    @Test
    void unknownGameIsNotFound() {
        assertStatus(HttpStatus.NOT_FOUND, () -> service.start("no-such-game"));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.reportResult("no-such-game", List.of(1L, 2L)));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getResult("no-such-game"));
    }

    // ---------- 결과 ----------

    @Test
    void theLastPlayerToFinishIsTheWinnerAndRanksFollowTheFinishOrder() {
        GameSession game = newGame(1L, 2L, 3L);
        service.start(game.getGameId());

        GameResultView view = service.reportResult(game.getGameId(), List.of(2L, 3L, 1L));

        assertEquals("P1", view.selectedName(), "가장 늦게 도착한 사람이 당첨");
        assertEquals(List.of(new RankEntry(1, 2L, "P2"), new RankEntry(2, 3L, "P3"), new RankEntry(3, 1L, "P1")), view.ranking());
        assertEquals(3, view.participantCount());
        assertEquals(GameStatus.FINISHED, repository.store.get(game.getGameId()).getStatus());
    }

    @Test
    void resultCanBeReportedOnlyOnce() {
        GameSession game = newGame(1L, 2L);
        service.start(game.getGameId());
        service.reportResult(game.getGameId(), List.of(1L, 2L));

        assertStatus(HttpStatus.CONFLICT, () -> service.reportResult(game.getGameId(), List.of(2L, 1L)));
        assertEquals("P2", service.getResult(game.getGameId()).selectedName(), "처음 보고한 결과가 유지된다");
    }

    @Test
    void resultOfAGameThatWasNeverStartedIsRejected() {
        GameSession game = newGame(1L, 2L);

        assertStatus(HttpStatus.CONFLICT, () -> service.reportResult(game.getGameId(), List.of(1L, 2L)));
    }

    @Test
    void finishOrderMustContainEachParticipantExactlyOnce() {
        GameSession game = newGame(1L, 2L, 3L);
        service.start(game.getGameId());
        String id = game.getGameId();

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.reportResult(id, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.reportResult(id, List.of(1L, 2L)));          // 빠진 사람
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.reportResult(id, List.of(1L, 2L, 2L)));      // 중복
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.reportResult(id, List.of(1L, 2L, 99L)));     // 다른 사람
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.reportResult(id, List.of(1L, 2L, 3L, 3L)));  // 길이 초과

        // 잘못된 보고는 상태를 바꾸지 않는다 — 올바른 보고는 여전히 받아진다.
        assertEquals("P3", service.reportResult(id, List.of(1L, 2L, 3L)).selectedName());
    }

    @Test
    void resultIsNotAvailableBeforeItIsReported() {
        GameSession game = newGame(1L, 2L);
        service.start(game.getGameId());

        assertStatus(HttpStatus.NOT_FOUND, () -> service.getResult(game.getGameId()));
    }
}
