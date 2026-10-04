# 04. React 개념 정리

Vanilla JS 버전을 React로 옮기기 전에, 이 프로젝트 코드를 예시로 React의 핵심 개념을 정리한 학습 노트다.

> 원래 따로 있던 기록 7개를 2026-10-04에 하나로 묶었다. 내용은 그대로 두고 시간 순서대로 이어 붙였으며, 각 절 제목의 "[구 NN]"이 원래 문서 번호다.

**포함된 기록**

- [구 15] React — 사고방식이 왜 다른가 (JSX 첫걸음)
- [구 16] 컴포넌트와 Props — "화면 조각을 만드는 함수"의 진화형
- [구 17] State와 useState — `main.js`의 `state` 객체가 똑똑해진다
- [구 18] useEffect와 useRef — 이 프로젝트에서 가장 중요한 두 가지 훅
- [구 19] 이벤트 처리, 폼(제어 컴포넌트), 리스트 렌더링
- [구 20] TypeScript와 Vite — 개발 환경이 실제로 뭐가 달라지는가
- [구 21] 외부 라이브러리를 React 안에서 쓰기 — Matter.js 실전 사례


> **AI 코멘트 — 이 노트를 어떻게 활용하면 좋은가**
>
> **학습 페이지와의 관계**: 이 노트는 "이 프로젝트의 Vanilla 코드(`main.js`, `game.js`)를 React로 옮기면 어떻게 되나"를 기준으로 쓴 것이고, 공부하기 > React 페이지(`devhelp/11`)는 "강의 예제" 기준입니다. 같은 개념을 서로 다른 각도에서 다뤄서 함께 보면 좋습니다. 학습 페이지의 정리 탭에는 요즘 기준의 AI 코멘트(예: `useEffect`를 "동기화"로 이해하기)가 있습니다.
>
> **이 노트의 경고가 실제로 일어난 사례**
>
> - [구 18]의 "개발 모드 StrictMode 이중 실행" 경고는 나중에 학습 페이지를 만들 때 실제로 터졌습니다. 생명주기를 `console.log`로 보여주는 예제의 로그가 두 배로 찍혀서, 데모만 StrictMode 밖의 별도 루트에서 실행하도록 바꿨습니다(`devhelp/11`). 미리 적어 둔 덕분에 원인을 바로 알아볼 수 있었습니다. 이 노트는 React 18 기준으로 썼지만 지금 쓰는 React 19도 같은 동작입니다.
> - [구 21]의 "Matter.js 코드는 거의 그대로 두고 React는 마운트·정리만 맡는다"는 원칙은 지금도 `PinballBoard`의 구조 그대로입니다. 물리 엔진을 React state로 옮기지 않은 판단 덕분에, 이후 물리 튜닝(`devhelp/02`)을 React와 무관하게 할 수 있었습니다.
>
> **지금 코드 기준으로 더 보면 좋은 주제** (이 노트에는 없지만 프로젝트에서 실제로 쓰거나 개선 여지가 있는 것)
>
> | 주제 | 프로젝트에서 | 볼 점 |
> |---|---|---|
> | 데이터 불러오기 | 관리자 표들이 `useEffect` 안에서 직접 `fetch`하고 결과를 state에 넣음 | 화면을 떠난 뒤 늦게 도착한 응답을 무시하는 처리(취소 플래그·`AbortController`), 로딩·오류 상태를 한 패턴으로 묶기 |
> | 코드 분할 | 학습 페이지와 하이라이터를 `React.lazy` + `Suspense`로 필요할 때만 불러옴 | 첫 화면 번들 크기를 지키는 방법(`devhelp/11`) |
> | `key`로 컴포넌트 새로 만들기 | `PinballBoard`에 `key={gameId}`를 줘서 새 게임마다 보드를 새로 만듦 | [구 19]의 리스트 `key`와 같은 원리를 "초기화" 용도로 쓰는 것 |
> | 상태 위치 | `GamePage`가 참가자 목록을 들고 자식에게 내려줌 | state 끌어올리기의 실제 사례. 여기서 더 깊어지면 Context나 상태 관리 도구를 고려 |
>
> **면접 대비 한 줄**: "Vanilla에서 React로 옮길 때 무엇이 쉬워지고 무엇이 어려워졌나"를 물으면, 쉬워진 것은 화면 상태 동기화(선언형 렌더링), 어려워진 것은 **React 밖에서 DOM을 직접 다루는 물리 엔진과의 경계**(마운트·정리, 이중 실행)라고 이 노트와 [구 21]을 근거로 답할 수 있습니다.

---

## [구 15] React — 사고방식이 왜 다른가 (JSX 첫걸음)

> **이 문서부터 21번까지는 지금까지의 devhelp 문서(1~14번, "우리가 뭘 했는지"에 대한 기록)와는 성격이 다릅니다.** 아직 React 마이그레이션은 시작하지 않았고, `plan/06_react-migration-plan.md`를 실제로 진행하기 전에 미리 읽어볼 수 있도록 만든 **React 입문 가이드**입니다. 자바스크립트(바닐라 JS)는 익숙하지만 React는 처음이라는 전제로, 지금 이 프로젝트의 실제 코드(`main.js`, `admin.js`, `game.js`)와 나란히 비교하면서 설명합니다.

### 1. 가장 큰 차이: "명령형" vs "선언형"

지금 `main.js`가 참가자 목록을 화면에 그리는 방식을 보면:

```js
// main.js — 지금 방식 (명령형: "어떻게" 할지 하나하나 지시)
function renderPlayerList() {
  els.playerList.innerHTML = '';               // 1. 기존 내용을 지운다
  state.players.forEach((player) => {
    const chip = document.createElement('span'); // 2. 요소를 만든다
    chip.className = 'player-chip';
    chip.innerHTML = `${player.name} · ${player.birthDate}`;
    els.playerList.appendChild(chip);            // 3. 화면에 붙인다
  });
}

// 그리고 데이터가 바뀔 때마다, 이 함수를 다시 호출해야 화면이 갱신된다
state.players.push(newPlayer);
renderPlayerList(); // ← 이걸 깜빡하면 화면이 안 바뀜
```

이 방식의 특징: **"데이터가 바뀌었으니 화면을 다시 그려줘"를 개발자가 매번 직접 챙겨야 합니다.** `state.players`를 바꾸고 `renderPlayerList()` 호출을 깜빡하면, 데이터와 화면이 서로 어긋나는 버그가 생깁니다. 실제로 이 프로젝트에서도 비슷한 종류의 버그가 있었죠 — `devhelp/02(구 12)`에서 고친 "게임 시작 버튼이 다시 활성화 안 되는" 문제도, "상태가 바뀌었는데 화면 쪽 코드를 안 건드려서" 생긴 문제였습니다.

React는 이 순서를 뒤집습니다.

```jsx
// React 방식 (선언형: "무엇을" 보여줄지만 기술)
function PlayerList({ players }) {
  return (
    <div className="player-list">
      {players.map((player) => (
        <span key={player.playerId} className="player-chip">
          {player.name} · {player.birthDate}
        </span>
      ))}
    </div>
  );
}
```

여기엔 `createElement`도, `innerHTML`도, "다시 그리기" 호출도 없습니다. **"players 배열이 주어지면 화면은 항상 이렇게 생겨야 한다"는 규칙만 적어두면**, `players` 값이 바뀔 때 화면을 실제로 갱신하는 건 React가 알아서 합니다. 이게 "선언형(declarative)"이라는 말의 의미입니다 — "어떻게 갱신할지"가 아니라 "결과가 어떤 모습이어야 하는지"만 적습니다.

### 2. JSX란 무엇인가

위 코드에서 `<div className="player-list">...</div>`처럼 자바스크립트 함수 안에 HTML처럼 생긴 걸 쓰는 게 **JSX**입니다. 이건 새로운 언어가 아니라, 결국 이런 자바스크립트 함수 호출로 "번역"되는 문법 설탕(syntactic sugar)입니다.

```jsx
// 이렇게 쓰면
<span className="player-chip">{player.name}</span>

// 실제로는 이렇게 변환된다 (빌드 도구가 자동으로 해줌)
React.createElement('span', { className: 'player-chip' }, player.name)
```

여러분이 지금 `` `<span class="player-chip">${player.name}</span>` `` 같은 **템플릿 문자열**로 HTML을 만들던 것과 목적은 비슷하지만, 결정적인 차이가 있습니다: 템플릿 문자열은 그냥 "문자열"이라 오타가 나도 실행해봐야 알지만, JSX는 실제 자바스크립트 함수 호출로 변환되기 때문에 **빌드 시점에 문법 오류를 잡아줍니다** (그리고 TypeScript를 함께 쓰면 타입 오류까지).

#### JSX의 몇 가지 규칙 (바닐라 JS와 헷갈리기 쉬운 부분)

| 바닐라 JS/HTML | JSX | 이유 |
|---|---|---|
| `class="player-chip"` | `className="player-chip"` | `class`는 자바스크립트 예약어라서 못 씀 |
| `onclick="..."` (문자열) | `onClick={handleClick}` (함수 참조) | 실제 함수를 직접 연결, 카멜케이스 |
| `<input>` (닫는 태그 없어도 됨) | `<input />` (반드시 self-close) | JSX는 모든 태그가 정확히 닫혀야 함 |
| 여러 요소를 나란히 반환 불가 | 반드시 **하나의 루트**로 감싸야 함(`<div>...</div>` 또는 `<>...</>`) | 함수는 값을 하나만 반환할 수 있으므로 |
| `${표현식}` | `{표현식}` | 중괄호 안에 순수 자바스크립트 표현식(식)을 그대로 쓸 수 있음 — `if`문은 안 되고 삼항연산자(`? :`)나 `&&`는 됨 |

### 3. 왜 이렇게 바뀌는 게 이득인가 (이 프로젝트 관점에서)

`admin.js`를 보면 참가자 목록/게임 목록/로그 목록, 이렇게 "배열을 받아서 반복해서 그리는" 패턴이 3번 반복됩니다 (`loadPlayers`, `loadGames`, `loadLogs` — 각각 `.map().join('')`으로 HTML 문자열을 조립). React에서는 이게 각각 "props로 배열을 받아서 JSX를 반환하는 함수(컴포넌트)"가 되는데, `<PlayersTable players={players} />`처럼 **재사용 가능한 조각**으로 명확히 나뉩니다. `05_improvement-backlog.md`의 A3(페이징)를 나중에 추가할 때도, "표를 그리는 로직"과 "페이지 번호 상태"를 분리하기가 지금 구조보다 훨씬 쉬워집니다.

---

---

## [구 16] 컴포넌트와 Props — "화면 조각을 만드는 함수"의 진화형

### 1. 지금 코드에도 이미 "컴포넌트"와 비슷한 게 있다

`game.js`를 보면 이런 함수가 있습니다.

```js
// game.js — 공 하나를 나타내는 DOM 요소를 만드는 함수
function createBallEl(sceneEl, name, colorIndex) {
  const palette = ['#6b7280', '#4ade80', /* ... */];
  const el = document.createElement('div');
  el.className = 'ball';
  el.style.background = palette[colorIndex % palette.length];
  el.textContent = name;
  sceneEl.appendChild(el);
  return el;
}
```

이 함수는 사실 이미 "컴포넌트"의 정신에 아주 가깝습니다 — **입력값(name, colorIndex)을 받아서 화면 조각을 만들어내는 함수**니까요. React의 "컴포넌트"는 정확히 이 아이디어를 언어 차원에서 정식으로 지원하는 것입니다.

```jsx
// React 컴포넌트 버전
function Ball({ name, colorIndex }) {
  const palette = ['#6b7280', '#4ade80', /* ... */];
  return (
    <div className="ball" style={{ background: palette[colorIndex % palette.length] }}>
      {name}
    </div>
  );
}

// 사용할 때
<Ball name="홍길동" colorIndex={0} />
```

차이점:
- `createBallEl`은 **DOM 요소를 직접 만들어서 붙이는(부수효과, side effect)** 함수입니다. `Ball`은 **JSX(화면이 어떻게 생겨야 하는지에 대한 설명)를 반환만 하는** 함수입니다 — 실제로 DOM에 붙이는 건 React가 알아서 합니다.
- 함수 인자 `(sceneEl, name, colorIndex)` 대신, React는 **하나의 객체**(`{ name, colorIndex }`)로 몰아서 받습니다. 이걸 **props**(properties의 줄임말)라고 부릅니다. JSX에서 `<Ball name="..." colorIndex={0} />`처럼 속성처럼 쓴 값들이 함수 안에서 `props.name`, `props.colorIndex`로(위 예시처럼 구조 분해하면 `{ name, colorIndex }`) 들어옵니다.

### 2. Props는 "읽기 전용"이다

가장 중요한 규칙: **컴포넌트는 자기가 받은 props를 직접 바꾸면 안 됩니다.** props는 "부모가 나에게 준 설정값"이라, 자식이 마음대로 고치면 안 됩니다 (자바스크립트 함수에서 매개변수를 함수 안에서 재할당하지 않는 것과 비슷한 관례인데, React에서는 이게 **규칙**입니다). 값을 바꾸고 싶으면 다음 문서([구 17])에서 다룰 **state**를 씁니다.

### 3. 지금 코드의 다른 "화면 조각 함수"들도 컴포넌트가 된다

`main.js`의 `renderFortuneGrid`를 보면:

```js
// main.js — 지금 방식
function renderFortuneGrid(results) {
  results.forEach((fortune) => {
    const player = state.players.find((p) => p.playerId === fortune.playerId);
    const card = document.createElement('div');
    card.className = `fortune-card tier-${fortune.buff.tier}`;
    // ... innerHTML 조립 ...
    els.fortuneGrid.appendChild(card);
  });
}
```

이건 React에서 **두 개의 컴포넌트**로 자연스럽게 나뉩니다 — "카드 하나"와 "카드들을 감싸는 그리드".

```jsx
function FortuneCard({ fortune, playerName }) {
  const buffLabel = fortune.buff.tier === 0 ? '버프 없음' : `버프 ${fortune.buff.tier}`;
  return (
    <div className={`fortune-card tier-${fortune.buff.tier}`}>
      <div className="name">{playerName}</div>
      {fortune.source === 'NONE' ? (
        <div className="message">생년월일을 입력하지 않아 운세 없이 참여합니다</div>
      ) : (
        <>
          <div className="score">{fortune.fortuneScore}점</div>
          <div className="message">"{fortune.fortuneMessage}"</div>
        </>
      )}
      <div className="stats">
        <span>{buffLabel}</span>
        <span>시작 높이 +{fortune.buff.startY}</span>
      </div>
    </div>
  );
}

function FortuneGrid({ results, players }) {
  return (
    <div className="fortune-grid">
      {results.map((fortune) => (
        <FortuneCard
          key={fortune.playerId}
          fortune={fortune}
          playerName={players.find((p) => p.playerId === fortune.playerId)?.name}
        />
      ))}
    </div>
  );
}
```

`main.js`에서 `fortune.source === 'NONE' ? ... : ...`처럼 삼항연산자를 그대로 썼던 것(`scoreBlock` 변수)도 JSX 안에서 거의 똑같은 모양으로 쓸 수 있다는 걸 눈여겨보세요 — 로직 자체를 새로 배울 필요는 없고, "이걸 문자열 조립 대신 JSX로 표현한다"는 문법만 바뀝니다.

### 4. 컴포넌트를 나누는 기준

정해진 규칙은 없지만, 이 프로젝트에서 자연스러운 기준은 **"지금 코드에서 이미 별도 함수로 뽑혀 있는 화면 단위"**입니다. `plan/06_react-migration-plan.md`의 컴포넌트 분리안(`RegistrationForm`, `PlayerList`, `FortuneCard`, `PlayersTable` 등)도 정확히 이 기준 — 지금 `render...()` 함수들의 경계를 그대로 따라간 것입니다. 즉 **"뭘 컴포넌트로 나눌지 새로 고민할 필요 없이, 지금 있는 `render` 함수들을 하나씩 컴포넌트로 옮기면 된다"**고 생각하면 마이그레이션이 훨씬 덜 막막하게 느껴질 겁니다.

---

---

## [구 17] State와 useState — `main.js`의 `state` 객체가 똑똑해진다

### 1. 지금 코드의 상태 관리

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

### 2. `useState` — "이 값이 바뀌면 자동으로 다시 그려줘"

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

### 3. 중요한 규칙: 기존 값을 "직접 수정"하면 안 된다

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

### 4. `fortunes`(Map)는 어떻게 되는가

지금 `state.fortunes`는 `Map`인데, React state로 Map/Set을 쓰는 건 가능은 하지만 매번 새 Map을 복사해야 해서(`new Map(fortunes).set(...)`) 번거롭습니다. 이 프로젝트 규모에서는 **일반 배열이나 객체로 바꾸는 게 더 React스럽습니다** — 예를 들어 `fortunes`를 아예 별도 state로 두지 않고, `results`(운세 확인 API 응답 배열) 자체를 `useState`로 들고 있다가 `FortuneCard`에 그대로 넘겨도 충분합니다. 이런 사소한 자료구조 선택은 실제 마이그레이션 시점에 정리하면 됩니다.

### 5. 여러 개의 state, 언제 나누고 언제 합칠까

지금 `main.js`는 상태가 사실상 `players`, `fortunes` 둘뿐이라 고민할 게 적지만, 일반적인 기준은 이렇습니다.

- **서로 독립적으로 바뀌는 값**은 따로 `useState`로 (예: `players`와 `resultSection`이 보이는지 여부는 서로 무관하게 바뀜 → 따로).
- **항상 같이 바뀌는 값**은 객체 하나로 묶어도 됨. 다만 객체로 묶으면 일부만 바꿀 때도 스프레드로 나머지를 복사해야 해서(`setForm({...form, name: e.target.value})`), 아주 강하게 묶여있지 않다면 그냥 나누는 편이 코드가 단순할 때가 많습니다.

이 프로젝트는 상태 개수가 적어서(`players`, `results`, `game`, `result` 정도), 복잡한 상태관리 라이브러리(Redux 등) 없이 `useState` 몇 개로 충분하다고 `plan/06_react-migration-plan.md`에서 이미 판단해뒀습니다.

---

---

## [구 18] useEffect와 useRef — 이 프로젝트에서 가장 중요한 두 가지 훅

이 문서는 이번 마이그레이션에서 **가장 핵심적인 두 개념**을 다룹니다. 특히 `useEffect`는 `PinballBoard`(Matter.js를 감싸는 컴포넌트) 설계의 핵심이라, `plan/06_react-migration-plan.md`에서 "가장 까다로운 지점"으로 짚었던 부분과 바로 연결됩니다.

### 1. `useEffect` — "화면에 나타났을 때 한 번 실행되는 코드"

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

### 2. `useRef` — "React 몰래 진짜 DOM을 직접 만지고 싶을 때"

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

### 3. cleanup 함수 — "컴포넌트가 사라질 때 뒷정리"

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

### 4. ⚠️ 실전 함정: React 개발 모드의 "이중 실행"

React 18의 `<StrictMode>`(Vite로 새 프로젝트를 만들면 기본으로 켜져 있음)는 **개발 모드에서만**, 버그를 미리 잡기 위해 일부러 `useEffect`를 "실행 → cleanup → 다시 실행"으로 **두 번** 돌립니다. cleanup 함수를 제대로 안 만들어뒀다면:

1. 첫 번째 effect 실행 → Matter.js 물리 시뮬레이션 시작 (World A)
2. StrictMode가 일부러 cleanup 호출 → **cleanup이 비어있으면 World A는 계속 돎**
3. 두 번째 effect 실행 → 물리 시뮬레이션이 또 시작됨 (World B)
4. **결과: 공이 2배로 겹쳐서 떨어지는, 디버깅하기 아주 괴로운 버그**

이건 실제로 이 패턴을 처음 쓸 때 아주 흔하게 겪는 문제라 미리 적어둡니다. 해결책은 위 3번처럼 **`adapter.dispose()`를 cleanup 함수 안에서 반드시 호출**하는 것 — 그러면 StrictMode의 "일부러 두 번 실행"도 안전하게 통과합니다 (오히려 이 테스트를 통과한다는 것 자체가 "정리 코드가 제대로 있다"는 걸 검증해주는 셈입니다).

---

---

## [구 19] 이벤트 처리, 폼(제어 컴포넌트), 리스트 렌더링

### 1. 이벤트 처리 — `addEventListener` 대신 JSX 속성

지금 `main.js`는 이렇게 이벤트를 연결합니다.

```js
els.form.addEventListener('submit', async (event) => {
  event.preventDefault();
  // ...
});

els.fortuneBtn.addEventListener('click', async () => {
  // ...
});
```

React에서는 JSX 태그에 직접 이벤트 속성(`onClick`, `onSubmit` 등, 카멜케이스)으로 연결합니다.

```jsx
function RegistrationForm({ onRegister }) {
  function handleSubmit(event) {
    event.preventDefault(); // 똑같이 필요함 — 기본 폼 제출(새로고침) 막기
    // ...
    onRegister(name, birthDate); // 부모에게 "등록해줘"라고 알림 (아래 3번 참고)
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* ... */}
    </form>
  );
}
```

`addEventListener`로 리스너를 "붙이고 떼는" 관리를 직접 할 필요가 없어집니다 — 컴포넌트가 사라지면 React가 알아서 정리합니다.

### 2. 폼 입력값 — "제어 컴포넌트(Controlled Component)"

지금 코드는 필요할 때(제출 시점에) `input.value`를 읽습니다.

```js
// main.js — "제출할 때 가서 읽는다"
const name = els.nameInput.value.trim();
const rawBirth = els.birthInput.value.trim();
```

React에서 권장하는 방식은 **입력값 자체를 state로 관리**하는 것입니다 — 이걸 "제어 컴포넌트"라고 부릅니다.

```jsx
function RegistrationForm({ onRegister }) {
  const [name, setName] = useState('');
  const [birthRaw, setBirthRaw] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    // name, birthRaw는 이미 state에 최신값으로 들어있음 — 따로 DOM에서 읽어올 필요 없음
    onRegister(name, birthRaw);
    setName('');       // 제출 후 입력창 비우기
    setBirthRaw('');
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={name}                              // state가 "정답"이 됨 (React가 DOM을 이 값으로 유지)
        onChange={(e) => setName(e.target.value)}  // 사용자가 타이핑 -> state 갱신
        placeholder="이름 / 별칭"
      />
      <input
        value={birthRaw}
        onChange={(e) => setBirthRaw(e.target.value)}
        placeholder="생년월일 6자리 (선택, 예: 880324)"
        inputMode="numeric"
        maxLength={6}
      />
      <button type="submit">참가자 등록</button>
    </form>
  );
}
```

이렇게 하는 이유: "화면에 보이는 값"과 "실제 데이터(state)"가 항상 같다는 걸 보장할 수 있습니다. 지금 `main.js`의 `parseBirthDateShorthand(raw)` 함수는 그대로 재사용 가능합니다 — `handleSubmit` 안에서 `birthRaw`를 그 함수에 넘겨서 변환하면 됩니다. **검증/변환 로직 자체는 하나도 새로 짤 필요가 없고, "어디서 값을 읽어오는지"만 바뀝니다.**

> 참고: 모든 입력을 꼭 state로 관리해야 하는 건 아닙니다(폼이 아주 간단하면 지금처럼 제출 시점에 읽는 "비제어 컴포넌트" 방식도 `useRef`로 가능합니다). 다만 이 프로젝트처럼 입력값을 검증하고 에러 메시지를 보여줘야 하는 폼은, 제어 컴포넌트 방식이 React에서 더 일반적이고 다루기 쉽습니다.

### 3. 자식 → 부모로 데이터 전달: "콜백 props"

위 예시에서 `<RegistrationForm onRegister={...} />`처럼 **함수를 props로 내려주는** 패턴이 나왔습니다. React는 데이터가 항상 부모 → 자식으로 흐르기 때문에(단방향 데이터 흐름), 자식이 "부모의 상태를 바꿔야 하는" 상황(예: 등록 버튼을 눌렀을 때 부모가 들고 있는 `players` 배열에 추가)에는 **부모가 함수를 만들어서 자식에게 props로 내려주고, 자식은 그 함수를 호출**하는 방식을 씁니다.

```jsx
function GamePage() {
  const [players, setPlayers] = useState([]);

  async function handleRegister(name, birthRaw) {
    const birthDate = birthRaw ? parseBirthDateShorthand(birthRaw) : null;
    if (birthRaw && !birthDate) {
      // 에러 처리
      return;
    }
    const player = await api.registerPlayer(name, birthDate);
    setPlayers([...players, player]);
  }

  return (
    <>
      <RegistrationForm onRegister={handleRegister} />
      <PlayerList players={players} />
    </>
  );
}
```

`RegistrationForm`은 "등록"이 실제로 어떻게 처리되는지(API 호출, state 갱신)는 전혀 모릅니다 — 그냥 `onRegister(name, birthDate)`를 호출할 뿐입니다. 이렇게 **"실제 로직은 부모(GamePage)가 갖고 있고, 자식은 언제 그걸 불러야 하는지만 안다"**는 구조가 React에서 컴포넌트를 나누는 전형적인 방식입니다.

### 4. 리스트 렌더링과 `key`

`.map()`으로 JSX 배열을 만드는 건 여러분이 `main.js`/`admin.js`에서 이미 수없이 써온 `.map()`/`.forEach()`와 사실상 똑같습니다.

```jsx
{players.map((player) => (
  <PlayerChip key={player.playerId} player={player} onRemove={handleRemove} />
))}
```

여기서 `key`가 낯설 텐데, **"이 리스트의 각 항목을 구분하는 고유한 값"**을 React에게 알려주는 용도입니다. React는 리스트가 바뀔 때(추가/삭제/순서변경) "어떤 항목이 그대로고 어떤 게 새로/사라졌는지" 판단해야 하는데, `key`가 없으면 이걸 순서(인덱스)로만 추측하다가 엉뚱한 항목이 갱신되는 버그가 생길 수 있습니다. 다행히 이 프로젝트의 모든 리스트(참가자, 운세 카드, 참가자 표 행, 순위 항목)는 이미 `playerId`/`gameId`처럼 명확한 고유 ID가 있어서, `key={player.playerId}`처럼 그 값을 그대로 쓰면 됩니다 — **배열 인덱스(`key={index}`)는 되도록 쓰지 마세요**, 항목이 삭제/재정렬될 때 버그의 원인이 되기 쉽습니다.

---

---

## [구 20] TypeScript와 Vite — 개발 환경이 실제로 뭐가 달라지는가

지금까지는 **React 문법**(JSX, 컴포넌트, state, effect) 얘기였다면, 이 문서는 그걸 실제로 "어떤 도구로 실행/빌드하는지"에 대한 이야기입니다.

### 1. 지금과 가장 크게 달라지는 점: 빌드 과정이 생긴다

지금 `frontend/`는 빌드 도구가 전혀 없습니다 — `index.html`을 열면(또는 정적 서버로 서빙하면) `<script src="js/main.js">`가 브라우저에서 그대로 실행됩니다. JSX나 TypeScript는 브라우저가 직접 이해할 수 없는 문법이라, **빌드 도구(Vite)가 이걸 브라우저가 이해할 수 있는 순수 JS로 미리 변환**해줘야 합니다.

```
지금:      index.html → <script src="js/main.js"> → 브라우저가 그대로 실행
React 이후: npm run dev  → Vite가 JSX/TS를 변환하며 실시간으로 서빙 (개발 중)
           npm run build → dist/ 폴더에 순수 HTML/CSS/JS로 "구워낸" 결과물 생성 (배포용)
```

### 2. Vite 프로젝트의 기본 파일 구조

```
frontend-react/
├── package.json          # 지금 backend/build.gradle과 같은 역할 — 의존성 목록 + 실행 명령어
├── vite.config.ts         # Vite 설정 (프록시, 빌드 옵션 등)
├── tsconfig.json          # TypeScript 컴파일 규칙
├── index.html             # 지금과 달리, <script>가 아니라 <script type="module" src="/src/main.tsx">만 있음
└── src/
    ├── main.tsx           # 앱의 진입점 — React를 실제 DOM에 "붙이는" 코드
    ├── App.tsx            # 라우팅 등 최상위 컴포넌트
    ├── pages/
    │   ├── GamePage.tsx
    │   └── AdminPage.tsx
    ├── components/
    │   ├── RegistrationForm.tsx
    │   ├── PinballBoard.tsx
    │   └── ...
    ├── api/
    │   └── client.ts       # 지금의 api.js
    └── styles/
        └── style.css       # 지금의 style.css (1단계는 그대로 가져옴)
```

`package.json`의 주요 명령어(지금의 `./gradlew bootRun`, `./gradlew test`에 대응):

```bash
npm install     # 의존성 설치 (backend의 ./gradlew build가 의존성을 자동으로 받아오는 것과 비슷)
npm run dev     # 개발 서버 실행 (지금의 npx serve frontend 대체, 코드 바꾸면 자동 새로고침(HMR)까지 됨)
npm run build   # dist/ 폴더에 배포용 정적 파일 생성
```

### 3. TypeScript — "이 값이 어떤 모양인지" 미리 적어두는 것

TypeScript는 자바스크립트에 **타입**을 추가한 언어입니다. 지금 `api.js`의 함수를 보면:

```js
// api.js — 지금 (순수 JS, 어떤 모양의 데이터가 오가는지는 주석이나 문서를 봐야 앎)
getFortune(playerId) {
  return request('/fortune', { method: 'POST', body: JSON.stringify({ playerId }) });
}
```

이 함수가 정확히 어떤 모양의 객체를 반환하는지는 `plan/02_api-spec.md` 문서를 봐야 알 수 있습니다. TypeScript를 쓰면 이걸 **코드 자체에** 적어둘 수 있습니다.

```ts
// api/client.ts — TypeScript 버전
interface Buff {
  tier: number;
  startY: number;
}

interface FortuneResponse {
  playerId: number;
  fortuneScore: number | null;   // devhelp/03(구 13)에서 다뤘듯, 생년월일 없으면 null
  luckyNumber: number | null;
  fortuneMessage: string | null;
  buff: Buff;
  source: 'AI' | 'CACHE' | 'NONE'; // 이 세 값 중 하나만 가능하다고 못박아둠
}

async function getFortune(playerId: number): Promise<FortuneResponse> {
  return request('/fortune', { method: 'POST', body: JSON.stringify({ playerId }) });
}
```

이렇게 해두면 좋은 점:
- 에디터가 `fortune.fortuneScore`를 자동완성으로 제안해주고, 존재하지 않는 필드(`fortune.score`처럼 오타)를 쓰면 **실행하기 전에** 빨간 줄로 알려줍니다.
- `fortuneScore`가 `number | null`이라고 타입에 적어뒀기 때문에, `fortune.fortuneScore.toFixed(0)`처럼 null 체크 없이 쓰면 컴파일러가 "null일 수도 있는데?"라고 미리 경고해줍니다 — 실제로 `devhelp/03(구 13)`에서 다뤘던 "생년월일 없으면 null" 케이스를 놓치는 버그를 미리 막아주는 셈입니다.
- 백엔드(`FortuneController.FortuneResponse` — Java record)와 프론트엔드 타입이 **구조적으로 나란히 대응**하게 되어서, API 응답 모양이 바뀌면 타입 정의도 같이 고쳐야 한다는 게 명확해집니다.

#### 얼마나 깊이 알아야 하는가

이 프로젝트 수준에서 필요한 TypeScript는 많지 않습니다. 주로 쓰게 될 것:
- `interface`(또는 `type`)로 객체 모양 정의 — 위 예시가 전부입니다.
- 함수 매개변수/반환값에 타입 적기 — `function foo(x: number): string`.
- Props 타입 — `function Ball({ name, colorIndex }: { name: string; colorIndex: number })`.
- 제네릭은 몰라도 됩니다(`useState<Player[]>([])`처럼 아주 가끔 등장하는 정도만 알면 충분).

### 4. CORS 관련 — 잊지 말아야 할 설정 한 가지

Vite 개발 서버는 기본적으로 `http://localhost:5173`에서 뜹니다. 지금 `WebConfig`(백엔드)의 CORS 허용 목록에는 `5500`, `5501`만 있으므로, React 프로젝트를 실제로 실행해보기 전에 **`5173`(또는 실제로 뜬 포트)을 백엔드 CORS 설정에 추가**해야 API 호출이 막히지 않습니다. `plan/06_react-migration-plan.md`의 1단계("뼈대 세팅")에 이미 이 내용이 포함되어 있습니다.

---

---

## [구 21] 외부 라이브러리를 React 안에서 쓰기 — Matter.js 실전 사례

지금까지(15~20번) 배운 걸 전부 합쳐서, `plan/06_react-migration-plan.md`가 "가장 까다로운 지점"이라고 짚었던 `game.js` → `PinballBoard` 컴포넌트 전환을 실제 코드로 끝까지 보여드립니다. 이 문서가 이해되면 마이그레이션에 필요한 핵심 지식은 다 갖춘 셈입니다.

### 1. 원칙: "Matter.js 관련 코드는 거의 그대로 둔다"

`game.js`는 이미 아주 잘 설계돼 있습니다 — `MatterAdapter` 클래스, `buildBoard()`, `spawnX()`/`spawnY()`, `runPinballGame()` 같은 함수들이 **DOM 요소 하나(`boardEl`)만 있으면** 동작하도록 짜여 있습니다. React로 옮길 때 이 로직을 "React식으로 다시 쓰려는" 유혹을 참아야 합니다 — devhelp/02(구 09·11·12)에서 실제로 겪은 물리 버그(벽 끼임, 즉시 낙하 등)를 다시 밟을 위험이 있기 때문입니다. **할 일은 "이 함수에게 어떤 DOM 요소를 넘겨줄지"를 React가 관리하도록 바꾸는 것뿐**입니다.

### 2. 전체 비교

#### 지금 (main.js + game.js)

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

#### React로 옮긴 뒤

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

### 3. 눈여겨볼 점

- **`runPinballGame` 함수 시그니처가 하나도 안 바뀌었습니다** — `boardEl`, `rankListEl`, `participants`, `onComplete`를 받는 건 지금과 동일합니다. 단지 `boardEl`을 "미리 구해둔 전역 DOM"이 아니라 "React가 `ref`로 넘겨준 DOM"으로 받을 뿐입니다.
- `game && <PinballBoard ... />`처럼 **조건부 렌더링**으로 "게임이 생성됐을 때만 컴포넌트를 마운트"하는 방식을 썼습니다. `game`이 `null`에서 실제 값으로 바뀌는 순간 `PinballBoard`가 마운트되고, `useEffect`가 실행되면서 게임이 시작됩니다 — 지금의 "버튼 클릭 → `runPinballGame` 직접 호출"과 결과적으로 똑같은 타이밍이지만, "state가 바뀌면 화면(과 그 화면이 촉발하는 부수효과)이 따라간다"는 React식 흐름을 타는 것입니다.
- `cancelled` 플래그를 쓴 이유: 만약 게임이 끝나기 전에 사용자가 페이지를 벗어나면(예: 라우터로 관리자 화면 이동), 이미 사라진 컴포넌트의 `onComplete`가 뒤늦게 불려서 오류가 나는 걸 방지하기 위함입니다. 18번 문서의 cleanup 함수 개념이 실전에서 이런 식으로도 쓰입니다.

### 4. TypeScript 타입은 어디서 가져오는가

`GameParticipant`, `GameResultView` 같은 타입은 20번 문서에서 설명한 대로, 백엔드 응답 모양(`plan/02_api-spec.md`)을 그대로 옮겨서 `api/client.ts`에 정의해두고 재사용합니다. 새로 지어내는 게 아니라 **이미 있는 계약 문서를 타입으로 옮기는 작업**이라고 생각하면 됩니다.

---

### 보너스: React Router로 페이지 전환하기

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
