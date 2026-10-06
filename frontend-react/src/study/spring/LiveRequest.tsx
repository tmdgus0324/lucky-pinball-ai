import { useState } from 'react';
import { BACKEND_ORIGIN } from '../../api/client';
import { HighlightedCode } from '../react/HighlightedCode';
import type { LiveRequestSpec } from './chapters';

interface Result {
  status: number;
  elapsedMs: number;
  body: string;
}

function pretty(text: string): string {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

/**
 * 핀볼 백엔드에 실제 요청을 보내고 응답을 그대로 보여준다.
 * 잘못된 입력이나 없는 주소처럼 데이터를 저장하지 않는 요청만 쓴다.
 */
function LiveRequestItem({ spec }: { spec: LiveRequestSpec }) {
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function send() {
    setSending(true);
    setError(null);
    const started = performance.now();
    try {
      const response = await fetch(BACKEND_ORIGIN + spec.path, {
        method: spec.method,
        headers: spec.body === undefined ? undefined : { 'Content-Type': 'application/json' },
        body: spec.body === undefined ? undefined : JSON.stringify(spec.body),
      });
      const body = await response.text();
      setResult({ status: response.status, elapsedMs: Math.round(performance.now() - started), body: pretty(body) });
    } catch (e) {
      setResult(null);
      setError(`요청을 보내지 못했습니다(${(e as Error).message}). 서버가 쉬고 있으면 첫 요청에 1~2분 걸릴 수 있습니다.`);
    } finally {
      setSending(false);
    }
  }

  const request =
    `${spec.method} ${spec.path}` + (spec.body === undefined ? '' : `\nContent-Type: application/json\n\n${JSON.stringify(spec.body, null, 2)}`);

  return (
    <div className="live-request">
      <div className="panel-head">
        <h3>{spec.label}</h3>
        <button type="button" disabled={sending} onClick={send}>
          {sending ? '보내는 중...' : '보내 보기'}
        </button>
      </div>
      <p className="desc">{spec.note}</p>
      <HighlightedCode code={request} language="http" />
      {error && <p className="status-text error">{error}</p>}
      {result && (
        <>
          <p className="live-status">
            응답 <strong>{result.status}</strong> · {result.elapsedMs}ms
          </p>
          <HighlightedCode code={result.body || '(본문 없음)'} language="json" />
        </>
      )}
    </div>
  );
}

export function LiveRequest({ specs }: { specs: LiveRequestSpec[] }) {
  return (
    <div className="live-requests">
      <p className="desc">
        이 사이트의 핀볼 게임 백엔드({BACKEND_ORIGIN})에 실제로 요청을 보냅니다. 데이터를 저장하지 않는 요청만 준비했습니다.
      </p>
      {specs.map((spec) => (
        <LiveRequestItem spec={spec} key={spec.label} />
      ))}
    </div>
  );
}
