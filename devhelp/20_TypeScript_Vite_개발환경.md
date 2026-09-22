# 20. TypeScript와 Vite — 개발 환경이 실제로 뭐가 달라지는가

지금까지는 **React 문법**(JSX, 컴포넌트, state, effect) 얘기였다면, 이 문서는 그걸 실제로 "어떤 도구로 실행/빌드하는지"에 대한 이야기입니다.

## 1. 지금과 가장 크게 달라지는 점: 빌드 과정이 생긴다

지금 `frontend/`는 빌드 도구가 전혀 없습니다 — `index.html`을 열면(또는 정적 서버로 서빙하면) `<script src="js/main.js">`가 브라우저에서 그대로 실행됩니다. JSX나 TypeScript는 브라우저가 직접 이해할 수 없는 문법이라, **빌드 도구(Vite)가 이걸 브라우저가 이해할 수 있는 순수 JS로 미리 변환**해줘야 합니다.

```
지금:      index.html → <script src="js/main.js"> → 브라우저가 그대로 실행
React 이후: npm run dev  → Vite가 JSX/TS를 변환하며 실시간으로 서빙 (개발 중)
           npm run build → dist/ 폴더에 순수 HTML/CSS/JS로 "구워낸" 결과물 생성 (배포용)
```

## 2. Vite 프로젝트의 기본 파일 구조

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

## 3. TypeScript — "이 값이 어떤 모양인지" 미리 적어두는 것

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
  fortuneScore: number | null;   // devhelp/13에서 다뤘듯, 생년월일 없으면 null
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
- `fortuneScore`가 `number | null`이라고 타입에 적어뒀기 때문에, `fortune.fortuneScore.toFixed(0)`처럼 null 체크 없이 쓰면 컴파일러가 "null일 수도 있는데?"라고 미리 경고해줍니다 — 실제로 `devhelp/13`에서 다뤘던 "생년월일 없으면 null" 케이스를 놓치는 버그를 미리 막아주는 셈입니다.
- 백엔드(`FortuneController.FortuneResponse` — Java record)와 프론트엔드 타입이 **구조적으로 나란히 대응**하게 되어서, API 응답 모양이 바뀌면 타입 정의도 같이 고쳐야 한다는 게 명확해집니다.

### 얼마나 깊이 알아야 하는가

이 프로젝트 수준에서 필요한 TypeScript는 많지 않습니다. 주로 쓰게 될 것:
- `interface`(또는 `type`)로 객체 모양 정의 — 위 예시가 전부입니다.
- 함수 매개변수/반환값에 타입 적기 — `function foo(x: number): string`.
- Props 타입 — `function Ball({ name, colorIndex }: { name: string; colorIndex: number })`.
- 제네릭은 몰라도 됩니다(`useState<Player[]>([])`처럼 아주 가끔 등장하는 정도만 알면 충분).

## 4. CORS 관련 — 잊지 말아야 할 설정 한 가지

Vite 개발 서버는 기본적으로 `http://localhost:5173`에서 뜹니다. 지금 `WebConfig`(백엔드)의 CORS 허용 목록에는 `5500`, `5501`만 있으므로, React 프로젝트를 실제로 실행해보기 전에 **`5173`(또는 실제로 뜬 포트)을 백엔드 CORS 설정에 추가**해야 API 호출이 막히지 않습니다. `plan/06_react-migration-plan.md`의 1단계("뼈대 세팅")에 이미 이 내용이 포함되어 있습니다.

---

다음 문서: [`21_외부_라이브러리와_React_Matter.js_사례.md`](./21_외부_라이브러리와_React_Matter.js_사례.md) — 지금까지 배운 내용을 종합해서, `game.js` 전체가 `PinballBoard` 컴포넌트로 어떻게 옮겨지는지 실제 코드로 끝까지 보여드립니다.
