# 16. 컴포넌트와 Props — "화면 조각을 만드는 함수"의 진화형

## 1. 지금 코드에도 이미 "컴포넌트"와 비슷한 게 있다

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

## 2. Props는 "읽기 전용"이다

가장 중요한 규칙: **컴포넌트는 자기가 받은 props를 직접 바꾸면 안 됩니다.** props는 "부모가 나에게 준 설정값"이라, 자식이 마음대로 고치면 안 됩니다 (자바스크립트 함수에서 매개변수를 함수 안에서 재할당하지 않는 것과 비슷한 관례인데, React에서는 이게 **규칙**입니다). 값을 바꾸고 싶으면 다음 문서(`17_State와_useState.md`)에서 다룰 **state**를 씁니다.

## 3. 지금 코드의 다른 "화면 조각 함수"들도 컴포넌트가 된다

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

## 4. 컴포넌트를 나누는 기준

정해진 규칙은 없지만, 이 프로젝트에서 자연스러운 기준은 **"지금 코드에서 이미 별도 함수로 뽑혀 있는 화면 단위"**입니다. `plan/06_react-migration-plan.md`의 컴포넌트 분리안(`RegistrationForm`, `PlayerList`, `FortuneCard`, `PlayersTable` 등)도 정확히 이 기준 — 지금 `render...()` 함수들의 경계를 그대로 따라간 것입니다. 즉 **"뭘 컴포넌트로 나눌지 새로 고민할 필요 없이, 지금 있는 `render` 함수들을 하나씩 컴포넌트로 옮기면 된다"**고 생각하면 마이그레이션이 훨씬 덜 막막하게 느껴질 겁니다.

---

다음 문서: [`17_State와_useState.md`](./17_State와_useState.md) — `main.js`의 `state` 객체가 React에서는 어떻게 바뀌는지, 그리고 "바뀌면 자동으로 다시 그려지는" 마법이 실제로 어떻게 동작하는지.
