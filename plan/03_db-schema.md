# AI Lucky Pinball — DB 스키마 설계 (MVP)

> **AI 코멘트 — 처음 스키마와 실제 DB (2026-10-05, 프로젝트를 마치며)**
>
> 지금 기준 스키마는 [`devdocs/03_DB_스키마.md`](../devdocs/03_DB_스키마.md)입니다.
>
> **달라진 것**
>
> - **저장소**: H2 파일 하나 → 로컬·테스트는 H2, 운영은 Neon PostgreSQL. 엔티티 코드는 그대로 두고 설정(환경변수)만 바꿨습니다(`devhelp/07`).
> - **게임**: §3의 "인메모리" → 게임·참가자·순위 테이블 3개로 저장. 순위는 (게임, 참가자) 조합이 유일합니다.
> - **오류 로그 테이블** 추가(최근 1000건 유지).
> - **`source` 값**에 `FALLBACK`(Claude 실패 시 임시 점수) 추가. 캐시 재사용 대상에서 뺍니다.
> - **`player_id`의 외래 키**: 이 문서에는 `FK → PLAYER.id`로 적었지만, 실제 엔티티는 숫자 컬럼만 두고 JPA 연관관계를 맺지 않아서 **DB에 외래 키가 만들어지지 않습니다.** 2026-10-05에 참가자 목록의 N+1을 고칠 때 fetch join 대신 "전체를 한 번에 읽고 메모리에서 묶기"를 쓴 이유이기도 합니다(`devhelp/07`).
>
> **잘 된 것**
>
> 캐시를 위해 이름·생년월일을 운세 이력에 함께 저장(비정규화)하고 그 이유를 적어 둔 것. 다른 등록 건이라도 이름+생년월일이 같으면 캐시를 공유한다는 의도가 그대로 구현됐습니다.
>
> **다음에는**
>
> - **스키마 변경 방법을 첫날 정합니다.** 지금은 `ddl-auto: update`라 컬럼 추가는 되지만, 이름 변경·삭제·데이터 옮기기는 자동으로 안 되고 운영 DB에 변경 이력도 남지 않습니다. 처음부터 Flyway를 쓰면 해결됩니다(`devhelp/07` 6절).
> - **인덱스와 제약을 테이블과 같이 설계합니다.** 가장 자주 조회하는 캐시 조건(이름, 생년월일)과 참가자별 이력(`player_id`)에 인덱스가 없습니다. 같은 신원이 동시에 저장되는 문제는 서버 잠금(`KeyedLock`)으로 풀었는데, 서버가 여러 대가 되면 DB 유니크 제약이 필요합니다.

MVP에서는 **H2 파일 기반 DB** 하나만 사용한다 (`backend/data/luckypinball.mv.db`). 목적은 참가자와 운세 이력을 영속화해서, 관리자 화면(`GET /api/admin/players`)에서 참가자별 과거 기록을 조회할 수 있게 하기 위함이다 (점수 계산에는 사용하지 않는 순수 기록용).

## 1. `PLAYER`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT, PK, auto increment | |
| `name` | VARCHAR(100), NOT NULL | 이름 또는 별칭 |
| `birth_date` | DATE, **NULL 허용** | 생년월일 — 2026-09-22부터 선택사항. NULL이면 AI 호출 없이 버프 없는 상태로 참여 |
| `created_at` | TIMESTAMP, NOT NULL | 최초 등록 시각 |

## 2. `FORTUNE_RESULT`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT, PK, auto increment | |
| `player_id` | BIGINT, FK → `PLAYER.id`, NOT NULL | |
| `name` | VARCHAR(100), NOT NULL | 조회 당시 참가자 이름 (비정규화, 아래 캐시 조회용) |
| `birth_date` | DATE, NOT NULL | 조회 당시 생년월일 (비정규화, 아래 캐시 조회용) — 이 테이블에 저장되는 행은 항상 생년월일이 있는 경우만이다 |
| `fortune_score` | INT, NOT NULL (0~100) | Claude가 생성한 점수 (또는 캐시 재사용 시 과거 값 그대로) |
| `fortune_message` | VARCHAR(255) | |
| `lucky_number` | INT | |
| `created_date` | DATE, NOT NULL | 조회한 날짜 |
| `source` | VARCHAR(10) | `"AI"`(이번에 Claude를 실제로 호출함) 또는 `"CACHE"`(과거 기록 재사용) |

참가자가 `POST /api/fortune`을 호출할 때마다(생년월일이 있는 경우에 한해) 한 행이 새로 쌓인다.

### 이 테이블의 용도 (2026-09-22, Claude 연동과 함께 갱신)
- **관리자 화면**(`GET /api/admin/players`)에서 참가자별 과거 기록을 보여주는 용도.
- **AI 호출 캐시**: `name` + `birth_date`가 일치하는 과거 행이 있으면(=같은 사주), Claude를 다시 호출하지 않고 그 점수/메시지를 재사용한다 (`FortuneQueryService` 참고). `player_id`가 아니라 `name`+`birth_date`로 조회하는 이유는, 서로 다른 등록 건(다른 player_id)이라도 이름+생년월일이 같으면 같은 사람으로 보고 캐시를 공유해야 하기 때문 — 그래서 이 두 컬럼을 비정규화해서 함께 저장해둔다. 이 캐시는 날짜 제한 없이 영구적이다(하루 단위 캐시가 아님).

## 3. (참고) DB 테이블이 아닌 것들

`GameSession` / `GameResult`(완주 순위 `ranking` + 당첨자 `selectedName` 포함)는 이번 MVP에서는 **인메모리**로만 유지한다 (재시작하면 사라짐). 기획서의 `GAME_RESULT` 테이블은 Phase 2에서 실제 테이블(순위 목록을 담을 별도 자식 테이블 포함)로 승격할 예정 — 지금은 참가자 조회에 필요한 `PLAYER`/`FORTUNE_RESULT`만 영속화해서 범위를 최소화한다.

## 4. 향후 확장 (Phase 2, 지금 설계 안 함)
- `GAME_RESULT` 테이블 추가.
- H2 → MySQL 전환 (Spring Data JPA 인터페이스는 그대로 유지, `application.yml` datasource 설정만 교체).
- (참고) 애초 계획했던 "Redis + 하루 단위 캐시"는 실제로는 `FORTUNE_RESULT` 테이블 기반의 **영구 캐시**로 대체 구현됐다 (`devhelp/03(구 13)` 참고) — 하루 단위 갱신이 필요해지면 그때 `created_date` 기준으로 조회 조건을 좁히면 된다.
