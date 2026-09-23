import { useState } from 'react';
import { api, type GameCreateResponse, type GameResultView, type Player } from '../api/client';
import { RegistrationForm } from '../components/RegistrationForm';
import { FortuneSection } from '../components/FortuneSection';
import { PinballBoard } from '../components/PinballBoard';
import { ResultBanner } from '../components/ResultBanner';
import { randomHiddenScore, startYFromHiddenScore } from '../utils/quickAddBuff';

export function GamePage() {
  const [players, setPlayers] = useState<Player[]>([]);
  // 빠른 추가로 들어온 참가자의 playerId -> 무작위 숨김 점수. 오늘의 운세 화면에는 절대
  // 노출하지 않고, 게임 시작 시 참가자의 buff.startY를 이 값 기반으로 덮어쓰는 데만 쓴다.
  const [hiddenScores, setHiddenScores] = useState<Map<number, number>>(new Map());
  const [fortunesReady, setFortunesReady] = useState(false);
  const [game, setGame] = useState<GameCreateResponse | null>(null);
  const [result, setResult] = useState<GameResultView | null>(null);
  const [gameStatus, setGameStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });
  const [starting, setStarting] = useState(false);

  function handleRegistered(player: Player) {
    setPlayers((prev) => [...prev, player]);
  }

  function handleQuickAdd(newPlayers: Player[]) {
    setPlayers((prev) => [...prev, ...newPlayers]);
    setHiddenScores((prev) => {
      const next = new Map(prev);
      newPlayers.forEach((p) => next.set(p.playerId, randomHiddenScore()));
      return next;
    });
  }

  function handleRemove(playerId: number) {
    setPlayers((prev) => prev.filter((p) => p.playerId !== playerId));
    setHiddenScores((prev) => {
      if (!prev.has(playerId)) return prev;
      const next = new Map(prev);
      next.delete(playerId);
      return next;
    });
    setFortunesReady(false);
  }

  async function handleStartGame() {
    setStarting(true);
    setResult(null);
    setGameStatus({ text: '', error: false });

    try {
      const playerIds = players.map((p) => p.playerId);
      const created = await api.createGame(playerIds);
      await api.startGame(created.gameId);

      // 빠른 추가 참가자는 서버가 생년월일 없음(NONE)으로 판단해 버프를 안 주지만,
      // 게임에서는 차등을 보여줘야 하므로 등록 시 미리 뽑아둔 숨김 점수로 시작 높이만
      // 클라이언트에서 덮어쓴다(오늘의 운세 화면에는 이 값이 어디에도 노출되지 않는다).
      const participants = created.participants.map((participant) => {
        const hiddenScore = hiddenScores.get(participant.playerId);
        if (hiddenScore === undefined) return participant;
        return { ...participant, buff: { ...participant.buff, startY: startYFromHiddenScore(hiddenScore) } };
      });

      setGame({ ...created, participants });
    } catch (error) {
      setGameStatus({ text: `게임 시작 실패: ${(error as Error).message}`, error: true });
      setStarting(false);
    }
  }

  async function handleGameComplete(finishOrder: number[]) {
    if (!game) return;
    try {
      const resultView = await api.reportResult(game.gameId, finishOrder);
      setResult(resultView);
    } catch (error) {
      setGameStatus({ text: `결과 보고 실패: ${(error as Error).message}`, error: true });
    } finally {
      // 몇 번이든 다시 시작할 수 있게 버튼을 다시 활성화한다 — 매번 새 게임(새 gameId)이
      // 만들어지고 물리 시뮬레이션도 다시 도니 이전 판과 같은 결과가 나오지 않는다.
      setStarting(false);
    }
  }

  return (
    <>
      <header className="top">
        <h2>참가자 / 게임 화면</h2>
      </header>

      <RegistrationForm
        players={players}
        onRegistered={handleRegistered}
        onQuickAdd={handleQuickAdd}
        onRemove={handleRemove}
      />

      <FortuneSection
        players={players}
        quickAddIds={new Set(hiddenScores.keys())}
        onFortunesReady={() => setFortunesReady(true)}
      />

      <section className="panel">
        <h2>
          3. 게임 진행 — 핀볼 맵 <span className="badge">클래식 갈톤보드</span>
        </h2>
        <p className="desc">
          갈톤보드/플린코 스타일의 못(peg) 배열을 통과해 <strong>결승선을 먼저 통과하는 순서</strong>로 순위가
          정해집니다. 가장 마지막에 결승선을 통과한 참가자가 "당첨자"가 됩니다.
        </p>

        <div className="actions-row">
          <button type="button" disabled={!fortunesReady || starting} onClick={handleStartGame}>
            게임 시작
          </button>
        </div>
        <p className={`status-text${gameStatus.error ? ' error' : ''}`}>{gameStatus.text}</p>

        {game && <PinballBoard key={game.gameId} participants={game.participants} onComplete={handleGameComplete} />}
      </section>

      {result && <ResultBanner result={result} />}
    </>
  );
}
