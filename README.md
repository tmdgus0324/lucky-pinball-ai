# 🎱 AI Lucky Pinball

이름과 생년월일을 입력하면 AI(Claude)가 오늘의 운세를 분석해 시작 위치 버프를 주고, 참가자들이 물리 엔진 기반 핀볼 맵에서 결승선을 향해 경쟁합니다. 가장 마지막에 결승선을 통과한 사람이 당첨자가 되는 웹 추첨 게임입니다.

- 데모(프론트): https://lucky-pinball-ai.vercel.app
- API(백엔드): https://lucky-pinball-backend.onrender.com
- 백엔드가 무료 티어라 한동안 접속이 없으면 잠들고, 첫 요청은 30~60초 걸릴 수 있습니다. 이때 저장된 데이터도 초기화됩니다.

## 주요 기능

- **참가자 등록**: 이름 + 생년월일(선택, 6자리). 생년월일이 없으면 AI 호출 없이 바로 참여합니다.
- **AI 운세**: Claude(Haiku 4.5)가 점수·행운의 숫자·메시지를 생성합니다.
- **DB 재사용**: 같은 이름+생년월일은 처음 한 번만 AI를 호출하고 이후에는 DB 값을 씁니다.
- **상대평가 버프**: 이번 판 참가자 중 최고점자 대비 점수 차이만큼(최대 10점 = 공 10개) 아래에서 출발합니다.
- **핀볼 게임**: Matter.js 갈톤보드에 회전 막대 장애물이 있고, 카메라가 선두 공을 따라갑니다.
- **관리자 화면**: 참가자별 운세 이력(AI 호출 여부 Y/N), 게임 결과, 서버 오류 로그를 보여줍니다. 참가자 목록은 20개씩 페이징됩니다.
- **테스트 버튼**: `빠른 추가`(AI 없이 즉시 참가), `AI TEST`(임의 신원으로 실제 AI 호출), `DB TEST`(기존 신원으로 재사용 확인)

## 기술 스택

| 영역 | 기술 |
|---|---|
| 백엔드 | Java 17, Spring Boot 4, Gradle, H2(파일), Anthropic Java SDK |
| 프론트엔드 | React 19 + TypeScript + Vite, Matter.js (기존 Vanilla JS 버전 `frontend/` 병행 유지) |
| 배포 | Render(Docker) + Vercel |

## 구조 특징

- 교체 지점을 인터페이스로 분리했습니다: `FortuneService`(Mock → Claude로 실제 교체), `GameRepository`, `PlayerJpaRepository`, `MatterAdapter`.
- 예외는 `GlobalExceptionHandler`가 모아 `/api/admin/logs`로 조회됩니다.
- 물리 엔진 코드는 React 밖에 두고, `PinballBoard`가 마운트·정리만 담당합니다.

## 폴더

```
backend/         Spring Boot (Dockerfile 포함)
frontend-react/  React 프론트 (주력)
frontend/        Vanilla JS 프론트 (기존)
plan/            설계·계획 문서
devhelp/         구현 과정과 트러블슈팅 기록
```

## 로컬 실행

```bash
# 백엔드 (http://localhost:8080) — ANTHROPIC_API_KEY 환경변수 필요(없으면 생년월일 없는 참가자만 가능)
cd backend && ./gradlew bootRun

# 프론트 (http://localhost:5173)
cd frontend-react && npm install && npm run dev
```

## 상태

백엔드(Render)와 프론트(Vercel)가 배포되어 있고, 전체 흐름(등록 → AI 운세 → 게임 → 결과 → 관리자)은 로컬에서 확인했습니다. 앞으로 할 일은 [`plan/05_improvement-backlog.md`](./plan/05_improvement-backlog.md)에 있습니다(관리자 인증, 로그·게임 결과 DB 저장, 모바일 화면 대응 등).
