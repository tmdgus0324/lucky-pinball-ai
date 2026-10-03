import { useEffect, useRef, type ReactNode } from 'react';
import { createRoot, type Root } from 'react-dom/client';

/**
 * 예제를 앱과 분리된 별도의 React 루트에서 실행한다.
 *
 * 이 앱은 전체가 <StrictMode>로 감싸져 있는데, 개발 모드의 StrictMode는 componentDidMount·useEffect를
 * 일부러 두 번 실행한다. 생명주기를 console.log로 보여주는 예제(6, 7장)가 개발 모드에서만 로그가 두 배로
 * 찍혀 오해를 부르므로, StrictMode 바깥의 루트에서 돌려 원본 index.js(React 17)와 같은 동작을 보여준다.
 * (배포 빌드에서는 StrictMode의 이중 실행이 없어서 개발/배포 결과가 같아진다.)
 */
export function IsolatedRoot({ children }: { children: ReactNode }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const rootRef = useRef<Root | null>(null);

  useEffect(() => {
    // 효과가 다시 실행될 때마다(StrictMode 포함) 새 div에 새 루트를 만든다 — 같은 div에 createRoot를 두 번 부르지 않게.
    const host = document.createElement('div');
    containerRef.current?.appendChild(host);
    const root = createRoot(host);
    rootRef.current = root;

    return () => {
      rootRef.current = null;
      // 렌더링 도중에 동기적으로 unmount하면 경고가 나므로 한 박자 늦춘다.
      queueMicrotask(() => {
        root.unmount();
        host.remove();
      });
    };
  }, []);

  // 부모가 다시 렌더링될 때마다 최신 children을 별도 루트에 반영한다.
  useEffect(() => {
    rootRef.current?.render(children);
  });

  return <div ref={containerRef} />;
}
