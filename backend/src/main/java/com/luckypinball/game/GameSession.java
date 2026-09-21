package com.luckypinball.game;

import java.time.Instant;
import java.util.List;

/**
 * 게임 1판의 상태. 이번 MVP에서는 DB가 아니라 인메모리(GameRepository)로만 유지한다.
 * 물리 시뮬레이션은 브라우저(Matter.js)에서 돌아가므로, ranking/selectedName은
 * 클라이언트가 POST /api/game/result로 보고해줄 때 비로소 채워진다.
 */
public class GameSession {

    private final String gameId;
    private final List<GameParticipant> participants;
    private final Instant createdAt;

    private GameStatus status = GameStatus.CREATED;
    private Instant startedAt;
    private Instant finishedAt;
    private List<RankEntry> ranking;
    private String selectedName;

    public GameSession(String gameId, List<GameParticipant> participants) {
        this.gameId = gameId;
        this.participants = List.copyOf(participants);
        this.createdAt = Instant.now();
    }

    public void start() {
        this.status = GameStatus.STARTED;
        this.startedAt = Instant.now();
    }

    public void reportResult(List<RankEntry> ranking, String selectedName) {
        this.ranking = List.copyOf(ranking);
        this.selectedName = selectedName;
        this.status = GameStatus.FINISHED;
        this.finishedAt = Instant.now();
    }

    public boolean hasResult() {
        return ranking != null;
    }

    public String getGameId() {
        return gameId;
    }

    public List<GameParticipant> getParticipants() {
        return participants;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public List<RankEntry> getRanking() {
        return ranking;
    }

    public String getSelectedName() {
        return selectedName;
    }
}
