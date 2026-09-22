# 13. Claude API로 실제 운세 연동

지금까지 `MockFortuneGenerator`(가짜 규칙 기반 생성기)로 대체해뒀던 자리에, 실제 Claude API를 붙여서 이름+생년월일을 분석하고 점수/행운의 숫자/메시지를 받아오도록 만들었습니다. 처음부터 `FortuneService` 인터페이스를 만들어둔 게 정확히 이 순간을 위한 것이었습니다 — 인터페이스를 구현하는 새 클래스 하나만 추가하면 됐고, 이걸 호출하는 다른 코드(`FortuneQueryService`, `FortuneController`, `GameService`)는 단 한 줄도 바뀌지 않았습니다.

## 1. 왜 "구조화된 출력(Structured Output)"을 썼는가

Claude에게 "점수, 행운의 숫자, 메시지를 알려줘"라고만 물어보면, 답변이 자유 텍스트로 와서 직접 파싱(정규식 등)해야 합니다 — 형식이 조금만 달라져도 파싱이 깨지기 쉽습니다. 대신 Claude Java SDK의 **구조화된 출력** 기능을 쓰면, 원하는 결과 형태를 자바 record로 미리 정의해두고 그 타입 그대로 돌려받을 수 있습니다.

```java
public record AiFortune(int score, int luckyNumber, String message) {}

StructuredMessageCreateParams<AiFortune> params = MessageCreateParams.builder()
    .model("claude-haiku-4-5")
    .outputConfig(AiFortune.class)   // 이 record 형태로 응답하도록 강제
    .addUserMessage("...")
    .build();

StructuredMessage<AiFortune> response = client.messages().create(params);
AiFortune result = response.content().stream()
    .flatMap(block -> block.text().stream())
    .findFirst()
    .map(StructuredTextBlock::text)   // 여기서 바로 AiFortune 타입으로 나옴
    .orElseThrow(...);
```

`@JsonPropertyDescription`으로 각 필드에 설명("0~100 사이 정수이며 높을수록 좋은 운세" 등)을 달아두면, Claude가 그 설명을 참고해서 값을 채웁니다.

### SDK 클래스 이름을 어떻게 찾았는가

`StructuredMessageCreateParams<T>.build()`를 호출하면 `client.messages().create(...)`가 뭘 반환하는지 문서 예제만 봐서는 정확한 타입 이름(`Message`인지 다른 타입인지)이 애매했습니다. 처음엔 `Message`로 짜서 컴파일했더니, **컴파일러가 정확히 어떤 타입이 필요한지 에러 메시지로 알려줬습니다**(`StructuredMessage<T>`). 그다음 `jar tf`/`javap`로 SDK jar 안의 실제 클래스 구조를 직접 열어봐서 `StructuredMessage` → `StructuredContentBlock` → `StructuredTextBlock`으로 이어지는 정확한 메서드 체인을 확인하고 고쳤습니다. **SDK 사용법을 추측하지 않고, 실제로 컴파일해보면서 컴파일러 에러를 따라가는 방식**이 문서만 보고 추측하는 것보다 훨씬 빠르고 정확했습니다.

## 2. AI 호출을 아예 안 하는 경로도 있다 — 생년월일 선택사항

사용자 피드백에 따라 등록 흐름을 이렇게 바꿨습니다.

- **생년월일을 입력하지 않으면**: `POST /api/fortune`이 Claude를 전혀 호출하지 않고 즉시 "버프 없음"(시작 높이 +0) 결과를 돌려줍니다. `source: "NONE"`으로 표시됩니다. 비용이 0입니다.
- **생년월일을 입력하면**: 아래 3번 캐시 로직을 거쳐 Claude를 호출하거나, 과거 기록을 재사용합니다.

`FortuneQueryService.getTodayFortune()`이 이 분기를 담당합니다. `FortuneQueryResult`의 `fortuneScore`/`luckyNumber`/`fortuneMessage`를 `int`가 아니라 `Integer`(nullable)로 바꾼 이유가 이것 — "운세 자체가 없는 상태"를 표현해야 했기 때문입니다.

## 3. "같은 사주"면 Claude를 다시 안 부른다 — DB 캐시

이름+생년월일이 같으면(=같은 사람), **최초 1회만 Claude를 호출**하고 그 이후로는 DB에 저장된 값을 그대로 재사용합니다. 기획서 초기부터 있었던 "AI 토큰 절약" 목표가 이제 실제 비용이 드는 진짜 문제가 됐기 때문에, 이번에 제대로 구현했습니다.

```java
Optional<FortuneResultEntity> cached = fortuneResultJpaRepository
    .findFirstByNameAndBirthDateOrderByCreatedDateAsc(player.getName(), player.getBirthDate());

if (cached.isPresent()) {
    // DB에 저장된 점수/메시지를 그대로 재사용 (source = "CACHE")
} else {
    // Claude를 호출해서 새로 생성 (source = "AI")
}
```

**중요한 설계 포인트**: 이 캐시는 "오늘 하루만" 유효한 게 아니라 **영구적**입니다 — 날짜가 지나도 같은 이름+생년월일이면 계속 같은 결과를 씁니다 (기존에 계획했던 "하루 단위 캐시"보다 더 적극적인 절약 방식으로, 사용자가 이번에 직접 요청한 방식입니다). 그리고 이 캐시 키는 `player_id`가 아니라 **이름+생년월일 자체**입니다 — 그래서 `FortuneResultEntity`에 `player_id`뿐 아니라 `name`/`birthDate`도 함께 저장해뒀습니다 (비정규화). 서로 다른 등록 건(다른 `player_id`)이라도 이름+생년월일이 같으면 캐시를 공유해야 하는데, `player_id`로만 저장하면 이걸 할 수 없기 때문입니다.

캐시를 재사용한 경우에도 이번에 조회한 `player_id` 기준으로 이력을 한 줄 남겨두는데(값은 캐시에서 복사), `source` 컬럼에 `"AI"`/`"CACHE"`를 표시해서 관리자 화면이나 로그에서 구분할 수 있게 했습니다.

## 4. 서버가 API 키 없이도 뜨도록 만들기

`AnthropicOkHttpClient.fromEnv()`를 클래스 필드 초기화 시점(생성자)에서 바로 호출했다면, `ANTHROPIC_API_KEY`가 없는 환경에서는 **스프링 앱 자체가 뜨지도 못하고 죽었을 것**입니다 — 운세 기능을 아예 안 쓰는 요청(예: 참가자 목록 조회)까지 전부 영향을 받게 됩니다. 그래서 클라이언트 생성을 **실제로 처음 호출되는 순간까지 미뤄뒀습니다**(지연 초기화).

```java
private synchronized AnthropicClient client() {
    if (client == null) {
        client = AnthropicOkHttpClient.fromEnv();
    }
    return client;
}
```

실제로 테스트해보니 `fromEnv()` 자체는 키가 없어도 예외를 던지지 않고 클라이언트를 만들었고, 대신 실제 API를 호출하는 순간 Claude 서버가 `401 x-api-key header is required`로 응답했습니다 — 이것도 `AnthropicServiceException`으로 잡아서 `502 Bad Gateway` + 명확한 한글 에러 메시지로 바꿔 내려주도록 처리해뒀습니다. 서버는 정상적으로 계속 뜬 채로, 운세 기능만 명확한 에러를 내는 것을 실제로 확인했습니다.

## 5. 프론트엔드: 일부만 실패해도 전체가 막히면 안 된다

참가자 여러 명 중 일부만 생년월일을 입력했다면, `POST /api/fortune`을 병렬로 여러 번 호출하게 되는데, 원래 코드는 `Promise.all`을 썼습니다 — 이러면 **한 명이라도 실패(예: API 키 미설정)하면 전체가 실패 처리**되어 생년월일 없는(원래 성공했어야 할) 참가자의 카드까지 안 보였습니다. `Promise.allSettled`로 바꿔서, 성공한 참가자는 카드로 보여주고 실패한 참가자만 상태 메시지에 에러로 모아 보여주도록 고쳤습니다.

## 6. 실제로 확인한 것 (API 키 없이 확인 가능한 범위까지)

아직 Anthropic API 키를 발급받지 않은 상태라 실제 Claude 호출 자체는 확인하지 못했지만, 그 앞뒤 로직은 전부 실제로 돌려서 확인했습니다.

- 서버가 `ANTHROPIC_API_KEY` 없이도 정상 기동 (`./gradlew test`의 스프링 컨텍스트 로딩 테스트 통과 포함).
- 생년월일 없이 등록 → `POST /api/fortune` 호출 시 AI 호출 없이 즉시 `{"source":"NONE", "buff":{"tier":0,"startY":0}}` 응답.
- 생년월일 입력 후 조회 → API 키가 없어 `502` + 명확한 에러 메시지 (서버 다운 없음).
- Playwright로 생년월일 있는 참가자 2명 + 없는 참가자 2명을 섞어 등록 → "운세 확인" 클릭 시 없는 2명은 정상적으로 카드가 뜨고, 있는 2명만 에러 메시지로 안내되는 것을 화면으로 확인.

## 7. 앞으로 필요한 것 — API 키 발급

1. https://console.anthropic.com 에서 계정 생성/로그인 후 API Keys 메뉴에서 키 발급.
2. 백엔드를 실행하는 터미널에서 환경변수로 설정:
   ```bash
   export ANTHROPIC_API_KEY=sk-ant-...
   ./gradlew bootRun
   ```
   (Windows PowerShell이라면 `$env:ANTHROPIC_API_KEY="sk-ant-..."`)
3. 키는 **절대 코드/git에 커밋하지 않는다** — 환경변수로만 전달한다. `.gitignore`에 이미 `data/`, `build/` 등은 제외돼 있지만, 혹시라도 키를 파일에 적어두는 습관을 들이지 않는 게 중요하다.

키를 설정한 뒤 생년월일이 있는 참가자로 "운세 확인"을 눌러보면, 이번에는 실제 Claude가 생성한 점수/메시지가 나오는지 확인할 수 있습니다 — 같은 이름+생년월일로 두 번째 조회하면 `source: "CACHE"`로 바뀌면서 즉시(AI 호출 없이) 응답이 오는 것도 함께 확인해보면 좋습니다.
