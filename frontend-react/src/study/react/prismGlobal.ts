import { Prism } from 'prism-react-renderer';

// prismjs의 언어 파일(prism-java.js 등)은 전역 변수 Prism에 문법을 등록하는 방식이다.
// prism-react-renderer가 쓰는 Prism을 전역에 걸어 두면, 그 파일들을 불러오기만 해도 이 Prism에 등록된다.
// Highlighter.tsx에서 언어 파일보다 먼저 import해야 한다.
(globalThis as { Prism?: typeof Prism }).Prism = Prism;
