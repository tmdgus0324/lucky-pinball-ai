# AI Lucky Pinball

[![CI](https://github.com/tmdgus0324/lucky-pinball-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/tmdgus0324/lucky-pinball-ai/actions/workflows/ci.yml)

사다리타기 대신 만든 웹 추첨 게임입니다. 이름과 생년월일을 입력하면 Claude가 그날의 운세를 분석해서, 운세가 좋을수록 핀볼 트랙에서 더 낮은(불리한) 위치에서 출발합니다. 참가자들은 물리 엔진 위에서 결승선까지 굴러가고, 가장 늦게 도착한 사람이 당첨자입니다.

- 데모: https://lucky-pinball-ai.vercel.app
- 관리자 화면(`/admin`) 데모 계정: 아이디 `admin` / 비밀번호 `1234` — 포트폴리오 시연용으로 공개한 계정이고, 조회 기능(참가자·게임·오류 로그)만 있습니다. 비밀번호는 코드가 아니라 환경변수(`ADMIN_USERNAME`, `ADMIN_PASSWORD`)로 주입합니다.
- Render 무료 티어라 한동안 접속이 없으면 서버가 슬립 상태가 되어 첫 요청에 1~2분 정도 걸립니다(그동안 화면이 멈춘 것처럼 보일 수 있습니다). DB는 Neon(PostgreSQL)이라 서버가 슬립해도 데이터는 유지됩니다.

## 기능

- **참가자 등록 → AI 운세 → 상대평가 버프**: 이름 + 생년월일(선택)을 등록하면 Claude(Haiku 4.5)가 점수·행운의 숫자·메시지를 생성합니다. 처음엔 절대 점수로 시작 위치를 정했는데, 실제로 테스트해보니 점수가 60~70점대로 몰려서 버프 차이가 눈에 안 보이는 문제가 있었습니다. 그래서 "이번 판 최고점자 대비 몇 점 차이나는지"로 바꿔, 최대 10점(공 10개 높이) 이내에서 상대적으로 갈리도록 했습니다.
- **같은 사람이면 AI를 다시 부르지 않음**: 이름+생년월일이 같으면 최초 1회만 Claude를 호출하고, 이후에는 DB에 저장된 결과를 재사용합니다. 관리자 화면에서 AI 호출 여부(Y/N)를 참가자별로 볼 수 있습니다.
- **핀볼 게임**: Matter.js 갈톤보드 + 회전 막대 장애물, 선두 공을 따라가는 카메라. 벽 틈에 공이 끼는 문제, 좁은 화면에서 보드가 잘리는 문제 등은 실제 플레이·모바일 테스트로 찾아서 고쳤습니다(자세한 과정은 `devhelp/` 참고).
- **관리자 화면**: 로그인(환경변수 계정 + 토큰 헤더) 후 참가자 이력, 게임 결과, 서버 오류 로그를 조회합니다. 참가자 목록은 20개씩 페이징됩니다.
- **테스트용 버튼**: 빠른 추가(AI 호출 없이 즉시 참가), AI TEST(임의 신원으로 실제 AI 호출), DB TEST(기존 신원 재등록으로 캐시 재사용 시연).
- **공부하기 > React (extra)**: 예전에 React를 공부하며 만든 예제(`React_basic`의 3~15장)를 웹에서 직접 실행해 보고, 코드와 챕터별 정리를 접어 두었다 펼쳐 볼 수 있는 학습 페이지입니다. 예제는 원본 코드를 그대로 옮겼고, 웹에서 돌리면서 부딪힌 문제(StrictMode 이중 실행, 전역 CSS 번짐, 오타 import 등)는 `devhelp/44`에 정리했습니다.

## 기술 스택

- 백엔드: Java 17, Spring Boot 4, Spring Data JPA, Gradle, Anthropic Java SDK
- DB: 로컬 개발은 H2(파일), 배포는 Neon(PostgreSQL) — 접속 정보를 환경변수로 바꿔 끼웁니다(`devhelp/35`)
- 프론트엔드: React 19 + TypeScript + Vite, Matter.js (기존 Vanilla JS 버전 `frontend/`도 병행 유지)
- 배포: Render(Docker) + Vercel + Neon

## 구조 특징

- 처음에는 Vanilla JS(`frontend/`)로 만들었다가, 이후 React로 마이그레이션했습니다(`frontend-react/`). React를 배우면서 정리한 내용은 `devhelp/15`~`23`에 남겨뒀습니다.
- `FortuneService`: 초기 개발 단계에서는 API 연동 없이 게임 로직·화면부터 만들기 위해 인터페이스로 분리하고 Mock 구현체(`MockFortuneGenerator`)로 먼저 뼈대를 갖췄습니다. 이후 실제 Claude API 연동(`ClaudeFortuneGenerator`)으로 교체했고, Mock 구현체는 API 호출 없이 로직만 검증하는 단위 테스트용으로 남겨뒀습니다.
- 같은 이유로 `GameRepository`, `PlayerJpaRepository`, `MatterAdapter`도 인터페이스로 분리해, 구현체를 바꿔도 호출부 코드는 그대로 유지되도록 했습니다.
- 모든 요청에 추적 ID(`traceId`)를 붙여서, 서버 로그 줄과 에러 응답이 같은 ID를 갖습니다. 화면에 뜬 "오류 ID"를 알려주면 서버 로그에서 그 요청의 흐름(AI 호출, 캐시 적중, 예외 스택트레이스)을 바로 찾을 수 있습니다. 사용자 응답에는 서버 내부 사정을 싣지 않고 일반 문구와 ID만 줍니다(`devhelp/40`).
- Claude API는 타임아웃 10초·재시도 1회로 제한했습니다(SDK 기본값은 10분·2회). 실패하면 유형별로 504/503/502와 일반 문구를 주고, 기본 설정에서는 규칙 기반 **임시 점수(FALLBACK)** 로 게임을 이어갑니다. 임시 점수는 신원 캐시에 넣지 않아서 AI가 복구되면 같은 사람이 진짜 결과를 받습니다(`devhelp/41`).
- AI가 안 될 때 게임을 돌려보지 않아도, 관리자 화면의 "AI 연결 확인" 버튼으로 Claude API에 최소 요청(출력 1토큰)을 보내 정상/인증 실패/크레딧 부족/타임아웃/연결 불가를 구분해서 볼 수 있습니다. 유료 호출이라 5초 간격으로 제한하고 관리자 토큰이 있어야 호출됩니다(`devhelp/43`).
- 오류가 발생하면 `GlobalExceptionHandler`가 모아서 관리자 화면(`/api/admin/logs`)에서 바로 확인할 수 있도록 구현했습니다. DB에 저장하므로 서버가 재시작·재배포돼도 남고, 최근 1000건만 유지해 무한히 쌓이지 않습니다. 오류를 기록하다 실패해도 원래 응답은 망가지지 않게 했습니다(`devhelp/42`).
- 물리 엔진 코드는 React 밖에 두고, `PinballBoard`가 마운트·정리만 담당합니다.
- 매번 Claude API를 부르면 토큰 비용이 계속 쌓입니다. 그래서 이름+생년월일을 키로 삼아 DB를 캐시처럼 쓰고, 같은 신원은 재호출 없이 이전 결과를 재사용합니다.

## 폴더

```
backend/         Spring Boot (Dockerfile 포함)
frontend-react/  React 프론트 (주력)
frontend/        Vanilla JS 프론트 (기존)
plan/            설계·계획 문서
devhelp/         구현 과정과 트러블슈팅 기록
```

로컬 개발 환경 준비 과정은 [`devhelp/01`](./devhelp/01_개발_환경_준비.md)에 정리해뒀습니다.

## 상태와 개선 예정

배포된 라이브 URL(Render + Vercel) 기준으로 전체 흐름(등록 → AI 운세 → 게임 → 결과 → 관리자)을 직접 확인했습니다.

**만난 문제와 해결**
- 문제: Vercel에 올린 SPA를 `/admin`에서 새로고침하면 404. → 해결: `vercel.json`에 SPA rewrite 규칙 추가.
- 문제: 좁은 화면(모바일)에서 핀볼 보드가 약 39% 잘림. → 해결: 실제 렌더링 폭 기준으로 `fitScale`을 계산해 카메라 변환에 반영.
- 문제: 인증 없는 공개 서비스라 비용·보안 리스크가 있음. → 해결: H2 콘솔 비활성화, 입력 검증, XSS 방지, `/api/fortune` 요청 제한 적용.
- 문제: 존재하지 않는 경로 요청까지 500으로 처리돼 관리자 오류 로그가 노이즈로 오염됨. → 해결: `NoResourceFoundException` 전용 처리 추가.

자세한 과정은 [`devhelp/28`](./devhelp/28_배포_검증과_두_버그.md), [`devhelp/29`](./devhelp/29_비용과_보안_방어_Phase1.md) 참고.

**진행 및 보완 예정**
- 같은 신원 동시 요청 시 AI 중복 호출 방지
- 모바일에서 운세 결과 카드 텍스트가 길면 잘리는 문제 보완

자세한 계획은 [`plan/05_improvement-backlog.md`](./plan/05_improvement-backlog.md)에 정리해뒀습니다.

## 출처와 참고

- 게임 방식(결승선 통과 순서 경쟁)과 회전 막대 장애물 아이디어는 lazygyu의 [Marble Roulette](https://lazygyu.github.io/roulette/)([lazygyu/roulette](https://github.com/lazygyu/roulette), MIT 라이선스)에서 영감을 받았습니다.
- 물리와 맵은 Matter.js로 직접 구현했습니다. 맵 구조(갈톤보드), 장애물 크기·배치·회전 속도 등 모든 수치와 코드는 이 프로젝트에서 새로 작성했고, 원본의 코드·맵 데이터·이미지는 사용하지 않았습니다.
- AI 운세 분석, 상대평가 버프, 캐시 구조는 이 프로젝트의 독자 기능입니다.
- "Marble Roulette"와 "마블 룰렛"은 lazygyu의 상표이며, 이 프로젝트는 원작자와 제휴·후원 관계가 없는 개인 포트폴리오 프로젝트입니다.
- 물리 엔진 [Matter.js](https://brm.io/matter-js/)는 MIT 라이선스입니다.
