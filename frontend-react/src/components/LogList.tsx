import { useEffect, useState } from 'react';
import { api, isUnauthorizedError, type AdminLog } from '../api/client';

interface LogListProps {
  reloadKey: number;
  onUnauthorized: () => void;
}

export function LogList({ reloadKey, onUnauthorized }: LogListProps) {
  const [logs, setLogs] = useState<AdminLog[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .adminGetLogs()
      .then(setLogs)
      .catch((err: Error) => {
        if (isUnauthorizedError(err)) {
          onUnauthorized();
          return;
        }
        setError(err.message);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadKey]);

  return (
    <section className="panel">
      <h2>
        오류 로그 <span className="badge">GET /api/admin/logs</span>
      </h2>
      <p className="desc">서버 재시작 전까지만 유지되는 인메모리 로그입니다 (영속화는 Phase 2).</p>
      <div className="log-list">
        {error && <div className="log-item">불러오기 실패: {error}</div>}
        {!error && logs === null && <div className="log-item">불러오는 중...</div>}
        {!error && logs !== null && logs.length === 0 && <div className="log-item">기록된 오류가 없습니다.</div>}
        {!error &&
          logs?.map((log, index) => (
            <div className="log-item" key={index}>
              <span className="path">{log.path}</span>
              {log.message} · {log.timestamp}
            </div>
          ))}
      </div>
    </section>
  );
}
