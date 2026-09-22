package com.luckypinball.fortune;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

/**
 * 참가자별 운세 조회 이력. 관리자 화면(GET /api/admin/players)에서 과거 기록을 보여주는
 * 용도이자, "동일한 이름+생년월일(=같은 사주)"이면 Claude를 다시 호출하지 않고 이 테이블의
 * 값을 재사용하기 위한 캐시 조회 대상이기도 하다 (FortuneQueryService 참고).
 * name/birthDate를 player_id와 별도로 이 테이블에 함께 저장(비정규화)해둔 이유는,
 * 서로 다른 player_id(=다른 등록 건)라도 이름+생년월일이 같으면 같은 사람으로 보고
 * 캐시를 공유해야 하기 때문 — player_id로만 조회하면 이 재사용을 할 수 없다.
 */
@Entity
public class FortuneResultEntity {

    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_CACHE = "CACHE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long playerId;

    private String name;

    private LocalDate birthDate;

    private int fortuneScore;

    private String fortuneMessage;

    private int luckyNumber;

    private LocalDate createdDate;

    /** "AI"(이번에 실제로 Claude를 호출함) 또는 "CACHE"(과거 기록을 재사용함). */
    private String source;

    protected FortuneResultEntity() {
        // JPA
    }

    public FortuneResultEntity(Long playerId, String name, LocalDate birthDate, int fortuneScore,
                                String fortuneMessage, int luckyNumber, LocalDate createdDate, String source) {
        this.playerId = playerId;
        this.name = name;
        this.birthDate = birthDate;
        this.fortuneScore = fortuneScore;
        this.fortuneMessage = fortuneMessage;
        this.luckyNumber = luckyNumber;
        this.createdDate = createdDate;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
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

    public String getSource() {
        return source;
    }
}
