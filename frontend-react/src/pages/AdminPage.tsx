import { useState } from 'react';
import { api } from '../api/client';
import { AdminLogin } from '../components/AdminLogin';
import { AiHealthPanel } from '../components/AiHealthPanel';
import { PlayersTable } from '../components/PlayersTable';
import { GamesTable } from '../components/GamesTable';
import { LogList } from '../components/LogList';

export function AdminPage() {
  const [loggedIn, setLoggedIn] = useState(api.isAdminLoggedIn());

  if (!loggedIn) {
    return <AdminLogin onSuccess={() => setLoggedIn(true)} />;
  }

  async function handleLogout() {
    await api.adminLogout();
    setLoggedIn(false);
  }

  // 조회 중 하나라도 401을 받으면(서버 재시작 등으로 토큰이 만료된 경우) 로그인 화면으로
  // 돌려보낸다 — 안 그러면 "로그인된 화면인데 전부 불러오기 실패"인 채로 멈춰있게 된다
  // (devhelp/08(구 34)에서 실제로 겪은 문제).
  function handleUnauthorized() {
    void api.adminLogout(); // 서버 호출은 실패해도 상관없다 — 로컬 토큰만 확실히 지운다.
    setLoggedIn(false);
  }

  return (
    <>
      <header className="top">
        <h2>🛠 관리자 화면</h2>
        <div className="actions-row spread">
          <p style={{ margin: 0 }}>참가자 · 게임 · 오류 로그 조회와 AI 연결 확인.</p>
          <button type="button" className="secondary" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </header>

      <PlayersTable onUnauthorized={handleUnauthorized} />
      <GamesTable onUnauthorized={handleUnauthorized} />
      <LogList onUnauthorized={handleUnauthorized} />
      <AiHealthPanel onUnauthorized={handleUnauthorized} />
    </>
  );
}
