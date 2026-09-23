package com.luckypinball.fortune;

/**
 * 운세 점수를 버프(시작 높이)로 변환하는 순수 함수.
 * 승부 방식이 "결승선 통과 순서 경쟁"이라 HP/충돌 데미지 개념은 없고,
 * 시작 Y 오프셋 하나만 존재한다 (운세가 안 좋을수록 결승선에 가깝게 출발).
 *
 * startY는 점수에 선형으로 비례한다 — 예전에는 20점 단위 5단계로만 나눠서
 * 같은 구간 안에서는 점수가 달라도(예: 61점과 79점) 시작 위치가 완전히 같았다.
 * 1점 차이도 항상 화면에 드러나도록 구간을 없애고 연속값으로 바꿨다.
 * tier는 운세 카드 배지 색상 구분용으로만 남겨둔다(게임 로직에는 더 이상 안 쓰임).
 */
public final class BuffCalculator {

    private BuffCalculator() {
    }

    // score 0점 -> startY 200(px), score 100점 -> startY 0(px). 프론트(engine.ts)의
    // 낙하 시작 구역(드롭존) 높이가 이 최대값(200)을 넉넉히 수용하도록 맞춰져 있다.
    private static final double START_Y_PER_POINT = 2.0;

    public record Buff(int tier, int startY) {
    }

    public static Buff fromScore(int fortuneScore) {
        int clamped = Math.max(0, Math.min(100, fortuneScore));
        int startY = (int) Math.round((100 - clamped) * START_Y_PER_POINT);
        return new Buff(tierOf(clamped), startY);
    }

    private static int tierOf(int score) {
        if (score >= 80) return 0;
        if (score >= 60) return 1;
        if (score >= 40) return 2;
        if (score >= 20) return 3;
        return 4;
    }
}
