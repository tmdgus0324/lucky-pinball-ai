# 02. API 명세 (현재 코드 기준)

기본 주소: 로컬 `http://localhost:8080`, 운영 `https://lucky-pinball-backend.onrender.com`. 모든 요청·응답은 JSON입니다.

> `plan/02_api-spec.md`는 MVP 설계 당시의 명세라 지금과 다른 부분이 있습니다. 이 문서가 현재 코드(`*Controller.java`) 기준입니다.

## 1. 공통

**응답 헤더**: 모든 응답에 `X-Trace-Id`(8자리 16진수)가 붙습니다. 서버가 항상 새로 만들고, 클라이언트가 보낸 값은 쓰지 않습니다.

**오류 응답 형식**

```json
{ "error": "사용자에게 보여줄 메시지", "traceId": "3b46fc46" }
```

| 상태 | 언제 |
|---|---|
| 400 | 입력 검증 실패, 깨진 JSON, 형식 오류 |
| 401 | 관리자 토큰 없음·만료, 로그인 실패 |
| 404 | 없는 참가자·게임, 결과가 아직 없음, 없는 경로 |
| 405 / 415 | 지원하지 않는 메서드 / Content-Type |
| 409 | 게임 상태 충돌(결과 중복 보고, 끝난 게임 재시작, 시작 전 결과 보고) |
| 429 | 요청 제한 초과 — `/api/fortune` 방문자 IP당 분당 20회, `/api/admin/login` 분당 10회(IP는 앞단 Cloudflare가 넣는 `True-Client-IP` 기준, 경로별로 따로 셈). AI 연결 확인 5초 이내 재요청 |
| 500 | 예상하지 못한 서버 오류(메시지는 일반 문구, 원인은 서버 로그) |
| 502 / 503 / 504 | Claude 실패 / Claude 요청 과다 / Claude 타임아웃 — 기본 설정에서는 임시 점수로 대체되어 거의 나오지 않음 |

## 2. 참가자

### `POST /api/player` → 201

```json
// 요청 — birthDate는 선택(없으면 버프 없이 참여, AI 호출 안 함)
{ "name": "홍길동", "birthDate": "1990-01-01" }
// 응답
{ "playerId": 1, "name": "홍길동", "birthDate": "1990-01-01" }
```

- `name`: 필수, 공백 불가, 최대 20자 / `birthDate`: 과거 날짜만

### `GET /api/players/reusable` → 200

DB TEST 버튼용 공개 조회. 운세 결과가 있는(AI·CACHE) 신원만 최소 필드로 돌려줍니다.

```json
[ { "playerId": 3, "name": "홍길동", "birthDate": "1990-01-01", "fortuneSource": "AI" } ]
```

## 3. 운세

### `POST /api/fortune` → 200 (요청 제한 대상)

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

| `source` | 의미 |
|---|---|
| `AI` | 이번에 Claude를 호출함 |
| `CACHE` | 같은 이름+생년월일의 이전 결과를 재사용(비용 0) |
| `FALLBACK` | Claude 실패로 규칙 기반 임시 점수(메시지는 안내 문구) |
| `NONE` | 생년월일 미입력 — 점수·메시지 `null`, 버프 없음 |

## 4. 게임

| 메서드·경로 | 요청 | 응답 |
|---|---|---|
| `POST /api/game/create` → 201 | `{ "playerIds": [1, 2] }` (2~8명) | `{ "gameId", "participants": [{ playerId, name, fortuneScore, luckyNumber, fortuneMessage, buff }] }` |
| `POST /api/game/start` → 200 | `{ "gameId" }` | `{ "gameId", "status": "STARTED", "startedAt" }` |
| `POST /api/game/result` → 200 | `{ "gameId", "finishOrder": [2, 1] }` (도착 순서, 참가자 각 1번씩) | `{ "gameId", "ranking": [{ rank, playerId, name }], "selectedName", "participantCount", "createdAt" }` |
| `GET /api/game/result/{gameId}` → 200 | — | 위와 같음 |

- 시작 높이(`buff.startY`)는 **이번 판 최고점자 대비** 점수 차이 × 26px(최대 10점 차이)입니다. 최고점자는 0(맨 위, 결승선에서 가장 멂).
- 당첨자(`selectedName`)는 **가장 늦게 도착한 사람**입니다.
- 상태 규칙: 이미 시작한 게임의 시작 요청은 그대로 돌려줌, 끝난 게임 재시작·시작 전 결과 보고·결과 중복 보고는 409.

## 5. 관리자 (`/api/admin/**`, 로그인 토큰 필요)

헤더: `Authorization: Bearer <token>` — 토큰은 서버 메모리에 12시간 보관.

| 메서드·경로 | 설명 |
|---|---|
| `POST /api/admin/login` | `{ "username", "password" }` → `{ "token" }` (토큰 불필요). 계정은 환경변수 `ADMIN_USERNAME`/`ADMIN_PASSWORD`. IP당 분당 10회까지 시도 가능(초과 시 429) |
| `POST /api/admin/logout` | 토큰 폐기 → 204 |
| `GET /api/admin/players` | 참가자 목록(최신순)과 운세 이력, `fortuneSource`(AI > CACHE > FALLBACK > null) |
| `GET /api/admin/games` | 게임 목록 `{ gameId, selectedName, participantCount, createdAt }` |
| `GET /api/admin/logs` | 오류 로그 최신 200건 `{ timestamp, path, message, traceId }` |
| `POST /api/admin/ai-health` | Claude 연결 확인(출력 1토큰 실제 호출). 결과는 항상 200, 5초 이내 재요청은 429 |

`ai-health` 응답:

```json
{ "ok": false, "status": "CREDIT_EXHAUSTED", "message": "크레딧 잔액이 부족합니다. ...", "upstreamStatus": 400,
  "latencyMillis": 312, "model": "claude-haiku-4-5", "timeoutSeconds": 10, "fallbackEnabled": true,
  "checkedAt": "2026-10-03T10:30:00Z" }
```

`status`: `OK` · `KEY_MISSING` · `AUTH_FAILED` · `CREDIT_EXHAUSTED` · `RATE_LIMITED` · `TIMEOUT` · `UNREACHABLE` · `UPSTREAM_ERROR` · `ERROR`

> 예전에 있던 `POST /api/admin/fortune/override`(가중치 조정 스텁, 501)는 제거했습니다.
