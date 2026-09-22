# 15. React — 사고방식이 왜 다른가 (JSX 첫걸음)

> **이 문서부터 21번까지는 지금까지의 devhelp 문서(1~14번, "우리가 뭘 했는지"에 대한 기록)와는 성격이 다릅니다.** 아직 React 마이그레이션은 시작하지 않았고, `plan/06_react-migration-plan.md`를 실제로 진행하기 전에 미리 읽어볼 수 있도록 만든 **React 입문 가이드**입니다. 자바스크립트(바닐라 JS)는 익숙하지만 React는 처음이라는 전제로, 지금 이 프로젝트의 실제 코드(`main.js`, `admin.js`, `game.js`)와 나란히 비교하면서 설명합니다.

## 1. 가장 큰 차이: "명령형" vs "선언형"

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

이 방식의 특징: **"데이터가 바뀌었으니 화면을 다시 그려줘"를 개발자가 매번 직접 챙겨야 합니다.** `state.players`를 바꾸고 `renderPlayerList()` 호출을 깜빡하면, 데이터와 화면이 서로 어긋나는 버그가 생깁니다. 실제로 이 프로젝트에서도 비슷한 종류의 버그가 있었죠 — `devhelp/12`에서 고친 "게임 시작 버튼이 다시 활성화 안 되는" 문제도, "상태가 바뀌었는데 화면 쪽 코드를 안 건드려서" 생긴 문제였습니다.

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

## 2. JSX란 무엇인가

위 코드에서 `<div className="player-list">...</div>`처럼 자바스크립트 함수 안에 HTML처럼 생긴 걸 쓰는 게 **JSX**입니다. 이건 새로운 언어가 아니라, 결국 이런 자바스크립트 함수 호출로 "번역"되는 문법 설탕(syntactic sugar)입니다.

```jsx
// 이렇게 쓰면
<span className="player-chip">{player.name}</span>

// 실제로는 이렇게 변환된다 (빌드 도구가 자동으로 해줌)
React.createElement('span', { className: 'player-chip' }, player.name)
```

여러분이 지금 `` `<span class="player-chip">${player.name}</span>` `` 같은 **템플릿 문자열**로 HTML을 만들던 것과 목적은 비슷하지만, 결정적인 차이가 있습니다: 템플릿 문자열은 그냥 "문자열"이라 오타가 나도 실행해봐야 알지만, JSX는 실제 자바스크립트 함수 호출로 변환되기 때문에 **빌드 시점에 문법 오류를 잡아줍니다** (그리고 TypeScript를 함께 쓰면 타입 오류까지).

### JSX의 몇 가지 규칙 (바닐라 JS와 헷갈리기 쉬운 부분)

| 바닐라 JS/HTML | JSX | 이유 |
|---|---|---|
| `class="player-chip"` | `className="player-chip"` | `class`는 자바스크립트 예약어라서 못 씀 |
| `onclick="..."` (문자열) | `onClick={handleClick}` (함수 참조) | 실제 함수를 직접 연결, 카멜케이스 |
| `<input>` (닫는 태그 없어도 됨) | `<input />` (반드시 self-close) | JSX는 모든 태그가 정확히 닫혀야 함 |
| 여러 요소를 나란히 반환 불가 | 반드시 **하나의 루트**로 감싸야 함(`<div>...</div>` 또는 `<>...</>`) | 함수는 값을 하나만 반환할 수 있으므로 |
| `${표현식}` | `{표현식}` | 중괄호 안에 순수 자바스크립트 표현식(식)을 그대로 쓸 수 있음 — `if`문은 안 되고 삼항연산자(`? :`)나 `&&`는 됨 |

## 3. 왜 이렇게 바뀌는 게 이득인가 (이 프로젝트 관점에서)

`admin.js`를 보면 참가자 목록/게임 목록/로그 목록, 이렇게 "배열을 받아서 반복해서 그리는" 패턴이 3번 반복됩니다 (`loadPlayers`, `loadGames`, `loadLogs` — 각각 `.map().join('')`으로 HTML 문자열을 조립). React에서는 이게 각각 "props로 배열을 받아서 JSX를 반환하는 함수(컴포넌트)"가 되는데, `<PlayersTable players={players} />`처럼 **재사용 가능한 조각**으로 명확히 나뉩니다. `05_improvement-backlog.md`의 A3(페이징)를 나중에 추가할 때도, "표를 그리는 로직"과 "페이지 번호 상태"를 분리하기가 지금 구조보다 훨씬 쉬워집니다.

---

다음 문서: [`16_컴포넌트와_Props.md`](./16_컴포넌트와_Props.md) — 지금 코드의 `createBallEl()`, `renderFortuneGrid()` 같은 "화면 조각을 만드는 함수"들이 React 컴포넌트로 어떻게 바뀌는지.
