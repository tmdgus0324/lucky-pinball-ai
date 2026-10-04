# 01. MVP 구현 기록

개발 환경을 준비하고 백엔드 도메인(참가자·운세·게임·관리자)과 첫 프론트엔드를 만든 초기 기록이다. 빌드·테스트·화면 확인 방법도 여기서 처음 정했다.

> 원래 따로 있던 기록 9개를 2026-10-04에 하나로 묶었다. 내용은 그대로 두고 시간 순서대로 이어 붙였으며, 각 절 제목의 "[구 NN]"이 원래 문서 번호다.

**포함된 기록**

- [구 01] 개발 환경 준비
- [구 02] 백엔드 공통 설정 (CORS, 예외 처리, DB 연결)
- [구 03] 참가자(Player) 도메인
- [구 04] 운세(Fortune) 도메인
- [구 05] 게임(Game) 도메인
- [구 06] 관리자(Admin) API
- [구 07] 빌드와 테스트 검증
- [구 08] 프론트엔드 구현
- [구 10] Playwright로 실제 화면 확인


> **AI 코멘트 — 이 문서를 읽을 때 주의할 점**
>
> 초기 설계 기록이라 **이후에 바뀐 내용이 많습니다.** 아래 표의 왼쪽은 이 문서에 적힌 내용이고, 지금 코드는 오른쪽입니다. 이 문서만 읽고 현재 동작으로 오해하지 않도록 정리했습니다.
>
> | 이 문서의 내용 | 지금 | 바뀐 기록 |
> |---|---|---|
> | 운세 이력 테이블은 "기록용, 과거 기록을 읽지 않음" ([구 04]) | 이름+생년월일 기준 **AI 결과 캐시** — 비용 절감의 핵심 | `devhelp/03`(구 13·27) |
> | 운세는 `MockFortuneGenerator`가 생성 ([구 04]) | Claude API. Mock은 단위 테스트와 장애 시 임시 점수용 | `devhelp/03`(구 13), `devhelp/09`(구 41) |
> | 버프는 5단계(0/15/30/45/60) ([구 04]) | 이번 판 최고점자 대비 점수 차이 × 26px, 최대 10점(상대평가) | `devhelp/02`(구 24), `devhelp/03`(구 26) |
> | 게임 세션은 메모리 저장 ([구 05]) | JPA로 DB 저장 | `devhelp/07` |
> | 게임 상태 검사는 "결과 중복 보고"만 ([구 05]) | 끝난 게임 재시작, 시작 전 결과 보고도 409 | `devhelp/09`(구 45) |
> | 오류 로그는 인메모리 리스트 ([구 02]) | DB 저장, 최근 1000건 유지 | `devhelp/09`(구 42) |
> | 관리자 API는 인증 없음 ([구 06]) | 환경변수 계정 + 로그인 토큰 | `devhelp/08`(구 32) |
> | 가중치 조정은 501 스텁 ([구 06]) | 기능을 하지 않기로 하고 제거 | `devhelp/08`(구 47) |
>
> **돌아보면 잘 맞은 판단**
>
> - [구 05]의 "`GameRepository`를 인터페이스로 분리해 두면 나중에 `JpaGameRepository`만 만들면 된다"는 예측은 실제로 그대로 됐습니다(`devhelp/07`). `GameService`는 저장 방식이 바뀌어도 거의 손대지 않았습니다. "인터페이스로 나눴다"보다 **"나눈 덕분에 실제로 교체했다"** 가 면접에서 훨씬 설득력 있는 근거입니다.
> - [구 07]의 Windows 인코딩(`x-windows-949`) 문제는 나중에 로그를 한글로 쓰면서 다시 나타났고, 운영 Docker 실행 옵션에 `-Dfile.encoding=UTF-8`을 넣는 것으로 이어졌습니다(`devhelp/09`(구 40)). "지금 안 터진다 ≠ 안전하다"는 이 문서의 말이 실제로 맞았던 사례입니다.
>
> **아쉬운 점**
>
> - [구 06]의 "501로 응답하는 스텁"은 당시엔 합리적이었지만, 결국 구현되지 않은 채 관리자 화면에 "준비 중" 버튼으로 오래 남았다가 제거됐습니다. 만들 계획이 확실하지 않은 기능은 **화면에는 노출하지 않고 백로그에만 두는 편**이 완성도에 낫습니다.
> - [구 10]에서 "버프가 통계적으로 의미 있게 작동하는지는 수십~수백 판을 돌려야 안다"고 적었는데, 이 기록들 안에서는 그 검증을 한 흔적이 없습니다. 맵 쪽은 나중에 반복 측정을 했지만(`devhelp/02`(구 36)), **버프 효과(운세가 나쁜 사람이 실제로 덜 당첨되는가)** 를 판 수로 측정한 결과는 아직 없습니다.

---

## [구 01] 개발 환경 준비

이 문서는 실제 코딩을 시작하기 직전에 어떤 준비 작업을 왜 했는지 설명합니다.

### 1. 왜 환경 점검부터 했는가

`plan/` 폴더의 설계 문서들은 이미 완성돼 있었지만, 백엔드는 Java + Spring Boot + Gradle로 만들기로 했었습니다. 코드를 짜는 것과 그 코드를 **빌드/실행해서 검증하는 것**은 별개 문제라서, 코딩을 시작하기 전에 먼저 이 PC에 아래가 있는지 확인했습니다.

```bash
java -version
gradle -version
```

결과: **둘 다 설치돼 있지 않았습니다.** 이 상태에서 코드만 잔뜩 작성하면, 오타나 설계 실수를 즉시 발견하지 못하고 나중에 한꺼번에 디버깅해야 하는 위험이 있습니다. 그래서 코딩에 들어가기 전에 사용자에게 "JDK를 지금 설치할지, 나중에 직접 설치할지"를 먼저 물어봤고, **지금 설치**하는 쪽으로 진행했습니다.

### 2. JDK 설치 — winget 사용

Windows에는 `winget`(Windows Package Manager)이 기본 내장돼 있는 경우가 많습니다. 먼저 어떤 패키지가 있는지 검색:

```powershell
winget search Temurin.17
```

`EclipseAdoptium.Temurin.17.JDK`(Eclipse Temurin — OpenJDK의 무료 배포판)를 확인하고 설치:

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-package-agreements --accept-source-agreements --silent
```

**중요한 함정**: 설치가 끝나도 winget이 시스템 PATH 환경변수를 갱신하는 건 "새로 여는 셸"부터 적용됩니다. 이미 떠 있는 터미널(이 세션의 Bash 도구 포함)은 설치 이전 환경을 그대로 들고 있어서, 설치 직후에 `java -version`을 쳐도 여전히 "command not found"가 나옵니다. 그래서 이후의 모든 빌드 명령에서는 매번 아래처럼 **직접 JAVA_HOME/PATH를 지정**해줬습니다.

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"
```

(참고: 이 프로젝트를 이어서 개발할 때 새 터미널을 열면 PATH가 이미 반영돼 있어서 이 export 없이도 `java -version`이 바로 동작할 가능성이 높습니다.)

### 3. 프로젝트 뼈대 — 직접 작성 대신 Spring Initializr 사용

Gradle 프로젝트는 `build.gradle`, `settings.gradle` 외에도 `gradlew`/`gradlew.bat`(래퍼 스크립트)와 `gradle/wrapper/gradle-wrapper.jar`(바이너리)가 있어야 "Gradle을 따로 설치하지 않아도" 빌드할 수 있습니다. 이 래퍼 파일들은 손으로 만드는 게 아니라 보통 `gradle wrapper` 명령으로 생성하는데, 로컬에 Gradle이 없으니 그 명령 자체를 못 씁니다.

대신 **Spring Initializr**(`https://start.spring.io`)가 제공하는 REST API를 그대로 호출해서, 올바른 래퍼가 포함된 프로젝트 zip을 받았습니다:

```bash
curl -s "https://start.spring.io/starter.zip" \
  -d dependencies=web,validation,data-jpa,h2 \
  -d type=gradle-project \
  -d language=java \
  -d javaVersion=17 \
  -d groupId=com.luckypinball \
  -d artifactId=backend \
  -d name=backend \
  -d packageName=com.luckypinball \
  -d baseDir=backend \
  -o backend.zip
```

이렇게 하면 실무에서도 흔히 쓰는 "start.spring.io에서 프로젝트 생성 → 압축 해제 → 코딩 시작" 흐름을 그대로 재현할 수 있고, 래퍼 파일을 손으로 잘못 만들 위험도 없앨 수 있습니다.

받은 프로젝트를 열어보니, 이 시점(2026년 9월) 기준 최신 안정 버전인 **Spring Boot 4.1.1**이 기본으로 선택돼 있었습니다. `plan/01_mvp-plan.md`를 쓸 때는 "Spring Boot 3.x"라고 가정했었는데, 실제로 프로젝트를 만들어보니 그 사이에 4.x가 나와 있던 것 — 이런 건 미리 다 예측하기보다는, **실제로 도구를 실행해서 확인하고 거기에 맞춰나가는 게** 더 정확합니다. Spring Boot 4에서는 의존성 이름도 일부 달라졌습니다 (예: `spring-boot-starter-web` 대신 `spring-boot-starter-webmvc`, H2 콘솔이 `spring-boot-h2console`로 분리). 이런 세부사항은 [구 02]와 `build.gradle`에서 확인할 수 있습니다.

### 4. 기본 생성물 중 정리한 것

- `HELP.md` (Initializr가 넣어주는 안내 문서) — 삭제. 우리 프로젝트에는 이 `devhelp/` 문서들이 그 역할을 대신합니다.
- `BackendApplication.java` → `LuckyPinballApplication.java`로 이름 변경 (계획 문서에 적어둔 이름과 맞추기 위해).
- `application.properties` → `application.yml`로 교체 (계획 문서(`03_db-schema.md`)에 YAML 설정을 기준으로 적어뒀기 때문).

### 5. 로컬 실행 명령어

이후로도 계속 쓰는 명령어라 여기 정리해둔다. 백엔드·프론트를 각각 다른 터미널에서 띄운다.

```bash
# 백엔드 (http://localhost:8080)
cd backend
./gradlew bootRun
```

`ANTHROPIC_API_KEY` 환경변수가 있어야 실제 운세 분석(Claude 호출)이 된다. 없어도 서버는 정상적으로 뜨고, 생년월일 없이 참여하는 참가자는 그대로 쓸 수 있다 (`ClaudeFortuneGenerator`가 클라이언트를 지연 생성하기 때문 — `devhelp/08(구 29)` 참고).

```bash
# 프론트엔드 (http://localhost:5173)
cd frontend-react
npm install   # 처음 한 번, 또는 package.json이 바뀐 뒤
npm run dev
```

둘 다 띄운 상태에서 `http://localhost:5173`으로 접속한다. 종료는 각 터미널에서 `Ctrl+C`.

---

## [구 02] 백엔드 공통 설정 (CORS, 예외 처리, DB 연결)

기능별 도메인(참가자/운세/게임/관리자)을 만들기 전에, 모든 기능이 공통으로 쓰는 뼈대부터 먼저 만들었습니다. 이렇게 순서를 잡은 이유는, 이후 도메인 코드를 짤 때 "에러가 나면 어떻게 응답할지", "DB는 어떻게 연결돼 있는지"를 이미 정해둔 상태에서 시작하면 코드가 더 일관되기 때문입니다.

### 1. `application.yml` — H2 파일 DB 연결

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/luckypinball;AUTO_SERVER=TRUE
```

`03_db-schema.md`(plan 문서)에서 정한 대로, MySQL이나 Redis 없이 **H2를 파일 모드**로 띄웁니다. `jdbc:h2:mem:...`(메모리 모드)이 아니라 `jdbc:h2:file:...`을 쓴 이유는, 서버를 껐다 켜도 참가자/운세 이력이 남아있어야 관리자 화면에서 "과거 기록"을 보여주는 의미가 있기 때문입니다. 실제로 실행하면 `backend/data/luckypinball.mv.db` 파일이 생기는데, 이건 `.gitignore`에 추가해서 저장소에는 올라가지 않게 했습니다.

`jpa.hibernate.ddl-auto: update`는 엔티티 클래스(`PlayerEntity`, `FortuneResultEntity`)를 보고 Hibernate가 테이블을 자동으로 만들어주는 설정입니다. 정식 서비스라면 위험한 설정(스키마가 의도치 않게 바뀔 수 있음)이지만, 아직 마이그레이션 도구(Flyway 등)를 두지 않은 MVP 단계에서는 실용적인 선택입니다.

### 2. `common` 패키지 — 에러 처리를 먼저 통일한 이유

`api-spec.md`를 보면 거의 모든 엔드포인트에 "존재하지 않으면 404", "중복이면 409" 같은 에러 규칙이 있습니다. 이걸 컨트롤러마다 각자 `if (...) return ResponseEntity.status(404)...`처럼 반복해서 쓰면 실수하기 쉽고, 나중에 응답 형식(`{"error": "..."}`)을 바꾸려면 모든 컨트롤러를 다 고쳐야 합니다.

그래서 이렇게 나눴습니다.

- **`ApiException`**: `RuntimeException`을 상속한 커스텀 예외. `HttpStatus`를 같이 들고 다닙니다. `ApiException.notFound(...)`, `.badRequest(...)`, `.conflict(...)`, `.notImplemented(...)`처럼 정적 팩토리 메서드로 만들어서, 서비스 코드에서는 `throw ApiException.notFound("Player not found: " + id);`처럼 한 줄로 던질 수 있게 했습니다.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`): 모든 컨트롤러의 예외를 한곳에서 가로챕니다. `ApiException`이 오면 그 안에 담긴 상태 코드로 응답하고, 검증 실패(`@Valid`가 막아낸 경우)는 400으로, 나머지 예상 못한 예외는 500으로 응답합니다. **Spring이 알아서 이 클래스를 찾아 적용**하기 때문에(어노테이션 기반), 각 컨트롤러는 이 존재를 몰라도 됩니다 — 이게 `@RestControllerAdvice`의 핵심 포인트입니다.
- **`ErrorLogStore` / `ErrorLogEntry`**: `GlobalExceptionHandler`가 예외를 처리하는 김에, 그 내용을 인메모리 리스트에도 같이 쌓아둡니다. 이게 나중에 관리자 화면의 "오류 로그 조회" 기능의 데이터 원천이 됩니다. 별도로 로깅 코드를 여기저기 넣지 않고, 에러 처리 로직 한곳에 자연스럽게 곁들인 것입니다.

### 3. `WebConfig` — CORS

프론트엔드(`http://localhost:5500` 같은 정적 서버)와 백엔드(`http://localhost:8080`)는 **오리진(origin)이 다릅니다.** 브라우저는 기본적으로 다른 오리진으로의 요청을 차단하는데(CORS 정책), 이걸 허용해주지 않으면 나중에 프론트엔드 `fetch()` 호출이 전부 실패합니다. `WebConfig`에서 `/api/**` 경로에 한해 로컬 정적 서버 오리진을 허용해뒀습니다.

### 검증

이 단계에서는 아직 실행해볼 도메인 로직이 없어서, 전체 코드가 다 갖춰진 뒤([구 07])에 한꺼번에 컴파일/실행 검증을 했습니다.

---

## [구 03] 참가자(Player) 도메인

가장 단순한 도메인부터 만들어서, 이후 도메인들이 따라갈 패턴(Entity → Repository → Service → Controller)을 먼저 확립했습니다.

### 왜 이 순서로 클래스를 나누는가

Spring Boot(그리고 대부분의 백엔드 프레임워크)에서 자주 쓰는 4단 구조입니다.

1. **Entity** (`PlayerEntity`) — DB 테이블 한 행을 자바 객체로 표현. `@Entity`가 붙으면 JPA(Hibernate)가 이 클래스를 보고 테이블을 만들고, 객체를 저장/조회할 때 자동으로 SQL을 생성해줍니다.
2. **Repository** (`PlayerJpaRepository`) — DB 접근을 담당하는 인터페이스. `JpaRepository<PlayerEntity, Long>`을 상속하기만 하면 `save()`, `findById()` 같은 기본 CRUD 메서드를 **직접 구현하지 않아도** Spring Data JPA가 런타임에 구현체를 만들어줍니다.
3. **Service** (`PlayerService`) — 실제 비즈니스 로직. "존재하지 않는 참가자를 조회하면 404 예외를 던진다" 같은 규칙이 여기 들어갑니다. 컨트롤러가 아니라 서비스에 이 로직을 두는 이유는, 나중에 이 로직을 다른 곳(예: 관리자 API)에서도 재사용할 수 있게 하기 위함입니다.
4. **Controller** (`PlayerController`) — HTTP 요청/응답만 담당. `@RequestBody`로 JSON을 받고, 서비스를 호출하고, 결과를 다시 JSON으로 돌려줍니다. **비즈니스 로직은 최대한 넣지 않습니다** — 그래야 나중에 "REST API가 아니라 배치 작업에서도 이 로직을 쓰고 싶다" 같은 요구가 와도 서비스만 재사용하면 됩니다.

### 코드에서 눈여겨볼 부분

- `PlayerEntity`에 **기본 생성자(`protected PlayerEntity() {}`)** 가 있습니다. JPA는 DB에서 데이터를 읽어와 객체를 만들 때 리플렉션으로 기본 생성자를 호출하기 때문에, `@Entity` 클래스에는 인자 없는 생성자가 반드시 있어야 합니다 (직접 코드에서 쓸 일은 없어서 `protected`로 막아뒀습니다).
- `PlayerController.RegisterPlayerRequest`/`PlayerResponse`를 **컨트롤러 안에 중첩 record**로 정의했습니다. 별도 `dto` 패키지를 만들 수도 있지만, `01_mvp-plan.md`에서 "포트폴리오 MVP 규모에 과한 추상화는 피한다"고 정해뒀던 방향과 맞춰서, 요청/응답 모양이 그 컨트롤러에서만 쓰인다면 굳이 파일을 분리하지 않았습니다.
- `@Valid` + `@NotBlank`/`@NotNull`: 요청 바디 검증. 예를 들어 `name`이 비어있으면 컨트롤러 로직에 도달하기도 전에 Spring이 막아주고, [구 02]에서 만든 `GlobalExceptionHandler`가 이를 400 응답으로 변환합니다.

### 검증

전체 빌드/실행 검증은 모든 도메인을 다 만든 뒤 한 번에 진행했습니다 ([구 07]). 그때 실제로 `POST /api/player`를 호출해서 참가자가 정상적으로 등록되고 `playerId`가 자동 증가하는 것을 확인했습니다.

---

## [구 04] 운세(Fortune) 도메인

이 프로젝트에서 가장 설계 결정이 많이 들어간 부분입니다. `plan/01_mvp-plan.md`와 `plan/04_pinball-map-design.md`에서 여러 번 바뀐 내용(재방문 가중치 도입 → 제거, HP/충돌 데미지 → 시작 높이만)이 최종적으로 이 코드에 반영돼 있습니다.

### 1. 왜 "생성기"와 "저장/조합"을 분리했는가

`FortuneService`는 인터페이스 하나뿐입니다.

```java
public interface FortuneService {
    FortuneResult analyze(String name, LocalDate birthDate);
}
```

지금은 `MockFortuneGenerator`가 이걸 구현해서 규칙 기반으로 점수를 만들지만, 나중에 실제 OpenAI API를 호출하는 `OpenAiFortuneGenerator`를 새로 만들어서 **이 인터페이스만 구현**하면, 그걸 호출하는 다른 코드(`FortuneQueryService`)는 한 줄도 안 바꿔도 됩니다. 이런 설계를 "구현을 인터페이스 뒤로 숨긴다"고 하는데, Spring에서는 `@Service`가 붙은 클래스가 자동으로 그 인터페이스 타입의 빈(bean)으로 등록되기 때문에, 나중에 구현체를 교체해도 `FortuneQueryService` 생성자의 `FortuneService fortuneService` 파라미터 타입은 그대로 둬도 됩니다.

### 2. `MockFortuneGenerator`가 "같은 사람, 같은 날"에 같은 결과를 주는 이유

```java
long seed = Objects.hash(name, birthDate, LocalDate.now());
Random random = new Random(seed);
```

완전 무작위(`new Random()`)로 만들면, 같은 사람이 같은 날 두 번 조회했을 때 점수가 매번 달라집니다. 기획서 원안에는 "같은 날 재조회 시 캐시된 결과 사용"이라는 요구사항이 있었는데, 실제 캐시(Redis)는 아직 안 만들었지만 **시드를 name+birthDate+오늘 날짜로 고정**하면 "같은 입력 → 같은 결과"라는 캐시와 똑같은 성질을 얻을 수 있습니다. 이건 나중에 진짜 캐시를 붙일 때도 자연스럽게 이어지는 설계입니다.

### 3. `BuffCalculator` — 왜 별도 클래스로 뺐는가

```java
public static Buff fromScore(int fortuneScore) { ... }
```

점수를 버프로 바꾸는 로직은 **입력이 같으면 출력도 항상 같은 순수 함수**입니다 (DB도, 외부 API도 건드리지 않음). 이런 로직은 Spring 빈으로 만들 필요 없이 `static` 메서드로 두는 게 더 간단하고, 테스트하기도 훨씬 쉽습니다 (`BuffCalculatorTest`에서 스프링 컨텍스트를 띄우지 않고도 순식간에 검증할 수 있었던 이유입니다).

버프 값(시작 Y 오프셋)은 여러 번의 대화를 거쳐 최종적으로 이렇게 확정됐습니다: HP나 충돌 데미지 차등은 전부 없어지고, **시작 높이 하나만** 존재합니다 (0/15/30/45/60). 운세가 안 좋을수록 결승선에 가까운 곳(오프셋이 큼)에서 출발합니다.

### 4. `FortuneQueryService` — 왜 컨트롤러 로직을 한 번 더 감쌌는가

`POST /api/fortune`(참가자 개인이 운세를 확인)과 `POST /api/game/create`(게임에 참여할 모든 참가자의 운세를 한꺼번에 확인)는 **완전히 같은 로직**(참가자 조회 → 운세 생성 → 버프 계산 → 이력 저장)이 필요합니다. 이 로직을 `FortuneController`에만 두면 `GameService`에서 코드를 복사/붙여넣기 해야 하는데, 그러면 나중에 로직이 하나만 바뀌고 다른 하나는 안 바뀌는 실수가 생기기 쉽습니다. 그래서 이 공통 로직을 `FortuneQueryService.getTodayFortune(playerId)` 하나로 뽑아뒀고, `FortuneController`와 `GameService` 둘 다 이걸 호출합니다.

### 5. `FortuneResultEntity` — 이제는 "기록용"일 뿐

원래 계획에는 이 테이블의 과거 점수를 오늘 점수와 가중평균하는 기능이 있었지만, 대화 중에 "버프가 복잡해 보인다"는 피드백과 함께 이 가중치 로직 자체를 없앴습니다. 그래서 지금 `FortuneQueryService`는 과거 기록을 **읽지 않고**, 매번 새로 만든 점수를 그대로 쓰되, 그 결과를 `FortuneResultJpaRepository`에 저장만 해둡니다 — 이게 나중에 관리자 화면(`GET /api/admin/players`)에서 "이 사람이 지금까지 몇 점을 받아왔는지" 보여주는 용도로 쓰입니다.

### 검증 요약 (자세한 내용은 [구 07])

- `BuffCalculatorTest`: 점수 구간 경계값(0, 19, 20, 39, ...)이 정확한 티어로 변환되는지 확인.
- `MockFortuneGeneratorTest`: 점수가 0~100 범위를 벗어나지 않는지, 같은 사람·같은 날은 결정적인지 확인.
- 실제 서버를 띄워서 `POST /api/fortune` 호출 → 응답의 `buff.startY`가 `BuffCalculator` 표와 정확히 일치하는 것을 curl로 확인.

---

## [구 05] 게임(Game) 도메인

이 프로젝트에서 승부 방식이 "HP 생존"에서 "결승선 통과 순서 경쟁"으로 바뀐 결정이 가장 크게 반영된 부분입니다 (`plan/04_pinball-map-design.md` §0 참고).

### 1. 왜 게임 데이터는 DB가 아니라 인메모리인가

`GameRepository`는 인터페이스이고, `InMemoryGameRepository`가 `ConcurrentHashMap<String, GameSession>`으로 구현합니다. 참가자(`Player`)·운세 이력(`FortuneResultEntity`)은 H2에 저장하면서 게임 세션은 왜 메모리에만 두었을까요?

- 이번 MVP의 목적은 "결승선 통과 → 당첨자 결정"이라는 핵심 루프가 실제로 동작하는지 보여주는 것이지, 게임 기록을 영구 보관하는 게 아닙니다.
- `GameRepository`를 **인터페이스로 분리**해뒀기 때문에, 나중에 정말 DB에 저장하고 싶어지면 `JpaGameRepository` 하나만 새로 만들면 됩니다 — `GameService`는 인터페이스에만 의존하므로 코드를 바꿀 필요가 없습니다. ([구 03]에서 설명한 것과 같은 패턴입니다.)

### 2. 결승선 통과 순서를 어떻게 표현했는가

물리 시뮬레이션(Matter.js)은 **브라우저**에서 돌아갑니다. 백엔드는 공이 어떻게 움직이는지 전혀 모르기 때문에, "누가 몇 등인지"를 스스로 계산할 수 없습니다. 그래서:

1. `POST /api/game/create`로 게임을 만들면, 참가자 각각의 운세/버프가 정해진 `GameSession`이 생깁니다 (아직 순위 없음, `status = CREATED`).
2. 브라우저에서 게임이 다 끝나면, 프론트엔드가 **완주한 순서 그대로** `playerId` 목록을 `POST /api/game/result`로 보고합니다: `{"finishOrder": [4, 3, 2]}` = 4번이 1등, 2번이 꼴찌.
3. `GameService.reportResult()`가 이 목록을 검증합니다 — 참가자 수와 정확히 같은 개수인지, 중복/누락 없이 참가자 집합과 정확히 일치하는지 (`Set.equals()`로 비교). 통과하면 `RankEntry` 리스트를 만들고, **목록의 마지막 사람을 "당첨자"(selectedName)로 기록**합니다.

이 방식의 한계도 문서에 명시해뒀습니다: 클라이언트가 조작된 순서를 보고할 수 있다는 것 — 포트폴리오 MVP에서는 감수하기로 한 트레이드오프입니다 (`01_mvp-plan.md` §3.6).

### 3. 왜 결과 보고에 409(Conflict)를 뒀는가

`GameSession.hasResult()`가 `true`인데 또 `POST /api/game/result`가 오면 409를 던집니다. 이게 없으면 실수로 같은 게임 결과를 두 번 보고했을 때 당첨자가 조용히 바뀌어버릴 수 있는데, 추첨 결과처럼 "한 번 정해지면 바뀌면 안 되는" 데이터에는 이런 방어가 중요합니다. (`GameService.java`의 `reportResult` 메서드 참고.)

### 4. `GameParticipant`를 왜 `record`로 만들었는가

`GameParticipant`(참가자 1명의 이름/운세/버프)는 한 번 만들어지면 게임이 끝날 때까지 절대 바뀌지 않는 값입니다. 이렇게 "불변 데이터 덩어리"에는 자바의 `record`가 딱 맞습니다 — getter, `equals()`, `toString()`을 자동으로 만들어주고, 실수로 값을 바꾸는 코드를 원천 차단합니다. 반면 `GameSession`은 시간이 지나며 상태(`status`, `ranking`, `selectedName`)가 바뀌어야 해서 일반 클래스로 만들었습니다 — "값이 바뀌어야 하면 class, 안 바뀌면 record"가 이 프로젝트 전체에서 따른 기준입니다.

### 검증 요약 (자세한 내용은 [구 07])

curl로 실제 3명 참가자 게임을 만들고, `finishOrder`를 임의로 지정해서 결과를 보고해봤습니다.
- 정상 케이스: 순위/당첨자가 의도대로 계산됨.
- 같은 게임에 결과를 두 번 보고 → 409 확인.
- 존재하지 않는 gameId 조회 → 404 확인.

---

## [구 06] 관리자(Admin) API

`plan/01_mvp-plan.md`에서 정한 대로, 관리자 화면은 "조회 기능은 실제로 동작, 가중치 조정은 골격만"으로 만들었습니다.

### 1. 이미 만들어둔 조각들을 재사용

`AdminController`를 보면 새로운 로직이 거의 없다는 걸 알 수 있습니다.

- `GET /api/admin/players` → 이미 있는 `PlayerJpaRepository.findAll()`과 `FortuneResultJpaRepository.findByPlayerIdOrderByCreatedDateAsc()`를 조합.
- `GET /api/admin/games` → 이미 있는 `GameRepository.findAll()`을 그대로 노출.
- `GET /api/admin/logs` → [구 02]에서 만든 `ErrorLogStore.recent()`.

이건 우연이 아니라, 앞선 도메인들을 만들 때 **Repository를 인터페이스로 분리**해뒀기 때문에 가능한 결과입니다. 관리자 API는 "새 기능"이 아니라 "이미 있는 데이터를 다른 각도로 보여주는 창구"일 뿐이라, 별도 서비스 계층 없이 컨트롤러에서 리포지토리를 직접 조합해도 무리가 없다고 판단했습니다 (도메인 로직이 있는 `PlayerService`/`GameService`와 달리, 여기는 단순 조회 조합뿐이라서).

### 2. 스텁 엔드포인트를 "그냥 안 만들기"가 아니라 "501로 응답"하게 만든 이유

```java
@PostMapping("/api/admin/fortune/override")
@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public void overrideFortune(...) {
    throw ApiException.notImplemented("Not implemented yet — planned for Phase 2");
}
```

엔드포인트 자체를 안 만들면 프론트엔드에서 호출했을 때 `404 Not Found`가 오는데, 이건 "이 API가 아예 존재하지 않는다"는 뜻처럼 보입니다. 반면 `501 Not Implemented`는 "API는 있는데, 아직 기능이 구현 안 됐다"는 걸 명확히 구분해서 알려줍니다. [구 02]에서 만든 `GlobalExceptionHandler`/`ApiException` 덕분에, 이 구분을 위해 새 코드를 추가할 필요 없이 `ApiException.notImplemented(...)`만 던지면 끝났습니다 — 공통 인프라를 먼저 만들어둔 효과를 여기서 체감할 수 있습니다.

### 3. 인증이 없다는 것을 왜 계속 언급하는가

이 컨트롤러는 로그인 없이 누구나 호출할 수 있습니다. 로컬 개발/데모 단계에서는 문제가 없지만, 실제로 인터넷에 공개 배포하면 누구나 다른 사람의 참가자 목록이나 오류 로그를 볼 수 있게 됩니다. `plan/01_mvp-plan.md`의 Phase 2 목록에 "관리자 인증"이 명시적으로 들어있는 이유이기도 하고, 이 devhelp 문서에서도 반복해서 짚는 이유입니다 — 나중에 배포 단계로 넘어갈 때 절대 빠뜨리면 안 되는 항목이기 때문입니다.

---

## [구 07] 빌드와 테스트 검증

코드를 다 작성한 뒤, "작성했다"와 "동작한다"는 다르다는 원칙에 따라 실제로 컴파일 → 단위 테스트 → 서버 실행 → API 호출까지 전부 확인했습니다. 이 과정에서 실제로 겪은 문제와 해결 방법을 기록합니다.

### 1. 컴파일 확인

```bash
./gradlew compileJava compileTestJava
```

`BUILD SUCCESSFUL`. 이 시점에서 이미 몇 가지를 검증한 셈입니다 — 패키지 간 참조(`fortune` 패키지가 `player` 패키지를 참조하는 것 등)가 잘못되지 않았는지, 존재하지 않는 클래스/메서드를 부르고 있지 않은지.

### 2. 단위 테스트 실행

```bash
./gradlew test
```

`BuffCalculatorTest`, `MockFortuneGeneratorTest`, 그리고 스프링 전체를 띄워보는 `LuckyPinballApplicationTests`(컨텍스트 로딩 테스트)까지 전부 통과했습니다. 특히 컨텍스트 로딩 테스트가 통과했다는 건 "모든 `@Service`/`@Repository`/`@RestController`가 스프링 빈으로 정상 등록되고, H2 DB 연결도 정상"이라는 걸 의미합니다 — 개별 유닛 테스트보다 훨씬 넓은 범위를 한 번에 검증해줍니다.

### 3. 실제 서버 실행 + API 호출 검증

```bash
./gradlew bootRun
```

서버를 띄운 뒤, `plan/02_api-spec.md`에 정의된 흐름을 curl로 그대로 재현했습니다: 참가자 3명 등록 → 운세 조회 → 게임 생성 → 게임 시작 → 결과 보고 → 결과 조회 → 관리자 화면 조회. 모두 문서에 정의된 응답 형태와 정확히 일치했습니다.

### 4. 겪은 문제 — Windows + 한글 이름을 curl로 테스트할 때

처음에 curl 명령 안에 한글 이름(`"홍길동"`)을 직접 타이핑해서 보냈더니, 서버가 이런 에러를 냈습니다.

```json
{"error":"Unexpected error"}
```

로그를 보니 원인은 `JSON parse error: Invalid UTF-8 start byte`. 이건 **서버 코드 버그가 아니라 이 Windows 환경의 터미널/셸이 한글 문자열을 명령줄 인자로 넘길 때 UTF-8이 아닌 다른 인코딩(예: 한글 Windows 기본 코드페이지)으로 바이트를 만들어버려서** 생기는 문제였습니다. JSON은 항상 UTF-8이어야 하는데, 깨진 바이트가 들어오니 Jackson(JSON 파서)이 정확히 그 지점에서 파싱을 거부한 것 — 오히려 파서가 **제대로 동작하고 있다는 증거**입니다.

**해결 방법**: 한글이 포함된 JSON은 명령줄에 직접 타이핑하지 않고, 먼저 UTF-8 파일로 저장한 뒤 `curl --data-binary @파일.json`으로 보냈습니다.

```bash
curl -X POST http://localhost:8080/api/player \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @p1.json
```

이렇게 하니 `{"playerId":2,"name":"홍길동", ...}`처럼 정상적으로 응답이 왔습니다. **이건 테스트 방법의 문제였지, 실제 서비스에는 영향이 없습니다** — 브라우저의 `fetch()`는 항상 UTF-8로 JSON을 보내기 때문에, 나중에 실제 프론트엔드에서 호출할 때는 이런 문제가 생기지 않습니다.

다만 안전장치로, 서버 쪽 JVM/컴파일 인코딩도 명시적으로 UTF-8로 고정해뒀습니다 (`build.gradle`):

```gradle
tasks.named('bootRun') {
    jvmArgs '-Dfile.encoding=UTF-8'
}
tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

실행 중이던 프로세스를 살펴보니 JVM 기본 인코딩이 `x-windows-949`(한글 Windows 코드페이지)로 잡혀 있었습니다 — 지금 당장 문제를 일으키진 않았지만(JSON 처리는 Jackson이 항상 UTF-8을 강제하므로), 콘솔 로그 출력이나 향후 파일 입출력에서 미묘한 한글 깨짐을 유발할 수 있는 잠재적 지뢰라 미리 명시적으로 고정해뒀습니다. **"지금 당장 안 터진다"와 "안전하다"는 다르다**는 걸 보여주는 사례입니다.

### 5. 검증 결과 요약

| 확인 항목 | 결과 |
|---|---|
| 컴파일 | 성공 |
| 단위 테스트(BuffCalculator, MockFortuneGenerator) | 성공 |
| 스프링 컨텍스트 로딩 | 성공 |
| 참가자 등록(한글 이름 포함) | 성공 |
| 운세/버프 계산 (점수별 티어 매핑) | 문서 표와 정확히 일치 |
| 게임 생성 → 시작 → 결과 보고 → 조회 | 성공, 응답 형태가 `02_api-spec.md`와 일치 |
| 중복 결과 보고 → 409 | 성공 |
| 존재하지 않는 리소스 조회 → 404 | 성공 |
| 관리자 조회(참가자/게임) | 성공 |
| 관리자 스텁(가중치 조정) → 501 | 성공 |
| 오류 로그 자동 적재 | 성공 |

백엔드는 이 시점에서 `plan/01_mvp-plan.md`가 정의한 MVP 범위를 전부 충족합니다. 다음 단계는 프론트엔드(정적 HTML/CSS/JS + Matter.js)를 실제로 동작하는 코드로 만드는 것이며, 별도의 devhelp 문서(`08_...`부터)로 이어집니다.

---

## [구 08] 프론트엔드 구현

`plan/mockups/index.html`·`admin.html`(정적 목업)을 실제로 백엔드와 통신하고 Matter.js 물리엔진이 동작하는 진짜 화면으로 바꾼 단계입니다.

### 1. 목업을 얼마나 재사용했는가

`frontend/css/style.css`는 목업의 스타일을 거의 그대로 가져왔습니다 — 색상 팔레트, 카드/배지/테이블 스타일은 코드가 아니라 "디자인 결정"이라 바뀔 이유가 없었습니다. 대신 다음은 새로 추가했습니다.

- `.peg`, `.wall-visual`: 목업에서는 CSS `background-image`(반복 패턴)로 못을 "그림처럼" 표현했지만, 실제 게임에서는 **물리 엔진의 못 좌표와 화면에 보이는 못 위치가 정확히 일치해야** 공이 못에 부딪히는 게 시각적으로도 말이 됩니다. 그래서 못/벽을 JS가 계산한 좌표로 `<div>` 를 하나씩 동적으로 생성하는 방식으로 바꿨습니다.
- `[hidden] { display:none !important; }`: 결과 섹션을 처음엔 숨겨뒀다가 게임이 끝나면 보여주는데, `hidden` 속성을 JS에서 `el.hidden = true/false`로 토글하는 방식을 쓰기 위해 추가했습니다 (`style.display`를 직접 건드리는 것보다 의도가 명확합니다).

### 2. `api.js` — 서버와의 계약을 한 파일에 모으기

`plan/02_api-spec.md`에 정의된 엔드포인트 하나당 함수 하나씩 대응시켰습니다. 모든 함수가 내부적으로 `request()` 공통 함수를 거치는데, 여기서 `response.ok`가 아니면 백엔드가 `{"error": "..."}` 형태로 내려주는 메시지를 그대로 꺼내 `Error`로 던집니다 — 이 덕분에 `main.js`/`admin.js`에서는 `catch (error) { ... error.message ... }` 한 줄이면 백엔드의 실제 에러 메시지를 화면에 보여줄 수 있습니다. ([구 06]에서 다룬 백엔드의 `{"error": "..."}` 공통 포맷과 프론트엔드가 정확히 맞물리는 지점입니다.)

### 3. `game.js` — 왜 `<canvas>`가 아니라 `<div>`들로 그렸는가

Matter.js를 쓰는 예제 대부분은 `Matter.Render`(캔버스에 자동으로 그려주는 기능)를 함께 씁니다. 하지만 이 프로젝트는 목업 단계부터 이미 "구슬 = 이름표가 붙은 동그란 `<div>`" 형태로 디자인해뒀기 때문에(이름이 공 위에 보여야 함), 캔버스 렌더러 대신 **Matter.js에게는 물리 계산만 시키고(`Matter.Engine`, `Matter.Runner`), 화면에 보이는 것은 매 프레임 `body.position`을 읽어와 `<div>`의 `style.left/top`을 갱신하는 방식**을 택했습니다. 이렇게 하면 CSS로 만든 예쁜 공 디자인(이름표, 그림자, 완주 표시)을 그대로 유지하면서 물리 시뮬레이션을 얹을 수 있습니다.

#### `MatterAdapter` — 설계 문서(`plan/04_pinball-map-design.md` §5)를 그대로 코드로

이 프로젝트의 설계 문서에는 "나중에 Box2D로 물리엔진을 바꿀 수도 있으니, Matter.js 관련 코드를 어댑터 뒤에 숨겨두자"는 결정이 있었습니다. `game.js`의 `MatterAdapter` 클래스가 그 약속을 지킨 것입니다 — `runPinballGame` 함수(게임 로직 본체)는 `Matter.xxx`를 단 한 번도 직접 호출하지 않고, 전부 `adapter.addBall(...)`, `adapter.getPosition(...)` 같은 어댑터 메서드를 통해서만 물리 엔진과 대화합니다. 나중에 `Box2dAdapter`를 만들어 이 자리에 끼워 넣어도 `runPinballGame`은 한 줄도 안 바뀝니다.

#### 맵 생성 로직

`plan/04_pinball-map-design.md`의 표(캔버스 560×640, 못 8행, 위 560→아래 460으로 좁아지는 벽, 결승선 y=470 등)를 `CLASSIC_GALTON_MAP` 객체 하나로 그대로 옮겼습니다. `buildPegPositions()`가 이 설정에서 못 좌표를 계산하고, `buildBoard()`가 그 좌표로 물리 바디(`adapter.addStaticCircle`)와 화면 요소(`renderPegs`)를 동시에 만듭니다 — **같은 함수(`buildPegPositions`)의 결과를 물리와 화면 양쪽에서 그대로 재사용**했기 때문에, 둘이 어긋날 일이 없습니다.

#### 버프 → 시작 위치

`spawnY(buffStartY)`가 `plan/01_mvp-plan.md`의 버프 표(시작 Y 오프셋 0/15/30/45/60)를 실제 픽셀 좌표로 변환합니다. 운세가 나쁠수록(오프셋이 클수록) 낙하 시작 구역 안에서 더 아래쪽 — 즉 못 1행에 더 가까운 곳 — 에서 출발합니다.

### 4. `main.js` / `admin.js` — 화면 로직

- `main.js`는 "등록 → 운세 확인 → 게임 시작 → 결과" 4단계를 그대로 함수로 나눴습니다. 각 단계는 이전 단계가 끝나야 다음 버튼이 활성화되는 구조(`disabled` 토글)라, 사용자가 순서를 건너뛸 수 없습니다.
- `runPinballGame`의 `onComplete` 콜백 안에서 `api.reportResult(gameId, finishOrder)`를 호출합니다 — 게임 화면(`game.js`)은 서버가 있다는 사실조차 몰라도 되고, "게임이 끝나면 이 배열을 넘겨준다"는 계약만 지키면 됩니다. 이것도 관심사 분리의 한 예시입니다 (물리 시뮬레이션과 API 통신을 분리).
- `admin.js`는 페이지 로드 시 3개 GET 요청(참가자/게임/로그)을 병렬로 날리고, 가중치 조정 폼은 제출 시 501 에러가 오는 것을 "정상적인 예상된 실패"로 처리해서 사용자에게 "아직 준비 중"이라고 보여줍니다.

### 5. 개발 서버 포트 관련 메모

원래 계획(`01_mvp-plan.md`)은 프론트엔드를 `5500` 포트로 띄우는 걸 가정했는데, 실제로 이 PC에서 확인해보니 **VS Code가 이미 5500 포트를 쓰고 있어서** `npx serve`가 조용히 다른 포트(3960)로 떠버리는 걸 발견했습니다. 이런 "환경마다 다를 수 있는" 문제를 피하려고, 백엔드 CORS 설정(`WebConfig`)에 `5500`과 `5501` 둘 다 허용해뒀습니다. 이 프로젝트를 실행할 때 5500이 이미 사용 중이면 `npx serve frontend -l 5501`처럼 5501로 띄우면 됩니다.

실제 실행/검증 결과와, 이 과정에서 실제로 발견한 물리 버그는 다음 문서에 이어집니다.

---

## [구 10] Playwright로 실제 화면 확인

`devhelp/02`(구 09)에서 남겨뒀던 숙제 — "실제 브라우저에서 화면이 어떻게 보이는지는 확인하지 못했다" — 를 이번에 해결했습니다.

### 1. 무엇이 없었고, 무엇을 설치했는가

이전까지는 curl로 텍스트(JSON/HTML)만 주고받을 수 있었지, **렌더링된 화면을 눈으로 보는 방법이 없었습니다.** [Playwright](https://playwright.dev/)는 마이크로소프트가 만든 브라우저 자동화 라이브러리로, 실제 Chromium/Firefox/WebKit 브라우저를 코드로 조작(페이지 이동, 클릭, 입력, 스크린샷)할 수 있게 해줍니다.

이번에도 `frontend/` 프로젝트 자체에 설치한 게 아니라, **스크래치패드에 별도 Node 프로젝트**로 설치했습니다 (`npm install playwright` + `npx playwright install chromium`으로 브라우저 실행 파일까지 받음). `frontend/`는 여전히 "빌드 도구 없는 순수 정적 파일"이라는 원칙을 유지합니다 — Playwright는 **테스트 도구**일 뿐, 이 프로젝트를 실행하는 데 필요한 의존성이 아닙니다.

### 2. 실제로 한 일

Playwright 스크립트로 다음을 자동으로 수행하고, 각 단계마다 스크린샷을 저장했습니다.

1. `http://localhost:5501/` 접속
2. 참가자 4명(홍길동/김유나/박도윤/이서연) 등록
3. "운세 확인" 클릭
4. "게임 시작" 클릭 → 물리 시뮬레이션 진행 중 스크린샷
5. 결과 배너가 나타날 때까지 대기(최대 60초) → 최종 결과 스크린샷
6. `admin.html`로 이동 → 관리자 화면 스크린샷

이 스크린샷들을 제 Read 도구로 직접 열어봤습니다 (Claude는 이미지를 볼 수 있는 멀티모달 모델입니다).

### 3. 확인된 것

- 참가자 등록/삭제 칩, 운세 카드(티어별 색상 테두리), 핀볼 맵(못 배열 + 깔때기 벽 + 결승선), 실시간 순위 패널, 당첨자 배너, 관리자 화면 4개 섹션 — **전부 의도한 대로 렌더링**됐습니다.
- 브라우저 콘솔에 자바스크립트 에러가 하나도 없었습니다 (`page.on('pageerror', ...)`로 감시).
- 게임이 실제로 몇 초 안에 정상 종료되고, 결과 배너에 정확한 당첨자 이름과 게임 ID가 표시됐습니다.

### 4. 실행해보고 알게 된 점 (버그는 아니지만 기록해둘 것)

이번 실행에서는 공교롭게도 **버프가 가장 큰(버프4, 시작 높이 가장 유리한) "홍길동"이 결국 당첨자(가장 늦게 도착)가 됐습니다.** 이게 버그처럼 보일 수 있는데, 설계 문서(`plan/04_pinball-map-design.md` §4)에 이미 적어둔 대로 **의도된 동작**입니다:

> "시작 높이가 결과를 100% 결정하지는 않는다 — 못 구간에서 서로 부딪히며 순서가 섞이기 때문에, '오늘 운 없는 사람에게 유리한 확률적 이점'을 주는 정도로 설계된 것."

즉 버프는 "유리하게 만들어주는 것"이지 "결과를 보장하는 것"이 아니며, 이게 오히려 추첨 도구로서는 바람직한 특성입니다(버프만 보면 결과가 뻔히 예측되면 재미가 없음). 한 번의 실행만으로는 이게 "버프가 통계적으로 유의미하게 작동하는지"까지는 확인할 수 없다는 점도 참고해두면 좋습니다 — 정말로 검증하려면 같은 조건으로 수십~수백 판을 돌려서 버프4 참가자가 통계적으로 당첨(꼴찌)될 확률이 낮은지 확인해야 하는데, 이건 이번 범위를 넘어서는 통계적 튜닝 작업입니다.

### 5. 여전히 남은 한계

- 스크린샷은 데스크톱 해상도(1000px 너비) 기준으로만 확인했습니다. 좁은 화면(모바일)에서 레이아웃이 어떻게 깨지는지는 확인하지 않았습니다.
- Playwright 스크립트는 스크래치패드의 임시 파일이라 저장소에 남아있지 않습니다 — 나중에 이 프로젝트에 정식 E2E 테스트가 필요해지면, `frontend`와 별개로 `e2e/` 같은 폴더에 자체 `package.json`을 가진 테스트 스위트를 새로 구성하는 걸 권장합니다.
