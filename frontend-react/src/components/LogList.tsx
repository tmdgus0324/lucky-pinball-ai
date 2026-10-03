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
      <p className="desc">서버가 재시작되어도 남는 최근 오류 목록입니다 (DB 저장, 최근 1000건 유지, 화면에는 200건).</p>
      <div className="log-list">
        {error && <div className="log-item">불러오기 실패: {error}</div>}
        {!error && logs === null && <div className="log-item">불러오는 중...</div>}
        {!error && logs !== null && logs.length === 0 && <div className="log-item">기록된 오류가 없습니다.</div>}
        {!error &&
          logs?.map((log, index) => (
            <div className="log-item" key={index}>
              <span className="path">{log.path}</span>
              {log.traceId && (
                <span className="trace-id" title="서버 로그에서 이 ID로 같은 요청의 로그를 찾을 수 있습니다">
                  #{log.traceId}
                </span>
              )}
              {log.message} · {log.timestamp}
            </div>
          ))}
      </div>
    </section>
  );
}
