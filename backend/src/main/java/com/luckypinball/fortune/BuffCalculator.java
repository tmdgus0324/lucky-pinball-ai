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
    // (참고: 실제 게임 시작 시 startY는 fromScore가 아니라 relativeStartY로 다시 계산되어
    // 대체된다 — fromScore의 startY는 더 이상 게임에 안 쓰이고, tier만 남아서 쓰인다.)
    private static final double START_Y_PER_POINT = 2.0;

    // 상대평가(devhelp/03(구 26)): AI 점수가 좁은 범위(예: 72~76점)에 몰려도 시작 높이 차이가
    // 항상 눈에 띄도록, 절대 0~100점이 아니라 "이번 판 참가자 중 최고점자 대비 몇 점 차이인지"로
    // 시작 높이를 다시 계산한다. 차이가 지나치게 크게 벌어지는 걸 막기 위해 10점(=공 10개)에서
    // 잘라낸다.
    private static final int RELATIVE_GAP_CAP = 10; // 상대 차이는 최대 10점까지만 반영
    // frontend engine.ts CLASSIC_GALTON_MAP.ballRadius(13)*2와 반드시 일치해야 한다 —
    // "공 1개 차이"라는 의미 자체가 이 값에 달려있다.
    private static final int BALL_DIAMETER_PX = 26;

    public record Buff(int tier, int startY) {
    }

    public static Buff fromScore(int fortuneScore) {
        int clamped = Math.max(0, Math.min(100, fortuneScore));
        int startY = (int) Math.round((100 - clamped) * START_Y_PER_POINT);
        return new Buff(tierOf(clamped), startY);
    }

    /**
     * 이번 판 참가자 중 최고점자(maxScoreInGame) 대비 이 사람의 점수가 얼마나 낮은지를
     * "공 개수" 단위로 환산한 시작 높이. 최고점자 본인은 gap=0(맨 위, 결승선에서 가장 먼 곳).
     */
    public static int relativeStartY(int myScore, int maxScoreInGame) {
        int gap = Math.min(RELATIVE_GAP_CAP, Math.max(0, maxScoreInGame - myScore));
        return gap * BALL_DIAMETER_PX;
    }

    private static int tierOf(int score) {
        if (score >= 80) return 0;
        if (score >= 60) return 1;
        if (score >= 40) return 2;
        if (score >= 20) return 3;
        return 4;
    }
}
