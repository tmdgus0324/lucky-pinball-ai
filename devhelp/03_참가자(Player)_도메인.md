# 03. 참가자(Player) 도메인

가장 단순한 도메인부터 만들어서, 이후 도메인들이 따라갈 패턴(Entity → Repository → Service → Controller)을 먼저 확립했습니다.

## 왜 이 순서로 클래스를 나누는가

Spring Boot(그리고 대부분의 백엔드 프레임워크)에서 자주 쓰는 4단 구조입니다.

1. **Entity** (`PlayerEntity`) — DB 테이블 한 행을 자바 객체로 표현. `@Entity`가 붙으면 JPA(Hibernate)가 이 클래스를 보고 테이블을 만들고, 객체를 저장/조회할 때 자동으로 SQL을 생성해줍니다.
2. **Repository** (`PlayerJpaRepository`) — DB 접근을 담당하는 인터페이스. `JpaRepository<PlayerEntity, Long>`을 상속하기만 하면 `save()`, `findById()` 같은 기본 CRUD 메서드를 **직접 구현하지 않아도** Spring Data JPA가 런타임에 구현체를 만들어줍니다.
3. **Service** (`PlayerService`) — 실제 비즈니스 로직. "존재하지 않는 참가자를 조회하면 404 예외를 던진다" 같은 규칙이 여기 들어갑니다. 컨트롤러가 아니라 서비스에 이 로직을 두는 이유는, 나중에 이 로직을 다른 곳(예: 관리자 API)에서도 재사용할 수 있게 하기 위함입니다.
4. **Controller** (`PlayerController`) — HTTP 요청/응답만 담당. `@RequestBody`로 JSON을 받고, 서비스를 호출하고, 결과를 다시 JSON으로 돌려줍니다. **비즈니스 로직은 최대한 넣지 않습니다** — 그래야 나중에 "REST API가 아니라 배치 작업에서도 이 로직을 쓰고 싶다" 같은 요구가 와도 서비스만 재사용하면 됩니다.

## 코드에서 눈여겨볼 부분

- `PlayerEntity`에 **기본 생성자(`protected PlayerEntity() {}`)** 가 있습니다. JPA는 DB에서 데이터를 읽어와 객체를 만들 때 리플렉션으로 기본 생성자를 호출하기 때문에, `@Entity` 클래스에는 인자 없는 생성자가 반드시 있어야 합니다 (직접 코드에서 쓸 일은 없어서 `protected`로 막아뒀습니다).
- `PlayerController.RegisterPlayerRequest`/`PlayerResponse`를 **컨트롤러 안에 중첩 record**로 정의했습니다. 별도 `dto` 패키지를 만들 수도 있지만, `01_mvp-plan.md`에서 "포트폴리오 MVP 규모에 과한 추상화는 피한다"고 정해뒀던 방향과 맞춰서, 요청/응답 모양이 그 컨트롤러에서만 쓰인다면 굳이 파일을 분리하지 않았습니다.
- `@Valid` + `@NotBlank`/`@NotNull`: 요청 바디 검증. 예를 들어 `name`이 비어있으면 컨트롤러 로직에 도달하기도 전에 Spring이 막아주고, `02_백엔드_공통_설정.md`에서 만든 `GlobalExceptionHandler`가 이를 400 응답으로 변환합니다.

## 검증

전체 빌드/실행 검증은 모든 도메인을 다 만든 뒤 한 번에 진행했습니다 (`07_빌드와_테스트_검증.md`). 그때 실제로 `POST /api/player`를 호출해서 참가자가 정상적으로 등록되고 `playerId`가 자동 증가하는 것을 확인했습니다.

다음 문서: [`04_운세(Fortune)_도메인.md`](./04_운세(Fortune)_도메인.md)
