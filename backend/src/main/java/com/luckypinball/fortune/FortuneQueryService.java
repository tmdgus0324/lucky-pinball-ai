package com.luckypinball.fortune;

import com.luckypinball.common.ApiException;
import com.luckypinball.common.ErrorLogStore;
import com.luckypinball.common.KeyedLock;
import com.luckypinball.common.TraceId;
import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * "오늘의 운세 + 버프"를 만드는 공통 로직.
 * {@link com.luckypinball.fortune.FortuneController}와
 * {@link com.luckypinball.game.GameService}(게임 생성 시 참가자별 운세/버프 조회)가 함께 사용한다.
 *
 * 생년월일 입력 여부에 따라 동작이 갈린다:
 * - 생년월일이 없으면: AI를 호출하지 않고 버프 없이 즉시 참여(비용 0).
 * - 생년월일이 있으면: 이름+생년월일이 과거에 조회된 적 있는지(=같은 사주) 먼저 DB에서 확인해서,
 *   있으면 그 결과를 재사용(무료)하고, 없으면 그때만 Claude를 호출(1회성 비용)한다.
 * - Claude 호출이 실패하면(타임아웃, 장애, 크레딧 소진 등) 임시 점수(FALLBACK)로 대신 진행한다.
 *   임시 점수는 정체성 캐시로 재사용하지 않는다 — 재사용하면 그 신원이 계속 가짜 점수를 쓰게 된다.
 * - 같은 신원의 요청이 동시에 들어오면 신원별 잠금으로 줄을 세워, AI는 첫 요청만 호출한다.
 */
@Service
public class FortuneQueryService {

    private static final Logger log = LoggerFactory.getLogger(FortuneQueryService.class);

    private final PlayerService playerService;
    private final FortuneService fortuneService;
    private final FortuneResultJpaRepository fortuneResultJpaRepository;
    private final ErrorLogStore errorLogStore;
    private final boolean fallbackEnabled;

    /** 같은 신원(이름+생년월일)의 "캐시 확인 → AI 호출 → 저장"을 한 번에 하나씩만 실행하기 위한 잠금. */
    private final KeyedLock<Identity> identityLocks = new KeyedLock<>();

    private record Identity(String name, LocalDate birthDate) {
    }

    /** 이름+생년월일+날짜로 점수를 정하는 규칙 기반 생성기 — 같은 사람은 하루 동안 항상 같은 임시 점수를 받는다. */
    private final FortuneService fallbackGenerator = new MockFortuneGenerator();

    public FortuneQueryService(PlayerService playerService,
                                FortuneService fortuneService,
                                FortuneResultJpaRepository fortuneResultJpaRepository,
                                ErrorLogStore errorLogStore,
                                @Value("${fortune.fallback-enabled:true}") boolean fallbackEnabled) {
        this.playerService = playerService;
        this.fortuneService = fortuneService;
        this.fortuneResultJpaRepository = fortuneResultJpaRepository;
        this.errorLogStore = errorLogStore;
        this.fallbackEnabled = fallbackEnabled;
    }

    private static final List<String> CACHEABLE_SOURCES =
            List.of(FortuneResultEntity.SOURCE_AI, FortuneResultEntity.SOURCE_CACHE);

    static final String FALLBACK_NOTICE = "AI 연결이 원활하지 않아 임시 점수로 진행합니다.";

    /** 사용자가 "운세 확인"을 눌렀을 때. 이전 임시 점수가 있어도 AI를 다시 시도한다(재시도할 수 있어야 한다). */
    public FortuneQueryResult getTodayFortune(Long playerId) {
        return resolve(playerId, false);
    }

    /**
     * 게임 생성 때 참가자별 운세를 다시 조회하는 경로. 이 참가자가 이미 임시 점수를 받았다면 그대로 쓴다 —
     * AI가 느리거나 죽은 상태에서 참가자마다 다시 호출하면 8명 × 최대 20초를 기다리게 되고, 방금 카드로
     * 보여준 점수와 게임에 쓰이는 점수가 달라질 수도 있다.
     */
    public FortuneQueryResult getFortuneForGame(Long playerId) {
        return resolve(playerId, true);
    }

    private FortuneQueryResult resolve(Long playerId, boolean reuseOwnFallback) {
        PlayerEntity player = playerService.getById(playerId);

        if (player.getBirthDate() == null) {
            log.debug("생년월일 미입력 — 운세 없이 참여: playerId={}", playerId);
            return new FortuneQueryResult(
                    playerId, player.getName(), null, null, null,
                    new Buff(0, 0), "NONE");
        }

        // 같은 신원의 요청이 동시에 들어오면 모두 "캐시 없음"을 보고 각자 AI를 부른다 — 동시 6건이면 AI 6번 호출(테스트로 재현).
        // 신원별로 잠가서 첫 요청만 AI를 부르고, 기다리던 요청은 잠금을 얻은 뒤 캐시를 다시 보고 그 결과를 재사용한다.
        // (이 메서드는 트랜잭션 밖이라 저장이 즉시 커밋되므로, 다음 요청은 잠금을 얻는 순간 방금 저장된 결과를 본다.)
        return identityLocks.withLock(new Identity(player.getName(), player.getBirthDate()),
                () -> resolveForIdentity(player, playerId, reuseOwnFallback));
    }

    /** 신원 잠금 안에서 실행된다. 캐시 확인과 AI 호출·저장이 한 덩어리로 묶여야 중복 호출이 생기지 않는다. */
    private FortuneQueryResult resolveForIdentity(PlayerEntity player, Long playerId, boolean reuseOwnFallback) {
        // FALLBACK(임시 점수)은 일부러 캐시 조회 대상에서 뺀다 — AI가 복구되면 같은 신원이 진짜 결과를 받아야 한다.
        Optional<FortuneResultEntity> cached = fortuneResultJpaRepository
                .findFirstByNameAndBirthDateAndSourceInOrderByCreatedDateAsc(
                        player.getName(), player.getBirthDate(), CACHEABLE_SOURCES);

        int fortuneScore;
        int luckyNumber;
        String fortuneMessage;
        String source;

        if (cached.isPresent()) {
            FortuneResultEntity entity = cached.get();
            fortuneScore = entity.getFortuneScore();
            luckyNumber = entity.getLuckyNumber();
            fortuneMessage = entity.getFortuneMessage();
            source = FortuneResultEntity.SOURCE_CACHE;
            log.info("운세 캐시 적중 — AI 호출 생략: playerId={}", playerId);
        } else {
            Optional<FortuneResultEntity> ownFallback = reuseOwnFallback
                    ? fortuneResultJpaRepository.findFirstByPlayerIdAndSourceOrderByCreatedDateDesc(
                            playerId, FortuneResultEntity.SOURCE_FALLBACK)
                    : Optional.empty();

            if (ownFallback.isPresent()) {
                FortuneResultEntity entity = ownFallback.get();
                fortuneScore = entity.getFortuneScore();
                luckyNumber = entity.getLuckyNumber();
                fortuneMessage = entity.getFortuneMessage();
                source = FortuneResultEntity.SOURCE_FALLBACK;
                log.info("이미 받은 임시 점수 재사용 — AI 호출 생략: playerId={}", playerId);
            } else {
                log.info("운세 캐시 미스 — AI 호출: playerId={}", playerId);
                FortuneResult raw;
                try {
                    raw = fortuneService.analyze(player.getName(), player.getBirthDate());
                    source = FortuneResultEntity.SOURCE_AI;
                } catch (ApiException e) {
                    // 외부(Claude) 쪽 실패만 폴백 대상이다. 우리 코드의 버그(그 외 예외)는 숨기지 않고 그대로 터뜨린다.
                    if (!fallbackEnabled || !e.getStatus().is5xxServerError()) {
                        throw e;
                    }
                    String cause = summarize(e);
                    log.warn("AI 호출 실패 — 임시 점수로 대체: playerId={} 원인={}", playerId, cause);
                    // 사용자 응답은 성공(200)으로 나가므로 예외 핸들러가 기록하지 않는다 — 관리자 오류 로그에 직접 남긴다.
                    errorLogStore.record("AI 호출", "임시 점수로 대체: " + cause, TraceId.current());
                    raw = fallbackGenerator.analyze(player.getName(), player.getBirthDate());
                    source = FortuneResultEntity.SOURCE_FALLBACK;
                }
                fortuneScore = raw.fortuneScore();
                luckyNumber = raw.luckyNumber();
                // 규칙 기반 문구를 운세처럼 보여주지 않는다 — 임시 점수라는 사실을 그대로 알린다.
                fortuneMessage = FortuneResultEntity.SOURCE_FALLBACK.equals(source) ? FALLBACK_NOTICE : raw.fortuneMessage();
            }
        }

        // 캐시로 재사용한 경우에도, 이 참가자(playerId) 기준 이력은 남겨둔다 —
        // 관리자 화면에서 "이 사람이 언제 조회했는지"를 볼 수 있어야 하기 때문.
        fortuneResultJpaRepository.save(new FortuneResultEntity(
                playerId, player.getName(), player.getBirthDate(),
                fortuneScore, fortuneMessage, luckyNumber, LocalDate.now(), source));

        Buff buff = BuffCalculator.fromScore(fortuneScore);
        return new FortuneQueryResult(playerId, player.getName(), fortuneScore, luckyNumber, fortuneMessage, buff, source);
    }

    /** 관리자 오류 로그·WARN 로그용 한 줄 요약: 가장 깊은 원인의 종류와 메시지(길면 자른다). */
    static String summarize(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = root.getClass().getSimpleName() + ": " + root.getMessage();
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    public record FortuneQueryResult(
            Long playerId,
            String name,
            Integer fortuneScore,
            Integer luckyNumber,
            String fortuneMessage,
            Buff buff,
            String source
    ) {
    }
}
