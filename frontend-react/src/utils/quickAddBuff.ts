/**
 * "빠른 추가"(생년월일/AI 없이 인원수만 선택해 즉시 참가)로 등록된 참가자를 위한
 * 무작위 숨김 점수 → 시작 높이 변환. 서버에 점수를 보내지도, 서버에서 받지도 않는다 —
 * 오늘의 운세 화면(2번 섹션)에는 절대 노출하지 않고, 게임 시작 시 buff.startY를
 * 클라이언트에서만 덮어써서 "점수는 몰라도 시작 높이는 다르게" 만드는 용도로만 쓴다.
 *
 * START_Y_PER_POINT는 backend/.../fortune/BuffCalculator.java의 같은 상수와 값이
 * 반드시 일치해야 한다 — 실제 AI 참가자와 빠른 추가 참가자의 버프 크기가 시각적으로
 * 어긋나지 않게 하기 위함이다.
 */
const START_Y_PER_POINT = 2.0;

export function randomHiddenScore(): number {
  return Math.floor(Math.random() * 101); // 0~100
}

export function startYFromHiddenScore(score: number): number {
  const clamped = Math.max(0, Math.min(100, score));
  return Math.round((100 - clamped) * START_Y_PER_POINT);
}
