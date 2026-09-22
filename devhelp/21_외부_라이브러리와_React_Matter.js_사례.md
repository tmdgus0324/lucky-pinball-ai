# 21. 외부 라이브러리를 React 안에서 쓰기 — Matter.js 실전 사례

지금까지(15~20번) 배운 걸 전부 합쳐서, `plan/06_react-migration-plan.md`가 "가장 까다로운 지점"이라고 짚었던 `game.js` → `PinballBoard` 컴포넌트 전환을 실제 코드로 끝까지 보여드립니다. 이 문서가 이해되면 마이그레이션에 필요한 핵심 지식은 다 갖춘 셈입니다.

## 1. 원칙: "Matter.js 관련 코드는 거의 그대로 둔다"

`game.js`는 이미 아주 잘 설계돼 있습니다 — `MatterAdapter` 클래스, `buildBoard()`, `spawnX()`/`spawnY()`, `runPinballGame()` 같은 함수들이 **DOM 요소 하나(`boardEl`)만 있으면** 동작하도록 짜여 있습니다. React로 옮길 때 이 로직을 "React식으로 다시 쓰려는" 유혹을 참아야 합니다 — devhelp 09/11/12에서 실제로 겪은 물리 버그(벽 끼임, 즉시 낙하 등)를 다시 밟을 위험이 있기 때문입니다. **할 일은 "이 함수에게 어떤 DOM 요소를 넘겨줄지"를 React가 관리하도록 바꾸는 것뿐**입니다.

## 2. 전체 비교

### 지금 (main.js + game.js)

```js
// main.js
els.startGameBtn.addEventListener('click', async () => {
  const game = await api.createGame(playerIds);
  await api.startGame(game.gameId);

  await runPinballGame({
    boardEl: els.pinballBoard,        // document.getElementById('pinballBoard')로 미리 구해둔 DOM
    rankListEl: els.rankList,
    participants: game.participants,
    onComplete: async (finishOrder) => {
      const result = await api.reportResult(game.gameId, finishOrder);
      showResult(result);
    },
  });
});
```

### React로 옮긴 뒤

```tsx
// components/PinballBoard.tsx
import { useEffect, useRef } from 'react';
import { runPinballGame } from '../game/engine'; // game.js를 옮긴 파일 (내용은 거의 동일)
import type { GameParticipant } from '../api/client';

interface PinballBoardProps {
  participants: GameParticipant[];
  onComplete: (finishOrder: number[]) => void;
}

export function PinballBoard({ participants, onComplete }: PinballBoardProps) {
  const boardRef = useRef<HTMLDivElement>(null);
  const rankListRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!boardRef.current || !rankListRef.current) return;

    let cancelled = false;

    runPinballGame({
      boardEl: boardRef.current,
      rankListEl: rankListRef.current,
      participants,
      onComplete: (finishOrder) => {
        if (!cancelled) onComplete(finishOrder); // 언마운트 후 콜백 실행 방지
      },
    });

    return () => {
      cancelled = true;
      // MatterAdapter.dispose()는 게임이 끝나면 runPinballGame 내부에서 이미 호출됨.
      // 다만 "게임 도중에 컴포넌트가 갑자기 사라지는" 경우까지 대비하려면
      // runPinballGame이 자신의 adapter를 ref로 밖에 노출하게 만들고 여기서도 dispose() 호출 —
      // 18번 문서의 "StrictMode 이중 실행" 항목과 직결되는 부분이라 실제 구현 시 신경 써야 함.
    };
  }, [participants]); // participants가 바뀌면(새 게임 시작) effect를 다시 실행

  return (
    <div className="game-layout">
      <div ref={boardRef} className="pinball-board" />
      <aside className="rank-panel">
        <h3>실시간 순위</h3>
        <div ref={rankListRef} />
      </aside>
    </div>
  );
}
```

```tsx
// pages/GamePage.tsx (사용하는 쪽)
function GamePage() {
  const [game, setGame] = useState<GameCreateResponse | null>(null);
  const [result, setResult] = useState<GameResultView | null>(null);

  async function handleStartGame(playerIds: number[]) {
    const g = await api.createGame(playerIds);
    await api.startGame(g.gameId);
    setGame(g); // ← 이 state가 바뀌면 PinballBoard가 새로 마운트되면서 게임이 시작됨
  }

  return (
    <>
      {/* ... RegistrationForm, FortuneSection ... */}
      {game && (
        <PinballBoard
          participants={game.participants}
          onComplete={async (finishOrder) => {
            const r = await api.reportResult(game.gameId, finishOrder);
            setResult(r);
          }}
        />
      )}
      {result && <ResultBanner result={result} />}
    </>
  );
}
```

## 3. 눈여겨볼 점

- **`runPinballGame` 함수 시그니처가 하나도 안 바뀌었습니다** — `boardEl`, `rankListEl`, `participants`, `onComplete`를 받는 건 지금과 동일합니다. 단지 `boardEl`을 "미리 구해둔 전역 DOM"이 아니라 "React가 `ref`로 넘겨준 DOM"으로 받을 뿐입니다.
- `game && <PinballBoard ... />`처럼 **조건부 렌더링**으로 "게임이 생성됐을 때만 컴포넌트를 마운트"하는 방식을 썼습니다. `game`이 `null`에서 실제 값으로 바뀌는 순간 `PinballBoard`가 마운트되고, `useEffect`가 실행되면서 게임이 시작됩니다 — 지금의 "버튼 클릭 → `runPinballGame` 직접 호출"과 결과적으로 똑같은 타이밍이지만, "state가 바뀌면 화면(과 그 화면이 촉발하는 부수효과)이 따라간다"는 React식 흐름을 타는 것입니다.
- `cancelled` 플래그를 쓴 이유: 만약 게임이 끝나기 전에 사용자가 페이지를 벗어나면(예: 라우터로 관리자 화면 이동), 이미 사라진 컴포넌트의 `onComplete`가 뒤늦게 불려서 오류가 나는 걸 방지하기 위함입니다. 18번 문서의 cleanup 함수 개념이 실전에서 이런 식으로도 쓰입니다.

## 4. TypeScript 타입은 어디서 가져오는가

`GameParticipant`, `GameResultView` 같은 타입은 20번 문서에서 설명한 대로, 백엔드 응답 모양(`plan/02_api-spec.md`)을 그대로 옮겨서 `api/client.ts`에 정의해두고 재사용합니다. 새로 지어내는 게 아니라 **이미 있는 계약 문서를 타입으로 옮기는 작업**이라고 생각하면 됩니다.

---

## 보너스: React Router로 페이지 전환하기

지금 두 화면은 `index.html`/`admin.html` 두 파일과, 각 파일 안의 `<nav class="tabs"><a href="admin.html">...</a></nav>`로 이동합니다. React Router를 쓰면 이렇게 바뀝니다.

```tsx
// App.tsx
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <nav className="tabs">
        <Link to="/">참가자 / 게임 화면</Link>
        <Link to="/admin">관리자 화면</Link>
      </nav>
      <Routes>
        <Route path="/" element={<GamePage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </BrowserRouter>
  );
}
```

`<a href="...">`가 `<Link to="...">`로, 페이지 전체를 새로 불러오는 대신 **필요한 부분만 바꿔치기**됩니다(페이지 깜빡임 없음, 브라우저 전체 새로고침이 아님). `05_improvement-backlog.md`의 A1(좌측 사이드바 메뉴)도 이 `<nav>` 부분을 사이드바 레이아웃으로 바꾸기만 하면 되는 구조라, 구조 변경이 생각보다 간단합니다.

---

여기까지가 이번 마이그레이션에 필요한 React 기초 지식입니다. 실제 착수는 `plan/06_react-migration-plan.md`의 작업 순서(뼈대 세팅 → API 이식 → 관리자 화면 → 게임 화면 → `PinballBoard`)를 따라가면 되고, 진행하면서 새로 알게 되는 내용은 지금까지처럼 devhelp 문서(22번부터)로 이어서 기록합니다.
