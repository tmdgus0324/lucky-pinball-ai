/**
 * 오늘의 운세 화면(2번 섹션)에 보여줄 "시작 높이" 미리보기 계산.
 * 실제 게임 시작 시 서버(GameService.applyRelativeStartY)가 계산하는 것과 반드시 같은
 * 결과가 나와야 하므로, 두 상수(RELATIVE_GAP_CAP, BALL_DIAMETER_PX)는
 * backend/.../fortune/BuffCalculator.java의 같은 상수와 항상 일치시켜야 한다
 * (quickAddBuff.ts가 이미 같은 방식으로 START_Y_PER_POINT를 복제해둔 선례가 있음).
 *
 * 서버의 POST /api/fortune는 참가자 한 명씩 독립 호출이라 상대평가를 모른 채 절대값
 * buff.startY를 반환하므로, 화면에 보여줄 값은 이 함수로 프론트에서 다시 계산한다.
 */
const RELATIVE_GAP_CAP = 10;
const BALL_DIAMETER_PX = 26;

export function computeRelativeStartY(myScore: number, maxScoreInGroup: number): number {
  const gap = Math.min(RELATIVE_GAP_CAP, Math.max(0, maxScoreInGroup - myScore));
  return gap * BALL_DIAMETER_PX;
}
