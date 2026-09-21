# AI Lucky Pinball — DB 스키마 설계 (MVP)

MVP에서는 **H2 파일 기반 DB** 하나만 사용한다 (`backend/data/luckypinball.mv.db`). 목적은 참가자와 운세 이력을 영속화해서, 관리자 화면(`GET /api/admin/players`)에서 참가자별 과거 기록을 조회할 수 있게 하기 위함이다 (점수 계산에는 사용하지 않는 순수 기록용).

## 1. `PLAYER`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT, PK, auto increment | |
| `name` | VARCHAR(100), NOT NULL | 이름 또는 별칭 |
| `birth_date` | DATE, NOT NULL | 생년월일 |
| `created_at` | TIMESTAMP, NOT NULL | 최초 등록 시각 |

기획서의 `PLAYER` 테이블과 동일한 구조.

## 2. `FORTUNE_RESULT`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | BIGINT, PK, auto increment | |
| `player_id` | BIGINT, FK → `PLAYER.id`, NOT NULL | |
| `fortune_score` | INT, NOT NULL (0~100) | Mock 생성기가 만든 **원시 점수 그대로** (블렌딩 없음) |
| `fortune_message` | VARCHAR(255) | |
| `lucky_number` | INT | |
| `created_date` | DATE, NOT NULL | 조회한 날짜 (하루 1건이 일반적이나, 여러 게임에 참여하면 여러 건 가능) |

참가자가 `POST /api/fortune`을 호출할 때마다 한 행이 새로 쌓인다. 기획서의 `FORTUNE_RESULT` 테이블과 동일한 컬럼 구성이다.

### 이 테이블의 용도 (2026-09-22 단순화)
과거에는 이 이력을 오늘 점수와 가중평균하는 데 썼지만, 버프 체계를 단순화하면서 그 로직은 제거했다. 지금은 **`GET /api/admin/players`에서 참가자별 과거 기록을 보여주는 용도로만** 저장한다 — 오늘의 점수/버프 계산에는 전혀 영향을 주지 않는 순수 기록용 데이터다.

## 3. (참고) DB 테이블이 아닌 것들

`GameSession` / `GameResult`(완주 순위 `ranking` + 당첨자 `selectedName` 포함)는 이번 MVP에서는 **인메모리**로만 유지한다 (재시작하면 사라짐). 기획서의 `GAME_RESULT` 테이블은 Phase 2에서 실제 테이블(순위 목록을 담을 별도 자식 테이블 포함)로 승격할 예정 — 지금은 참가자 조회에 필요한 `PLAYER`/`FORTUNE_RESULT`만 영속화해서 범위를 최소화한다.

## 4. 향후 확장 (Phase 2, 지금 설계 안 함)
- `GAME_RESULT` 테이블 추가.
- Redis: `name+birthDate+date`를 키로 하는 캐시 레이어 (OpenAI 실연동 시 비용 절감용).
- H2 → MySQL 전환 (Spring Data JPA 인터페이스는 그대로 유지, `application.yml` datasource 설정만 교체).
