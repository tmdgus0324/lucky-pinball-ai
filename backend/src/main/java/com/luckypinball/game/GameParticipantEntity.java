package com.luckypinball.game;

import com.luckypinball.player.PlayerEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * 게임 1판에 참가한 참가자 1명의 스냅샷. 게임 생성 시점에 확정된 운세/버프 값을 그대로
 * 고정해서 저장한다 — 이후 그 사람의 운세 이력이 바뀌어도 이미 치른 게임의 기록은
 * 바뀌면 안 되기 때문에(FortuneResultEntity가 name/birthDate를 비정규화해둔 것과 같은 이유).
 */
@Entity
public class GameParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    private String name;
    private Integer fortuneScore;
    private Integer luckyNumber;
    private String fortuneMessage;
    private int buffTier;
    private int buffStartY;

    protected GameParticipantEntity() {
        // JPA
    }

    GameParticipantEntity(GameEntity game, PlayerEntity player, String name, Integer fortuneScore,
                           Integer luckyNumber, String fortuneMessage, int buffTier, int buffStartY) {
        this.game = game;
        this.player = player;
        this.name = name;
        this.fortuneScore = fortuneScore;
        this.luckyNumber = luckyNumber;
        this.fortuneMessage = fortuneMessage;
        this.buffTier = buffTier;
        this.buffStartY = buffStartY;
    }

    public Long getId() {
        return id;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public Integer getFortuneScore() {
        return fortuneScore;
    }

    public Integer getLuckyNumber() {
        return luckyNumber;
    }

    public String getFortuneMessage() {
        return fortuneMessage;
    }

    public int getBuffTier() {
        return buffTier;
    }

    public int getBuffStartY() {
        return buffStartY;
    }
}
