import { useState, type FormEvent } from 'react';
import { api, ApiError } from '../api/client';

export function OverrideForm({ onLogged }: { onLogged: () => void }) {
  const [playerId, setPlayerId] = useState('');
  const [overrideScore, setOverrideScore] = useState('');
  const [status, setStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setStatus({ text: '요청 중...', error: false });

    try {
      await api.adminOverrideFortune(Number(playerId), Number(overrideScore));
      // 501 응답은 api/client.ts에서 에러로 처리되므로 여기 도달하면 실제로 구현된 것 — 지금은 도달할 일이 없음.
      setStatus({ text: '적용되었습니다.', error: false });
    } catch (error) {
      const message =
        error instanceof ApiError && error.status === 501
          ? `아직 준비 중인 기능입니다 (${error.message})`
          : `실패: ${(error as Error).message}`;
      setStatus({ text: message, error: true });
      onLogged();
    }
  }

  return (
    <section className="panel">
      <h2>
        가중치 임의 조정 <span className="badge wip">준비 중 · Phase 2</span>
      </h2>
      <p className="desc">
        호출 시 현재는 <code>501 Not Implemented</code>를 반환합니다.
      </p>
      <form className="override-form" onSubmit={handleSubmit}>
        <input
          type="number"
          name="playerId"
          placeholder="playerId"
          required
          value={playerId}
          onChange={(e) => setPlayerId(e.target.value)}
        />
        <input
          type="number"
          name="overrideScore"
          placeholder="조정할 점수 (0-100)"
          min={0}
          max={100}
          required
          value={overrideScore}
          onChange={(e) => setOverrideScore(e.target.value)}
        />
        <button type="submit">적용</button>
      </form>
      <p className={`status-text${status.error ? ' error' : ''}`}>{status.text}</p>
    </section>
  );
}
