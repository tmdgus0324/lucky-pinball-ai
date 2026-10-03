export interface BallColor {
  hex: string;
  name: string;
}

/**
 * 게임 화면의 공 색. 공은 참가자 순서(등록 순서)대로 이 색을 배정받는다 — 같은 순서·같은 색을
 * 등록 목록, 운세 카드, 실시간 순위에도 점으로 보여줘서 작은 화면에서도 "누가 무슨 색 공인지" 알 수 있게 한다.
 * 8색이 서로 구분되도록 골랐다(하늘/청록처럼 비슷한 쌍은 피하고, 마지막 색은 밝은 흰색).
 */
export const BALL_COLORS: readonly BallColor[] = [
  { hex: '#6b7280', name: '회색' },
  { hex: '#4ade80', name: '초록' },
  { hex: '#38bdf8', name: '하늘' },
  { hex: '#a78bfa', name: '보라' },
  { hex: '#ffd166', name: '노랑' },
  { hex: '#f472b6', name: '분홍' },
  { hex: '#fb923c', name: '주황' },
  { hex: '#e5e7eb', name: '흰색' },
];

export function ballColorAt(index: number): BallColor {
  return BALL_COLORS[index % BALL_COLORS.length];
}
