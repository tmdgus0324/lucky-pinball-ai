package com.luckypinball.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.luckypinball.fortune.BuffCalculator.Buff;
import org.junit.jupiter.api.Test;

class BuffCalculatorTest {

    @Test
    void boundaryScoresMapToExpectedTiers() {
        assertEquals(new Buff(4, 60), BuffCalculator.fromScore(0));
        assertEquals(new Buff(4, 60), BuffCalculator.fromScore(19));
        assertEquals(new Buff(3, 45), BuffCalculator.fromScore(20));
        assertEquals(new Buff(3, 45), BuffCalculator.fromScore(39));
        assertEquals(new Buff(2, 30), BuffCalculator.fromScore(40));
        assertEquals(new Buff(2, 30), BuffCalculator.fromScore(59));
        assertEquals(new Buff(1, 15), BuffCalculator.fromScore(60));
        assertEquals(new Buff(1, 15), BuffCalculator.fromScore(79));
        assertEquals(new Buff(0, 0), BuffCalculator.fromScore(80));
        assertEquals(new Buff(0, 0), BuffCalculator.fromScore(100));
    }
}
