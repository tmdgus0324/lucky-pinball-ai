package com.luckypinball.game;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 게임 1판. gameId(UUID)를 그대로 PK로 쓴다 — 이미 API(/api/game/result/{gameId})에
 * 노출된 식별자라서, 별도 숫자 surrogate PK를 두면 같은 역할을 하는 컬럼이 두 개가 된다.
 */
@Entity
public class GameEntity {

    @Id
    private String gameId;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private String selectedName;

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    /**
     * participants/ranks 둘 다 List(bag)로 두면, 한 쿼리에서 두 컬렉션을 동시에 fetch join할 때
     * SQL 레벨에서 Cartesian product(참가자 수 × 순위 수)가 생기는데 List는 그 중복 행을
     * 못 걸러내서(Set과 달리) 참가자가 중복으로 들어가는 조용한 버그가 생긴다
     * (MultipleBagFetchException 자체는 둘 다 bag일 때만 터지는데, 하나만 bag이어도 그
     * bag 쪽은 똑같이 중복된다 — 실제로 겪은 문제). 그래서 둘 다 Set으로 둔다.
     */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<GameParticipantEntity> participants = new LinkedHashSet<>();

    /** 게임이 FINISHED 상태가 되기 전까지는 비어있다 — RankEntry는 결과가 나와야만 생기는 정보라서. */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rank ASC")
    private Set<GameRankEntity> ranks = new LinkedHashSet<>();

    protected GameEntity() {
        // JPA
    }

    GameEntity(String gameId, Instant createdAt) {
        this.gameId = gameId;
        this.status = GameStatus.CREATED;
        this.createdAt = createdAt;
    }

    void start(Instant startedAt) {
        this.status = GameStatus.STARTED;
        this.startedAt = startedAt;
    }

    void finish(String selectedName, Instant finishedAt) {
        this.status = GameStatus.FINISHED;
        this.selectedName = selectedName;
        this.finishedAt = finishedAt;
    }

    void addParticipant(GameParticipantEntity participant) {
        participants.add(participant);
    }

    void addRank(GameRankEntity rank) {
        ranks.add(rank);
    }

    public String getGameId() {
        return gameId;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getSelectedName() {
        return selectedName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Set<GameParticipantEntity> getParticipants() {
        return participants;
    }

    public Set<GameRankEntity> getRanks() {
        return ranks;
    }
}
