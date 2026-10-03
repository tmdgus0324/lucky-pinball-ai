import { useMemo, useState } from 'react';
import { api, type FortuneResponse, type Player } from '../api/client';
import { computeRelativeGap } from '../utils/relativeBuff';
import { BallDot } from './BallDot';

/** "3칸 아래에서 출발" 같은 한 줄 요약. 버프 티어·AI/캐시 출처 같은 내부 정보는 참가자
 * 화면에서는 빼고(관리자 화면의 "AI 호출 Y/N" 컬럼에 그대로 남아있다), 실제로 어디서
 * 출발하는지만 바로 이해되게 한다. */
function describeStart(gap: number): string {
  return gap === 0 ? '가장 높은 곳에서 출발!' : `${gap}칸 아래에서 출발`;
}

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

  // 실제 게임 시작 시 서버가 계산할 상대평가와 같은 결과를 미리 보여준다(devhelp/26) —
  // 생년월일 미입력(NONE) 참가자는 애초에 점수가 없으니 이 비교 대상에서 제외.
  const maxScoreInGroup = useMemo(() => {
    const scores = results
      .filter((r): r is FortuneResponse & { fortuneScore: number } => r.source !== 'NONE' && r.fortuneScore != null)
      .map((r) => r.fortuneScore);
    return scores.length ? Math.max(...scores) : 0;
  }, [results]);

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
      <div className="panel-head">
        <h2>2. 오늘의 운세 — 좋을수록 더 높은 곳에서 출발!</h2>
        <button type="button" disabled={!canCheck || checking} onClick={handleCheckFortune}>
          운세 확인
        </button>
      </div>
      <p className="desc">운세가 좋을수록 더 높은 곳에서, 안 좋을수록 결승선 가까이에서 출발합니다.</p>
      <p className={`status-text${status.error ? ' error' : ''}`}>{status.text}</p>
      <div className="fortune-grid" style={{ marginTop: 14 }}>
        {checked &&
          quickPlayers.map((player) => (
            <div className="fortune-card tier-quick" key={player.playerId}>
              <div className="name">
                <BallDot index={players.indexOf(player)} />
                {player.name}
              </div>
              <div className="message">🎲 무작위로 배정된 시작 높이 — 게임에서만 확인할 수 있습니다</div>
            </div>
          ))}
        {results.map((fortune) => {
          const player = players.find((p) => p.playerId === fortune.playerId);
          // 참가자에게는 "몇 칸 차이로 어디서 출발하는지"만 한 줄로 보여준다 — 버프 티어
          // 숫자나 AI/캐시 출처 같은 내부 정보는 뺐다(관리자 화면 "AI 호출 Y/N"에 남아있음).
          const startLine =
            fortune.source === 'NONE' || fortune.fortuneScore == null
              ? null
              : describeStart(computeRelativeGap(fortune.fortuneScore, maxScoreInGroup));
          return (
            <div className={`fortune-card tier-${fortune.buff.tier}`} key={fortune.playerId}>
              <div className="name">
                {player && <BallDot index={players.indexOf(player)} />}
                {player ? player.name : fortune.playerId}
              </div>
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
              {startLine && <div className="start-line">{startLine}</div>}
            </div>
          );
        })}
      </div>
    </section>
  );
}
