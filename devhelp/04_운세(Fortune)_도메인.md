# 04. 운세(Fortune) 도메인

이 프로젝트에서 가장 설계 결정이 많이 들어간 부분입니다. `plan/01_mvp-plan.md`와 `plan/04_pinball-map-design.md`에서 여러 번 바뀐 내용(재방문 가중치 도입 → 제거, HP/충돌 데미지 → 시작 높이만)이 최종적으로 이 코드에 반영돼 있습니다.

## 1. 왜 "생성기"와 "저장/조합"을 분리했는가

`FortuneService`는 인터페이스 하나뿐입니다.

```java
public interface FortuneService {
    FortuneResult analyze(String name, LocalDate birthDate);
}
```

지금은 `MockFortuneGenerator`가 이걸 구현해서 규칙 기반으로 점수를 만들지만, 나중에 실제 OpenAI API를 호출하는 `OpenAiFortuneGenerator`를 새로 만들어서 **이 인터페이스만 구현**하면, 그걸 호출하는 다른 코드(`FortuneQueryService`)는 한 줄도 안 바꿔도 됩니다. 이런 설계를 "구현을 인터페이스 뒤로 숨긴다"고 하는데, Spring에서는 `@Service`가 붙은 클래스가 자동으로 그 인터페이스 타입의 빈(bean)으로 등록되기 때문에, 나중에 구현체를 교체해도 `FortuneQueryService` 생성자의 `FortuneService fortuneService` 파라미터 타입은 그대로 둬도 됩니다.

## 2. `MockFortuneGenerator`가 "같은 사람, 같은 날"에 같은 결과를 주는 이유

```java
long seed = Objects.hash(name, birthDate, LocalDate.now());
Random random = new Random(seed);
```

완전 무작위(`new Random()`)로 만들면, 같은 사람이 같은 날 두 번 조회했을 때 점수가 매번 달라집니다. 기획서 원안에는 "같은 날 재조회 시 캐시된 결과 사용"이라는 요구사항이 있었는데, 실제 캐시(Redis)는 아직 안 만들었지만 **시드를 name+birthDate+오늘 날짜로 고정**하면 "같은 입력 → 같은 결과"라는 캐시와 똑같은 성질을 얻을 수 있습니다. 이건 나중에 진짜 캐시를 붙일 때도 자연스럽게 이어지는 설계입니다.

## 3. `BuffCalculator` — 왜 별도 클래스로 뺐는가

```java
public static Buff fromScore(int fortuneScore) { ... }
```

점수를 버프로 바꾸는 로직은 **입력이 같으면 출력도 항상 같은 순수 함수**입니다 (DB도, 외부 API도 건드리지 않음). 이런 로직은 Spring 빈으로 만들 필요 없이 `static` 메서드로 두는 게 더 간단하고, 테스트하기도 훨씬 쉽습니다 (`BuffCalculatorTest`에서 스프링 컨텍스트를 띄우지 않고도 순식간에 검증할 수 있었던 이유입니다).

버프 값(시작 Y 오프셋)은 여러 번의 대화를 거쳐 최종적으로 이렇게 확정됐습니다: HP나 충돌 데미지 차등은 전부 없어지고, **시작 높이 하나만** 존재합니다 (0/15/30/45/60). 운세가 안 좋을수록 결승선에 가까운 곳(오프셋이 큼)에서 출발합니다.

## 4. `FortuneQueryService` — 왜 컨트롤러 로직을 한 번 더 감쌌는가

`POST /api/fortune`(참가자 개인이 운세를 확인)과 `POST /api/game/create`(게임에 참여할 모든 참가자의 운세를 한꺼번에 확인)는 **완전히 같은 로직**(참가자 조회 → 운세 생성 → 버프 계산 → 이력 저장)이 필요합니다. 이 로직을 `FortuneController`에만 두면 `GameService`에서 코드를 복사/붙여넣기 해야 하는데, 그러면 나중에 로직이 하나만 바뀌고 다른 하나는 안 바뀌는 실수가 생기기 쉽습니다. 그래서 이 공통 로직을 `FortuneQueryService.getTodayFortune(playerId)` 하나로 뽑아뒀고, `FortuneController`와 `GameService` 둘 다 이걸 호출합니다.

## 5. `FortuneResultEntity` — 이제는 "기록용"일 뿐

원래 계획에는 이 테이블의 과거 점수를 오늘 점수와 가중평균하는 기능이 있었지만, 대화 중에 "버프가 복잡해 보인다"는 피드백과 함께 이 가중치 로직 자체를 없앴습니다. 그래서 지금 `FortuneQueryService`는 과거 기록을 **읽지 않고**, 매번 새로 만든 점수를 그대로 쓰되, 그 결과를 `FortuneResultJpaRepository`에 저장만 해둡니다 — 이게 나중에 관리자 화면(`GET /api/admin/players`)에서 "이 사람이 지금까지 몇 점을 받아왔는지" 보여주는 용도로 쓰입니다.

## 검증 요약 (자세한 내용은 `07_빌드와_테스트_검증.md`)

- `BuffCalculatorTest`: 점수 구간 경계값(0, 19, 20, 39, ...)이 정확한 티어로 변환되는지 확인.
- `MockFortuneGeneratorTest`: 점수가 0~100 범위를 벗어나지 않는지, 같은 사람·같은 날은 결정적인지 확인.
- 실제 서버를 띄워서 `POST /api/fortune` 호출 → 응답의 `buff.startY`가 `BuffCalculator` 표와 정확히 일치하는 것을 curl로 확인.

다음 문서: [`05_게임(Game)_도메인.md`](./05_게임(Game)_도메인.md)
