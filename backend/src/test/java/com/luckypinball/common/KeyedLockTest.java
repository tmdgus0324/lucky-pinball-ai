package com.luckypinball.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class KeyedLockTest {

    private final KeyedLock<String> locks = new KeyedLock<>();

    /** keys의 각 키로 동시에 작업을 돌리고, 같은 순간 몇 개의 작업이 안에 있었는지(최대 동시 실행 수)를 잰다. */
    private int maxConcurrency(List<String> keys) throws Exception {
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(keys.size());
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (String key : keys) {
                futures.add(pool.submit(() -> {
                    go.await();
                    return locks.withLock(key, () -> {
                        max.accumulateAndGet(inside.incrementAndGet(), Math::max);
                        sleep(100);
                        inside.decrementAndGet();
                        return null;
                    });
                }));
            }
            go.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        return max.get();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void sameKeyRunsOneAtATime() throws Exception {
        assertEquals(1, maxConcurrency(List.of("a", "a", "a", "a", "a")));
    }

    @Test
    void differentKeysRunInParallel() throws Exception {
        assertTrue(maxConcurrency(List.of("a", "b", "c", "d")) > 1, "다른 키끼리 줄을 서면 안 된다");
    }

    @Test
    void entriesAreRemovedWhenNobodyUsesThemSoMemoryDoesNotGrow() throws Exception {
        maxConcurrency(List.of("a", "a", "b", "c", "c", "d"));

        assertEquals(0, locks.activeKeys(), "다 끝나면 맵에 아무것도 남지 않아야 한다");
    }

    @Test
    void aFailingActionStillReleasesTheLock() {
        assertThrows(IllegalStateException.class, () -> locks.withLock("a", () -> {
            throw new IllegalStateException("AI 호출 실패 같은 예외");
        }));

        // 잠금이 풀리지 않았다면 여기서 영원히 기다린다.
        assertEquals("ok", locks.withLock("a", () -> "ok"));
        assertEquals(0, locks.activeKeys());
    }

    @Test
    void theSameThreadCanReenterWithoutDeadlock() {
        // ReentrantLock이라 같은 스레드가 같은 키를 다시 잡아도 멈추지 않는다.
        String result = locks.withLock("a", () -> locks.withLock("a", () -> "inner"));

        assertEquals("inner", result);
        assertEquals(0, locks.activeKeys());
    }
}
