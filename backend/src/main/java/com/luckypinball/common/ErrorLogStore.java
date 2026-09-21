package com.luckypinball.common;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * 서버 재시작 전까지만 유지되는 인메모리 오류 로그 (MVP 한정, 영속화는 Phase 2).
 */
@Component
public class ErrorLogStore {

    private static final int MAX_ENTRIES = 200;

    private final CopyOnWriteArrayList<ErrorLogEntry> entries = new CopyOnWriteArrayList<>();

    public void record(String path, String message) {
        entries.add(0, new ErrorLogEntry(Instant.now(), path, message));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    public List<ErrorLogEntry> recent() {
        return List.copyOf(entries);
    }
}
