package com.luckypinball.fortune;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

/**
 * 참가자별 운세 조회 이력. 점수 계산에는 더 이상 쓰이지 않고,
 * 관리자 화면(GET /api/admin/players)에서 과거 기록을 보여주는 용도로만 저장한다.
 */
@Entity
public class FortuneResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long playerId;

    private int fortuneScore;

    private String fortuneMessage;

    private int luckyNumber;

    private LocalDate createdDate;

    protected FortuneResultEntity() {
        // JPA
    }

    public FortuneResultEntity(Long playerId, int fortuneScore, String fortuneMessage, int luckyNumber, LocalDate createdDate) {
        this.playerId = playerId;
        this.fortuneScore = fortuneScore;
        this.fortuneMessage = fortuneMessage;
        this.luckyNumber = luckyNumber;
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public int getFortuneScore() {
        return fortuneScore;
    }

    public String getFortuneMessage() {
        return fortuneMessage;
    }

    public int getLuckyNumber() {
        return luckyNumber;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }
}
