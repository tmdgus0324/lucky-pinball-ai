package com.luckypinball.game;

import com.luckypinball.fortune.BuffCalculator.Buff;

/**
 * 게임 세션에 참여한 참가자 1명 — 운세/버프가 이미 확정된 상태.
 */
public record GameParticipant(
        Long playerId,
        String name,
        int fortuneScore,
        int luckyNumber,
        String fortuneMessage,
        Buff buff
) {
}
