# 17. State와 useState — `main.js`의 `state` 객체가 똑똑해진다

## 1. 지금 코드의 상태 관리

`main.js` 맨 위에 이런 게 있습니다.

```js
const state = {
  players: [],
  fortunes: new Map(),
};
```

그리고 이 값을 바꿀 때마다, **관련된 화면을 다시 그리는 함수를 손으로 호출**합니다.

```js
state.players.push(player);
renderPlayerList();        // 참가자 목록 화면 갱신
updateFortuneButtonState(); // 버튼 활성/비활성 갱신
```

문제는 `state.players`가 바뀌는 곳이 늘어날수록(등록할 때, 삭제할 때...), **"이 값을 바꿨으면 어떤 화면들을 다시 그려야 하는지"를 매번 다 기억하고 있어야 한다**는 점입니다. 잊어버리면 화면과 실제 데이터가 어긋나는 버그가 생깁니다.

## 2. `useState` — "이 값이 바뀌면 자동으로 다시 그려줘"

React에서는 이렇게 씁니다.

```jsx
import { useState } from 'react';

function GamePage() {
  const [players, setPlayers] = useState([]); // 초기값 []

  function handleRegister(newPlayer) {
    setPlayers([...players, newPlayer]); // "players를 이 새 배열로 바꿔줘"
  }

  return (
    <div>
      <PlayerList players={players} />
      {/* players가 바뀌면 PlayerList는 알아서 다시 그려진다 — render 함수를 따로 안 불러도 됨 */}
    </div>
  );
}
```

`useState(초기값)`을 호출하면 **[현재 값, 그 값을 바꾸는 함수]** 두 개짜리 배열을 돌려줍니다 (그래서 `const [players, setPlayers] = ...`처럼 배열 구조 분해로 받는 게 관례입니다). `setPlayers(...)`를 호출하면:

1. `players`라는 값이 새 값으로 바뀌고,
2. 이 컴포넌트(그리고 이 값을 쓰는 자식들)가 **자동으로 다시 렌더링**됩니다.

`renderPlayerList()`를 직접 호출할 필요가 없어집니다 — **"어떤 함수를 다시 불러야 하는지" 자체를 생각할 필요가 없어지는 게 핵심 이득**입니다.

## 3. 중요한 규칙: 기존 값을 "직접 수정"하면 안 된다

바닐라 JS에서는 `state.players.push(newPlayer)`처럼 배열을 **직접 변형(mutate)** 하는 게 자연스러웠습니다. React에서는 이렇게 하면 안 됩니다.

```jsx
// ❌ 이렇게 하면 React가 "값이 바뀐 걸" 못 알아차릴 수 있다
players.push(newPlayer);
setPlayers(players); // 같은 배열 참조라 React가 "달라졌다"고 인식 못 할 수 있음

// ✅ 항상 "새 배열/새 객체"를 만들어서 넘긴다
setPlayers([...players, newPlayer]);
```

React는 "이전 값과 새 값이 **같은 참조인지 다른 참조인지**"를 비교해서 다시 그릴지 말지 판단합니다(참조 비교). 그래서 배열/객체는 **항상 복사본을 만들어서** 넘기는 습관이 필요합니다 — 스프레드 문법(`[...players, x]`, `{...obj, key: value}`)이 이래서 React 코드에 자주 등장합니다.

`main.js`의 삭제 로직도 같은 방식으로 바뀝니다.

```js
// 지금 방식
state.players = state.players.filter((p) => p.playerId !== player.playerId);
renderPlayerList();

// React 방식
setPlayers(players.filter((p) => p.playerId !== player.playerId));
// filter()는 원래 새 배열을 만드는 함수라 자연스럽게 규칙을 따름
```

사실 `main.js`가 이미 `.filter()`, `.map()`처럼 원본을 바꾸지 않는(불변, immutable) 배열 메서드 위주로 짜여 있어서, React로 옮길 때 이 부분에서는 거의 새로 배울 게 없습니다.

## 4. `fortunes`(Map)는 어떻게 되는가

지금 `state.fortunes`는 `Map`인데, React state로 Map/Set을 쓰는 건 가능은 하지만 매번 새 Map을 복사해야 해서(`new Map(fortunes).set(...)`) 번거롭습니다. 이 프로젝트 규모에서는 **일반 배열이나 객체로 바꾸는 게 더 React스럽습니다** — 예를 들어 `fortunes`를 아예 별도 state로 두지 않고, `results`(운세 확인 API 응답 배열) 자체를 `useState`로 들고 있다가 `FortuneCard`에 그대로 넘겨도 충분합니다. 이런 사소한 자료구조 선택은 실제 마이그레이션 시점에 정리하면 됩니다.

## 5. 여러 개의 state, 언제 나누고 언제 합칠까

지금 `main.js`는 상태가 사실상 `players`, `fortunes` 둘뿐이라 고민할 게 적지만, 일반적인 기준은 이렇습니다.

- **서로 독립적으로 바뀌는 값**은 따로 `useState`로 (예: `players`와 `resultSection`이 보이는지 여부는 서로 무관하게 바뀜 → 따로).
- **항상 같이 바뀌는 값**은 객체 하나로 묶어도 됨. 다만 객체로 묶으면 일부만 바꿀 때도 스프레드로 나머지를 복사해야 해서(`setForm({...form, name: e.target.value})`), 아주 강하게 묶여있지 않다면 그냥 나누는 편이 코드가 단순할 때가 많습니다.

이 프로젝트는 상태 개수가 적어서(`players`, `results`, `game`, `result` 정도), 복잡한 상태관리 라이브러리(Redux 등) 없이 `useState` 몇 개로 충분하다고 `plan/06_react-migration-plan.md`에서 이미 판단해뒀습니다.

---

다음 문서: [`18_useEffect와_useRef.md`](./18_useEffect와_useRef.md) — "컴포넌트가 화면에 나타났을 때 한 번 실행할 코드"(API 호출, Matter.js 시작 등)와, React가 모르게 직접 DOM을 만지고 싶을 때 쓰는 탈출구.
