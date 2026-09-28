# 30. GitHub Actions CI 추가

Phase 5의 첫 항목. `git push`할 때마다 백엔드 테스트와 프론트엔드 빌드가 자동으로 돌아가도록 CI를 붙였다.

## 왜 필요한가

지금까지는 테스트를 항상 로컬에서 손으로 실행했다. 실수로 테스트가 깨지는 코드를 그대로 푸시해도, 누가 직접 `./gradlew test`를 돌려보기 전까지는 아무도 모른다. CI는 그 확인 과정을 자동화해서, 커밋마다 "빌드/테스트가 통과했는지"를 GitHub이 대신 보여주게 한다.

## 사전에 확인한 것

CI에 `ANTHROPIC_API_KEY` 같은 시크릿을 넣어야 하는지부터 확인했다. 기존 테스트(`RateLimitFilterTest`, `BuffCalculatorTest`, `MockFortuneGeneratorTest`, 컨텍스트 로딩 테스트) 중 실제 Claude API를 호출하는 건 없었다. `ClaudeFortuneGenerator`가 API 클라이언트를 최초 호출 시점에 지연 생성하도록 이미 만들어져 있어서, 키가 없어도 스프링 컨텍스트 자체는 정상적으로 뜬다. 그래서 **CI 저장소에 시크릿을 등록할 필요가 없었다.**

## 구성

`.github/workflows/ci.yml`에 두 잡을 병렬로 구성:

| 잡 | 내용 |
|---|---|
| `backend-test` | JDK 17(Temurin) 설치 → Gradle 캐시 복원 → `./gradlew test` |
| `frontend-build` | Node 20 설치 → `npm ci` → `npm run lint`(oxlint) → `npm run build`(`tsc -b && vite build`, 타입 에러도 여기서 잡힘) |

`master`로의 push와 `master` 대상 pull request에서 동작하도록 설정했다. 지금은 혼자 작업하는 저장소라 PR 트리거는 당장 쓰이지 않지만, 나중에 브랜치를 나눠 작업하게 되면 그대로 활용할 수 있다.

Gradle 캐시는 `~/.gradle/caches`, `~/.gradle/wrapper`를 `backend/**/*.gradle*`과 wrapper 설정 파일 해시로 키를 잡아서, 의존성이 안 바뀌면 다음 실행부터 빠르게 돈다.

## README 배지

```
[![CI](https://github.com/tmdgus0324/lucky-pinball-ai/actions/workflows/ci.yml/badge.svg)](...)
```
README 맨 위에 추가해서, 저장소를 열어보는 사람이 바로 "테스트가 자동으로 돌아가는 프로젝트"라는 걸 알 수 있게 했다.

## 검증

워크플로우를 추가하기 전에 로컬에서 세 가지를 먼저 확인했다 — `./gradlew test`, `npm run lint`, `npm run build` 모두 종료 코드 0으로 통과. 실제 GitHub Actions에서의 동작은 푸시 후 Actions 탭에서 최종 확인했다.

## 남은 것

관리자 화면 인증은 Phase 5의 나머지 항목으로 남겨뒀다(이번 작업 범위 밖).
