# 02. API와 DB

기본 주소는 로컬 `http://localhost:8080`, 운영 `https://lucky-pinball-backend.onrender.com`입니다. 요청과 응답은 모두 JSON입니다.

## 1. API 목록

| 메서드 | 경로 | 하는 일 | 로그인 |
|---|---|---|---|
| POST | `/api/player` | 참가자 등록 → 201 | |
| GET | `/api/players/reusable` | DB TEST용, 운세 결과가 있는 참가자 목록 | |
| POST | `/api/fortune` | 운세 확인(IP당 분당 20회) | |
| POST | `/api/game/create` | 게임 생성, 출발 높이 계산 → 201 | |
| POST | `/api/game/start` | 게임 시작 표시 | |
| POST | `/api/game/result` | 도착 순서 보고, 당첨자 확정 | |
| GET | `/api/game/result/{gameId}` | 결과 조회 | |
| POST | `/api/admin/login` | 관리자 로그인 → 토큰(IP당 분당 10회) | |
| POST | `/api/admin/logout` | 로그아웃 → 204 | 필요 |
| GET | `/api/admin/players` | 참가자 목록과 운세 이력 | 필요 |
| GET | `/api/admin/games` | 게임 목록 | 필요 |
| GET | `/api/admin/logs` | 오류 로그 최근 200건 | 필요 |
| POST | `/api/admin/ai-health` | Claude 연결 확인(5초에 한 번) | 필요 |

관리자 API는 `Authorization: Bearer <토큰>` 헤더를 보냅니다. 토큰은 12시간 동안 유효합니다.

## 2. 요청·응답 예시

**운세 확인**

```json
// 요청
{ "playerId": 1 }
// 응답
{
  "playerId": 1, "fortuneScore": 77, "luckyNumber": 7,
  "fortuneMessage": "말띠의 추진력이 빛나는 토요일...",
  "buff": { "tier": 1, "startY": 46 },
  "source": "AI"
}
```

`source`는 결과를 어디서 가져왔는지 알려 줍니다.

| 값 | 뜻 |
|---|---|
| `AI` | 이번에 Claude를 불렀음 |
| `CACHE` | 같은 이름+생년월일의 이전 결과를 재사용(비용 없음) |
| `FALLBACK` | Claude가 실패해서 임시 점수를 씀 |
| `NONE` | 생년월일이 없어서 운세 없이 참여 |

**결과 보고**

```json
// 요청: 도착한 순서대로 playerId
{ "gameId": "b3f1...", "finishOrder": [3, 1, 2] }
// 응답: 가장 늦게 도착한 사람이 당첨
{ "gameId": "b3f1...", "ranking": [{ "rank": 1, "playerId": 3, "name": "박도윤" }, ...],
  "selectedName": "김유나", "participantCount": 3, "createdAt": "2026-10-04T12:49:39Z" }
```

## 3. 오류 응답

모든 오류는 같은 형식입니다. `traceId`로 서버 로그를 찾을 수 있습니다.

```json
{ "error": "사용자에게 보여줄 메시지", "traceId": "3b46fc46" }
```

| 상태 | 언제 |
|---|---|
| 400 | 입력이 잘못됨(빈 이름, 20자 초과, 미래 생년월일 등) |
| 401 | 관리자 토큰이 없거나 만료됨, 로그인 실패 |
| 404 | 없는 참가자·게임, 아직 결과가 없음 |
| 409 | 게임 상태가 맞지 않음(결과 중복 보고, 끝난 게임 재시작 등) |
| 429 | 요청 제한 초과 |
| 500 | 예상하지 못한 서버 오류 |
| 502~504 | Claude 실패. 보통은 임시 점수로 대신해서 거의 나오지 않음 |

## 4. DB 테이블

| 테이블 | 저장하는 것 | 주요 컬럼 |
|---|---|---|
| `player_entity` | 참가자(등록할 때마다 새 행) | `id`, `name`, `birth_date`(선택), `created_at` |
| `fortune_result_entity` | 운세 조회 이력이자 AI 결과 캐시 | `player_id`, `name`, `birth_date`, `fortune_score`, `lucky_number`, `fortune_message`, `source` |
| `game_entity` | 게임 한 판 | `game_id`(UUID), `status`(CREATED → STARTED → FINISHED), `selected_name` |
| `game_participant_entity` | 그 판의 참가자와 출발 높이 | `game_id`, `player_id`, `fortune_score`, `buff_start_y` |
| `game_rank_entity` | 결과 순위 | `game_id`, `player_id`, `rank` |
| `error_log_entity` | 관리자 화면용 오류 로그(최근 1000건 유지) | `occurred_at`, `path`, `message`, `trace_id` |

알아 둘 점:

- 캐시는 `fortune_result_entity`에서 이름+생년월일로 찾습니다. 그래서 다른 날 다시 등록해도 같은 사람이면 결과를 재사용합니다. `FALLBACK` 행은 캐시로 쓰지 않습니다.
- `fortune_result_entity.player_id`에는 외래 키가 없습니다. 게임 쪽 테이블은 외래 키로 참가자와 연결되어 있습니다.
- 테이블은 JPA 엔티티를 보고 자동으로 맞춥니다(`ddl-auto: update`). 인덱스와 마이그레이션 도구(Flyway)는 아직 없습니다.
