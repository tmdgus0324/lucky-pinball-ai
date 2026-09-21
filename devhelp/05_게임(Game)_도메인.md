# 05. 게임(Game) 도메인

이 프로젝트에서 승부 방식이 "HP 생존"에서 "결승선 통과 순서 경쟁"으로 바뀐 결정이 가장 크게 반영된 부분입니다 (`plan/04_pinball-map-design.md` §0 참고).

## 1. 왜 게임 데이터는 DB가 아니라 인메모리인가

`GameRepository`는 인터페이스이고, `InMemoryGameRepository`가 `ConcurrentHashMap<String, GameSession>`으로 구현합니다. 참가자(`Player`)·운세 이력(`FortuneResultEntity`)은 H2에 저장하면서 게임 세션은 왜 메모리에만 두었을까요?

- 이번 MVP의 목적은 "결승선 통과 → 당첨자 결정"이라는 핵심 루프가 실제로 동작하는지 보여주는 것이지, 게임 기록을 영구 보관하는 게 아닙니다.
- `GameRepository`를 **인터페이스로 분리**해뒀기 때문에, 나중에 정말 DB에 저장하고 싶어지면 `JpaGameRepository` 하나만 새로 만들면 됩니다 — `GameService`는 인터페이스에만 의존하므로 코드를 바꿀 필요가 없습니다. (`03_참가자(Player)_도메인.md`에서 설명한 것과 같은 패턴입니다.)

## 2. 결승선 통과 순서를 어떻게 표현했는가

물리 시뮬레이션(Matter.js)은 **브라우저**에서 돌아갑니다. 백엔드는 공이 어떻게 움직이는지 전혀 모르기 때문에, "누가 몇 등인지"를 스스로 계산할 수 없습니다. 그래서:

1. `POST /api/game/create`로 게임을 만들면, 참가자 각각의 운세/버프가 정해진 `GameSession`이 생깁니다 (아직 순위 없음, `status = CREATED`).
2. 브라우저에서 게임이 다 끝나면, 프론트엔드가 **완주한 순서 그대로** `playerId` 목록을 `POST /api/game/result`로 보고합니다: `{"finishOrder": [4, 3, 2]}` = 4번이 1등, 2번이 꼴찌.
3. `GameService.reportResult()`가 이 목록을 검증합니다 — 참가자 수와 정확히 같은 개수인지, 중복/누락 없이 참가자 집합과 정확히 일치하는지 (`Set.equals()`로 비교). 통과하면 `RankEntry` 리스트를 만들고, **목록의 마지막 사람을 "당첨자"(selectedName)로 기록**합니다.

이 방식의 한계도 문서에 명시해뒀습니다: 클라이언트가 조작된 순서를 보고할 수 있다는 것 — 포트폴리오 MVP에서는 감수하기로 한 트레이드오프입니다 (`01_mvp-plan.md` §3.6).

## 3. 왜 결과 보고에 409(Conflict)를 뒀는가

`GameSession.hasResult()`가 `true`인데 또 `POST /api/game/result`가 오면 409를 던집니다. 이게 없으면 실수로 같은 게임 결과를 두 번 보고했을 때 당첨자가 조용히 바뀌어버릴 수 있는데, 추첨 결과처럼 "한 번 정해지면 바뀌면 안 되는" 데이터에는 이런 방어가 중요합니다. (`GameService.java`의 `reportResult` 메서드 참고.)

## 4. `GameParticipant`를 왜 `record`로 만들었는가

`GameParticipant`(참가자 1명의 이름/운세/버프)는 한 번 만들어지면 게임이 끝날 때까지 절대 바뀌지 않는 값입니다. 이렇게 "불변 데이터 덩어리"에는 자바의 `record`가 딱 맞습니다 — getter, `equals()`, `toString()`을 자동으로 만들어주고, 실수로 값을 바꾸는 코드를 원천 차단합니다. 반면 `GameSession`은 시간이 지나며 상태(`status`, `ranking`, `selectedName`)가 바뀌어야 해서 일반 클래스로 만들었습니다 — "값이 바뀌어야 하면 class, 안 바뀌면 record"가 이 프로젝트 전체에서 따른 기준입니다.

## 검증 요약 (자세한 내용은 `07_빌드와_테스트_검증.md`)

curl로 실제 3명 참가자 게임을 만들고, `finishOrder`를 임의로 지정해서 결과를 보고해봤습니다.
- 정상 케이스: 순위/당첨자가 의도대로 계산됨.
- 같은 게임에 결과를 두 번 보고 → 409 확인.
- 존재하지 않는 gameId 조회 → 404 확인.

다음 문서: [`06_관리자(Admin)_API.md`](./06_관리자(Admin)_API.md)
