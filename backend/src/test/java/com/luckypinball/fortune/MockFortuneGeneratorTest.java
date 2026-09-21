package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MockFortuneGeneratorTest {

    private final MockFortuneGenerator generator = new MockFortuneGenerator();

    @Test
    void scoreAndLuckyNumberStayWithinValidRange() {
        FortuneResult result = generator.analyze("홍길동", LocalDate.of(1995, 4, 12));

        assertTrue(result.fortuneScore() >= 0 && result.fortuneScore() <= 100);
        assertTrue(result.luckyNumber() >= 1 && result.luckyNumber() <= 99);
    }

    @Test
    void samePersonSameDayIsDeterministic() {
        FortuneResult first = generator.analyze("홍길동", LocalDate.of(1995, 4, 12));
        FortuneResult second = generator.analyze("홍길동", LocalDate.of(1995, 4, 12));

        assertEquals(first, second);
    }

    @Test
    void differentPeopleTendToGetDifferentResults() {
        FortuneResult a = generator.analyze("홍길동", LocalDate.of(1995, 4, 12));
        FortuneResult b = generator.analyze("김유나", LocalDate.of(2000, 11, 3));

        assertTrue(!a.equals(b), "다른 사람은 보통 다른 결과를 받아야 한다 (드물게 우연히 같을 수는 있음)");
    }
}
