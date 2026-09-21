package com.luckypinball.fortune;

import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.player.PlayerEntity;
import com.luckypinball.player.PlayerService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * "오늘의 운세 + 버프"를 만드는 공통 로직.
 * {@link com.luckypinball.fortune.FortuneController}와
 * {@link com.luckypinball.game.GameService}(게임 생성 시 참가자별 운세/버프 조회)가 함께 사용한다.
 */
@Service
public class FortuneQueryService {

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

        FortuneResult raw = fortuneService.analyze(player.getName(), player.getBirthDate());
        Buff buff = BuffCalculator.fromScore(raw.fortuneScore());

        fortuneResultJpaRepository.save(new FortuneResultEntity(
                playerId, raw.fortuneScore(), raw.fortuneMessage(), raw.luckyNumber(), LocalDate.now()));

        return new FortuneQueryResult(
                playerId, player.getName(), raw.fortuneScore(), raw.luckyNumber(), raw.fortuneMessage(), buff);
    }

    public record FortuneQueryResult(
            Long playerId,
            String name,
            int fortuneScore,
            int luckyNumber,
            String fortuneMessage,
            Buff buff
    ) {
    }
}
