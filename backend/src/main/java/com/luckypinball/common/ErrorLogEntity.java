package com.luckypinball.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

/**
 * 관리자 오류 로그 한 건. 서버가 재시작·재배포돼도 남도록 DB에 저장한다.
 * 컬럼 길이는 넘치면 INSERT 자체가 실패하므로(= 오류를 기록하다 또 오류) 저장 전에 {@link ErrorLogStore}가 잘라서 맞춘다.
 */
@Entity
public class ErrorLogEntity {

    static final int PATH_LENGTH = 500;
    static final int MESSAGE_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "timestamp"는 DB마다 예약어/타입 이름이라 컬럼명으로 피한다.
    private Instant occurredAt;

    @Column(length = PATH_LENGTH)
    private String path;

    @Column(length = MESSAGE_LENGTH)
    private String message;

    @Column(length = 16)
    private String traceId;

    protected ErrorLogEntity() {
        // JPA
    }

    public ErrorLogEntity(Instant occurredAt, String path, String message, String traceId) {
        this.occurredAt = occurredAt;
        this.path = path;
        this.message = message;
        this.traceId = traceId;
    }

    public Long getId() {
        return id;
    }

    ErrorLogEntry toEntry() {
        return new ErrorLogEntry(occurredAt, path, message, traceId);
    }
}
