import { lazy, type ComponentType, type LazyExoticComponent } from 'react';

/**
 * "공부하기 > React" 챕터 목록.
 * 데모 컴포넌트는 02_React_basic(React_basic 저장소)의 chapter_03~15를 그대로 복사한 것이고,
 * 원본에서 index.js의 주석으로 하나씩 갈아 끼우던 실행 부분을 이 목록이 대신한다.
 *
 * 번호: 폴더(chapter_NN)는 README의 "섹션"보다 번호가 하나 작다(폴더 03 = README 섹션4 JSX … 폴더 15 = README ch16 CSS).
 * 화면에는 폴더 번호를 쓰고, 정리(notes)는 폴더 번호 기준 파일(notes/chNN.md)로 나눠 둔다.
 */
export interface Chapter {
  /** URL과 노트 파일 이름에 쓰는 값: ch03 … ch15 */
  id: string;
  /** 원본 폴더 이름 */
  folder: string;
  title: string;
  summary: string;
  /** 이 챕터에서 배우는 키워드(카드에 칩으로 표시) */
  keywords: string[];
  demo: LazyExoticComponent<ComponentType>;
  /** 데모가 console.log로 동작을 보여주는 챕터 — 화면에 로그 패널을 같이 보여준다 */
  console?: boolean;
  /** 데모 아래에 붙이는 안내 문구(원본과 달라진 점 등) */
  demoNote?: string;
}

export const CHAPTERS: Chapter[] = [
  {
    id: 'ch03',
    folder: 'chapter_03',
    title: 'JSX와 첫 컴포넌트',
    summary: 'JSX 문법으로 컴포넌트를 만들고, 하나의 컴포넌트(Book)를 여러 번 재사용합니다.',
    keywords: ['JSX', '컴포넌트', '재사용'],
    demo: lazy(() => import('./chapter_03/Library')),
  },
  {
    id: 'ch04',
    folder: 'chapter_04',
    title: '엘리먼트와 렌더링',
    summary: '1초마다 화면을 다시 그려 현재 시간을 보여줍니다. React는 바뀐 부분만 DOM에 반영합니다.',
    keywords: ['Element', '렌더링', 'Virtual DOM'],
    demo: lazy(() => import('./adapters/ClockDemo')),
    demoNote:
      '원본은 index.js에서 setInterval로 root.render()를 1초마다 다시 호출했습니다. 웹 데모에서는 같은 효과를 내는 래퍼가 1초마다 Clock을 다시 렌더링합니다.',
  },
  {
    id: 'ch05',
    folder: 'chapter_05',
    title: '컴포넌트와 Props',
    summary: '부모가 Props로 값을 넘겨 자식 컴포넌트(Comment)의 출력을 정합니다. 배열은 map으로 목록을 만듭니다.',
    keywords: ['Props', 'map', 'key 경고'],
    demo: lazy(() => import('./chapter_05/CommentList')),
    demoNote: '원본 그대로 key를 넣지 않았습니다. 브라우저 콘솔에 key 경고가 뜨는 것이 정상입니다(10장에서 다룹니다).',
  },
  {
    id: 'ch06',
    folder: 'chapter_06',
    title: 'State와 생명주기',
    summary: '클래스 컴포넌트의 state와 생명주기 메서드. 알림이 1초마다 하나씩 추가됩니다.',
    keywords: ['State', 'setState', '생명주기', 'class'],
    demo: lazy(() => import('./chapter_06/NotificationList')),
    console: true,
  },
  {
    id: 'ch07',
    folder: 'chapter_07',
    title: 'Hooks',
    summary: 'useState, useEffect와 직접 만든 훅(useCounter)으로 입장/퇴장 인원을 관리합니다.',
    keywords: ['useState', 'useEffect', 'Custom Hook'],
    demo: lazy(() => import('./chapter_07/Accommodate')),
    console: true,
  },
  {
    id: 'ch08',
    folder: 'chapter_08',
    title: '이벤트 처리',
    summary: '같은 버튼을 클래스 컴포넌트와 함수 컴포넌트 두 방식으로 만들어 이벤트 핸들러를 비교합니다.',
    keywords: ['onClick', 'this 바인딩', '화살표 함수'],
    demo: lazy(() => import('./adapters/ConfirmButtonsDemo')),
  },
  {
    id: 'ch09',
    folder: 'chapter_09',
    title: '조건부 렌더링',
    summary: '로그인 여부에 따라 로그인/로그아웃 버튼과 인사말을 다르게 보여줍니다.',
    keywords: ['조건부 렌더링', '삼항 연산자', '&&'],
    demo: lazy(() => import('./chapter_09/LanginPage')),
  },
  {
    id: 'ch10',
    folder: 'chapter_10',
    title: '리스트와 키',
    summary: '배열을 map으로 목록으로 바꾸고, 항목을 구분하는 고유한 key를 지정합니다.',
    keywords: ['List', 'key', 'map'],
    demo: lazy(() => import('./chapter_10/AttendanceBook')),
  },
  {
    id: 'ch11',
    folder: 'chapter_11',
    title: '폼',
    summary: '입력값을 state로 관리하는 제어 컴포넌트(textarea, input, select)와 제출 처리.',
    keywords: ['Form', '제어 컴포넌트', 'onChange'],
    demo: lazy(() => import('./chapter_11/SignUp')),
    demoNote: '원본의 오타 import(userState)만 제거했습니다. 번들러에서는 존재하지 않는 이름을 import하면 실행 오류가 납니다.',
  },
  {
    id: 'ch12',
    folder: 'chapter_12',
    title: 'State 끌어올리기',
    summary: '섭씨/화씨 입력창이 하나의 state를 공유하도록 state를 공통 부모(Calculator)로 올립니다.',
    keywords: ['Shared State', 'Lifting State Up'],
    demo: lazy(() => import('./chapter_12/Calculator')),
  },
  {
    id: 'ch13',
    folder: 'chapter_13',
    title: '합성(Composition)',
    summary: 'children props로 빈 상자(Card)를 만들고 안에 무엇이든 넣어 쓰는 합성. React는 상속 대신 합성을 씁니다.',
    keywords: ['Composition', 'children', 'Specialization'],
    demo: lazy(() => import('./chapter_13/ProfileCard')),
  },
  {
    id: 'ch14',
    folder: 'chapter_14',
    title: 'Context',
    summary: 'props를 층층이 내려보내지 않고 Context로 테마(라이트/다크)를 필요한 곳에 바로 전달합니다.',
    keywords: ['Context', 'Provider', 'useContext'],
    demo: lazy(() => import('./chapter_14/DarkOrLight')),
    demoNote: '원본은 화면 전체(100vw × 100vh)를 쓰는 예제라, 웹 데모에서는 아래 상자 크기에 맞춰 보여줍니다.',
  },
  {
    id: 'ch15',
    folder: 'chapter_15',
    title: '스타일링(styled-components)',
    summary: 'CSS를 JavaScript 안에서 컴포넌트로 만들고, props 값으로 스타일을 바꿉니다.',
    keywords: ['styled-components', 'CSS-in-JS', 'props로 스타일'],
    demo: lazy(() => import('./chapter_15/Blocks')),
    demoNote:
      '원본 코드 그대로라 브라우저 콘솔에 경고 두 가지가 뜹니다: ① 목록에 key가 없음, ② styled-components v6는 backgroundColor 같은 props를 DOM까지 넘겨 "인식하지 못하는 prop" 경고를 냅니다(해결: $backgroundColor처럼 $를 붙인 transient props).',
  },
];

export function findChapter(id: string | undefined): Chapter | undefined {
  return CHAPTERS.find((chapter) => chapter.id === id);
}
