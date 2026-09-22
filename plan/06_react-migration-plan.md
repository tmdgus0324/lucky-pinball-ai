# AI Lucky Pinball — React 마이그레이션 계획 (착수 전)

> 이 문서도 백로그 문서(`05_improvement-backlog.md`)와 마찬가지로 **분석과 계획만** 담고 있습니다. 실제 마이그레이션은 나중에 진행 요청을 받으면 이 문서를 출발점 삼아 시작합니다.
>
> React를 처음 접하신다면, 착수 전에 `devhelp/15`~`21`번 문서(React 입문 가이드, 이 프로젝트의 실제 코드와 비교하며 설명)를 먼저 읽어보시는 걸 추천합니다 — 특히 `devhelp/21`이 이 문서의 2번 항목(`game.js`/Matter.js를 React와 공존시키는 방법)을 실제 코드로 끝까지 풀어서 보여줍니다.

---

## 1. 현재 구조 분석

`frontend/`는 빌드 도구 없는 순수 HTML/CSS/JS(바닐라)로 만든 **2페이지 MPA**(index.html, admin.html)입니다. 파일별 역할과 규모는 이렇습니다.

| 파일 | 줄 수 | 역할 | 특징 |
|---|---|---|---|
| `index.html` | 83 | 참가자 등록~게임~결과 화면 골격 | 섹션 4개(등록/운세/게임/결과) |
| `admin.html` | 74 | 관리자 화면 골격 | 표 2개 + 로그 목록 + 조정 폼 |
| `css/style.css` | ~420 | 전역 스타일 | CSS 커스텀 프로퍼티(`--bg` 등) 기반 다크 테마, 두 페이지 공용 |
| `js/api.js` | 89 | 백엔드 fetch 래퍼 | 프레임워크 의존성 전혀 없는 순수 함수 모음 — **재사용 가치가 가장 높음** |
| `js/main.js` | 214 | index.html 진행 흐름 제어 | IIFE 클로저 안의 `state` 객체로 상태 관리, `document.createElement`/`innerHTML`로 직접 DOM 조작 |
| `js/admin.js` | 87 | admin.html 진행 흐름 제어 | 위와 같은 패턴, 상태는 사실상 없음(불러와서 그리기만 함) |
| `js/game.js` | 366 | 핀볼 물리 시뮬레이션 + 렌더링 | **가장 복잡하고 마이그레이션 난이도가 높은 파일** (아래 2번 참고) |

**상태 관리 현황**: Redux 같은 라이브러리는 물론 없고, `main.js`의 `state = { players: [], fortunes: new Map() }`가 사실상 전부입니다. 이 정도 복잡도면 React로 옮겨도 `useState`/`useReducer` 이상은 필요 없어 보입니다 — Redux/Zustand 같은 상태관리 라이브러리 도입은 **과한 설계**라고 판단합니다.

**API 계층**: `api.js`는 이미 프레임워크와 무관한 순수 함수라, React로 옮길 때 **거의 그대로 재사용**하고 TypeScript 타입만 입히면 됩니다.

---

## 2. 가장 까다로운 지점 — `game.js`(Matter.js)와 React를 어떻게 공존시킬까

`game.js`는 매 프레임(초당 60번) `body.position`을 읽어서 공/못 DOM 요소의 `style.left/top`을 직접 바꿉니다. 이걸 그대로 "React식"으로 옮겨서 공 하나하나를 React 컴포넌트로 만들고 매 프레임 `setState`로 리렌더링하면:

- 초당 60번 × 최대 8개 공 = **초당 최대 480번의 리렌더링**이 발생 — React의 재조정(reconciliation) 비용을 생각하면 성능 문제가 생길 가능성이 높습니다.
- 지금까지 devhelp 09/11/12에서 어렵게 잡아둔 물리 버그(벽 끼임, 즉시 낙하 등)를 다시 건드릴 위험도 있습니다.

**결론(권장 방향)**: `game.js`의 물리 엔진 코드는 **React 바깥의 "순수 명령형 섬(island)"으로 그대로 유지**합니다.

- `PinballBoard`라는 React 컴포넌트를 하나 만들고, 그 안에서 `useRef`로 DOM 컨테이너를 잡은 뒤 `useEffect`에서 지금의 `runPinballGame(...)` 함수를 **거의 그대로** 호출합니다.
- React는 "이 컴포넌트가 마운트되면 게임을 시작하고, 언마운트되면 정리(`adapter.dispose()`)한다" 정도의 얇은 경계 역할만 합니다. 공 위치 갱신 같은 고빈도 작업은 지금처럼 순수 DOM 조작으로 남겨둡니다.
- 순위 패널(`RankPanel`)은 갱신 빈도가 낮으므로(순위가 바뀔 때만) React 상태로 옮겨도 괜찮습니다 — 엔진이 "순위 바뀜" 콜백을 호출하면 그때만 `setState`.

이 경계를 명확히 하는 게 이번 마이그레이션에서 가장 중요한 설계 결정입니다.

### 개발 중 주의할 점(StrictMode 함정)
React 18+ 개발 모드의 `StrictMode`는 버그를 잡기 위해 `useEffect`를 **일부러 두 번** 실행합니다. `runPinballGame`을 정리(cleanup) 없이 `useEffect`에 그냥 넣으면, 물리 시뮬레이션이 **동시에 두 개** 도는 사고가 날 수 있습니다 — cleanup 함수에서 `adapter.dispose()`와 DOM 정리를 반드시 짝지어야 합니다.

---

## 3. 빌드 도구 추천

**Vite + React + TypeScript**를 추천합니다.

- **Vite**: 현재 표준적으로 가장 많이 쓰이는 선택지입니다(CRA는 유지보수가 사실상 중단됨). 개발 서버가 빠르고(HMR), `npm run build`로 정적 파일(`dist/`)이 나와서 지금 쓰는 `npx serve` 방식이나 나중의 GitHub Pages 배포와도 잘 맞습니다.
- **TypeScript**: 백엔드가 이미 정적 타입 언어(Java)라 API 응답 모양(`FortuneResponse`, `GameParticipant` 등)을 타입으로 그대로 옮겨두면 프론트-백엔드 계약이 어긋나는 실수를 컴파일 타임에 잡을 수 있습니다. 포트폴리오 완성도 측면에서도 유리합니다. (원하시면 순수 JS로 진행할 수도 있습니다 — 착수 시 확인.)
- **Matter.js**: 지금은 CDN `<script>` 태그로 전역 `Matter` 객체를 쓰고 있는데, Vite 프로젝트에서는 `npm install matter-js`로 정식 설치해서 `import Matter from 'matter-js'`로 바꾸는 게 자연스럽습니다.
- **React Router**: 지금의 `index.html` ↔ `admin.html` 2페이지 구조를 SPA 안의 라우트 2개(`/`, `/admin`)로 그대로 옮깁니다.
- **(선택) TanStack Query(React Query)**: 관리자 화면이 GET 3개(참가자/게임/로그)를 불러와서 로딩/에러 상태를 손으로 관리하고 있는데(`admin.js`), React Query를 쓰면 이 보일러플레이트가 크게 줄어듭니다. 필수는 아니고, 지금 규모에서는 `useEffect` + `useState`로도 충분히 가능 — 착수 시 결정.

---

## 4. 컴포넌트 분리안

```
App (라우터 설정)
├── Layout (공용 헤더 + 내비게이션 탭 — 추후 05번 백로그의 "좌측 메뉴" 개편과 자연스럽게 맞물림)
│
├── GamePage  ("/")
│   ├── RegistrationForm       # 이름 + 생년월일 6자리 입력, 등록 처리
│   ├── PlayerList              # 등록된 참가자 칩 목록 + 삭제
│   ├── FortuneSection
│   │   └── FortuneCard[]       # 티어별 색상, AI/CACHE/NONE source 표시
│   ├── GameSection
│   │   ├── PinballBoard        # ⚠ game.js 엔진을 감싸는 명령형 "섬" (2번 항목 참고)
│   │   └── RankPanel           # 순위 변경 콜백으로만 갱신되는 저빈도 상태
│   └── ResultBanner            # 당첨자 발표
│
└── AdminPage  ("/admin")
    ├── PlayersTable            # 05번 백로그의 페이징(20줄)을 이번에 같이 반영하기 좋은 지점
    ├── GamesTable
    ├── LogList
    └── OverrideForm            # 지금처럼 501 스텁 처리

공용
├── api/client.ts                # 지금의 api.js를 타입만 입혀서 거의 그대로 재사용
└── styles/                      # 지금의 style.css를 1단계에서는 그대로 전역 import (아래 6번 참고)
```

---

## 5. 예상 작업 순서 (단계별, 각 단계마다 눈으로 확인 가능하게)

한 번에 다 바꾸는 대신, **위험이 낮은 부분부터** 단계적으로 옮기는 순서를 제안합니다.

1. **뼈대 세팅**: `npm create vite@latest`로 새 React+TS 프로젝트 생성, 라우팅 뼈대(빈 GamePage/AdminPage)만 연결, 백엔드 `WebConfig`의 CORS 허용 목록에 Vite 개발 서버 포트(기본 5173) 추가. 빌드/개발 서버가 뜨는지만 확인.
2. **API 계층 이식**: `api.js` → `api/client.ts`. UI 없이 타입과 함수만 옮기는 가장 안전한 단계.
3. **관리자 화면부터 구현**: 물리 엔진이 없는 `AdminPage`를 먼저 만들어서, React+TS+빌드 파이프라인+스타일 적용이 잘 되는지 검증하는 "연습 라운드"로 삼습니다.
4. **게임 화면의 "쉬운 부분"**: `RegistrationForm`/`PlayerList`/`FortuneSection`/`ResultBanner`— 아직 물리 엔진은 손대지 않고 폼/카드/목록만 React화.
5. **가장 어려운 부분 — `PinballBoard`**: `game.js`의 로직을 **최대한 그대로** 옮기고 React는 mount/unmount 경계만 담당하게 감쌉니다. 새로 재작성하려는 유혹을 참고, 기존 버그 수정 이력(devhelp 09/11/12)이 녹아있는 로직을 그대로 이식하는 데 집중합니다.
6. **스타일**: 1차로는 기존 `style.css`를 그대로 전역으로 import — "프레임워크 전환"과 "스타일 전환"을 동시에 하지 않기 위함입니다. CSS Modules 같은 정교화는 필요해지면 그 다음에.
7. **기능 동등성 확인 후 전환**: 기존 수동 테스트 체크리스트(생년월일 유무별 등록, 운세 다양성, 전체 게임 1판, 관리자 3개 표, override 501)를 React 버전에서 동일하게 재현되는지 확인한 뒤, 기존 `frontend/`를 교체합니다.
8. **(선택) 이번에 같이 해결하기 좋은 백로그 항목**: 05번 문서의 A1(좌측 메뉴), A3(관리자 페이징)은 React 구조에서 훨씬 자연스럽게 구현되므로, React 전환과 같은 타이밍에 진행하는 것도 고려할 만합니다 — 다만 결합할지는 착수 시 결정.

---

## 6. 영향받지 않는 부분

**백엔드(Java/Spring Boot)는 전혀 손대지 않습니다.** REST API 계약(`02_api-spec.md`)이 그대로 유지되는 한, 이번 마이그레이션은 순수하게 프론트엔드 내부 구조 교체입니다. 다만 배포 방식 문서(`01_mvp-plan.md` §5)는 "빌드 산출물(`dist/`)을 배포한다"는 내용으로 갱신이 필요해질 수 있습니다 — 이것도 착수 시점에 반영.
