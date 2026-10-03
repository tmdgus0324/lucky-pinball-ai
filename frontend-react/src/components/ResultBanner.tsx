import { useState } from 'react';
import type { GameResultView } from '../api/client';

/**
 * 게임이 끝나면 맵 위에 겹쳐 뜨는 당첨자 배너. 카메라가 선두 공을 보드의 세로 중앙에 고정해서
 * 따라가므로 게임 끝 무렵 사용자의 시선은 보드 중앙 부근에 있다 — 그 위쪽에 띄워 스크롤 없이 보이게 한다.
 */
export function ResultBanner({ result }: { result: GameResultView }) {
  const [open, setOpen] = useState(true);
  if (!open) return null;

  return (
    <div className="board-overlay">
      <div className="result-banner" id="resultSection">
        <button type="button" className="close" aria-label="닫기" onClick={() => setOpen(false)}>
          ✕
        </button>
        <div className="bell">🔔</div>
        <div className="selected-name">{result.selectedName} 님 당첨!</div>
        <div className="sub">참가자 {result.participantCount}명 중 가장 마지막으로 결승선 통과</div>
      </div>
    </div>
  );
}
