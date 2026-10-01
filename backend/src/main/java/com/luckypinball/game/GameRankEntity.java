package com.luckypinball.game;

import com.luckypinball.player.PlayerEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 게임이 FINISHED 상태가 되어야만 생기는 순위 결과 1행. GameParticipantEntity와 합치지 않고
 * 분리해둔 이유: 참가자 행은 게임 생성 시점에 이미 다 채워지는데, 순위는 그보다 늦게(결과
 * 보고 시점에) 생기는 정보라서 — 분리하면 "아직 결정 안 된 null 컬럼"이 생기지 않는다.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "player_id"}))
public class GameRankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    private int rank;
    private String name;

    protected GameRankEntity() {
        // JPA
    }

    GameRankEntity(GameEntity game, PlayerEntity player, int rank, String name) {
        this.game = game;
        this.player = player;
        this.rank = rank;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }
}
