package com.luckypinball.fortune;

import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerService;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Service
public class FortuneQueryService {

    private static final Logger log = LoggerFactory.getLogger(FortuneQueryService.class);

    private final PlayerService playerService;
    private final FortuneService fortuneService;
    private final FortuneResultJpaRepository fortuneResultJpaRepository;

    public FortuneQueryService(PlayerService playerService,
                                FortuneService fortuneService,
                                FortuneResultJpaRepository fortuneResultJpaRepository) {
        this.playerService = playerService;
        this.fortuneService = fortuneService;
        this.fortuneResultJpaRepository = fortuneResultJpaRepository;
    }

    public FortuneQueryResult getTodayFortune(Long playerId) {
        PlayerEntity player = playerService.getById(playerId);

        if (player.getBirthDate() == null) {
            log.debug("생년월일 미입력 — 운세 없이 참여: playerId={}", playerId);
            return new FortuneQueryResult(
                    playerId, player.getName(), null, null, null,
                    new Buff(0, 0), "NONE");
        }

        Optional<FortuneResultEntity> cached = fortuneResultJpaRepository
                .findFirstByNameAndBirthDateOrderByCreatedDateAsc(player.getName(), player.getBirthDate());

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
            log.info("운세 캐시 미스 — AI 호출: playerId={}", playerId);
            FortuneResult raw = fortuneService.analyze(player.getName(), player.getBirthDate());
            fortuneScore = raw.fortuneScore();
            luckyNumber = raw.luckyNumber();
            fortuneMessage = raw.fortuneMessage();
            source = FortuneResultEntity.SOURCE_AI;
        }

        // 캐시로 재사용한 경우에도, 이 참가자(playerId) 기준 이력은 남겨둔다 —
        // 관리자 화면에서 "이 사람이 언제 조회했는지"를 볼 수 있어야 하기 때문.
        fortuneResultJpaRepository.save(new FortuneResultEntity(
                playerId, player.getName(), player.getBirthDate(),
                fortuneScore, fortuneMessage, luckyNumber, LocalDate.now(), source));

        Buff buff = BuffCalculator.fromScore(fortuneScore);
        return new FortuneQueryResult(playerId, player.getName(), fortuneScore, luckyNumber, fortuneMessage, buff, source);
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
