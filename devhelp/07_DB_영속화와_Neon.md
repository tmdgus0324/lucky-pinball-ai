# 07. 게임 결과 DB 영속화 + Neon(Postgres) 연결

> 기존 35번 문서다(2026-10-04 문서 통합 때 번호만 바뀜).

> **AI 코멘트 — 이 문서 이후에 알게 된 것과 다음 단계**
>
> **1. 3장의 버그 3개는 JPA 면접 단골 주제다.** 메모리 참조 공유로 우연히 동작하던 코드, `LazyInitializationException`, `MultipleBagFetchException`과 중복 저장은 모두 "JPA를 실제로 써 봤는가"를 가르는 질문과 직결됩니다. 각각 "증상 → 원인(영속성 컨텍스트·지연 로딩·카테시안 곱) → 해결(`@Transactional` 경계, fetch join, `Set`)" 순서로 말할 수 있게 정리해 두면 좋습니다.
>
> **2. 이 테이블 설계 이후에 생긴 것**
>
> - 오류 로그 테이블(`error_log_entity`)이 추가됐습니다(`devhelp/09`(구 42)). 지금 전체 ERD는 `devdocs/03`에 있습니다.
> - 운세 이력 테이블은 외래키 없이 이름·생년월일을 함께 저장하는 **캐시 겸 이력**이고, 게임 쪽 테이블만 외래키로 연결했습니다. 1장의 "FK를 실제로 건 이유"와 함께, **어디에는 걸고 어디에는 안 걸었는지**를 같이 설명할 수 있으면 판단 기준이 더 분명하게 보입니다.
> - 인덱스는 아직 하나도 없습니다. 캐시 조회(`name`, `birth_date`)와 참가자별 이력 조회(`player_id`)가 데이터가 늘면 가장 먼저 느려질 곳입니다. 참가자 목록 API의 N+1 문제(`devhelp/06` AI 코멘트)와 함께 보면 좋습니다.
>
> **3. 6장(Flyway)을 적용한다면 가장 작은 첫걸음**
>
> 운영 DB에 이미 테이블과 데이터가 있으므로, 처음부터 마이그레이션 파일로 테이블을 다시 만드는 것이 아니라 **지금 스키마를 기준점(baseline)으로 잡는 방식**이 안전합니다.
>
> 1. 현재 운영 스키마를 `V1__init.sql`로 추출
> 2. `spring.flyway.baseline-on-migrate=true`로 기존 DB를 "V1까지 적용됨"으로 표시
> 3. `ddl-auto`를 `validate`로 바꿔, 엔티티와 실제 스키마가 어긋나면 서버가 뜨지 않게 함
> 4. 이후 변경(예: 인덱스 추가)은 `V2__...sql`로
>
> 로컬·테스트는 H2라서 SQL 문법 차이가 문제 될 수 있습니다. 그래서 Flyway를 넣는 시점에 테스트도 PostgreSQL(Testcontainers)로 옮기는 것을 같이 검토하는 것이 자연스럽습니다.
>
> **4. 데이터 보호**
>
> 4-2장의 주의사항(서버가 켜져 있을 때 로컬 DB 파일을 지우지 말 것)은 실제로 겪은 사고에서 나온 것입니다. 운영 쪽은 Neon이 제공하는 백업·복원 범위(무료 플랜에서 얼마나 과거로 되돌릴 수 있는지)를 한 번 확인해 두는 것이 좋습니다. 저는 그 범위를 확인하지 못해서 수치는 적지 않았습니다.

지금까지 게임(`GameSession`)은 `ConcurrentHashMap`에만 있어서 서버가 재시작되면 무조건 사라졌다. 거기다 H2도 파일 기반이긴 하지만 Render 무료 티어는 재배포·슬립마다 디스크가 초기화돼서, 결국 "DB가 있는데도 영구 저장이 안 되는" 상태였다. 이번에 ① 게임도 JPA로 영속화하고, ② H2를 실제 외부 DB(Neon, 관리형 Postgres)로 바꿔서 두 문제를 한 번에 해결했다.

---

## 1. 테이블 설계 — 소스 레벨

### 왜 3개 테이블로 나눴나

```
game_entity              game_participant_entity       game_rank_entity
┌──────────────┐         ┌──────────────────┐         ┌──────────────────┐
│ game_id (PK) │◄────────│ game_id (FK)      │         │ game_id (FK)      │◄─┐
│ status       │         │ player_id (FK)    │──┐      │ player_id (FK)    │  │
│ selected_name│         │ name              │  │      │ rank              │  │
│ created_at   │         │ fortune_score     │  │      │ name              │  │
│ started_at   │         │ ...               │  │      └──────────────────┘  │
│ finished_at  │         └──────────────────┘   └─────► player_entity ◄──────┘
└──────────────┘
```

**`game_id`를 그대로 PK로 쓴 이유**: 이미 `UUID.randomUUID()`로 발급해서 `/api/game/result/{gameId}` URL에 노출하고 있는 식별자라서, 숫자 surrogate PK(`id BIGINT AUTO_INCREMENT`)를 따로 두면 "같은 역할을 하는 컬럼이 두 개"가 된다. `PlayerEntity`/`FortuneResultEntity`는 DB 내부에서만 쓰이는 식별자라 surrogate를 쓴 것과 대조된다.

**참가자(`game_participant_entity`)와 순위(`game_rank_entity`)를 분리한 이유**: 참가자 정보는 게임 **생성 시점**에 확정되는데, 순위는 게임이 **끝나야만** 생기는 정보다. 하나로 합치면 "아직 결정 안 된 null 컬럼"(`finish_rank`)이 생성 직후부터 한동안 존재하게 된다. 분리하면 각 테이블이 "그 시점에 반드시 다 채워진 데이터"만 갖는다 — 생명주기(CREATED→STARTED→FINISHED)가 테이블 존재 여부로 자연스럽게 표현된다.

**FK를 실제로 건 이유**: 실무(배치 위주, 여러 시스템이 같은 테이블에 직접 쓰는 환경)에서는 FK가 성능/운영 유연성 문제로 기피되는 경우가 많다. 하지만 이 프로젝트는 쓰기 경로가 Spring Data JPA 하나뿐이고 규모도 작아서, 그런 제약이 전혀 해당하지 않는다. 오히려 FK로 **무결성을 DB가 보장**하게 하는 쪽이 안전하다 — 존재하지 않는 `playerId`로 게임 참가자가 잘못 저장되는 걸 DB가 즉시 막아준다.

### 핵심 코드 — `GameEntity.java`

```java
@Entity
public class GameEntity {

    @Id
    private String gameId;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private String selectedName;
    private Instant createdAt, startedAt, finishedAt;

    // participants/ranks 둘 다 List(bag)로 두면, 한 쿼리에서 두 컬렉션을 동시에 fetch join할 때
    // SQL 레벨에서 Cartesian product(참가자 수 × 순위 수)가 생기는데 List는 그 중복 행을
    // 못 걸러내서(Set과 달리) 참가자가 중복으로 들어가는 조용한 버그가 생긴다 (2번 참고).
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<GameParticipantEntity> participants = new LinkedHashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rank ASC")
    private Set<GameRankEntity> ranks = new LinkedHashSet<>();
}
```

`GameParticipantEntity`/`GameRankEntity`는 각각 `@ManyToOne @JoinColumn(name = "player_id")`로 `PlayerEntity`를 참조하고, `GameRankEntity`에는 `@UniqueConstraint(columnNames = {"game_id", "player_id"})`를 걸어서 한 참가자가 같은 게임에서 순위가 중복 기록되는 걸 DB가 막게 했다.

---

## 2. `JpaGameRepository` — 기존 인터페이스를 그대로 구현

`GameService`는 원래부터 `GameRepository` 인터페이스(`save`/`findById`/`findAll`)만 알고, `GameSession`/`GameParticipant`/`RankEntry` 같은 평범한 도메인 객체만 다뤘다. 그래서 영속화 방식을 인메모리에서 JPA로 바꾸는 데 `GameService`/`GameController`/`AdminController`는 **한 줄도 안 건드렸다** — `InMemoryGameRepository`를 지우고 `JpaGameRepository`로 교체하기만 하면 끝났다. (이 프로젝트가 처음부터 `FortuneService`를 인터페이스로 분리해서 Mock→Claude로 교체했던 것과 같은 패턴.)

```java
@Repository
public class JpaGameRepository implements GameRepository {

    @Override
    @Transactional
    public GameSession save(GameSession session) {
        GameEntity entity = gameJpaRepository.findById(session.getGameId())
                .orElseGet(() -> new GameEntity(session.getGameId(), session.getCreatedAt()));
        // ... 참가자/순위를 GameEntity에 채우고
        return toDomain(gameJpaRepository.save(entity));
    }
    // findById, findAll, toDomain() ...
}
```

`GameSession`에는 저장소가 복원할 때만 쓰는 패키지 전용 팩토리를 추가했다:

```java
// GameSession.java — JpaGameRepository 전용, start()/reportResult()처럼 "지금 시각을 찍는"
// 부수효과 없이 과거 상태를 그대로 재구성한다.
static GameSession restore(String gameId, List<GameParticipant> participants, Instant createdAt,
                            GameStatus status, Instant startedAt, Instant finishedAt,
                            List<RankEntry> ranking, String selectedName) { ... }
```

---

## 3. 과정에서 만난 버그 3개 (로컬 테스트로 실제 재현·수정)

### ① "메모리 참조 공유"로만 우연히 동작하던 버그

```java
// Before
public GameSession start(String gameId) {
    GameSession session = getSessionOrThrow(gameId);
    session.start();
    return session;   // ← save()를 안 부름
}
```

인메모리 `ConcurrentHashMap`에서는 `getSessionOrThrow()`가 반환한 객체가 Map 안의 **바로 그 객체**라서, `session.start()`로 필드를 바꾸면 Map 안의 데이터도 자동으로 바뀐 것처럼 보였다. JPA로 바꾸니 `findById()`가 매번 **새로 변환한 객체**를 반환해서, `save()`를 명시적으로 호출 안 하면 변경사항이 그냥 사라졌다. `gameRepository.save(session)`을 추가해서 해결 — 저장소 패턴에서 당연히 지켜야 할 계약인데, 인메모리 구현이 그 실수를 가려주고 있었던 것.

### ② `LazyInitializationException` — 세션 끝난 뒤 지연로딩 접근

```
Cannot lazily initialize collection of role 'GameEntity.participants' (no session)
```

`findById()`가 반환한 엔티티를 트랜잭션(세션) 밖에서 `toDomain()`으로 변환하면서 `participants`(LAZY) 컬렉션을 읽으려다 터졌다. `findById()`엔 `@Transactional(readOnly = true)`를 붙여서 변환까지 세션이 열린 채로 끝내게 했고, `findAll()`은 애초에 N+1을 막기 위해 fetch join 쿼리(`findAllWithDetails()`)로 한 번에 다 채워서 가져오게 했다.

### ③ `MultipleBagFetchException` + 참가자 중복 저장

```java
@Query("SELECT DISTINCT g FROM GameEntity g LEFT JOIN FETCH g.participants LEFT JOIN FETCH g.ranks")
```

`participants`/`ranks`를 둘 다 `List`로 두고 한 쿼리에서 동시에 fetch join했더니 `MultipleBagFetchException`(bag 두 개를 동시에 못 가져옴)이 터졌다. `ranks`만 `Set`으로 바꾸니 예외는 사라졌는데, **2명 참가 게임인데 `participantCount: 4`로 나오는 더 조용한 버그**가 남아있었다 — 두 컬렉션을 동시에 join하면 SQL 레벨에서 Cartesian product(참가자 수 × 순위 수)가 생기는데, `List`는 그 중복 행을 못 걸러내서(Set과 달리) 참가자가 중복으로 들어간 것. `participants`도 `Set`으로 바꿔서 완전히 해결했다. **"예외가 안 난다"가 "정답이다"의 증거는 아니라는 걸 실측으로 확인한 사례** — curl로 실제 응답값(`participantCount`)까지 비교해보지 않았으면 못 잡았을 버그다.

---

## 4. H2 → Neon(Postgres) 전환

### `build.gradle` — 드라이버만 추가

```gradle
runtimeOnly 'com.h2database:h2'
runtimeOnly 'org.postgresql:postgresql'   // 로컬(H2)/배포(Neon) 둘 다 지원
```

### `application.yml` — 환경변수가 yml보다 항상 우선이라는 Spring Boot 기본 동작을 활용

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/luckypinball;AUTO_SERVER=TRUE
    username: sa
    password:
```

`driver-class-name`을 일부러 안 적었다 — URL 접두사(`jdbc:h2:` / `jdbc:postgresql:`)만 보고 Spring Boot가 알맞은 드라이버를 자동으로 고른다. 그래서 **이 파일은 그대로 두고, Render에만 환경변수 3개(`SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`)를 등록하는 것만으로** 로컬은 H2, 배포는 Neon을 쓰게 분기된다.

### Neon 연결 문자열 → JDBC URL 변환

Neon이 주는 연결 문자열은 `psql`/Node 스타일이라 그대로 못 쓴다:

```
# Neon이 준 것 (psql 스타일)
postgresql://neondb_owner:<비밀번호>@ep-xxxx-pooler.aws.neon.tech/neondb?sslmode=require&channel_binding=require

# Spring datasource.url (사용자명/비밀번호는 따로 분리)
jdbc:postgresql://ep-xxxx-pooler.aws.neon.tech/neondb?sslmode=require&prepareThreshold=0
```

- `channel_binding=require`는 뺐다 — libpq(psql) 전용 파라미터라 Java PostgreSQL 드라이버가 못 알아들을 수 있다.
- `prepareThreshold=0`을 추가했다 — 호스트명의 `-pooler`는 Neon이 **PgBouncer**(커넥션 풀러)를 거친다는 뜻인데, 이 모드에서는 JDBC 드라이버의 서버사이드 prepared statement 캐싱이 충돌할 수 있어서 꺼둔다(Neon 공식 가이드 권장 설정).

### 4-2. 로컬 DB와 운영 DB는 어디서 정해지나 (나중에 헷갈릴 때 보는 정리)

**한 줄 요약**: 소스에는 DB 설정이 **하나(H2)** 뿐이고, 운영이 Neon을 보는 것은 **Render의 환경변수**가 그 설정을 덮어쓰기 때문이다. Neon의 주소·계정·비밀번호는 소스 어디에도 없다.

| | 로컬 | 운영(Render) |
|---|---|---|
| DB | H2 파일 DB — `backend/data/luckypinball.mv.db` | Neon PostgreSQL |
| 무엇이 정하나 | `application.yml`의 기본값(`jdbc:h2:file:./data/luckypinball;AUTO_SERVER=TRUE`) | Render 환경변수 `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` |
| 데이터 | 내 PC의 파일에만 쌓임 | Neon에만 쌓임 |
| 테스트(`./gradlew test`, CI) | 둘 다 아님 — 실행마다 새로 만들어지는 **메모리 H2**(`src/test/resources/application.properties`) |

- **Neon에는 운영 DB 하나만 있다.** 로컬 데이터를 Neon에서 따로 관리하는 구조가 아니다. 로컬과 운영은 서로의 데이터를 보지 못한다.
- 소스에 있는 것: `build.gradle`에 `h2`와 `postgresql` 드라이버가 둘 다 `runtimeOnly`. 프로필 파일(`application-prod.yml` 등)은 **없다**. 설정 파일은 `application.yml` 하나다.
- 환경변수가 yml보다 우선하는 것은 Spring Boot 기본 동작이다(`SPRING_DATASOURCE_URL` → `spring.datasource.url`).
- 이 방식을 **유지하기로 했다.** 비밀번호가 git에 올라가지 않고, 소스가 단순하다. 단점은 "운영이 어떤 DB인지"가 코드 저장소만 봐서는 드러나지 않는다는 점이라, 이 문서로 보완한다.

**지금 어떤 DB에 붙어 있는지 확인하는 방법**

| 확인할 곳 | 방법 | 보이는 것 |
|---|---|---|
| 운영 | Render 배포 로그에서 `Database dialect` 검색 | `PostgreSQLDialect`면 Neon, `H2Dialect`면 H2(환경변수가 빠진 것) |
| 운영 | Render → Environment 화면 | `SPRING_DATASOURCE_*` 3개가 있는지 |
| 운영 데이터 | Neon 콘솔 또는 DBeaver로 조회 | 테이블/행 |
| 로컬 | 서버 로그의 `Database dialect` | 환경변수를 안 줬다면 `H2Dialect` |
| 로컬 | `backend/data/` 폴더 | `luckypinball.mv.db`가 로컬 DB 파일 |

**주의할 점**

- **로컬에서 운영 환경변수를 주고 서버를 켜면 운영 DB를 직접 수정한다.** `ddl-auto: update`가 운영 테이블 구조까지 바꿀 수 있다. 운영 데이터를 보기만 할 때는 DBeaver로 **조회만** 한다.
- **로컬 DB 초기화**는 서버를 먼저 끄고 `backend/data/luckypinball*.db`를 지우면 된다(다음 기동 때 새로 만들어진다). **서버가 켜져 있는 상태에서 지우지 않는다** — 실제로 이렇게 지웠다가 켜져 있던 서버의 DB가 사라진 적이 있다.
- 로컬(H2)과 운영(Postgres)은 **DB 엔진이 다르다.** 지금은 JPA만 써서 문제가 없지만, 엔진마다 다르게 동작하는 SQL(예: 네이티브 쿼리, 날짜 함수)을 쓰기 시작하면 로컬과 CI(둘 다 H2)에서는 통과하고 운영에서만 실패할 수 있다. 그런 쿼리를 추가할 때는 운영에서 따로 확인한다.

**검토했지만 지금은 하지 않기로 한 대안**

| 대안 | 얻는 것 | 안 한 이유 |
|---|---|---|
| Spring 프로필 분리(`application-local.yml`/`application-prod.yml`) | 소스만 봐도 "운영은 Postgres"가 보임 | 접속 주소·비밀번호는 어차피 환경변수로 받아야 해서 이 규모에서는 이득이 작음 |
| Neon 브랜치로 개발용 DB 추가 | 로컬에서도 Postgres 엔진 사용 | 무료 플랜의 브랜치 한도를 확인하지 못함, 인터넷 필요 |
| 로컬 Docker Postgres | 로컬도 Postgres 엔진 | Docker 설치/실행 부담 |

엔진 차이로 실제 문제가 생기면 그때 Docker Postgres나 Neon 브랜치로 엔진을 맞추는 것을 다시 검토한다.

---

## 5. 검증 과정

1. 로컬에서 참가자 등록 → 게임 생성(AI 호출) → 시작 → 결과 보고 → 조회 → 관리자 목록 전체 흐름 확인
2. **로컬 서버를 완전히 재시작한 뒤에도 게임 데이터가 남아있는지** 확인 (H2 단계에서 먼저 검증)
3. Render 배포 로그에서 `Database dialect: PostgreSQLDialect`, `Database driver: PostgreSQL JDBC Driver` 확인 (H2가 아니라 Neon에 붙었다는 증거)
4. Render에서 **Suspend → Resume**(완전 정지 후 재개, 단순 재시작보다 더 확실한 테스트)으로 컴퓨트 인스턴스 자체가 사라졌다 다시 떠도 데이터가 남아있는지 확인
5. Neon 대시보드에서 직접 테이블 조회로 데이터 실존 확인

---

## 6. 실무에서는 어떻게 하나 — Flyway (지금 이 프로젝트엔 적용 안 함, 참고자료)

### `ddl-auto: update`의 문제

이 프로젝트는 `spring.jpa.hibernate.ddl-auto: update`로 테이블을 만들었다 — Hibernate가 `@Entity` 클래스를 보고 "이 모양의 테이블이 있어야 한다"를 추론해서 **자동으로 DDL을 실행**한다. 1인 포트폴리오 MVP엔 편하지만, 운영 환경에서는 쓰면 안 되는 방식으로 꼽힌다:

- **변경 이력이 없다** — "지금 스키마가 어떻게 생겼는지"만 알 수 있고, "언제 왜 이렇게 바뀌었는지"는 git 커밋 로그를 뒤져야 함
- **리뷰 불가능** — 실제로 어떤 SQL이 실행될지 사람이 미리 보고 승인하는 과정이 없음
- **위험한 자동 추론** — 필드명을 바꾸면 Hibernate는 "기존 컬럼 삭제 + 새 컬럼 추가"로 이해해버려서, 운영 DB의 데이터가 조용히 날아갈 수 있음

### Flyway란

**SQL 마이그레이션 파일을 버전 관리하고, 한 번 적용된 파일은 다시 실행되지 않도록 추적해주는 도구.** DBeaver/Tibero Admin으로 DDL을 직접 짜시던 것과 본질은 같다 — "사람이 SQL을 직접 통제한다"는 원칙은 유지하면서, 그 실행 이력을 자동으로 관리해주는 것.

**동작 원리**:
1. `src/main/resources/db/migration/` 폴더에 `V1__create_player_table.sql`, `V2__create_game_tables.sql`처럼 **버전 번호가 붙은 SQL 파일**을 둔다.
2. 앱이 뜰 때 Flyway가 DB 안의 `flyway_schema_history` 테이블(자기가 만듦)을 보고, **아직 적용 안 된 버전 번호만 순서대로** 실행한다.
3. 이미 적용된 파일은 **체크섬(해시)을 기록**해둬서, 나중에 그 파일 내용이 바뀌면("이미 돌아간 마이그레이션을 수정하면 안 된다"는 원칙 위반) 앱이 아예 기동을 거부하고 에러를 낸다. 고칠 게 있으면 기존 파일을 수정하지 말고 **새 버전 파일(`V3__...sql`)을 추가**해야 한다.

**Spring Boot 연동**:
```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Flyway가 스키마를 만들고, Hibernate는 "내 엔티티랑 실제 스키마가 일치하는지"만 검증
  flyway:
    enabled: true
```

**이미 운영 중인 DB에 Flyway를 처음 도입할 때**는 `baseline-on-migrate: true`가 필요하다 — "지금 있는 스키마를 V1으로 간주하고, 그 이후 버전부터 추적 시작"하라는 옵션. 맨땅에서 새로 시작하는 프로젝트라면 필요 없다.

### 이 프로젝트에 지금 적용하지 않는 이유

규모(1인 개발, 테이블 5개)에서는 `ddl-auto: update`의 리스크가 실질적으로 작고, Flyway 도입 자체가 과한 엔지니어링이다. 다만 **면접에서 "운영 환경이면 어떻게 하시겠어요?"에 "Flyway로 전환하겠다"고 답할 수 있어야 하고, 이 문서가 그 근거**다 — 카파시 CLAUDE.md 문서처럼, 이번 프로젝트엔 적용 안 하고 다음 프로젝트 또는 실무에서 참고할 자료로 남겨둔다.

---

## 7. H2 vs Tibero/Oracle vs Neon(Postgres) — 개념 정리

### H2를 썼다 = 캐시(메모리)를 썼다?

**아니다.** H2는 두 가지 모드가 있다:

| 모드 | 예시 URL | 특징 |
|---|---|---|
| 메모리 모드 | `jdbc:h2:mem:testdb` | RAM에만 존재, 프로세스 끄면 100% 사라짐 — 이게 "캐시"에 가까움 |
| **파일 모드**(이 프로젝트가 쓴 것) | `jdbc:h2:file:./data/luckypinball` | 디스크에 실제 파일로 저장, 프로세스 재시작해도 파일이 남아있으면 유지 |

이 프로젝트의 `application.yml`은 파일 모드였다 — **H2 자체는 Postgres처럼 디스크에 진짜로 쓰는 DB였지, 캐시가 아니었다.** Render에서 데이터가 날아간 건 H2가 캐시라서가 아니라, **Render 무료 티어가 컨테이너의 디스크 전체를 재시작/슬립마다 초기화**하기 때문이다 — H2 파일은 멀쩡히 저장되고 있었는데, 그 디스크 자체가 플랫폼에 의해 사라진 것.

진짜 "캐시(메모리만)"에 해당했던 건 따로 있었다 — 3번에서 고친 `InMemoryGameRepository`의 `ConcurrentHashMap`이 그것이다. 이건 디스크를 아예 거치지 않고 JVM 메모리에만 있어서, H2든 Neon이든 상관없이 서버 재시작마다 무조건 사라지는 구조였다(그래서 가장 먼저 JPA 엔티티로 바꿔야 했다).

```
진짜 캐시(메모리만)   → InMemoryGameRepository (ConcurrentHashMap) — 이번에 JPA로 교체함
파일 기반 실제 DB     → H2 (jdbc:h2:file:...) — 그 자체는 멀쩡한데, Render 디스크가 날아감
관리형 외부 DB 서버   → Neon(Postgres) — Render 디스크와 무관하게 독립적으로 영속화
```

### H2 = Tibero/Oracle = Neon(Postgres)? — "DB 역할"은 같고 "제품"은 다르다

**개념적으로는 동일하다.** 셋 다 "JDBC로 접속해서 SQL을 주고받는 관계형 DB"라는 역할은 똑같고, JPA(`@Entity`)로 짠 코드는 세 제품 중 무엇에 붙어도 **코드 변경 없이** 그대로 동작한다 — 실제로 어제 `application.yml`의 URL만 바꿨지 `GameEntity` 코드는 한 줄도 안 고쳤다.

다만 **제품(엔진) 자체는 다르고**, 차이의 본질은 "DB가 똑똑하냐 아니냐"가 아니라 **배포 형태 / 기능 패키지 / 라이선스 비용 모델**이다:

| | H2 (개발용 임베디드) | Oracle / Tibero (상용 엔터프라이즈) | Postgres / Neon |
|---|---|---|---|
| 배포 형태 | 앱과 같은 프로세스 안에 내장 | 별도 서버 프로세스, DBA가 운영 | 오픈소스 엔진 + Neon이 서버 운영 대행 |
| 핵심 차이 | 운영 기능(복제·클러스터링·감사) 자체가 없음 — 개발/테스트 전용 | RAC(클러스터링), Data Guard(복제), PL/SQL, 유상 기술지원 계약이 패키지로 포함 | 기능은 충분히 엔터프라이즈급(ACID, 복제, 확장기능) — 유상 지원 계약 모델이 다를 뿐 |
| 비용 | 무료 | 코어/사용자당 상용 라이선스(고가) — Tibero는 Oracle 호환 + 저렴한 라이선스로 공공/금융권에서 대체재로 많이 씀 | 오픈소스 무료, Neon 같은 관리형 서비스는 사용량 기반 과금 |
| 왜 쓰는가 | 빠른 로컬 개발/테스트 | "장애나면 벤더가 책임지고 대응"하는 계약이 필요한 대규모 미션크리티컬 시스템(은행·공공기관) | 라이선스 비용 없이 충분한 기능으로 스타트업~중견 서비스까지 폭넓게 |

**오해하기 쉬운 포인트**: "Postgres가 Oracle보다 기능이 떨어진다"는 요즘은 사실이 아니다 — 차이는 기술력보다 **"장애 시 벤더가 책임지고 대응해주는 유상 지원 계약이 필요한가"**에 가깝다. 금융권/공공기관이 Oracle·Tibero를 고수하는 이유도 대부분 이 지원계약+기존 시스템과의 호환성 때문이지, Postgres가 기술적으로 못해서가 아니다.

**이 추상화(JPA)의 실전 증거**: Hibernate가 접속한 DB 제품을 보고 알맞은 SQL 방언으로 자동 번역해준다 — Render 로그의 `Database dialect: PostgreSQLDialect`가 그 증거다(H2에 붙으면 H2Dialect, Oracle이면 OracleDialect). 그래서 "H2로 개발하고 Postgres로 배포"라는, 실무에서도 흔한 전환을 엔티티 코드 변경 없이 설정값만 바꿔서 어제 직접 검증한 것이다.
