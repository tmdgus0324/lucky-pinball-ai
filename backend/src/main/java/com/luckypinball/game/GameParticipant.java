package com.luckypinball.game;

import com.luckypinball.fortune.BuffCalculator.Buff;

/**
 * 게임 세션에 참여한 참가자 1명 — 운세/버프가 이미 확정된 상태.
 * fortuneScore/luckyNumber/fortuneMessage는 생년월일을 입력하지 않은 참가자의 경우 null이다
 * (버프 없이 참여, {@link com.luckypinball.fortune.FortuneQueryService} 참고).
 */
public record GameParticipant(
        Long playerId,
        String name,
        Integer fortuneScore,
        Integer luckyNumber,
        String fortuneMessage,
        Buff buff
) {
}
