import { useEffect, useRef } from 'react';
import { runPinballGame } from '../game/engine';
import type { GameParticipant } from '../api/client';

interface PinballBoardProps {
  participants: GameParticipant[];
  onComplete: (finishOrder: number[]) => void;
}

/**
 * game.js(runPinballGame)를 감싸는 명령형 "섬" — 물리 시뮬레이션 코드 자체는 건드리지
 * 않고, React는 "마운트되면 시작하고 언마운트되면 정리한다"는 경계 역할만 한다.
 * (devhelp/18 StrictMode 이중 실행 항목, devhelp/21 참고)
 */
export function PinballBoard({ participants, onComplete }: PinballBoardProps) {
  const boardRef = useRef<HTMLDivElement>(null);
  const rankListRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!boardRef.current || !rankListRef.current) return;

    let cancelled = false;

    const dispose = runPinballGame({
      boardEl: boardRef.current,
      rankListEl: rankListRef.current,
      participants,
      onComplete: (finishOrder) => {
        if (!cancelled) onComplete(finishOrder);
      },
    });

    return () => {
      cancelled = true;
      dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [participants]);

  return (
    <div className="game-layout" style={{ marginTop: 14 }}>
      <div ref={boardRef} className="pinball-board" />
      <aside className="rank-panel">
        <h3>실시간 순위</h3>
        <div ref={rankListRef}>
          <div className="rank-item pending">게임을 시작하면 여기에 순위가 표시됩니다</div>
        </div>
      </aside>
    </div>
  );
}
