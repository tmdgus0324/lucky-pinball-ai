# AI Lucky Pinball — MVP 아키텍처 계획

> 이 문서는 코딩을 시작하기 전에 확정한 MVP 범위와 설계 결정을 정리한 것이다. 실제 구현(백엔드 Gradle 프로젝트, 프론트엔드 실동작 JS)은 이 문서와 `02_api-spec.md`, `03_db-schema.md`, `mockups/`에 대한 검토가 끝난 뒤 별도로 진행한다.
>
> **읽는 순서**: `01_mvp-plan.md`(이 문서, 전체 개요) → `02_api-spec.md`(API 상세) → `03_db-schema.md`(DB 스키마) → `04_pinball-map-design.md`(핀볼 맵/물리엔진 상세) → `05_improvement-backlog.md`(착수 전 개선 백로그) → `06_react-migration-plan.md`(착수 전 React 마이그레이션 계획, 둘 다 진행 요청 시 시작).

## 1. 목표와 범위

기획서(`00_original-concept.md`)가 그리는 전체 아키텍처(Spring Boot + MySQL/H2 + Redis + OpenAI + GitHub Pages/Render 배포)를 한 번에 만들지 않고, **핵심 루프가 실제로 동작하는 MVP**부터 만든다.

핵심 루프: 참가자 등록 → 운세 분석 → 버프(시작 높이) 적용 → Matter.js 핀볼 맵에서 결승선 통과 순서 경쟁 → 가장 마지막 통과자를 "당첨자"로 결정 → 결과 기록.

이번 MVP에 포함하는 것 / 미루는 것:

| 포함 | 미룸 (Phase 2) |
|---|---|
| H2 파일 기반 DB (참가자 + 운세 이력, **조회 + AI 호출 캐시 겸용**) | MySQL 전환 |
| **실제 Claude API 연동** (Haiku 4.5, 구조화된 출력) | — (완료됨, 2026-09-22 3차 변경 참고) |
| 관리자 화면 골격(조회 + 오류 로그) | 관리자 인증, 로그 영속화, 가중치 수동 조정 실기능 |
| 실제 핀볼 맵(갈톤보드 1종) + 낙하 애니메이션 | 맵 여러 종 추가, 맵 선택 UI |
| 로컬 동작 + 독립 git 저장소 준비 + GitHub 푸시 | 실제 GitHub Pages/Render 배포 |

> **변경 이력(2026-09-22, 1차)**: 처음엔 재방문자의 과거 평균 점수를 오늘 점수와 가중평균하는 기능을 넣기로 했었으나, 버프 체계를 단순화하면서 **가중치 블렌딩 로직은 제거**했다. 이제 `POST /api/fortune`은 매번 새로 생성한 mock 점수를 그대로 사용한다. 다만 참가자 이력은 계속 H2에 기록해서 관리자 화면(`GET /api/admin/players`)에서 과거 기록을 조회할 수 있게는 유지한다 — 단지 점수 계산에 더는 쓰이지 않을 뿐이다.
>
> **변경 이력(2026-09-22, 2차)**: 사용자가 참고한 [lazygyu의 "Marble Roulette"](https://lazygyu.github.io/roulette/)(구슬들이 물리엔진으로 결승선까지 경쟁하는 추첨 도구)을 반영해, **승부 방식을 "HP 생존"에서 "결승선 통과 순서 경쟁"으로 전환**했다. 더 이상 HP·충돌 데미지 개념이 없다 — 모든 참가자의 구슬이 핀볼 맵을 통과해 결승선에 도달하며, 도착 순서로 순위가 매겨지고 **가장 마지막에 도착한 참가자가 "당첨자"**가 된다. 버프(시작 높이)는 그대로 유지하되, 그 의미가 "생존에 유리"에서 "결승선에 가깝게 출발해 먼저 도착 → 당첨에서 멀어짐"으로 바뀌었다.
>
> **변경 이력(2026-09-22, 3차)**: `MockFortuneGenerator`를 실제 **Claude API(Haiku 4.5)** 호출로 교체했다(`ClaudeFortuneGenerator`, `devhelp/03(구 13)` 참고) — 기획서 원안의 "OpenAI"가 아니라 Claude로 결정. 동시에 생년월일을 **선택사항**으로 바꿔서, 입력하지 않으면 AI를 호출하지 않고 버프 없이 참여할 수 있게 했다. 생년월일을 입력한 경우에도 이름+생년월일이 같으면(=같은 사주) DB에 저장된 과거 결과를 영구적으로 재사용해서, 동일 인물에 대해 Claude는 딱 한 번만 호출된다.

## 2. 폴더 구조

```
01_LuckyPinballAI/
├── plan/                            # 이 문서를 포함한 설계 문서 + 목업 (현재 단계 산출물)
│   ├── 00_original-concept.md      # 원본 기획서
│   ├── 01_mvp-plan.md
│   ├── 02_api-spec.md
│   ├── 03_db-schema.md
│   ├── 04_pinball-map-design.md
│   └── mockups/
│       ├── style.css
│       ├── index.html
│       └── admin.html
├── backend/                         # (다음 단계) Spring Boot 3.x + Java 17 + Gradle
│   └── src/main/java/com/luckypinball/
│       ├── LuckyPinballApplication.java
│       ├── config/WebConfig.java              # CORS
│       ├── common/GlobalExceptionHandler.java # 예외 처리 + 오류 로그 인메모리 적재
│       ├── player/   (PlayerEntity, PlayerJpaRepository, PlayerController, PlayerService)
│       ├── fortune/  (FortuneResultEntity, FortuneService, MockFortuneGenerator,
│       │             FortuneAnalysisService, BuffCalculator, FortuneController)
│       ├── game/     (GameSession, GameParticipant, GameResult, GameRepository,
│       │             GameService, GameController)
│       └── admin/    (AdminController, ErrorLogEntry)
└── frontend/                         # (다음 단계) 순수 HTML/CSS/JS, 빌드 도구 없음
    ├── index.html / admin.html
    └── css/, js/
```

패키지는 계층(controller/service/repository)이 아니라 **기능 단위**(`player`, `fortune`, `game`, `admin`)로 나눈다 — MVP 규모에 적절하고, 관련 코드가 한 곳에 모여있어 탐색이 쉽다.

## 3. 핵심 설계

### 3.1 `FortuneService` — 교체 가능한 원시 운세 생성기
```java
public interface FortuneService {
    FortuneResult analyze(String name, LocalDate birthDate);
}
```
애초 `MockFortuneGenerator`(규칙 기반 `Random`)로 시작했지만, **2026-09-22부로 `ClaudeFortuneGenerator`(Claude API, Haiku 4.5, 구조화된 출력)로 교체됐다** — 인터페이스만 지키면 됐기 때문에 호출부 코드는 전혀 바꾸지 않았다. `MockFortuneGenerator`는 `@Service`를 떼고 클래스만 남겨서 단위 테스트용으로만 쓴다. 자세한 내용은 [`devhelp/03`(구 13)](../devhelp/03_AI_운세와_상대평가_버프.md) 참고.

### 3.2 참가자 이력 저장 + AI 호출 캐시 (2026-09-22 갱신)
생년월일은 **선택사항**이다. 없으면 `FortuneService`를 아예 호출하지 않고 버프 없이($`tier:0, startY:0`$) 즉시 응답한다(비용 0). 있으면 `FortuneResultEntity`에서 **이름+생년월일이 같은 과거 기록**(=같은 사주)을 먼저 찾아보고, 있으면 그 값을 재사용(`source: "CACHE"`)하고, 없을 때만 Claude를 호출(`source: "AI"`)한다 — 이 캐시는 하루 단위가 아니라 **영구적**이다. 어느 경로든 결과는 `FortuneResultEntity`에 한 행씩 저장돼 관리자 화면에서 참가자별 과거 기록으로 보인다.

### 3.3 `BuffCalculator` — 점수 → 버프 변환 (시작 높이만 차등, HP/데미지 없음)
버프 종류가 복잡하다는 피드백 + 승부 방식이 결승선 경쟁으로 바뀌면서, 버프는 **시작 Y(높이) 한 가지만** 남는다. HP·충돌 데미지 개념 자체가 없다 — 구슬은 물리적으로 부딪히고 튕기지만, 그로 인해 약해지거나 탈락하지 않는다. 오직 "결승선에 얼마나 가까운 곳에서 출발하는가"만 결과에 영향을 준다.

| 점수 구간 | 버프 티어 | 시작 Y 오프셋 (결승선 방향) |
|---|---|---|
| 80~100 | 없음 | 0 (가장 위 = 결승선에서 가장 멀리 출발) |
| 60~79 | 1 | 15 |
| 40~59 | 2 | 30 |
| 20~39 | 3 | 45 |
| 0~19 | 4 | 60 (결승선에 가장 가깝게 출발) |

시작 Y 오프셋은 낙하 시작 구역(핀볼 맵의 못 배열 시작 전 구간, `04_pinball-map-design.md` §3 참고) 안에서 적용된다. **운세가 좋을수록 결승선에서 먼 곳(위)에서 출발해 완주가 늦어지고, 운세가 안 좋을수록 결승선 가까운 곳(아래)에서 출발해 먼저 도착**한다 — 가장 늦게 도착한 사람이 "당첨자"가 되므로, 오늘 운이 없는 사람일수록 당첨에서 멀어지도록 설계했다.

### 3.4 관리자 화면 골격
- `GET /api/admin/players`, `GET /api/admin/games` — 실제 조회 기능으로 구현.
- `GET /api/admin/logs` — `GlobalExceptionHandler`가 예외 발생 시 자동으로 인메모리 리스트에 쌓아둔 오류 로그를 조회 (재시작하면 사라짐 — 영속화는 Phase 2).
- `POST /api/admin/fortune/override` — 골격만: 요청은 받되 `501 Not Implemented`를 반환. 실제로 특정 참가자의 점수/버프를 수동으로 덮어쓰는 로직은 다음 단계 설계 대상.
- 인증 없음 — 로컬/데모 용도로는 문제없으나, 실제 공개 배포 전에는 반드시 최소 인증을 추가해야 함 (Phase 2 필수 항목으로 기록).

### 3.5 핀볼 맵
게임 화면은 단순히 빈 캔버스에 공만 떨어뜨리는 게 아니라, **갈톤보드(Galton board)/플린코 스타일의 못(peg) 배열이 있는 실제 핀볼 맵** 위에서 진행된다. 맵은 나중에 여러 종류를 추가할 수 있도록 설정 객체(`PinballMapConfig`)로 추상화해두고, 이번에는 대표 맵 하나(`classic-galton`)만 만든다. 못 배열 구간을 지나 하단의 **결승선**을 통과하는 순서로 순위가 정해지는 구조이며, 전체 진행 시간이 1분 내외로 끝나도록 캔버스 크기/못 간격/중력/반발계수의 초기값을 잡아뒀다 (실제 코딩 후 실측 튜닝 필요). 자세한 내용은 [`04_pinball-map-design.md`](./04_pinball-map-design.md) 참고.

> **참고**: lazygyu의 Marble Roulette은 절차적으로 생성되는 미로형 장애물 코스를 쓰지만, 이번 MVP는 구현 범위를 줄이기 위해 기존에 설계해둔 갈톤보드(못 배열) 위에 결승선만 추가하는 방식으로 단순화했다. 트랙을 더 화려하게 만들고 싶으면 Phase 2에서 `PinballMapConfig`를 확장해 새 맵을 추가하면 된다.

> **물리엔진 확장성**: 지금은 Matter.js를 쓰지만, 나중에 더 정교한 물리감을 위해 lazygyu처럼 **Box2D(box2d-wasm)로 엔진을 교체할 가능성**을 열어뒀다. `game.js`의 물리 관련 코드를 `PhysicsAdapter` 인터페이스 뒤에 감춰두면, 교체 시 영향 범위를 어댑터 구현체 하나로 좁힐 수 있다 — 다만 초기화 방식(동기↔비동기)·좌표 단위·물리 파라미터는 엔진마다 달라 재작업이 필요하다는 점은 분명히 해둔다. 자세한 설계는 [`04_pinball-map-design.md` §5](./04_pinball-map-design.md#5-확장성-나중에-물리엔진을-box2d로-교체할-수-있을까) 참고.

### 3.6 게임 결과 처리 — 클라이언트 권위 방식, 완주 순서 전체 보고
물리 시뮬레이션(Matter.js)은 브라우저에서 돌아가므로, 백엔드는 결과를 스스로 알 수 없다. 그래서 기획서에 없던 `POST /api/game/result`를 추가해, 클라이언트가 모든 참가자가 결승선을 통과한 뒤 **완주 순서 전체(`finishOrder`)**를 서버에 보고하도록 한다. 백엔드는 이 목록의 마지막 참가자를 "당첨자"로 기록한다. `GET /api/game/result/{gameId}`는 기획서대로 조회 전용으로 유지하되, 전체 순위와 당첨자를 함께 반환한다 (`02_api-spec.md` §5, §6 참고). 이 방식의 한계(클라이언트가 조작된 결과를 보고할 수 있음)는 포트폴리오 MVP에서는 허용 가능한 수준으로 명시해둔다.

## 4. Phase 2 (이번 범위 밖)
- ~~실제 AI 연동~~ — **완료** (Claude API, `devhelp/03(구 13)` 참고)
- 가중치 수동 조정 실기능
- 관리자 인증, 오류 로그 영속화
- 실제 GitHub Pages(프론트) + Render/Railway(백엔드) 배포
- 기획서의 확장 아이디어(AI 캐릭터 생성, 응원 메시지, 랜덤 이벤트)

## 5. Git/배포 준비 범위
이번 단계에서는 로컬 동작 확인과, `01_LuckyPinballAI/`를 독립된 git 저장소로 준비하는 것까지만 다룬다. 실제 GitHub 원격 저장소 생성/푸시와 공개 배포는 로컬 검증이 끝난 뒤 별도로 승인받아 진행한다.
