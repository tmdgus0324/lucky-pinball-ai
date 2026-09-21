# 06. 관리자(Admin) API

`plan/01_mvp-plan.md`에서 정한 대로, 관리자 화면은 "조회 기능은 실제로 동작, 가중치 조정은 골격만"으로 만들었습니다.

## 1. 이미 만들어둔 조각들을 재사용

`AdminController`를 보면 새로운 로직이 거의 없다는 걸 알 수 있습니다.

- `GET /api/admin/players` → 이미 있는 `PlayerJpaRepository.findAll()`과 `FortuneResultJpaRepository.findByPlayerIdOrderByCreatedDateAsc()`를 조합.
- `GET /api/admin/games` → 이미 있는 `GameRepository.findAll()`을 그대로 노출.
- `GET /api/admin/logs` → `02_백엔드_공통_설정.md`에서 만든 `ErrorLogStore.recent()`.

이건 우연이 아니라, 앞선 도메인들을 만들 때 **Repository를 인터페이스로 분리**해뒀기 때문에 가능한 결과입니다. 관리자 API는 "새 기능"이 아니라 "이미 있는 데이터를 다른 각도로 보여주는 창구"일 뿐이라, 별도 서비스 계층 없이 컨트롤러에서 리포지토리를 직접 조합해도 무리가 없다고 판단했습니다 (도메인 로직이 있는 `PlayerService`/`GameService`와 달리, 여기는 단순 조회 조합뿐이라서).

## 2. 스텁 엔드포인트를 "그냥 안 만들기"가 아니라 "501로 응답"하게 만든 이유

```java
@PostMapping("/api/admin/fortune/override")
@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public void overrideFortune(...) {
    throw ApiException.notImplemented("Not implemented yet — planned for Phase 2");
}
```

엔드포인트 자체를 안 만들면 프론트엔드에서 호출했을 때 `404 Not Found`가 오는데, 이건 "이 API가 아예 존재하지 않는다"는 뜻처럼 보입니다. 반면 `501 Not Implemented`는 "API는 있는데, 아직 기능이 구현 안 됐다"는 걸 명확히 구분해서 알려줍니다. `02_백엔드_공통_설정.md`에서 만든 `GlobalExceptionHandler`/`ApiException` 덕분에, 이 구분을 위해 새 코드를 추가할 필요 없이 `ApiException.notImplemented(...)`만 던지면 끝났습니다 — 공통 인프라를 먼저 만들어둔 효과를 여기서 체감할 수 있습니다.

## 3. 인증이 없다는 것을 왜 계속 언급하는가

이 컨트롤러는 로그인 없이 누구나 호출할 수 있습니다. 로컬 개발/데모 단계에서는 문제가 없지만, 실제로 인터넷에 공개 배포하면 누구나 다른 사람의 참가자 목록이나 오류 로그를 볼 수 있게 됩니다. `plan/01_mvp-plan.md`의 Phase 2 목록에 "관리자 인증"이 명시적으로 들어있는 이유이기도 하고, 이 devhelp 문서에서도 반복해서 짚는 이유입니다 — 나중에 배포 단계로 넘어갈 때 절대 빠뜨리면 안 되는 항목이기 때문입니다.

다음 문서: [`07_빌드와_테스트_검증.md`](./07_빌드와_테스트_검증.md)
