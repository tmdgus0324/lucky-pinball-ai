# CLAUDE.md

AI Lucky Pinball — 참가자의 운세를 Claude API로 분석해 버프(시작 높이)를 주고, Matter.js 핀볼 맵에서 결승선 통과 순서로 당첨자를 뽑는 웹 추첨 게임. 포트폴리오 프로젝트이며 설명·주석·문서는 **한국어**로 작성한다.

## 구조

| 폴더 | 내용 |
|---|---|
| `backend/` | Java 17, Spring Boot 4, Gradle, H2(파일 DB `backend/data/`), Anthropic Java SDK |
| `frontend-react/` | React 19 + TypeScript + Vite, Matter.js — **현재 주력 프론트엔드** |
| `frontend/` | 기존 Vanilla HTML/CSS/JS 버전 — React 마이그레이션 7단계(교체) 전까지 병행 유지 |
| `plan/` | 설계 문서 (API 명세 `02`, DB `03`, 맵 `04`, 백로그 `05`, React 마이그레이션 `06`) |
| `devhelp/` | 구현 단계별 기록 (번호순, 현재 `27`까지) |

백엔드 패키지는 도메인별로 나뉜다: `player`, `fortune`, `game`, `admin`, `common`(예외·에러 로그), `config`(CORS).

## 실행 · 빌드 · 테스트

```bash
# 백엔드 (http://localhost:8080)
cd backend
./gradlew bootRun          # ANTHROPIC_API_KEY 환경변수 필요 (없으면 생년월일 없는 참가자만 가능)
./gradlew test
./gradlew build

# React 프론트 (http://localhost:5173)
cd frontend-react
npm run dev
npm run build              # tsc -b && vite build — 타입 체크 포함
npm run lint               # oxlint

# Vanilla 프론트 (http://localhost:5501)
cd frontend
npx serve . -l 5501
node --check js/main.js    # 빌드 도구가 없으므로 문법 검사는 이걸로
```

## 반드시 지킬 것

- **Claude API는 비용이 든다.** 운세는 `이름+생년월일` 기준으로 DB에 영구 캐시되며, 관리자 화면에서 `AI`/`CACHE`로 구분된다. 캐시를 우회하거나 테스트에서 실제 API를 호출하는 코드를 만들지 않는다. 단위 테스트는 `MockFortuneGenerator`(스프링 빈 아님)를 쓴다.
- **인터페이스 기반 교체 지점을 유지한다** — `FortuneService`, `GameRepository`, `PlayerJpaRepository`, `MatterAdapter`, `PinballMapConfig`. 호출부가 구현체에 직접 의존하게 바꾸지 않는다. 이 설계가 프로젝트의 핵심 포트폴리오 포인트다.
- **에러는 `ApiException`을 던지고 `GlobalExceptionHandler`에 맡긴다.** 핸들러가 `ErrorLogStore`에 쌓아 `/api/admin/logs`로 조회되므로, 컨트롤러마다 따로 try/catch·로깅을 넣지 않는다.
- **물리 엔진 코드는 React 밖에 둔다.** `frontend-react/src/game/engine.ts`는 명령형으로 DOM을 직접 갱신하고, `PinballBoard`는 `useRef`/`useEffect`로 감싸기만 한다. 공 위치를 React state로 옮기지 않는다 (이유: `plan/06` 2번 항목).
- **물리 튜닝은 신중히.** 벽 끼임·즉시 낙하 같은 버그를 어렵게 잡은 이력이 있다 (`devhelp/09`, `11`, `25`). 맵·충돌 값을 바꾸면 여러 번 실제로 플레이해서 확인한다. 특히 `PEG_WALL_CLEARANCE`(70)는 줄이면 공이 영구히 끼는 버그가 재발한다(실측으로 두 번 확인) — 벽 통로 문제는 `WALL_RESTITUTION`으로 다룬다. 또 `pegField.startY`는 공 최대 시작 위치(`20 + 최대 startY 260`)보다 60px 이상 아래여야 한다(`devhelp/26`).
- **`frontend-react/src/game/engine.ts`와 `frontend/js/game.js`는 같은 물리 로직의 두 사본이다.** 맵·물리·카메라 값을 바꾸면 둘 다 같이 고친다.
- **버프(시작 높이)는 이번 판 참가자끼리의 상대평가다.** 최고점자 대비 점수 차이 × 공 지름(26px), 차이는 최대 10점(`devhelp/26`). 실제 계산은 `GameService.applyRelativeStartY`(`BuffCalculator.relativeStartY`)이고, 2번 섹션 미리보기(`utils/relativeBuff.ts`, 레거시 `frontend/js/main.js`)는 같은 공식을 복제한 것이라 상수(10, 26)를 바꾸면 세 곳을 같이 고친다.
- **프론트 포트를 추가하면** `backend/.../config/WebConfig.java`의 CORS 허용 목록에도 추가한다. 배포 도메인은 `https://lucky-pinball-ai.vercel.app`.
- React의 API 주소는 `VITE_API_BASE` 환경변수(없으면 `http://localhost:8080`)를 쓴다. Vanilla `frontend/js/api.js`는 `localhost:8080`이 하드코딩되어 있다.
- `ANTHROPIC_API_KEY` 등 비밀 값은 코드·문서·커밋에 절대 넣지 않는다.

## 문서화 관례

의미 있는 기능 추가나 버그 수정을 마치면 `devhelp/`에 다음 번호로 기록 문서를 추가한다 (무엇을, 왜 그렇게, 어떤 문제를 만났는지). 계획이 바뀌면 `plan/` 문서의 진행 상태도 갱신한다.

## 배포

- 백엔드: `backend/Dockerfile` (멀티스테이지, Java 17) — Render(`https://lucky-pinball-backend.onrender.com`), `PORT` 환경변수로 포트 지정, `ANTHROPIC_API_KEY`는 Render 대시보드 환경변수로만 설정
- 프론트: Vercel (`https://lucky-pinball-ai.vercel.app`, Root Directory `frontend-react`, 환경변수 `VITE_API_BASE`)
- `master`에 푸시하면 Render·Vercel이 자동 재배포한다. Render 무료 티어는 유휴 시 슬립하고 **깨어날 때 H2 데이터가 초기화**된다(정상 동작).

## 수동 테스트 팁

화면의 `AI TEST`는 임의 신원을 만들어 실제 Claude를 호출하고(비용 발생), `DB TEST`는 DB에 결과가 이미 있는 신원을 다시 등록해 캐시 재사용(관리자 화면 `AI 호출 = N`)을 확인한다.
