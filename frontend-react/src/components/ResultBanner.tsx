import type { GameResultView } from '../api/client';

export function ResultBanner({ result }: { result: GameResultView }) {
  return (
    <section className="panel" id="resultSection">
      <h2>4. 결과</h2>
      <div className="result-banner">
        <div className="bell">🔔</div>
        <div className="selected-name">{result.selectedName} 님 당첨!</div>
        <div className="sub">
          참가자 {result.participantCount}명 중 가장 마지막으로 결승선 통과 · 게임 ID: {result.gameId}
        </div>
      </div>
    </section>
  );
}
