package com.luckypinball.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 오류 로그가 실제 DB(메모리 H2)에 저장·조회·정리되는지 확인한다. */
@SpringBootTest
class ErrorLogStoreTest {

    @Autowired ErrorLogJpaRepository repository;
    @Autowired ErrorLogStore store;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void savedErrorsComeBackNewestFirstWithAllFields() {
        store.record("/api/a", "첫 번째", "aaaa1111");
        store.record("/api/b", "두 번째", "bbbb2222");

        List<ErrorLogEntry> recent = store.recent();

        assertEquals(2, recent.size());
        assertEquals("두 번째", recent.get(0).message());
        assertEquals("/api/b", recent.get(0).path());
        assertEquals("bbbb2222", recent.get(0).traceId());
        assertEquals("첫 번째", recent.get(1).message());
        assertTrue(recent.get(0).timestamp() != null);
    }

    @Test
    void entriesSurviveARealDatabaseRoundTripNotJustMemory() {
        store.record("/api/a", "남아야 함", "cccc3333");

        // 서비스 객체를 거치지 않고 DB에서 직접 읽어도 있어야 한다 — 인메모리 리스트가 아니라는 증거.
        assertEquals(1, repository.count());
        assertEquals("남아야 함", repository.findAll().get(0).toEntry().message());
    }

    @Test
    void oldEntriesBeyondTheLimitArePrunedSoTheTableDoesNotGrowForever() {
        ErrorLogStore small = new ErrorLogStore(repository, 3);

        for (int i = 1; i <= 7; i++) {
            small.record("/api/x", "오류 " + i, "t" + i);
        }

        List<ErrorLogEntry> recent = small.recent();
        assertEquals(3, recent.size(), "최근 3건만 남아야 한다");
        assertEquals(List.of("오류 7", "오류 6", "오류 5"), recent.stream().map(ErrorLogEntry::message).toList());
        assertEquals(3, repository.count());
    }

    @Test
    void overlongTextIsTruncatedInsteadOfFailingTheInsert() {
        String huge = "가".repeat(5000);

        assertDoesNotThrow(() -> store.record("/" + "p".repeat(2000), huge, "dddd4444"));

        ErrorLogEntry entry = store.recent().get(0);
        assertEquals(1000, entry.message().length());
        assertTrue(entry.message().endsWith("…"));
        assertEquals(500, entry.path().length());
    }

    @Test
    void aFailingDatabaseNeverBreaksTheCaller() {
        ErrorLogJpaRepository broken = mock(ErrorLogJpaRepository.class);
        when(broken.save(any())).thenThrow(new IllegalStateException("DB 연결 끊김"));
        ErrorLogStore store = new ErrorLogStore(broken, 1000);

        // 오류를 기록하다 또 터지면, 사용자는 원래 오류 응답 대신 엉뚱한 500을 받게 된다.
        assertDoesNotThrow(() -> store.record("/api/a", "원래 오류", "eeee5555"));
    }

    @Test
    void nullTextIsStoredAsIs() {
        // ex.getMessage()는 null일 수 있다.
        assertDoesNotThrow(() -> store.record("/api/a", null, "ffff6666"));
        assertEquals(1, store.recent().size());
    }
}
