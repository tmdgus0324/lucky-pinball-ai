import { useRef, useState, type FormEvent } from 'react';
import { api, type Player } from '../api/client';
import { parseBirthDateShorthand } from '../utils/birthDate';

interface RegistrationFormProps {
  players: Player[];
  onRegistered: (player: Player) => void;
  onQuickAdd: (players: Player[]) => void;
  onRemove: (playerId: number) => void;
}

export function RegistrationForm({ players, onRegistered, onQuickAdd, onRemove }: RegistrationFormProps) {
  const [name, setName] = useState('');
  const [birthInput, setBirthInput] = useState('');
  const [status, setStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });
  const [quickCount, setQuickCount] = useState(1);
  const [quickAdding, setQuickAdding] = useState(false);
  const quickAddSeq = useRef(1);

  const remainingSlots = 8 - players.length;

  async function handleQuickAdd() {
    setQuickAdding(true);
    setStatus({ text: '', error: false });
    const count = Math.min(quickCount, remainingSlots);
    const added: Player[] = [];
    try {
      for (let i = 0; i < count; i++) {
        const guestName = `게스트${quickAddSeq.current++}`;
        // eslint-disable-next-line no-await-in-loop
        const player = await api.registerPlayer(guestName, null);
        added.push(player);
      }
      onQuickAdd(added);
      setStatus({ text: `${players.length + added.length}/8명 등록됨`, error: false });
    } catch (error) {
      setStatus({ text: `빠른 추가 실패: ${(error as Error).message}`, error: true });
      if (added.length > 0) onQuickAdd(added); // 일부라도 성공한 만큼은 반영
    } finally {
      setQuickAdding(false);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmedName = name.trim();
    const rawBirth = birthInput.trim();

    if (!trimmedName) {
      setStatus({ text: '이름을 입력해주세요.', error: true });
      return;
    }

    let birthDate: string | null = null;
    if (rawBirth) {
      birthDate = parseBirthDateShorthand(rawBirth);
      if (!birthDate) {
        setStatus({ text: '생년월일은 6자리 숫자로 입력해주세요 (예: 880324).', error: true });
        return;
      }
    }
    if (players.length >= 8) {
      setStatus({ text: '참가자는 최대 8명까지 등록할 수 있습니다.', error: true });
      return;
    }

    try {
      const player = await api.registerPlayer(trimmedName, birthDate);
      onRegistered(player);
      setName('');
      setBirthInput('');
      setStatus({ text: `${players.length + 1}/8명 등록됨`, error: false });
    } catch (error) {
      setStatus({ text: `등록 실패: ${(error as Error).message}`, error: true });
    }
  }

  return (
    <section className="panel">
      <h2>
        1. 참가자 등록 <span className="badge">최대 8명</span>
      </h2>
      <p className="desc">
        이름(또는 별칭)을 입력해 참가자를 등록합니다. 생년월일은 <strong>선택사항</strong>이며{' '}
        <strong>6자리 숫자</strong>로 입력합니다(예: <code>880324</code> → 1988-03-24, <code>130101</code> → 2013-01-01).
        입력하면 AI가 오늘의 운세를 분석해 버프를 부여하고, 비워두면 AI 호출 없이 버프 없는 상태로 바로 참여합니다.
      </p>

      <form className="reg-form" onSubmit={handleSubmit}>
        <span className="quick-add-inline">
          <select
            value={Math.min(quickCount, Math.max(remainingSlots, 1))}
            onChange={(e) => setQuickCount(Number(e.target.value))}
            disabled={remainingSlots <= 0}
            title="빠른 추가할 인원수"
          >
            {Array.from({ length: Math.max(remainingSlots, 1) }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}명
              </option>
            ))}
          </select>
          <button
            type="button"
            className="secondary"
            disabled={remainingSlots <= 0 || quickAdding}
            onClick={handleQuickAdd}
          >
            빠른 추가
          </button>
        </span>
        <input
          type="text"
          name="name"
          placeholder="이름 / 별칭"
          autoComplete="off"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          type="text"
          name="birthDate"
          placeholder="생년월일 6자리 (선택, 예: 880324)"
          inputMode="numeric"
          maxLength={6}
          value={birthInput}
          onChange={(e) => setBirthInput(e.target.value)}
        />
        <button type="submit">참가자 등록</button>
      </form>
      <p className="desc" style={{ marginTop: 6 }}>
        "빠른 추가"로 참여한 참가자는 이름/생년월일 입력과 AI 운세 호출 없이 즉시 참여합니다 — 대신 무작위로
        시작 높이가 정해지며, 그 값은 공개되지 않고 게임에서 높이 차이로만 드러납니다.
      </p>

      <div className="player-list">
        {players.map((player) => (
          <span className="player-chip" key={player.playerId}>
            {player.name} · {player.birthDate || '생년월일 미입력'}{' '}
            <button type="button" className="remove" onClick={() => onRemove(player.playerId)}>
              ✕
            </button>
          </span>
        ))}
      </div>
      <p className={`status-text${status.error ? ' error' : ''}`}>{status.text || `${players.length}/8명 등록됨`}</p>
    </section>
  );
}
