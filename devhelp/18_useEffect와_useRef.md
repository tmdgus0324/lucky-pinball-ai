# 18. useEffect와 useRef — 이 프로젝트에서 가장 중요한 두 가지 훅

이 문서는 이번 마이그레이션에서 **가장 핵심적인 두 개념**을 다룹니다. 특히 `useEffect`는 `PinballBoard`(Matter.js를 감싸는 컴포넌트) 설계의 핵심이라, `plan/06_react-migration-plan.md`에서 "가장 까다로운 지점"으로 짚었던 부분과 바로 연결됩니다.

## 1. `useEffect` — "화면에 나타났을 때 한 번 실행되는 코드"

`admin.js` 맨 아래를 보면 이런 코드가 있습니다.

```js
// admin.js — 페이지가 로드되면 즉시 실행
loadPlayers();
loadGames();
loadLogs();
```

이건 "스크립트 파일이 실행되면 바로 API를 호출해서 데이터를 가져온다"는 뜻인데, React 컴포넌트에서는 이 코드를 함수 본문에 그냥 적으면 안 됩니다 — 컴포넌트 함수는 **렌더링될 때마다(자주) 다시 실행**되기 때문에, API 호출 같은 "한 번만 일어나야 하는 부수효과(side effect)"를 그 안에 직접 적으면 렌더링될 때마다 API를 또 부르게 됩니다.

그래서 이런 "렌더링과 별개로 일어나야 하는 일"을 위한 전용 훅이 `useEffect`입니다.

```jsx
import { useState, useEffect } from 'react';

function AdminPage() {
  const [players, setPlayers] = useState([]);

  useEffect(() => {
    api.adminGetPlayers().then(setPlayers);
  }, []); // ← 이 배열("의존성 배열")이 비어있으면 "처음 화면에 나타났을 때 딱 한 번만" 실행

  return <PlayersTable players={players} />;
}
```

`useEffect(콜백함수, 의존성배열)` 구조입니다. 의존성 배열에 뭘 넣느냐에 따라 실행 시점이 달라집니다.

| 의존성 배열 | 언제 실행되는가 |
|---|---|
| 아예 생략 | 렌더링될 때마다 매번 (거의 안 씀) |
| `[]` (빈 배열) | 컴포넌트가 처음 화면에 나타났을 때 **딱 한 번** — `admin.js`의 `loadPlayers()` 등과 같은 타이밍 |
| `[gameId]` | 처음 나타났을 때 + **이후 `gameId` 값이 바뀔 때마다** 다시 실행 |

## 2. `useRef` — "React 몰래 진짜 DOM을 직접 만지고 싶을 때"

지금 `main.js`는 이렇게 DOM 요소를 직접 가져다 씁니다.

```js
const els = {
  pinballBoard: document.getElementById('pinballBoard'),
  // ...
};
```

React에서는 보통 `document.getElementById`를 직접 쓰지 않고(React가 알아서 DOM을 관리하기 때문), 대신 **"이 JSX 태그가 실제로 어떤 DOM 노드가 됐는지"를 참조**하고 싶을 때 `useRef`를 씁니다.

```jsx
import { useRef, useEffect } from 'react';

function PinballBoard({ participants, onComplete }) {
  const boardRef = useRef(null); // 처음엔 비어있음(null)

  useEffect(() => {
    // 이 시점엔 boardRef.current가 실제 <div> DOM 노드를 가리킨다
    runPinballGame({
      boardEl: boardRef.current, // 지금 game.js가 받는 것과 똑같은 DOM 요소!
      participants,
      onComplete,
    });
  }, []); // 컴포넌트가 화면에 나타났을 때 한 번만 게임 시작

  return <div ref={boardRef} className="pinball-board" />;
}
```

`<div ref={boardRef} />`처럼 JSX 태그에 `ref` 속성을 달아두면, React가 그 태그에 대응하는 **진짜 DOM 노드**를 `boardRef.current`에 넣어줍니다. 이게 `plan/06_react-migration-plan.md`에서 말한 "React 섬(island)" 패턴의 실제 구현 방법입니다 — `runPinballGame(...)` 함수 자체는 지금 `game.js`와 **거의 그대로**, `boardEl`로 이 `boardRef.current`(진짜 DOM)를 받아서 여전히 `document.createElement`/`style.left` 같은 방식으로 동작하면 됩니다. React는 "이 DOM 노드를 찾아서 넘겨주는 것"까지만 관여하고, 그 안에서 벌어지는 일(공 위치 갱신 등)은 예전처럼 순수 JS가 맡습니다.

## 3. cleanup 함수 — "컴포넌트가 사라질 때 뒷정리"

`useEffect`의 콜백 함수가 **함수를 반환**하면, 그게 "이 effect를 정리할 때 실행할 코드"가 됩니다.

```jsx
useEffect(() => {
  const adapter = new MatterAdapter();
  // ... 게임 시작 ...

  return () => {
    adapter.dispose(); // 컴포넌트가 사라지거나, effect가 다시 실행되기 직전에 호출됨
  };
}, []);
```

`game.js`의 `MatterAdapter.dispose()`(Runner 정지 + World 정리)가 정확히 이 cleanup 함수 자리에 들어가야 합니다. 이걸 빠뜨리면 어떻게 되는지가 아래 함정입니다.

## 4. ⚠️ 실전 함정: React 개발 모드의 "이중 실행"

React 18의 `<StrictMode>`(Vite로 새 프로젝트를 만들면 기본으로 켜져 있음)는 **개발 모드에서만**, 버그를 미리 잡기 위해 일부러 `useEffect`를 "실행 → cleanup → 다시 실행"으로 **두 번** 돌립니다. cleanup 함수를 제대로 안 만들어뒀다면:

1. 첫 번째 effect 실행 → Matter.js 물리 시뮬레이션 시작 (World A)
2. StrictMode가 일부러 cleanup 호출 → **cleanup이 비어있으면 World A는 계속 돎**
3. 두 번째 effect 실행 → 물리 시뮬레이션이 또 시작됨 (World B)
4. **결과: 공이 2배로 겹쳐서 떨어지는, 디버깅하기 아주 괴로운 버그**

이건 실제로 이 패턴을 처음 쓸 때 아주 흔하게 겪는 문제라 미리 적어둡니다. 해결책은 위 3번처럼 **`adapter.dispose()`를 cleanup 함수 안에서 반드시 호출**하는 것 — 그러면 StrictMode의 "일부러 두 번 실행"도 안전하게 통과합니다 (오히려 이 테스트를 통과한다는 것 자체가 "정리 코드가 제대로 있다"는 걸 검증해주는 셈입니다).

---

다음 문서: [`19_이벤트_폼_리스트_렌더링.md`](./19_이벤트_폼_리스트_렌더링.md) — `regForm`의 submit 처리와 `.map()` 리스트 렌더링이 JSX에서 어떻게 쓰이는지.
