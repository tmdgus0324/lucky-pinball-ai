package com.luckypinball.game;

import java.time.Instant;
import java.util.List;

/**
 * 게임 1판의 상태(도메인 객체) — GameRepository 구현체(JpaGameRepository)가 실제 저장을
 * 담당하고, 이 클래스 자체는 영속화 방식을 모른다. 물리 시뮬레이션은 브라우저(Matter.js)에서
 * 돌아가므로, ranking/selectedName은 클라이언트가 POST /api/game/result로 보고해줄 때
 * 비로소 채워진다.
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
        this(gameId, participants, Instant.now());
    }

    private GameSession(String gameId, List<GameParticipant> participants, Instant createdAt) {
        this.gameId = gameId;
        this.participants = List.copyOf(participants);
        this.createdAt = createdAt;
    }

    /**
     * 저장소에 이미 영속화된 상태를 그대로 복원할 때 쓴다(JpaGameRepository 전용) — 일반
     * 생성자와 달리 status/타임스탬프/순위까지 전부 그대로 주입하고, start()/reportResult()의
     * "지금 시각을 찍는다"는 부수효과 없이 과거 상태를 그대로 재구성한다.
     */
    static GameSession restore(String gameId, List<GameParticipant> participants, Instant createdAt,
                                GameStatus status, Instant startedAt, Instant finishedAt,
                                List<RankEntry> ranking, String selectedName) {
        GameSession session = new GameSession(gameId, participants, createdAt);
        session.status = status;
        session.startedAt = startedAt;
        session.finishedAt = finishedAt;
        session.ranking = ranking;
        session.selectedName = selectedName;
        return session;
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
