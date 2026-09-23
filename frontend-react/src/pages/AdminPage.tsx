import { useState } from 'react';
import { PlayersTable } from '../components/PlayersTable';
import { GamesTable } from '../components/GamesTable';
import { LogList } from '../components/LogList';
import { OverrideForm } from '../components/OverrideForm';

export function AdminPage() {
  const [logReloadKey, setLogReloadKey] = useState(0);

  return (
    <>
      <header className="top">
        <h2>🛠 관리자 화면</h2>
        <p>참가자 · 게임 · 오류 로그 조회 및 가중치 조정(예정) — 인증 없음(MVP 한정).</p>
      </header>

      <PlayersTable />
      <GamesTable />
      <LogList reloadKey={logReloadKey} />
      <OverrideForm onLogged={() => setLogReloadKey((k) => k + 1)} />
    </>
  );
}
