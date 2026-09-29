import { useState } from 'react';
import { api } from '../api/client';
import { AdminLogin } from '../components/AdminLogin';
import { PlayersTable } from '../components/PlayersTable';
import { GamesTable } from '../components/GamesTable';
import { LogList } from '../components/LogList';
import { OverrideForm } from '../components/OverrideForm';

export function AdminPage() {
  const [loggedIn, setLoggedIn] = useState(api.isAdminLoggedIn());
  const [logReloadKey, setLogReloadKey] = useState(0);

  if (!loggedIn) {
    return <AdminLogin onSuccess={() => setLoggedIn(true)} />;
  }

  async function handleLogout() {
    await api.adminLogout();
    setLoggedIn(false);
  }

  return (
    <>
      <header className="top">
        <h2>🛠 관리자 화면</h2>
        <div className="actions-row">
          <p style={{ margin: 0 }}>참가자 · 게임 · 오류 로그 조회 및 가중치 조정(예정).</p>
          <button type="button" className="secondary" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </header>

      <PlayersTable />
      <GamesTable />
      <LogList reloadKey={logReloadKey} />
      <OverrideForm onLogged={() => setLogReloadKey((k) => k + 1)} />
    </>
  );
}
