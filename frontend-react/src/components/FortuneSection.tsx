import { useState } from 'react';
import { api, type FortuneResponse, type FortuneSource, type Player } from '../api/client';

const SOURCE_LABEL: Record<FortuneSource, string> = {
  AI: 'AI 분석 (1회차)',
  CACHE: '이전 기록 재사용',
  NONE: '생년월일 미입력',
};

interface FortuneSectionProps {
  players: Player[];
  /** "빠른 추가"로 등록된 참가자 id — 이 참가자들은 AI 호출을 건너뛰고 결과를 가린 카드로만 보여준다. */
  quickAddIds: Set<number>;
  onFortunesReady: (fortunes: Map<number, FortuneResponse>) => void;
}

export function FortuneSection({ players, quickAddIds, onFortunesReady }: FortuneSectionProps) {
  const [results, setResults] = useState<FortuneResponse[]>([]);
  const [checked, setChecked] = useState(false);
  const [status, setStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });
  const [checking, setChecking] = useState(false);

  const canCheck = players.length >= 2 && players.length <= 8;
  const quickPlayers = players.filter((p) => quickAddIds.has(p.playerId));
  const apiPlayers = players.filter((p) => !quickAddIds.has(p.playerId));

  async function handleCheckFortune() {
    setChecking(true);
    setStatus({ text: '운세를 확인하는 중...', error: false });
    setResults([]);
    setChecked(false);

    // Promise.all이 아니라 allSettled를 쓴 이유: 생년월일을 입력한 참가자 중 한 명이라도
    // AI 호출에 실패하면(예: API 키 미설정) Promise.all은 전체를 실패시켜서 생년월일 없는
    // 참가자(원래 실패할 이유가 없는)의 카드까지 못 보여준다 — 참가자별로 독립적으로 처리한다.
    // 빠른 추가 참가자는 애초에 API를 호출하지 않으므로 이 배치에서 제외한다.
    const settled = await Promise.allSettled(apiPlayers.map((player) => api.getFortune(player.playerId)));

    const fulfilled: FortuneResponse[] = [];
    const errors: string[] = [];
    const fortuneMap = new Map<number, FortuneResponse>();

    settled.forEach((outcome, index) => {
      if (outcome.status === 'fulfilled') {
        fulfilled.push(outcome.value);
        fortuneMap.set(outcome.value.playerId, outcome.value);
      } else {
        errors.push(`${apiPlayers[index].name}: ${(outcome.reason as Error).message}`);
      }
    });

    setResults(fulfilled);
    setChecked(true);

    if (errors.length === 0) {
      setStatus({ text: '', error: false });
      onFortunesReady(fortuneMap);
    } else {
      setStatus({ text: `일부 운세 조회 실패 - ${errors.join(' / ')}`, error: true });
      setChecking(false); // 실패한 참가자가 있으니 재시도할 수 있게 다시 활성화
      return;
    }
    setChecking(false);
  }

  return (
    <section className="panel">
      <h2>2. 오늘의 운세 &amp; 버프</h2>
      <p className="desc">
        버프는 <strong>시작 높이</strong> 한 가지만 차등을 둡니다 — 운세가 좋을수록 결승선에서 먼 곳(위)에서, 운세가
        안 좋을수록 결승선에 가까운 곳(아래)에서 출발합니다.
      </p>

      <div className="actions-row">
        <button type="button" disabled={!canCheck || checking} onClick={handleCheckFortune}>
          운세 확인
        </button>
      </div>
      <p className={`status-text${status.error ? ' error' : ''}`}>{status.text}</p>
      <div className="fortune-grid" style={{ marginTop: 14 }}>
        {checked &&
          quickPlayers.map((player) => (
            <div className="fortune-card tier-quick" key={player.playerId}>
              <div className="name">{player.name}</div>
              <div className="message">🎲 무작위로 배정된 시작 높이 — 게임에서만 확인할 수 있습니다</div>
            </div>
          ))}
        {results.map((fortune) => {
          const player = players.find((p) => p.playerId === fortune.playerId);
          const buffLabel = fortune.buff.tier === 0 ? '버프 없음' : `버프 ${fortune.buff.tier}`;
          const sourceLabel = SOURCE_LABEL[fortune.source] ?? fortune.source;
          return (
            <div className={`fortune-card tier-${fortune.buff.tier}`} key={fortune.playerId}>
              <div className="name">{player ? player.name : fortune.playerId}</div>
              {fortune.source === 'NONE' ? (
                <div className="message">생년월일을 입력하지 않아 운세 없이 참여합니다</div>
              ) : (
                <>
                  <div className="score">
                    {fortune.fortuneScore}
                    <span style={{ fontSize: 12, color: 'var(--muted)' }}>점</span>
                  </div>
                  <div className="message">"{fortune.fortuneMessage}"</div>
                </>
              )}
              <div className="stats">
                <span>{buffLabel}</span>
                <span>시작 높이 +{fortune.buff.startY}</span>
                <span>{sourceLabel}</span>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
