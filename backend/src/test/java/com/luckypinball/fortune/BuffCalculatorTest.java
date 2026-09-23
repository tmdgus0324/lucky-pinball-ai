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
}
