package com.luckypinball.game;

import java.time.Instant;
import java.util.List;

public record GameResultView(
        String gameId,
        List<RankEntry> ranking,
        String selectedName,
        int participantCount,
        Instant createdAt
) {
}
