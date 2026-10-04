package com.luckypinball.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 키별 잠금. 같은 키의 작업은 한 번에 하나씩만 실행되고, 다른 키의 작업은 서로 기다리지 않는다.
 *
 * 전체를 하나의 잠금(synchronized)으로 묶으면 서로 다른 사람의 요청까지 줄을 서게 되고(AI 응답이 수 초라
 * 치명적이다), 키마다 잠금을 만들어 계속 쌓아 두면 메모리가 끝없이 늘어난다. 그래서 잠금을 쓰는 동안에만
 * 맵에 두고, 마지막 사용자가 끝나면 지운다.
 *
 * 한계: 서버 한 대(JVM 하나) 안에서만 유효하다. 서버를 여러 대로 늘리면 DB 잠금이나 분산 잠금이 필요하다.
 */
public class KeyedLock<K> {

    private static final class Entry {
        final ReentrantLock lock = new ReentrantLock();
        /** 이 잠금을 쓰고 있거나 기다리는 스레드 수. compute 안에서만 바꾸므로 키 단위로 원자적이다. */
        int users;
    }

    private final ConcurrentHashMap<K, Entry> entries = new ConcurrentHashMap<>();

    public <T> T withLock(K key, Supplier<T> action) {
        Entry entry = entries.compute(key, (k, existing) -> {
            Entry e = existing != null ? existing : new Entry();
            e.users++;
            return e;
        });
        entry.lock.lock();
        try {
            return action.get();
        } finally {
            entry.lock.unlock();
            // 마지막 사용자였다면 맵에서 지운다(null을 돌려주면 ConcurrentHashMap이 항목을 삭제한다).
            entries.computeIfPresent(key, (k, e) -> --e.users == 0 ? null : e);
        }
    }

    /** 지금 잠금을 쓰거나 기다리는 키의 수 — 다 끝난 뒤 0이 되는지(누수가 없는지) 테스트에서 확인한다. */
    int activeKeys() {
        return entries.size();
    }
}
