package com.luckypinball.common;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 화면(/api/admin/logs)에 보여줄 오류 로그. DB에 저장하므로 서버가 재시작·재배포돼도 남는다.
 * (서버 로그 파일/Render 로그와는 별개 — 이쪽은 "관리자가 화면에서 빠르게 훑어보는 최근 오류 목록"이다.)
 *
 * 두 가지를 지킨다:
 * - 오류를 기록하다가 다시 오류가 나서 원래 응답을 망치지 않는다 — DB 저장이 실패해도 삼키고 WARN만 남긴다.
 * - 무한히 쌓이지 않는다 — 최근 N건(기본 1000)만 남기고 오래된 것은 저장할 때마다 함께 지운다.
 */
@Component
public class ErrorLogStore {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogStore.class);

    /** 화면에 보여주는 최대 건수(기존 인메모리 구현과 같은 200건). */
    static final int RECENT_LIMIT = 200;

    private final ErrorLogJpaRepository repository;
    private final int maxEntries;

    public ErrorLogStore(ErrorLogJpaRepository repository,
                         @Value("${error-log.max-entries:1000}") int maxEntries) {
        this.repository = repository;
        this.maxEntries = maxEntries;
    }

    /**
     * REQUIRES_NEW: 호출한 쪽 트랜잭션이 롤백돼도 오류 기록은 남아야 하고, 반대로 기록 실패가 호출한 쪽
     * 트랜잭션을 rollback-only로 만들어서도 안 되므로 독립된 트랜잭션에서 저장한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String path, String message, String traceId) {
        try {
            ErrorLogEntity saved = repository.save(new ErrorLogEntity(
                    Instant.now(), truncate(path, ErrorLogEntity.PATH_LENGTH),
                    truncate(message, ErrorLogEntity.MESSAGE_LENGTH), traceId));
            repository.deleteOlderThanOrEqual(saved.getId() - maxEntries);
        } catch (RuntimeException e) {
            // 오류 로그 저장 실패가 사용자 응답 실패로 번지면 안 된다. 서버 로그에는 남긴다.
            log.warn("오류 로그 저장 실패 (원래 오류: {} {})", path, message, e);
        }
    }

    @Transactional(readOnly = true)
    public List<ErrorLogEntry> recent() {
        return repository.findAllByOrderByIdDesc(PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(ErrorLogEntity::toEntry)
                .toList();
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "…";
    }
}
