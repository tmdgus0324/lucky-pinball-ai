import { NavLink, Outlet } from 'react-router-dom';

/**
 * 좌측 사이드바 내비게이션 (05번 백로그 A1).
 * 기존 vanilla 버전의 상단 중앙 탭(nav.tabs)을 좌측 세로 메뉴로 재배치했다.
 */
export function Layout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <h1>🎱 AI Lucky Pinball</h1>
          <p>오늘 운이 없는 사람에게 작은 행운을</p>
        </div>
        <nav className="tabs">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
            참가자 / 게임 화면
          </NavLink>
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
            관리자 화면
          </NavLink>
        </nav>
      </aside>
      <main className="main-content">
        <div className="page">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
