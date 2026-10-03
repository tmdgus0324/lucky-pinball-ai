import { Component, Suspense, lazy, useState, type ErrorInfo, type ReactNode } from 'react';
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom';
import { CHAPTERS, findChapter } from '../study/react/chapters';
import { ConsolePanel } from '../study/react/ConsolePanel';
import { IsolatedRoot } from '../study/react/IsolatedRoot';

// 코드/정리 탭은 처음 열 때만 불러온다(마크다운 렌더러가 무거워서 챕터 데모 로딩에 끼지 않게).
const CodeView = lazy(() => import('../study/react/CodeView').then((m) => ({ default: m.CodeView })));
const NotesView = lazy(() => import('../study/react/NotesView').then((m) => ({ default: m.NotesView })));

type Tab = 'demo' | 'code' | 'notes';
const TABS: { id: Tab; label: string }[] = [
  { id: 'demo', label: '데모' },
  { id: 'code', label: '코드' },
  { id: 'notes', label: '정리' },
];

/** 예제 하나가 오류를 내도 페이지 전체가 하얗게 죽지 않게 데모 영역만 막는다. */
class DemoErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('데모 실행 오류', error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return <p className="status-text error">예제를 실행하는 중 오류가 났습니다: {this.state.error.message}</p>;
    }
    return this.props.children;
  }
}

export function StudyReactChapterPage() {
  const { chapterId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [runKey, setRunKey] = useState(0);

  const chapter = findChapter(chapterId);
  if (!chapter) {
    return <Navigate to="/study/react" replace />;
  }

  const tab = (TABS.find((t) => t.id === searchParams.get('tab'))?.id ?? 'demo') as Tab;
  const index = CHAPTERS.indexOf(chapter);
  const prev = CHAPTERS[index - 1];
  const next = CHAPTERS[index + 1];
  const Demo = chapter.demo;

  return (
    <>
      <header className="top">
        <p className="study-crumb">
          <Link to="/study/react">공부하기 · React</Link> / {chapter.id.replace('ch', '')}장
        </p>
        <h2>{chapter.title}</h2>
        <p>{chapter.summary}</p>
      </header>

      <div className="study-tabs" role="tablist">
        {TABS.map((t) => (
          <button
            type="button"
            role="tab"
            key={t.id}
            aria-selected={t.id === tab}
            className={t.id === tab ? 'study-tab active' : 'study-tab'}
            onClick={() => setSearchParams(t.id === 'demo' ? {} : { tab: t.id }, { replace: true })}
          >
            {t.label}
          </button>
        ))}
      </div>

      <section className="panel">
        {tab === 'demo' && (
          <>
            <div className="panel-head">
              <h2>실행 결과</h2>
              <button type="button" className="secondary" onClick={() => setRunKey((k) => k + 1)}>
                다시 실행
              </button>
            </div>
            <div className="study-demo">
              <IsolatedRoot key={`${chapter.id}-${runKey}`}>
                <DemoErrorBoundary>
                  <Suspense fallback={<p>불러오는 중...</p>}>
                    <Demo />
                  </Suspense>
                </DemoErrorBoundary>
              </IsolatedRoot>
            </div>
            {chapter.demoNote && <p className="status-text">ℹ️ {chapter.demoNote}</p>}
            {chapter.console && <ConsolePanel key={`${chapter.id}-${runKey}`} />}
          </>
        )}
        {tab === 'code' && (
          <Suspense fallback={<p className="status-text">불러오는 중...</p>}>
            <CodeView chapter={chapter} />
          </Suspense>
        )}
        {tab === 'notes' && (
          <Suspense fallback={<p className="status-text">불러오는 중...</p>}>
            <NotesView chapter={chapter} />
          </Suspense>
        )}
      </section>

      <nav className="study-pager">
        {prev ? <Link to={`/study/react/${prev.id}`}>← {prev.title}</Link> : <span />}
        {next ? <Link to={`/study/react/${next.id}`}>{next.title} →</Link> : <span />}
      </nav>
    </>
  );
}
