package com.luckypinball.fortune;

import com.luckypinball.fortune.BuffCalculator.Buff;
import com.luckypinball.fortune.FortuneQueryService.FortuneQueryResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FortuneController {

    private final FortuneQueryService fortuneQueryService;

    public FortuneController(FortuneQueryService fortuneQueryService) {
        this.fortuneQueryService = fortuneQueryService;
    }

    @PostMapping("/api/fortune")
    public FortuneResponse getTodayFortune(@Valid @RequestBody FortuneRequest request) {
        FortuneQueryResult result = fortuneQueryService.getTodayFortune(request.playerId());
        return FortuneResponse.from(result);
    }

    public record FortuneRequest(@NotNull Long playerId) {
    }

    public record FortuneResponse(
            Long playerId,
            Integer fortuneScore,
            Integer luckyNumber,
            String fortuneMessage,
            Buff buff,
            String source
    ) {
        static FortuneResponse from(FortuneQueryResult result) {
            return new FortuneResponse(
                    result.playerId(), result.fortuneScore(), result.luckyNumber(), result.fortuneMessage(),
                    result.buff(), result.source());
        }
    }
}
