import { Suspense, lazy } from 'react';
import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom';
import { GIT_CHAPTERS, findGitChapter, isReady } from '../study/git/chapters';
import { StudyGoals } from '../study/StudyGoals';

// 탭 내용은 처음 열 때만 불러온다(마크다운 렌더러와 하이라이터가 무거워서).
const MarkdownNotes = lazy(() => import('../study/react/NotesView').then((m) => ({ default: m.MarkdownNotes })));
const GitCodeView = lazy(() => import('../study/git/GitCodeView').then((m) => ({ default: m.GitCodeView })));

const notes = import.meta.glob('../study/git/notes/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;
const projectNotes = import.meta.glob('../study/git/project/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

type Tab = 'notes' | 'code' | 'project';
const TAB_LABELS: Record<Tab, string> = {
  notes: '정리',
  code: '명령과 결과',
  project: '이 프로젝트에서는',
};
const TABS: Tab[] = ['notes', 'code', 'project'];

const loading = <p className="status-text">불러오는 중...</p>;

export function StudyGitChapterPage() {
  const { chapterId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const chapter = findGitChapter(chapterId);
  if (!chapter || !isReady(chapter)) {
    return <Navigate to="/study/git" replace />;
  }

  const tab = TABS.find((t) => t === searchParams.get('tab')) ?? 'notes';
  const ready = GIT_CHAPTERS.filter(isReady);
  const index = ready.indexOf(chapter);
  const prev = ready[index - 1];
  const next = ready[index + 1];
  // 이전/다음 챕터로 가도 보던 탭을 유지한다.
  const tabSearch = tab === 'notes' ? '' : `?tab=${tab}`;

  return (
    <>
      <header className="top">
        <p className="study-crumb">
          <Link to="/study/git">공부하기 · Git</Link> / {Number(chapter.id.replace('ch', ''))}장
        </p>
        <h2>{chapter.title}</h2>
        <p>{chapter.summary}</p>
      </header>

      <StudyGoals goal={chapter.goal} outcome={chapter.outcome} />

      <div className="study-tabs" role="tablist">
        {TABS.map((t) => (
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
          {tab === 'notes' && <MarkdownNotes noteKey={`git-${chapter.id}`} loader={notes[`../study/git/notes/${chapter.id}.md`]} />}
          {tab === 'code' && <GitCodeView key={chapter.id} chapter={chapter} />}
          {tab === 'project' && (
            <MarkdownNotes noteKey={`git-project-${chapter.id}`} loader={projectNotes[`../study/git/project/${chapter.id}.md`]} />
          )}
        </Suspense>
      </section>

      <nav className="study-pager">
        {prev ? <Link to={`/study/git/${prev.id}${tabSearch}`}>← {prev.title}</Link> : <span />}
        {next ? <Link to={`/study/git/${next.id}${tabSearch}`}>{next.title} →</Link> : <span />}
      </nav>
    </>
  );
}
