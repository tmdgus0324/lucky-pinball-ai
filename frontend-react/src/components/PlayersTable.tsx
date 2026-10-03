import { useEffect, useMemo, useState } from 'react';
import { api, isUnauthorizedError, type AdminPlayer } from '../api/client';
import { Pagination } from './Pagination';

const PAGE_SIZE = 20;

const SOURCE_LABELS: Record<string, string> = {
  AI: 'AI 호출',
  CACHE: '기존 데이터',
  FALLBACK: '임시 점수(AI 실패)',
};

interface PlayersTableProps {
  /** 401(로그인 만료/무효)을 받으면 로그인 화면으로 돌려보내는 콜백. */
  onUnauthorized: () => void;
}

export function PlayersTable({ onUnauthorized }: PlayersTableProps) {
  const [players, setPlayers] = useState<AdminPlayer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    api
      .adminGetPlayers()
      .then(setPlayers)
      .catch((err: Error) => {
        if (isUnauthorizedError(err)) {
          onUnauthorized();
          return;
        }
        setError(err.message);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const totalPages = useMemo(
    () => (players ? Math.max(1, Math.ceil(players.length / PAGE_SIZE)) : 1),
    [players],
  );

  const pageItems = useMemo(() => {
    if (!players) return [];
    const start = (page - 1) * PAGE_SIZE;
    return players.slice(start, start + PAGE_SIZE);
  }, [players, page]);

  return (
    <section className="panel">
      <h2>
        참가자 목록 <span className="badge">GET /api/admin/players</span>
      </h2>
      <p className="desc">각 참가자의 운세 이력(회차별 점수)을 함께 보여줍니다.</p>
      <table className="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>이름</th>
            <th>생년월일</th>
            <th>AI 호출</th>
            <th>운세 이력 (날짜 · 점수 · 출처)</th>
          </tr>
        </thead>
        <tbody>
          {error && (
            <tr>
              <td colSpan={5}>불러오기 실패: {error}</td>
            </tr>
          )}
          {!error && players === null && (
            <tr>
              <td colSpan={5}>불러오는 중...</td>
            </tr>
          )}
          {!error && players !== null && players.length === 0 && (
            <tr>
              <td colSpan={5}>등록된 참가자가 없습니다.</td>
            </tr>
          )}
          {!error &&
            pageItems.map((player) => (
              <tr key={player.playerId}>
                <td>{player.playerId}</td>
                <td>{player.name}</td>
                <td>{player.birthDate || '미입력'}</td>
                <td>
                  {player.fortuneSource === 'AI' ? 'Y' : player.fortuneSource === 'CACHE' ? 'N' : player.fortuneSource === 'FALLBACK' ? '임시' : '-'}
                </td>
                <td>
                  {player.fortuneHistory.length
                    ? player.fortuneHistory
                        .map((h) => `${h.createdDate} · ${h.fortuneScore} · ${SOURCE_LABELS[h.source] ?? h.source}`)
                        .join('  →  ')
                    : '이력 없음'}
                </td>
              </tr>
            ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </section>
  );
}
