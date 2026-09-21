package com.luckypinball.fortune;

/**
 * 운세 점수를 버프(시작 높이)로 변환하는 순수 함수.
 * 승부 방식이 "결승선 통과 순서 경쟁"이라 HP/충돌 데미지 개념은 없고,
 * 시작 Y 오프셋 하나만 존재한다 (운세가 안 좋을수록 결승선에 가깝게 출발).
 */
public final class BuffCalculator {

    private BuffCalculator() {
    }

    public record Buff(int tier, int startY) {
    }

    public static Buff fromScore(int fortuneScore) {
        if (fortuneScore >= 80) {
            return new Buff(0, 0);
        }
        if (fortuneScore >= 60) {
            return new Buff(1, 15);
        }
        if (fortuneScore >= 40) {
            return new Buff(2, 30);
        }
        if (fortuneScore >= 20) {
            return new Buff(3, 45);
        }
        return new Buff(4, 60);
    }
}
