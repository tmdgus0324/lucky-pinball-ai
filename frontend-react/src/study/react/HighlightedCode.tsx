import { Suspense, lazy } from 'react';

// 하이라이터는 코드를 처음 보여줄 때만 내려받는다(lazy). 핀볼 첫 화면과 데모 탭은 이 코드를 받지 않는다.
const Highlighter = lazy(() => import('./Highlighter'));

interface HighlightedCodeProps {
  code: string;
  /** 마크다운 코드 펜스의 언어(jsx, js, css, html, json …). 모르는 언어면 색 없이 보여준다. */
  language?: string;
  lineNumbers?: boolean;
}

export function HighlightedCode({ code, language, lineNumbers }: HighlightedCodeProps) {
  // 하이라이터가 도착하기 전에도 코드는 바로 읽을 수 있게, 같은 모양의 색 없는 코드를 먼저 보여준다.
  const plain = (
    <pre className={lineNumbers ? 'code-block with-lines' : 'code-block'}>
      <code>{code}</code>
    </pre>
  );

  return (
    <Suspense fallback={plain}>
      <Highlighter code={code} language={language} lineNumbers={lineNumbers} />
    </Suspense>
  );
}
