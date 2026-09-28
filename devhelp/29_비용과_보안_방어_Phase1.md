# 29. 비용과 보안 방어 (Phase 1)

인증 없이 아무나 쓸 수 있는 공개 서비스이고, `/api/fortune`이 실제로 비용이 드는 Claude API를 호출한다. 이 조합이 갖는 두 가지 리스크(과금 폭탄, 악의적 입력)를 막는 최소한의 방어선을 깔았다.

## 왜 지금 하는가

- **DB 영속화(H2 → 실제 DB로 교체)보다 이걸 먼저 했다.** DB 영속화는 Render 무료 티어 자체가 영구 디스크를 지원하지 않아 재기동 시 초기화되는 "불편함"이지만, 이번 항목들은 지금 이 순간에도 공개 URL을 통해 실제로 노출돼 있는 리스크다. DB 마이그레이션은 핵심 계층을 건드리는 더 큰 작업이라, 면접 이후로 미뤘다.
- Anthropic 사용량 한도는 이 문서 범위 밖 — Anthropic 콘솔에서 본인이 직접 설정해야 하는 유일한 항목(월별 지출 한도 $5 + 자동충전 꺼둠으로 이중 방어 완료).

## 1. H2 콘솔 비활성화

`application.yml`:
```yaml
h2:
  console:
    enabled: ${H2_CONSOLE_ENABLED:false}   # 기본 false — 공개 배포 중이라 기본값을 꺼둔다
    path: /h2-console
```

### 진행 중 있었던 일 (기록해둘 가치가 있어서 남김)

처음에 "H2 콘솔을 꺼야 한다"고 설명 없이 바로 코드부터 고치려다가 제지당했다 — *"H2 콘솔 비활성화를 왜 해야하는거야?? Phase 1은 왜 해야하는지도 같이 설명해줘"*. 설명 후 진행 승인을 받았는데, 그 다음에 실제로 라이브 `/h2-console`에 접속해서 H2 자체의 `webAllowOthers` 기본 방어("Sorry, remote connections are disabled")가 이미 원격 접속을 막고 있다는 걸 스크린샷으로 확인해서 역으로 지적받았다.

"지금 당장 뚫려있다"는 처음 설명은 틀렸고, 정확한 상태는:
- **지금**: H2의 기본 설정(`webAllowOthers=false`)이 이미 막고 있어서 당장 SQL을 직접 실행할 수 있는 건 아니다.
- **그래도 끄는 이유**: 이건 프레임워크의 기본값에 기대는 방어일 뿐이라, 나중에 실수로 `web-allow-others: true`를 추가하거나, 프록시 뒤에서 IP 체크가 우회되는 설정 변화가 생기면 바로 뚫린다. 공개 서비스에 관리용 콘솔을 애초에 열어둘 이유가 없으니, 로컬 개발 때만 켜고(`H2_CONSOLE_ENABLED=true`) 배포 환경은 기본으로 꺼두는 게 맞다.

## 2. 입력 검증

`PlayerController.RegisterPlayerRequest`:
```java
public record RegisterPlayerRequest(
        @NotBlank @Size(max = 20) String name,
        @Past LocalDate birthDate
) {}
```

- `@Size(max = 20)`: 이름 길이 제한 없이 받으면, 아주 긴 문자열이 그대로 Claude API 프롬프트에 들어가 불필요한 토큰 비용을 유발할 수 있다.
- `@Past`: 생년월일에 미래 날짜가 들어오면 "운세 계산"이라는 도메인 자체가 의미를 잃는다. `birthDate`는 선택 필드라 null이면 검증 자체가 스킵된다(Bean Validation 기본 동작).

## 3. XSS 방지

React 쪽은 검사해보니 이미 안전했다 — JSX는 텍스트를 기본적으로 이스케이프하고, `dangerouslySetInnerHTML`을 쓰는 곳이 없다는 걸 grep으로 확인. 다만 `engine.ts`의 랭킹 패널(`renderRankPanel`)이 `innerHTML`에 참가자 이름을 문자열로 이어붙이고 있어서, 이것만 `document.createElement` + `textContent`/`Node.append(문자열)` 방식으로 바꿨다(문자열을 `append`하면 항상 안전한 텍스트 노드가 생긴다).

레거시 `frontend/js/*.js`는 모듈이 아니라 각자 독립된 `<script>` 파일이라 `escapeHtml()` 헬퍼를 `main.js`, `admin.js`에 각각 똑같이 추가하고, `innerHTML` 템플릿에 사용자 입력(이름, 운세 메시지, 로그 경로/메시지 등)이 들어가는 모든 지점에 적용했다.

## 4. `/api/fortune` Rate Limiting

`RateLimitFilter` (IP당 분당 20회, 고정 창 방식):

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String LIMITED_PATH = "/api/fortune";
    private static final int MAX_REQUESTS_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000;
    private final Map<String, Window> windowsByIp = new ConcurrentHashMap<>();
    // ...
}
```

- 클라이언트 IP는 `X-Forwarded-For` 헤더를 우선 사용 — Render가 Cloudflare 등 프록시 뒤에 있어서 `getRemoteAddr()`만 쓰면 실제 방문자가 아니라 프록시 주소로 잡힌다.
- 인메모리 카운터라 서버가 여러 대로 늘어나면 IP별 카운트가 서버마다 따로 세어지는 한계가 있지만, 지금 규모(단일 인스턴스, 무료 티어)에는 충분하다. Redis 등으로 옮기는 건 트래픽이 실제로 늘었을 때 할 일.
- 한 판에 참가자가 최대 8명 정도라 20회 한도는 정상 사용을 막지 않으면서, 반복 스크립트로 긁는 시도만 막는 수준.

`RateLimitFilterTest`로 3가지 케이스 검증: 한도까지 통과 후 21번째부터 429, IP별로 카운트 독립, `/api/fortune` 외 경로는 영향 없음.

커밋: `afa4cdc`

라이브 검증: 같은 playerId로 20회 연속 POST → 200, 21~25번째 → 429. `/api/admin/players`, `/api/player`는 영향 없이 정상 동작 확인.

## 5. (덤) `GlobalExceptionHandler`가 404를 500으로 잘못 처리하던 버그

H2 콘솔 비활성화를 배포하고 나서 라이브 관리자 로그(`/api/admin/logs`)를 확인하다가 우연히 발견한, **이번 작업과 무관하게 원래 있던 버그**.

### 증상
`/h2-console`, `/`, `/favicon.ico`처럼 존재하지 않는 경로에 요청이 오면 정상적으로 404가 나가야 하는데, 전부 `500 Unexpected error`로 응답하고 있었다. 게다가 이 요청들이 전부 관리자 오류 로그 화면(`/api/admin/logs`) — 이 프로젝트의 핵심 기능 중 하나 — 에 "장애"처럼 쌓이고 있었다.

### 원인
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex, HttpServletRequest request) {
    errorLogStore.record(request.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("Unexpected error"));
}
```
`Exception.class`를 잡는 catch-all 핸들러가 Spring의 `NoResourceFoundException`(존재하지 않는 정적 리소스 요청에 대한 404 전용 예외)까지 전부 삼켜서 500으로 바꿔버리고 있었다.

### 해결
더 구체적인 예외 타입의 핸들러가 우선 적용되는 Spring의 `@ExceptionHandler` 해석 규칙을 이용해서, `NoResourceFoundException` 전용 핸들러를 하나 추가했다 — 로그도 남기지 않고 조용히 404만 반환:

```java
// 존재하지 않는 경로 요청(예: /h2-console, /favicon.ico)은 정상적인 404 상황이지,
// 우리 서버가 잘못 동작한 게 아니다. Exception.class로 잡아버리면 이런 요청까지 전부
// "500 Unexpected error"로 둔갑하고, 운영 로그 화면(/api/admin/logs)이 실제 장애가
// 아닌 항목으로 가득 차서 정작 봐야 할 에러를 찾기 어려워진다 — 별도로 조용히 404만 낸다.
@ExceptionHandler(NoResourceFoundException.class)
public ResponseEntity<Void> handleNoResourceFound() {
    return ResponseEntity.notFound().build();
}
```

커밋: `2e7ecd0`

라이브 검증: `/`, `/h2-console`, `/favicon.ico` 모두 404로 응답 확인, `/api/admin/players` 등 정상 API는 계속 200, `/api/admin/logs`를 반복 호출해도 더 이상 새 항목이 쌓이지 않음(빈 배열 유지) 확인.

## 배운 점

"운영 로그" 기능을 포트폴리오의 핵심으로 내세우면서 정작 그 로그가 노이즈로 오염되고 있었다는 건, **기능을 만드는 것과 그 기능이 실제로 신뢰할 수 있는 데이터를 담고 있는지 확인하는 것은 별개**라는 걸 보여준다. 이번에도 로컬 테스트만으로는 안 걸리고, 라이브 배포 로그를 직접 들여다보다가 발견했다 — [[28_배포_검증과_두_버그]]에서 얻은 교훈과 같은 패턴.
