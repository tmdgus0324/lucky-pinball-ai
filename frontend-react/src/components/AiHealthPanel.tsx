import { useState } from 'react';
import { api, isUnauthorizedError, type AiHealth } from '../api/client';

const STATUS_LABELS: Record<string, string> = {
  OK: '정상',
  KEY_MISSING: 'API 키 없음',
  AUTH_FAILED: '인증 실패',
  CREDIT_EXHAUSTED: '크레딧 부족',
  RATE_LIMITED: '요청 한도 초과',
  TIMEOUT: '응답 지연',
  UNREACHABLE: '연결 불가',
  UPSTREAM_ERROR: 'Anthropic 서버 오류',
  ERROR: '오류',
};

export function AiHealthPanel({ onUnauthorized }: { onUnauthorized: () => void }) {
  const [checking, setChecking] = useState(false);
  const [result, setResult] = useState<AiHealth | null>(null);
  // 점검 결과가 아니라 "점검 요청 자체"가 실패한 경우(너무 자주 눌러 429, 서버 다운 등).
  const [requestError, setRequestError] = useState<string | null>(null);

  async function handleCheck() {
    setChecking(true);
    setRequestError(null);
    try {
      setResult(await api.adminCheckAi());
    } catch (error) {
      if (isUnauthorizedError(error)) {
        onUnauthorized();
        return;
      }
      setRequestError((error as Error).message);
    } finally {
      setChecking(false);
    }
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>
          AI 연결 확인 <span className="badge">POST /api/admin/ai-health</span>
        </h2>
        <button type="button" onClick={handleCheck} disabled={checking}>
          {checking ? '확인 중...' : 'AI 연결 확인'}
        </button>
      </div>
      <p className="desc">
        Claude API에 아주 짧은 요청(출력 1토큰)을 실제로 보내 지금 되는지 확인합니다. 게임을 돌려보지 않아도 키·크레딧·네트워크
        문제를 구분해서 볼 수 있습니다. 유료 호출이라 5초 간격으로 제한됩니다.
      </p>

      {checking && <p className="status-text">Claude API에 요청 중입니다. 최대 {result?.timeoutSeconds ?? 10}초 걸릴 수 있습니다...</p>}
      {requestError && <p className="status-text error">점검을 실행하지 못했습니다: {requestError}</p>}

      {result && !checking && (
        <div className={`health-result ${result.ok ? 'ok' : 'fail'}`}>
          <div className="health-title">
            {result.ok ? '✅' : '⚠️'} {STATUS_LABELS[result.status] ?? result.status}
          </div>
          <div>{result.message}</div>
          <div className="health-meta">
            응답 {result.latencyMillis}ms
            {result.upstreamStatus != null && ` · HTTP ${result.upstreamStatus}`} · 모델 {result.model} · 제한 시간{' '}
            {result.timeoutSeconds}초 · {new Date(result.checkedAt).toLocaleTimeString('ko-KR')} 확인
            <br />
            AI 실패 시 임시 점수로 진행: {result.fallbackEnabled ? '켜짐 (사용자는 게임을 계속할 수 있습니다)' : '꺼짐 (사용자에게 오류가 표시됩니다)'}
          </div>
        </div>
      )}
    </section>
  );
}
