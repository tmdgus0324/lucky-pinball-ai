# 44. 공부하기 > React — 학습 자료를 웹에서 실행하고 읽기

핀볼 프로젝트와는 별개로 하는 **extra 과제**다. 예전에 React를 공부하며 만든 `02_React_basic`(별도 저장소 `React_basic`)을, 사이드 메뉴 "공부하기 > React"에서 **예제를 실행해 보고, 코드를 보고, 정리를 펼쳐 읽는** 화면으로 옮겼다.

## 1. 구조

```
사이드바  게임하기 | 관리하기 | 공부하기 ▾ → React
           /study/react            챕터 카드 13개 (03~15장)
           /study/react/ch07       챕터 화면: [데모] [코드] [정리] 탭 + 이전/다음 챕터
```

```
frontend-react/src/
├─ pages/StudyReactPage.tsx, StudyReactChapterPage.tsx   ← 라우트 (둘 다 lazy 로딩)
└─ study/react/
   ├─ chapters.ts          챕터 목록(제목·요약·키워드·데모 컴포넌트·안내 문구)
   ├─ chapter_03 … 15/     02_React_basic의 예제를 그대로 복사 (23개 파일)
   ├─ adapters/            원본 index.js가 하던 일을 웹에서 대신하는 래퍼 2개
   ├─ IsolatedRoot.tsx     데모를 StrictMode 밖의 별도 React 루트에서 실행
   ├─ ConsolePanel.tsx     console.log를 화면에도 출력
   ├─ CodeView.tsx         실제 실행되는 파일을 ?raw로 읽어 코드 탭에 표시
   ├─ NotesView.tsx        notes/chNN.md를 `## ` 단위 접이식으로 표시
   └─ notes/ch03.md … ch15.md   README를 챕터별 마크다운으로 정리
```

- **복사본이다.** `02_React_basic`은 별도 저장소라서 핀볼 저장소를 배포하는 Vercel이 볼 수 없다. 그쪽 파일은 건드리지 않았고, 이쪽 복사본은 학습 당시 코드 그대로 둔다(린트 제외).
- 챕터 화면은 처음 열 때만 불러온다(`React.lazy`). 마크다운 렌더러(react-markdown)는 약 156kB(gzip 47kB)인데 **정리 탭을 열 때만** 받는다. 핀볼 첫 화면 번들(메인)은 약 5kB 늘었다.
- 새 의존성: `styled-components`(15장 예제), `react-markdown`, `remark-gfm`. 그 외 `tsconfig`에 `allowJs`를 켜서 `.jsx`를 import할 수 있게 했다(타입 검사는 안 함).

## 2. 번호 대응 (README ↔ 폴더)

README의 "섹션N"과 폴더 번호는 하나씩 어긋나 있고, 끝의 세 섹션은 이름 자체가 `ch14/ch15/ch16`이다. **폴더 03~15가 README 섹션4~16과 13개 모두 일대일**로 대응한다.

| 폴더 | 주제 | README |
|---|---|---|
| 03 | JSX와 첫 컴포넌트 | 섹션4 (+ 섹션2·3, 개발환경, JS 기초를 첫 챕터에 "시작하기 전에"로 붙임) |
| 04 | 엘리먼트와 렌더링 | 섹션5 |
| 05 | 컴포넌트와 Props | 섹션6 |
| 06 | State와 생명주기 | 섹션7 |
| 07 | Hooks | 섹션8 |
| 08 | 이벤트 처리 | 섹션9 |
| 09 | 조건부 렌더링 | 섹션10 |
| 10 | 리스트와 키 | 섹션11 |
| 11 | 폼 | 섹션12 |
| 12 | State 끌어올리기 | 섹션13 |
| 13 | 합성 | ch14 |
| 14 | Context | ch15 |
| 15 | 스타일링 | ch16 (CSS + styled-components) |

## 3. README를 어떻게 옮겼나

README는 1,482줄짜리 **들여쓴 텍스트 개요**(`(1)`, `->`)라서 마크다운으로 그대로 렌더링하면 들여쓴 줄은 코드 블록이 되고 나머지는 한 문단으로 합쳐진다. 그래서 챕터별 마크다운(`notes/chNN.md`)으로 다시 정리하고 **빌드에 포함**했다(GitHub에서 실행 시점에 가져오지 않음 — 서식 없는 원문이 보이고 네트워크에 의존하게 되므로).

- 내용은 원문 그대로 옮기되, **코드 오타는 바로잡았다**: `onChage`, `</lable>`, `evnet.target.cheched`, `toFahrengeit`, `lendth`, `class Toggle extend`, 닫히지 않은 따옴표·태그 등. `componentDidUnmount`는 실제 이름인 `componentWillUnmount`로 고치고 그 사실을 본문에 적었다.
- 원문에 없던 설명이나 빠진 코드(예: `useUserStatus` 구현, 합성/Context 예제 코드, `count && ...`가 `0`을 그리는 함정)는 **"(보충)"** 으로 표시해서 원문과 구분했다.
- `## ` 제목 단위로 접이식(`<details>`)이 되고 첫 항목만 펼쳐 둔다.
- 한계: 이 복사본은 **한 번 옮겨 온 스냅샷**이다. React_basic의 README를 고쳐도 여기에는 자동 반영되지 않는다.

## 4. 만들면서 발견한 것

1. **원본 코드의 오타 import가 번들러에서는 실행 오류**: `chapter_11/SignUp.jsx`의 `import React, {userState, useState}`. CRA(웹팩)는 넘어가지만 Vite에서는 존재하지 않는 이름을 import하면 모듈 로딩이 실패한다. 복사본에서 이 한 줄만 고쳤고 화면 안내에도 적었다.
2. **개발 모드의 StrictMode가 생명주기 예제를 망친다**: 이 앱 전체가 `<StrictMode>`라서 개발 모드에서는 `componentDidMount`/`useEffect`가 두 번 실행된다. 6장의 로그가 `Mount → Unmount → Mount`로 어지럽게 찍혔는데, 원본에서 "17버전으로 다운"했던 이유와 같은 현상이다. **데모를 별도 React 루트(`IsolatedRoot`, StrictMode 밖)에서 실행**하도록 바꾸자 `Mount → Update → 다음 알림 Mount` 순서로 정상 출력됐다. 이렇게 하면 개발/배포 결과가 같고 React 17 동작과도 같다.
   - 같은 div에 `createRoot`를 두 번 부르면 경고가 나므로, 효과가 실행될 때마다 새 div에 새 루트를 만든다.
3. **콘솔 로그를 화면에 보여줘야 학습이 된다**: 6·7장은 `console.log`로 동작을 보여주는 예제다. `ConsolePanel`이 화면에 떠 있는 동안만 `console.log`를 가로채 화면에도 출력한다(원래 콘솔 출력은 유지, 떠날 때 원복).
4. **원본은 index.js에서 하던 일을 래퍼로 대신한다**: 4장 시계는 `setInterval`로 `root.render`를 1초마다 다시 호출했으므로 래퍼가 같은 효과를 낸다. 8장은 `ConfirmButton`(클래스)과 `ConfirmButton2`(함수)를 나란히 보여준다.
5. **전역 CSS가 예제에 번진다**: 앱의 `button { 주황색 … }`가 예제 버튼에 적용됐다. 데모 영역을 흰 상자로 만들고 그 안에서 `button, input, textarea, select { all: revert }`로 브라우저 기본 모양을 되돌렸다.
6. **14장은 `100vw × 100vh` 예제**라 데모 상자 안에서 줄여 보여주도록 CSS를 따로 두었다.
7. **15장은 원본 그대로 콘솔 경고가 두 가지** 뜬다(key 없음, styled-components v6가 `backgroundColor`를 DOM까지 넘김). 학습 자료라 고치지 않고 안내 문구로 설명했다(해결법은 `$backgroundColor` 같은 transient props).
8. 린트: 복사한 예제는 학습 당시 코드 그대로 보여주는 용도라 `.oxlintrc.json`의 `ignorePatterns`로 제외했다. 새로 쓴 코드는 경고 0건이다.

## 5. 검증

- 브라우저(Playwright)로 **13개 챕터를 모두 실행**하고 상호작용을 확인했다: 시계가 1초마다 바뀜 / 알림이 1초마다 추가되고 로그가 올바른 순서 / 정원 10명에서 입장 버튼 비활성과 "정원이 가득찼습니다" / 두 확인 버튼 / 로그인↔로그아웃 / 섭씨 100 → 화씨 212와 "물이 끓습니다" / 테마 전환(흰색 → 검정) 등.
- 13개 정리 탭을 모두 열어 접이식 개수·코드 블록·표를 확인했고 오류 0건, 펼치기 동작과 모바일(390px) 가로 스크롤 없음을 확인했다.
- 사이드바: 게임 화면에서는 접혀 있고, 공부하기 화면에서는 펼쳐진 채로 시작하며, "React" 메뉴가 활성 표시된다. 모바일에서는 메뉴 한 줄에 칩처럼 늘어선다.
- `tsc`, `lint`, `build` 통과.

## 6. 나중에 얹을 수 있는 C안 — Sandpack

**Sandpack**은 CodeSandbox가 만든 오픈소스 컴포넌트로, 브라우저 안에서 **코드를 직접 고치고 바로 실행**하는 에디터+미리보기를 제공한다. 지금의 [코드] 탭(읽기 전용)을 이걸로 바꾸거나 [직접 실행] 탭을 하나 더 두는 방식이다.

**확인한 사실**(npm 기준): `@codesandbox/sandpack-react` 2.20.0, 라이선스 Apache-2.0, React 16.8~19를 지원, 압축을 푼 패키지 크기 약 1.2MB(번들에 들어가는 크기가 아니라 패키지 크기다).

**지금 구조에 붙이는 방법**: 이미 실행되는 소스를 `?raw`로 읽고 있으므로 그 문자열을 그대로 Sandpack의 `files`에 넘기면 된다.

```tsx
<SandpackProvider
  template="react"
  files={{ '/Library.jsx': libraryRaw, '/Book.jsx': bookRaw, '/index.js': entry /* 데모를 렌더링하는 진입 파일 */ }}
  customSetup={{ dependencies: { react: '17.0.2', 'react-dom': '17.0.2' } }}
>
  <SandpackLayout><SandpackCodeEditor /><SandpackPreview /></SandpackLayout>
</SandpackProvider>
```

- 장점
  - 코드를 **직접 수정해 보며** 공부할 수 있다(지금은 읽기만 가능).
  - **React 17을 그대로 선택**할 수 있어서 원본 학습 환경과 가장 가깝다(지금은 React 19 위에서 격리해 흉내 낸다).
  - 챕터별 진입 파일만 만들면 되고, 코드 표시/실행이 한 곳에서 해결된다.
- 단점과 위험
  - **외부 의존**: Sandpack은 기본적으로 CodeSandbox가 호스팅하는 번들러를 iframe으로 불러와 실행하는 구조로 알고 있다(구현 전에 최신 동작 확인 필요). 그렇다면 CodeSandbox 서비스가 막히거나 느린 환경(사내망, 오프라인)에서는 데모가 안 뜬다. 지금 방식은 우리 서버 파일만 쓰므로 이런 의존이 없다.
  - **무겁다**: 에디터와 번들러 클라이언트가 커서 반드시 lazy 로딩해야 하고, 첫 실행에 몇 초가 걸릴 수 있다(실측하지 않았다).
  - **모바일에서 코드 편집이 불편**하다.
  - 15장 styled-components처럼 외부 패키지는 `customSetup`으로 따로 지정해야 하고, 6·7장의 `console.log` 패널은 Sandpack 내부 콘솔로 대체해야 한다.
- 예상 작업량: 반나절 안팎(탭 추가, 챕터별 진입 파일 13개, 의존성 지정, 로딩/오류 처리, 모바일 확인).
- **판단**: 지금은 필요하지 않다. 읽고 실행해 보며 공부하는 목적은 이미 충족하고, 외부 서비스 의존 없이 항상 같은 결과가 나온다는 장점이 더 크다. "직접 고쳐 보며 실습"이 정말 필요해질 때 [직접 실행] 탭으로 **추가**하는 것을 권한다(기존 [코드] 탭은 읽기용으로 남겨 두고, Sandpack이 안 뜨는 환경에서도 코드를 볼 수 있게).

## 7. 한계

- 예제는 React 17 기준으로 공부한 코드를 **React 19에서 실행**한다. 이번 예제에서는 차이가 없었지만(제거된 API를 쓰지 않음), 앞으로 예제를 추가하면 확인이 필요하다.
- 코드/정리 모두 원본과 **따로 존재하는 복사본**이다. 원본을 고쳐도 자동으로 따라오지 않는다.
- 코드 하이라이트는 넣지 않았다(단색 표시). 필요하면 하이라이터를 lazy로 추가할 수 있다.
