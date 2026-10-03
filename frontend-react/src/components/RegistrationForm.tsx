import { useState, type FormEvent } from 'react';
import { api, type Player } from '../api/client';
import { parseBirthDateShorthand } from '../utils/birthDate';
import { randomBirthDateIso, randomKoreanName } from '../utils/aiTest';

const MAX_PLAYERS = 8;

interface RegistrationFormProps {
  players: Player[];
  onRegistered: (player: Player) => void;
  onQuickAdd: (players: Player[]) => void;
  onRemove: (playerId: number) => void;
  onClearAll: () => void;
}

export function RegistrationForm({ players, onRegistered, onQuickAdd, onRemove, onClearAll }: RegistrationFormProps) {
  const [name, setName] = useState('');
  const [birthInput, setBirthInput] = useState('');
  const [status, setStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });
  const [quickCount, setQuickCount] = useState(1);
  const [quickAdding, setQuickAdding] = useState(false);
  const [aiTesting, setAiTesting] = useState(false);
  const [dbTesting, setDbTesting] = useState(false);

  const remainingSlots = 8 - players.length;
  const busy = quickAdding || aiTesting || dbTesting;

  async function handleQuickAdd() {
    setQuickAdding(true);
    setStatus({ text: '', error: false });
    const count = Math.min(quickCount, remainingSlots);
    const added: Player[] = [];
    // "1번, 2번, ..."을 현재 목록에 없는 가장 작은 번호부터 채운다 — 전체 초기화 뒤에는 항상 1번부터
    // 나가고, 이미 1·2번이 있으면 3번부터 이어진다. 한 판에 같은 이름이 둘 생기면 엔진의 대기 목록
    // (pendingNames, 이름 기준 Set)과 순위 패널이 틀어지므로 번호를 매번 1부터 리셋하지는 않는다.
    const usedNames = new Set(players.map((p) => p.name));
    let seq = 1;
    try {
      for (let i = 0; i < count; i++) {
        while (usedNames.has(`${seq}번`)) seq++;
        const guestName = `${seq}번`;
        usedNames.add(guestName);
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

  /**
   * 이름/생년월일을 손으로 안 쳐도 실제 AI 호출·캐시 재사용 흐름을 바로 시연할 수 있게,
   * 임의의 한글 3글자 이름 + 임의 생년월일로 선택한 인원수만큼 즉시 등록한다. 빠른 추가와
   * 달리 생년월일을 실제로 넣어서 보내므로, "운세 확인"을 누르면 진짜 Claude가 호출된다.
   */
  async function handleAiTest() {
    setAiTesting(true);
    setStatus({ text: '', error: false });
    onClearAll(); // 반복 테스트로 참가자가 계속 쌓이지 않도록, 새로 채우기 전에 먼저 비운다.
    // remainingSlots는 초기화 전 인원 기준이라, 초기화 후 실제로 채울 수 있는 자리(최대 8)로 다시 계산한다.
    const count = Math.min(quickCount, MAX_PLAYERS);
    const added: Player[] = [];
    try {
      for (let i = 0; i < count; i++) {
        // eslint-disable-next-line no-await-in-loop
        const player = await api.registerPlayer(randomKoreanName(), randomBirthDateIso());
        added.push(player);
      }
      added.forEach(onRegistered);
      setStatus({ text: `${added.length}/8명 등록됨 (AI TEST)`, error: false });
    } catch (error) {
      setStatus({ text: `AI TEST 등록 실패: ${(error as Error).message}`, error: true });
      added.forEach(onRegistered); // 일부라도 성공한 만큼은 반영
    } finally {
      setAiTesting(false);
    }
  }

  /**
   * AI TEST는 매번 새 신원이라 항상 AI가 호출된다. "2회차 방문 → DB 재사용"을 시연하려면
   * 이미 운세 결과가 DB에 있는 신원(이름+생년월일)으로 다시 등록해야 한다 — 캐시 키가
   * playerId가 아니라 이름+생년월일이라서, 새 참가자 행(새 playerId)으로 재등록해도 캐시가
   * 적중하고, 관리자 화면에도 "AI 호출 = N"인 새 행이 뚜렷하게 남는다.
   */
  async function handleDbTest() {
    setDbTesting(true);
    setStatus({ text: '', error: false });
    onClearAll(); // 반복 테스트로 참가자가 계속 쌓이지 않도록, 새로 채우기 전에 먼저 비운다.
    const added: Player[] = [];
    try {
      // 관리자 전용 /admin/players 대신 공개 엔드포인트를 쓴다 — 관리자 화면에 로그인이
      // 생기면서, 로그인 안 한 일반 참가자도 쓰는 이 데모 기능까지 막히면 안 되기 때문
      // (devhelp/32 참고). 출처(AI/CACHE)가 실제로 기록된 신원만 걸러주는 건 이미
      // 서버(GET /api/players/reusable)가 해주므로, 여기서는 중복 제거만 한다.
      const existing = await api.getReusablePlayers();
      const seen = new Set<string>();
      const candidates = existing.filter((p) => {
        const key = `${p.name}|${p.birthDate}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });

      if (candidates.length === 0) {
        setStatus({
          text: 'DB에 재사용할 기존 참가자가 없습니다 — 먼저 AI TEST로 등록하고 운세 확인을 해주세요.',
          error: true,
        });
        return;
      }

      // Fisher-Yates가 아니어도 되는 규모라 정렬 기반으로 간단히 섞는다.
      // 초기화 후 실제로 채울 수 있는 자리(최대 8)로 뽑는다 — remainingSlots는 초기화 전 기준이라 못 쓴다.
      const picked = [...candidates].sort(() => Math.random() - 0.5).slice(0, Math.min(quickCount, MAX_PLAYERS));
      for (const c of picked) {
        // eslint-disable-next-line no-await-in-loop
        added.push(await api.registerPlayer(c.name, c.birthDate));
      }
      added.forEach(onRegistered);
      const shortfall = Math.min(quickCount, MAX_PLAYERS) - added.length;
      setStatus({
        text:
          `${added.length}/8명 등록됨 (DB TEST)` +
          (shortfall > 0 ? ` — 재사용 가능한 기존 참가자가 ${added.length}명뿐이라 그만큼만 등록했습니다.` : ''),
        error: false,
      });
    } catch (error) {
      setStatus({ text: `DB TEST 등록 실패: ${(error as Error).message}`, error: true });
      added.forEach(onRegistered);
    } finally {
      setDbTesting(false);
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
        이름 + 생년월일 6자리(선택, 예: <code>880324</code>)를 입력하세요 — 입력하면 AI 운세로 버프가, 비우면 버프
        없이 바로 참여합니다.
      </p>

      <form className="reg-form" onSubmit={handleSubmit}>
        <span className="quick-add-inline">
          <select
            value={quickCount}
            onChange={(e) => setQuickCount(Number(e.target.value))}
            disabled={busy}
            title="빠른 추가/AI TEST/DB TEST에 쓸 인원수"
          >
            {/* AI TEST·DB TEST는 선택 시 기존 참가자를 비우고 새로 채우므로 항상 8명까지 고를 수 있다.
                "빠른 추가"만 남은 자리(remainingSlots) 이상을 고르면 그 자리만큼만 채워진다. */}
            {Array.from({ length: MAX_PLAYERS }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}명
              </option>
            ))}
          </select>
          <button
            type="button"
            className="secondary"
            disabled={remainingSlots <= 0 || busy}
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
        <button type="submit" disabled={busy}>
          참가자 등록
        </button>
        <button type="button" className="secondary" disabled={busy} onClick={handleAiTest}>
          AI TEST
        </button>
        <button type="button" className="secondary" disabled={busy} onClick={handleDbTest}>
          DB TEST
        </button>
      </form>
      <p className="desc" style={{ marginTop: 6 }}>
        <strong>빠른 추가</strong>=운세 없이 즉시 참여 · <strong>AI TEST</strong>=임의 신원으로 실제 AI 호출 시연 ·{' '}
        <strong>DB TEST</strong>=기존 신원 재등록으로 캐시 재사용 시연
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
      <div className="actions-row spread" style={{ marginTop: 6 }}>
        <p className={`status-text${status.error ? ' error' : ''}`} style={{ margin: 0 }}>
          {status.text || `${players.length}/8명 등록됨`}
        </p>
        <button type="button" className="secondary" disabled={players.length === 0 || busy} onClick={onClearAll}>
          전체 초기화
        </button>
      </div>
    </section>
  );
}
