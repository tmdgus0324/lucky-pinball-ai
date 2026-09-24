package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.luckypinball.fortune.BuffCalculator.Buff;
import org.junit.jupiter.api.Test;

class BuffCalculatorTest {

    @Test
    void tierBoundariesStillMatchScoreBands() {
        assertEquals(4, BuffCalculator.fromScore(0).tier());
        assertEquals(4, BuffCalculator.fromScore(19).tier());
        assertEquals(3, BuffCalculator.fromScore(20).tier());
        assertEquals(3, BuffCalculator.fromScore(39).tier());
        assertEquals(2, BuffCalculator.fromScore(40).tier());
        assertEquals(2, BuffCalculator.fromScore(59).tier());
        assertEquals(1, BuffCalculator.fromScore(60).tier());
        assertEquals(1, BuffCalculator.fromScore(79).tier());
        assertEquals(0, BuffCalculator.fromScore(80).tier());
        assertEquals(0, BuffCalculator.fromScore(100).tier());
    }

    @Test
    void startYIsLinearInScoreSoEveryPointMakesAVisibleDifference() {
        assertEquals(new Buff(4, 200), BuffCalculator.fromScore(0));
        assertEquals(new Buff(1, 60), BuffCalculator.fromScore(70));
        assertEquals(new Buff(1, 58), BuffCalculator.fromScore(71));
        assertEquals(new Buff(0, 0), BuffCalculator.fromScore(100));
    }

    @Test
    void relativeStartYReflectsGapFromTheGamesTopScorer() {
        // 최고점자 본인은 격차 0 -> 맨 위(결승선에서 가장 먼 곳)에서 출발
        assertEquals(0, BuffCalculator.relativeStartY(76, 76));
        // 4점 차이 -> 공 4개(4*26=104px)만큼 아래에서 출발
        assertEquals(104, BuffCalculator.relativeStartY(72, 76));
        assertEquals(52, BuffCalculator.relativeStartY(74, 76));
    }

    @Test
    void relativeStartYCapsAtTenPointsEvenIfTheActualGapIsBigger() {
        // 격차가 10점을 넘어도(여기선 40점) 10점(=공 10개, 260px)에서 잘린다
        assertEquals(260, BuffCalculator.relativeStartY(20, 60));
        assertEquals(260, BuffCalculator.relativeStartY(0, 100));
    }
}
