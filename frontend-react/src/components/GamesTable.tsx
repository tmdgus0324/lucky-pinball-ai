import { useEffect, useState } from 'react';
import { api, isUnauthorizedError, type AdminGame } from '../api/client';
import { Pagination } from './Pagination';
import { usePagination } from '../hooks/usePagination';

const PAGE_SIZE = 20;

interface GamesTableProps {
  onUnauthorized: () => void;
}

export function GamesTable({ onUnauthorized }: GamesTableProps) {
  const [games, setGames] = useState<AdminGame[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .adminGetGames()
      // 서버 목록은 정렬이 정해져 있지 않아서(최신 게임이 맨 아래로 가곤 했다) 참가자 목록처럼 최신순으로 맞춘다.
      .then((list) => setGames([...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt))))
      .catch((err: Error) => {
        if (isUnauthorizedError(err)) {
          onUnauthorized();
          return;
        }
        setError(err.message);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { page, totalPages, pageItems, setPage } = usePagination(games, PAGE_SIZE);

  return (
    <section className="panel">
      <h2>
        게임 결과 목록 <span className="badge">GET /api/admin/games</span>
      </h2>
      <table className="admin-table">
        <thead>
          <tr>
            <th>게임 ID</th>
            <th>당첨자</th>
            <th>참가 인원</th>
            <th>생성 시각</th>
          </tr>
        </thead>
        <tbody>
          {error && (
            <tr>
              <td colSpan={4}>불러오기 실패: {error}</td>
            </tr>
          )}
          {!error && games === null && (
            <tr>
              <td colSpan={4}>불러오는 중...</td>
            </tr>
          )}
          {!error && games !== null && games.length === 0 && (
            <tr>
              <td colSpan={4}>아직 진행된 게임이 없습니다.</td>
            </tr>
          )}
          {!error &&
            pageItems.map((game) => (
              <tr key={game.gameId}>
                <td>{game.gameId}</td>
                <td>{game.selectedName || '(진행 중)'}</td>
                <td>{game.participantCount}</td>
                <td>{game.createdAt}</td>
              </tr>
            ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </section>
  );
}
