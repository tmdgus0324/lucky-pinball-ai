# frontend-react

AI Lucky Pinball의 주력 프론트엔드입니다(React 19 + TypeScript + Vite, Matter.js). 프로젝트 소개는 [루트 README](../README.md), 구조 설명은 [`devdocs/01_구조`](../devdocs/01_구조.md)에 있습니다.

## 실행

```bash
npm install
npm run dev       # http://localhost:5173
npm run build     # tsc -b && vite build (타입 검사 포함)
npm run lint      # oxlint
npm run preview   # 빌드 결과 미리 보기
```

- 백엔드 주소는 환경변수 `VITE_API_BASE`로 정합니다. 없으면 `http://localhost:8080`입니다.
- 개발 서버 포트를 바꾸면 백엔드 `WebConfig.java`의 CORS 허용 목록에도 추가해야 합니다.
- 배포는 Vercel(Root Directory `frontend-react`)이고, `master`에 push하면 자동으로 다시 배포됩니다.

## 폴더

| 위치 | 내용 |
|---|---|
| `src/pages/` | 게임(`/`), 관리자(`/admin`), 공부하기(`/study/react`) 화면 |
| `src/components/` | 등록 폼, 운세 카드, 핀볼 보드, 관리자 표·로그·AI 연결 확인 등 |
| `src/game/engine.ts` | 물리 엔진. React 밖에서 DOM을 직접 갱신하고, `PinballBoard`는 마운트·정리만 맡습니다 |
| `src/api/client.ts` | API 호출 공통 처리(관리자 토큰, 오류 메시지와 오류 ID) |
| `src/hooks/`, `src/utils/` | 목록 페이징 훅, 생년월일 파싱, 시작 높이 미리보기 계산 등 |
| `src/study/react/` | 공부하기 > React 학습 페이지의 예제와 정리 노트 |

물리 값(맵·충돌·카메라)을 바꿀 때는 레거시 `frontend/js/game.js`도 같이 고쳐야 합니다. 지켜야 할 규칙은 [`CLAUDE.md`](../CLAUDE.md)에 있습니다.
