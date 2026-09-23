# 23. React 마이그레이션 1차 구현 — 뼈대부터 전체 기능 동등성까지

`plan/06_react-migration-plan.md`에서 세운 8단계 작업 순서를 따라 `frontend-react/`(Vite + React + TypeScript)를 실제로 구현했다. 기존 `frontend/`(바닐라 JS)는 **그대로 남겨두고** 나란히 만들었다 — plan 06의 7단계("기능 동등성 확인 후 전환")가 아직 완료되지 않았으므로 교체는 하지 않았다.

## 1. 착수 시 결정한 것

- **언어: TypeScript** (plan 06 권장안 채택)
- **05번 백로그 A1(좌측 사이드바 메뉴)과 A3(관리자 페이징 20줄)을 이번 전환과 같이 진행** — plan 06의 8단계("이번에 같이 해결하기 좋은 백로그 항목")에서 미리 언급했던 항목을 실제로 결합했다.
- React Query는 도입하지 않음 — plan 06에서 이미 "필수 아님"으로 판단했고, `useEffect`+`useState`만으로 충분한 규모였다.

## 2. 실제로 한 일 (plan 06의 8단계 중 1~6단계 해당)

| 단계 | 내용 |
|---|---|
| 1. 뼈대 세팅 | `npm create vite@latest frontend-react -- --template react-ts`, `react-router-dom`/`matter-js`/`@types/matter-js` 설치. 백엔드 `WebConfig`의 CORS 허용 목록에 Vite 기본 포트(5173) 추가 |
| 2. API 계층 이식 | `frontend/js/api.js` → `frontend-react/src/api/client.ts`. 함수 시그니처는 그대로 두고 요청/응답 타입(`Player`, `FortuneResponse`, `GameParticipant`, `AdminPlayer` 등)을 인터페이스로 명시 |
| 3. 관리자 화면 | `AdminPage` + `PlayersTable`/`GamesTable`/`LogList`/`OverrideForm`. **A3 반영**: `PlayersTable`에 `Pagination` 컴포넌트를 붙여 20줄 단위로 자름 |
| 4. 게임 화면(쉬운 부분) | `RegistrationForm`, `FortuneSection`, `ResultBanner` — 폼 상태는 `useState`로, 6자리 생년월일 파싱(`parseBirthDateShorthand`)은 `utils/birthDate.ts`로 분리 |
| 5. `PinballBoard`(가장 어려운 부분) | `frontend/js/game.js`를 `game/engine.ts`로 **로직 변경 없이** 포팅(TS 타입만 추가). devhelp/21에서 미리 설계한 대로 `useRef`+`useEffect`로 감싸고, cleanup에서 `dispose()`를 호출해 StrictMode 이중 실행에 대비 |
| 6. 스타일 | 기존 `frontend/css/style.css`를 `src/styles/style.css`로 그대로 옮기고 전역 import. 여기에 **A1(사이드바)**과 **A3(페이지네이션)**을 위한 CSS만 추가 |
| (추가) A1 사이드바 | `components/Layout.tsx` — 기존 상단 중앙 탭(`nav.tabs`)을 좌측 세로 메뉴로 재배치. `react-router-dom`의 `NavLink`+`Outlet` 사용 |

## 3. 물리 엔진 이식 관련 — devhelp/21 설계가 실제로 맞았는가

devhelp/21에서 예상했던 접근(`runPinballGame` 시그니처를 그대로 두고 React는 mount/unmount 경계만 담당)이 실제로도 그대로 통했다. `game/engine.ts`에서 바뀐 것은:

- `Matter` import 방식: `<script>` 전역 대신 `import * as Matter from 'matter-js'` (CDN 0.19.0 → npm 0.20.0로 버전이 살짝 올라갔지만 사용한 API는 전부 호환)
- `runPinballGame`이 이제 **dispose 함수를 반환**하도록 살짝 바꿈 — devhelp/21이 언급했던 "게임 도중 컴포넌트가 갑자기 사라지는 경우"에 대비해, `PinballBoard`의 `useEffect` cleanup에서 이 dispose 함수를 직접 호출할 수 있게 했다 (원래 코드는 게임이 정상 종료될 때만 내부에서 `adapter.dispose()`를 불렀다).

그 외 갈톤보드 구성, spawnX/spawnY 계산, 정지 감지(`nudge`), 카메라 줌 로직은 전부 한 글자도 안 바꿨다 — devhelp 09/11/12에서 어렵게 잡은 버그를 다시 밟지 않기 위한 원칙을 그대로 지켰다.

## 4. 검증

`npm run build`(TypeScript 컴파일 + Vite 빌드)가 에러 없이 통과하는 것과 별개로, 실제 브라우저 동작을 Playwright로 확인했다:

1. `/`(게임 화면), `/admin`(관리자 화면) 진입 시 콘솔 에러 없음
2. 관리자 화면: 66명의 기존 테스트 참가자 데이터가 20줄씩 4페이지로 정상 분할되어 표시됨(A3 검증)
3. 실사용 흐름 전체 재현: 참가자 2명 등록(생년월일 미입력) → "운세 확인"(NONE 소스로 정상 표시) → "게임 시작" → 핀볼 보드에 못/공이 렌더링되고 실시간 순위 패널이 갱신됨 → 결승선 통과 후 결과 배너에 실제 백엔드 `gameId`와 당첨자 이름이 표시됨

캡처한 스크린샷 기준으로 vanilla JS 버전(`frontend/`)과 시각적으로 동일한 레이아웃·색상·애니메이션을 확인했다(사이드바 부분만 A1 반영으로 의도적으로 다름).

## 5. 남은 것

- **plan 06의 7단계(기능 동등성 확인 후 기존 `frontend/` 교체)는 아직 하지 않았다** — `frontend/`와 `frontend-react/`가 당분간 나란히 존재한다. 실제 교체(기존 폴더 삭제/대체) 여부는 별도 확인 후 진행한다.
- `plan/01_mvp-plan.md` §5(배포 방식 문서)는 이번 범위에 포함하지 않음 — plan 06에서 이미 "착수 시점에 반영"이라고 남겨둔 항목.
- `.gitignore`에 `dist/`, `*.tsbuildinfo`를 추가해 `frontend-react/`의 빌드 산출물이 커밋되지 않도록 함(`node_modules/`는 기존 규칙에 이미 포함되어 있었음).

## 6. 로컬 실행 방법

```bash
cd frontend-react
npm install       # 최초 1회
npm run dev        # http://localhost:5173
```

백엔드는 기존과 동일하게 `cd backend && ./gradlew bootRun`으로 별도 실행한다. CORS 허용 목록에 5173 포트가 추가되어 있어 별도 설정 없이 바로 통신된다.
