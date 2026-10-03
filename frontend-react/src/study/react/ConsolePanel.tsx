import { useEffect, useRef, useState } from 'react';

/**
 * 생명주기·useEffect 챕터는 console.log로 동작을 보여준다. 개발자 도구를 열지 않아도 보이도록,
 * 이 패널이 화면에 떠 있는 동안만 console.log를 가로채 화면에도 같이 출력한다(원래 콘솔 출력은 그대로 둔다).
 * 로그를 비우는 "다시 실행"은 부모가 key를 바꿔 이 컴포넌트를 새로 마운트하는 것으로 처리한다.
 */
export function ConsolePanel() {
  const [lines, setLines] = useState<string[]>([]);
  const boxRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const original = console.log;
    console.log = (...args: unknown[]) => {
      original(...args);
      const text = args.map((arg) => (typeof arg === 'string' ? arg : JSON.stringify(arg))).join(' ');
      // 렌더링 도중 다른 컴포넌트의 state를 바꾸지 않도록 한 박자 늦춰서 반영한다.
      queueMicrotask(() => setLines((prev) => [...prev.slice(-199), text]));
    };
    return () => {
      console.log = original;
    };
  }, []);

  useEffect(() => {
    const box = boxRef.current;
    if (box) box.scrollTop = box.scrollHeight;
  }, [lines]);

  return (
    <div className="console-panel">
      <div className="console-head">
        <span>콘솔 출력</span>
        <button type="button" className="secondary" onClick={() => setLines([])}>
          지우기
        </button>
      </div>
      <div className="console-lines" ref={boxRef}>
        {lines.length === 0 ? <span className="console-empty">아직 출력이 없습니다.</span> : null}
        {lines.map((line, index) => (
          <div key={index}>{line}</div>
        ))}
      </div>
    </div>
  );
}
