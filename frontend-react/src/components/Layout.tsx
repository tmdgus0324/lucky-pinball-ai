import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';

/**
 * 좌측 사이드바 내비게이션 (05번 백로그 A1).
 * 기존 vanilla 버전의 상단 중앙 탭(nav.tabs)을 좌측 세로 메뉴로 재배치했다.
 */
export function Layout() {
  const { pathname } = useLocation();
  // 공부하기 화면에 있으면 그룹을 펼친 채로 시작한다. 직접 접고 펼친 뒤에는 그 선택을 따른다.
  const [studyOpen, setStudyOpen] = useState(pathname.startsWith('/study'));
  const inStudy = pathname.startsWith('/study');

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <h1>🎱 AI Lucky Pinball</h1>
          <p>오늘 운이 없는 사람에게 작은 행운을</p>
        </div>
        <nav className="tabs">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
            게임하기
          </NavLink>
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
            관리하기
          </NavLink>
          <div className="nav-group">
            <button
              type="button"
              className={'nav-group-toggle' + (inStudy ? ' active' : '')}
              aria-expanded={studyOpen}
              onClick={() => setStudyOpen((open) => !open)}
            >
              공부하기 <span aria-hidden="true">{studyOpen ? '▾' : '▸'}</span>
            </button>
            {studyOpen && (
              <div className="nav-sub">
                <NavLink to="/study/react" className={({ isActive }) => (isActive ? 'active' : '')}>
                  React
                </NavLink>
              </div>
            )}
          </div>
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
