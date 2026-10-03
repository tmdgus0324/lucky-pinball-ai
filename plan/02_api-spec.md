# AI Lucky Pinball — API 명세서 (MVP)

베이스 URL: `http://localhost:8080/api` (로컬 개발 기준)

공통 사항:
- 모든 요청/응답은 `application/json`.
- 날짜는 `YYYY-MM-DD`(생년월일), 타임스탬프는 ISO-8601(`2026-09-22T10:00:00Z`).
- 검증 실패 시 `400 Bad Request` + `{ "error": "message" }` 형태로 응답 (공통 포맷은 `GlobalExceptionHandler`에서 처리).

---

## 1. `POST /api/player` — 참가자 등록

`birthDate`는 **선택사항**이다 (2026-09-22 변경). 생략하거나 `null`로 보내면 이후 `/api/fortune`에서 AI를 호출하지 않고 버프 없이 참여하게 된다.

**Request**
```json
{ "name": "홍길동", "birthDate": "1995-04-12" }
```
```json
{ "name": "김유나" }
```

**Response 201**
```json
{ "playerId": 1, "name": "홍길동", "birthDate": "1995-04-12" }
```
```json
{ "playerId": 2, "name": "김유나", "birthDate": null }
```

**에러**: `name` 누락 시 `400`.

---

## 2. `POST /api/fortune` — 오늘의 운세 + 버프 조회

`FortuneService`는 이제 `MockFortuneGenerator`가 아니라 **Claude API(Haiku 4.5)** 를 호출하는 `ClaudeFortuneGenerator`가 구현한다 (2026-09-22 변경, `04_pinball-map-design.md`/`devhelp/13` 참고). 게임 방식이 "결승선 통과 순서 경쟁"이라 버프는 **시작 높이(startY)** 한 가지뿐이다 — HP나 충돌 데미지 같은 개념 자체가 없다.

동작은 참가자의 `birthDate` 유무에 따라 갈린다:

- **`birthDate`가 없으면**: AI를 호출하지 않는다. `fortuneScore`/`luckyNumber`/`fortuneMessage`는 모두 `null`, 버프는 없음(`{tier:0, startY:0}`), `source`는 `"NONE"`.
- **`birthDate`가 있으면**: 이름+생년월일이 과거에 조회된 적 있으면(=같은 사주) DB에 저장된 값을 재사용(`source: "CACHE"`, AI 호출 없음, 무료). 처음 조회하는 조합이면 Claude를 호출해서 새로 생성(`source: "AI"`)하고 그 결과를 DB에 저장해 다음부터는 캐시로 재사용한다. **이 캐시는 날짜와 무관하게 영구적이다** — 같은 이름+생년월일이면 다음 날에도 캐시를 그대로 쓴다.

**Request**
```json
{ "playerId": 1 }
```

**Response 200 (생년월일 있음, 최초 조회 → AI 호출)**
```json
{
  "playerId": 1,
  "fortuneScore": 35,
  "luckyNumber": 7,
  "fortuneMessage": "오늘은 작은 행운이 따라옵니다",
  "buff": { "tier": 3, "startY": 45 },
  "source": "AI"
}
```

**Response 200 (생년월일 없음)**
```json
{
  "playerId": 2,
  "fortuneScore": null,
  "luckyNumber": null,
  "fortuneMessage": null,
  "buff": { "tier": 0, "startY": 0 },
  "source": "NONE"
}
```

**에러**: 존재하지 않는 `playerId` → `404`. Claude API 호출 실패(키 미설정 등) → `502` + `{"error": "..."}`.

---

## 3. `POST /api/game/create` — 게임 세션 생성

**Request**
```json
{ "playerIds": [1, 2, 3] }
```

**Response 201**
```json
{
  "gameId": "b3f1c2b0-...-uuid",
  "participants": [
    {
      "playerId": 1, "name": "홍길동",
      "fortuneScore": 35, "luckyNumber": 7, "fortuneMessage": "...",
      "buff": { "tier": 3, "startY": 45 }
    }
  ]
}
```

**에러**: 참가자 수가 2명 미만이거나 8명 초과면 `400`.

---

## 4. `POST /api/game/start` — 게임 시작 표시

물리 시뮬레이션 자체는 프론트엔드(Matter.js)에서 진행되므로, 이 엔드포인트는 세션 상태를 `STARTED`로 표시하는 정도의 형식적 역할을 한다.

**Request**
```json
{ "gameId": "b3f1c2b0-...-uuid" }
```

**Response 200**
```json
{ "gameId": "b3f1c2b0-...-uuid", "status": "STARTED", "startedAt": "2026-09-22T10:00:00Z" }
```

---

## 5. `POST /api/game/result` — 게임 결과(완주 순서) 보고 *(기획서에는 없던 엔드포인트)*

물리 시뮬레이션이 브라우저에서 돌아가기 때문에 백엔드는 결과를 스스로 알 수 없다. 클라이언트는 모든 참가자가 결승선을 통과한 뒤, **완주한 순서대로 나열한 `playerId` 목록(`finishOrder`)** 을 이 엔드포인트로 보고한다. 목록의 마지막 원소가 "당첨자"가 된다.

**Request**
```json
{ "gameId": "b3f1c2b0-...-uuid", "finishOrder": [3, 4, 2, 1] }
```

**Response 200**
```json
{
  "gameId": "b3f1c2b0-...-uuid",
  "ranking": [
    { "rank": 1, "playerId": 3, "name": "박도윤" },
    { "rank": 2, "playerId": 4, "name": "이서연" },
    { "rank": 3, "playerId": 2, "name": "김유나" },
    { "rank": 4, "playerId": 1, "name": "홍길동" }
  ],
  "selectedName": "홍길동",
  "participantCount": 4,
  "createdAt": "2026-09-22T10:03:21Z"
}
```

**에러**: 존재하지 않는 `gameId` → `404`. `finishOrder`가 참가자 수와 다르거나 중복/누락이 있으면 `400`. 이미 결과가 보고된 `gameId` → `409 Conflict`.

---

## 6. `GET /api/game/result/{gameId}` — 게임 결과 조회

**Response 200**: 5번과 동일한 형태 (`ranking` 전체 + `selectedName`).
**에러**: 결과가 아직 없으면(게임이 진행 중이거나 보고되지 않음) `404`.

---

## 7. 관리자 API (`/api/admin/*`, 인증 없음 — MVP 한정)

### `GET /api/admin/players`
```json
[
  {
    "playerId": 1, "name": "홍길동", "birthDate": "1995-04-12",
    "fortuneHistory": [
      { "fortuneScore": 62, "createdDate": "2026-09-21" },
      { "fortuneScore": 35, "createdDate": "2026-09-22" }
    ]
  }
]
```

### `GET /api/admin/games`
```json
[
  { "gameId": "...", "selectedName": "홍길동", "participantCount": 4, "createdAt": "..." }
]
```

### `GET /api/admin/logs`
```json
[
  { "timestamp": "2026-09-22T10:05:00Z", "path": "/api/fortune", "message": "Player not found: 99" }
]
```
(DB 저장 — 최신순 최대 200건 응답, 서버에는 최근 1000건 유지. `traceId` 필드도 함께 내려간다)

### `POST /api/admin/fortune/override` — **스텁 (미구현)**
**Request**
```json
{ "playerId": 1, "overrideScore": 90 }
```
**Response 501**
```json
{ "error": "Not implemented yet — planned for Phase 2" }
```
