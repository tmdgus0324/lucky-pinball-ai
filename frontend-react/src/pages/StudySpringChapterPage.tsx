import { Suspense, lazy } from 'react';
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom';
import { SPRING_CHAPTERS, findSpringChapter, isReady } from '../study/spring/chapters';

// 탭 내용은 처음 열 때만 불러온다(마크다운 렌더러와 하이라이터가 무거워서).
const MarkdownNotes = lazy(() => import('../study/react/NotesView').then((m) => ({ default: m.MarkdownNotes })));
const SpringCodeView = lazy(() => import('../study/spring/SpringCodeView').then((m) => ({ default: m.SpringCodeView })));
const LiveRequest = lazy(() => import('../study/spring/LiveRequest').then((m) => ({ default: m.LiveRequest })));

const notes = import.meta.glob('../study/spring/notes/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;
const projectNotes = import.meta.glob('../study/spring/project/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

type Tab = 'notes' | 'code' | 'project' | 'live';
const TAB_LABELS: Record<Tab, string> = {
  notes: '정리',
  code: '코드',
  project: '이 프로젝트에서는',
  live: '직접 요청',
};

const loading = <p className="status-text">불러오는 중...</p>;

export function StudySpringChapterPage() {
  const { chapterId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const chapter = findSpringChapter(chapterId);
  if (!chapter || !isReady(chapter)) {
    return <Navigate to="/study/spring" replace />;
  }

  const tabs: Tab[] = ['notes', 'code', 'project', ...(chapter.live ? (['live'] as Tab[]) : [])];
  const tab = tabs.find((t) => t === searchParams.get('tab')) ?? 'notes';
  const ready = SPRING_CHAPTERS.filter(isReady);
  const index = ready.indexOf(chapter);
  const prev = ready[index - 1];
  const next = ready[index + 1];
  // 이전/다음 챕터로 가도 보던 탭을 유지한다.
  const tabSearch = tab === 'notes' ? '' : `?tab=${tab}`;

  return (
    <>
      <header className="top">
        <p className="study-crumb">
          <Link to="/study/spring">공부하기 · Spring Boot</Link> / {Number(chapter.id.replace('ch', ''))}장
        </p>
        <h2>{chapter.title}</h2>
        <p>{chapter.summary}</p>
      </header>

      <div className="study-tabs" role="tablist">
        {tabs.map((t) => (
          <button
            type="button"
            role="tab"
            key={t}
            aria-selected={t === tab}
            className={t === tab ? 'study-tab active' : 'study-tab'}
            onClick={() => setSearchParams(t === 'notes' ? {} : { tab: t }, { replace: true })}
          >
            {TAB_LABELS[t]}
          </button>
        ))}
      </div>

      <section className="panel">
        <Suspense fallback={loading}>
          {tab === 'notes' && (
            <MarkdownNotes noteKey={`spring-${chapter.id}`} loader={notes[`../study/spring/notes/${chapter.id}.md`]} />
          )}
          {tab === 'code' && <SpringCodeView key={chapter.id} chapter={chapter} />}
          {tab === 'project' && (
            <MarkdownNotes
              noteKey={`spring-project-${chapter.id}`}
              loader={projectNotes[`../study/spring/project/${chapter.id}.md`]}
            />
          )}
          {tab === 'live' && chapter.live && <LiveRequest specs={chapter.live} />}
        </Suspense>
      </section>

      <nav className="study-pager">
        {prev ? <Link to={`/study/spring/${prev.id}${tabSearch}`}>← {prev.title}</Link> : <span />}
        {next ? <Link to={`/study/spring/${next.id}${tabSearch}`}>{next.title} →</Link> : <span />}
      </nav>
    </>
  );
}
