import { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { Chapter } from './chapters';

// 정리 노트는 빌드에 포함한다(GitHub에서 실행 시점에 가져오지 않는다) — 네트워크와 무관하게 항상 같은 내용이 보이도록.
const notes = import.meta.glob('./notes/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

interface Section {
  title: string | null;
  body: string;
}

/** `## ` 제목을 기준으로 나눈다. 첫 제목 앞의 글은 제목 없는 도입부가 된다. */
function splitSections(raw: string): Section[] {
  const sections: Section[] = [];
  let current: Section = { title: null, body: '' };
  let inFence = false;
  for (const line of raw.split('\n')) {
    if (line.trimStart().startsWith('```')) inFence = !inFence;
    if (!inFence && line.startsWith('## ')) {
      if (current.title !== null || current.body.trim()) sections.push(current);
      current = { title: line.slice(3).trim(), body: '' };
    } else {
      current.body += line + '\n';
    }
  }
  if (current.title !== null || current.body.trim()) sections.push(current);
  return sections;
}

/** 접이식 제목은 마크다운 렌더러를 거치지 않으므로, 제목 안의 `인라인 코드`만 <code>로 바꿔 보여준다. */
function renderTitle(title: string) {
  return title.split('`').map((part, index) => (index % 2 === 1 ? <code key={index}>{part}</code> : part));
}

export function NotesView({ chapter }: { chapter: Chapter }) {
  const loader = notes[`./notes/${chapter.id}.md`];
  const [loaded, setLoaded] = useState<{ id: string; sections: Section[] } | null>(null);
  const sections = loaded?.id === chapter.id ? loaded.sections : null;

  useEffect(() => {
    let cancelled = false;
    if (loader) {
      loader().then((raw) => {
        if (!cancelled) setLoaded({ id: chapter.id, sections: splitSections(raw) });
      });
    }
    return () => {
      cancelled = true;
    };
  }, [loader, chapter.id]);

  if (!loader) {
    return <p className="status-text">이 챕터의 정리는 아직 준비 중입니다.</p>;
  }
  if (!sections) {
    return <p className="status-text">불러오는 중...</p>;
  }

  return (
    <div className="notes-view">
      {sections.map((section, index) =>
        section.title === null ? (
          <div className="notes-intro markdown" key={index}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{section.body}</ReactMarkdown>
          </div>
        ) : (
          // 첫 항목만 펼쳐 두고 나머지는 접어 둔다 — 눌러서 펼쳐 보며 공부하는 용도.
          <details className="notes-section" key={index} open={index === 0 || (index === 1 && sections[0].title === null)}>
            <summary>{renderTitle(section.title)}</summary>
            <div className="markdown">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{section.body}</ReactMarkdown>
            </div>
          </details>
        ),
      )}
    </div>
  );
}
