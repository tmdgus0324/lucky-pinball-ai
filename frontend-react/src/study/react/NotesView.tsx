import {
  Children,
  isValidElement,
  useEffect,
  useState,
  type ComponentProps,
  type ReactNode,
} from 'react';
import ReactMarkdown, { type ExtraProps } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { Chapter } from './chapters';
import { HighlightedCode } from './HighlightedCode';

// 정리 노트는 빌드에 포함한다(GitHub에서 실행 시점에 가져오지 않는다) — 네트워크와 무관하게 항상 같은 내용이 보이도록.
const notes = import.meta.glob('./notes/*.md', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

/** 노트 안에서 AI 코멘트 블록을 알아보는 표식: 인용(>) 블록의 첫 문단이 정확히 이 글자일 때 */
const AI_MARKER = 'AI 코멘트';
const AI_PREF_KEY = 'study.showAiComments';

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

// react-markdown이 넘겨주는 hast 노드 중 글자를 읽는 데 필요한 부분만 쓴다(별도 타입 패키지에 기대지 않으려고).
type HastNode = { type: string; value?: string; children?: HastNode[] };

function textOf(node: HastNode): string {
  if (node.type === 'text') return node.value ?? '';
  return (node.children ?? []).map(textOf).join('');
}

/**
 * 인용(>) 블록 중 첫 문단이 "AI 코멘트"인 것은 말풍선 모양의 AI 코멘트로 보여준다.
 * 나머지 인용은 평범한 인용 그대로 둔다. 사용자가 쓴 원문과 AI가 보탠 설명을 눈으로 구분하기 위한 장치다.
 */
function Blockquote({ node, children }: ComponentProps<'blockquote'> & ExtraProps) {
  const hast = node as unknown as HastNode | undefined;
  const firstParagraph = hast?.children?.find((child) => child.type === 'element');

  if (firstParagraph && textOf(firstParagraph).trim() === AI_MARKER) {
    const parts = Children.toArray(children);
    const markerIndex = parts.findIndex((part) => typeof part !== 'string'); // 줄바꿈 글자를 건너뛴 첫 요소 = 표식 문단
    return (
      <aside className="ai-comment">
        <div className="ai-comment-title">💬 AI 코멘트</div>
        {parts.filter((_, index) => index !== markerIndex)}
      </aside>
    );
  }
  return <blockquote>{children}</blockquote>;
}

/** 코드 블록(```)은 하이라이터로 보여준다. 하이라이터는 lazy라 도착 전에는 색 없는 코드가 먼저 보인다. */
function Pre({ children }: ComponentProps<'pre'>) {
  const child = Children.toArray(children)[0];
  if (isValidElement<{ className?: string; children?: ReactNode }>(child)) {
    const language = /language-([\w-]+)/.exec(child.props.className ?? '')?.[1];
    const text = String(child.props.children ?? '').replace(/\n$/, '');
    return <HighlightedCode code={text} language={language} />;
  }
  return <pre>{children}</pre>;
}

const markdownComponents = { blockquote: Blockquote, pre: Pre };

function readShowAiPreference(): boolean {
  try {
    return localStorage.getItem(AI_PREF_KEY) !== 'off';
  } catch {
    return true; // 저장소를 못 쓰는 환경(시크릿 모드 등)에서도 기본값(보임)으로 동작한다.
  }
}

// 한국어 글은 "5~6장"처럼 범위에 물결표를 자주 쓴다. GFM 기본값은 물결표 하나(~)도 취소선으로 읽어서
// 두 물결표 사이 글자에 줄이 그어진다. 취소선은 ~~두 개~~일 때만 인식하게 한다.
const remarkPlugins: ComponentProps<typeof ReactMarkdown>['remarkPlugins'] = [[remarkGfm, { singleTilde: false }]];

function Markdown({ children }: { children: string }) {
  return (
    <ReactMarkdown remarkPlugins={remarkPlugins} components={markdownComponents}>
      {children}
    </ReactMarkdown>
  );
}

export function NotesView({ chapter }: { chapter: Chapter }) {
  return <MarkdownNotes noteKey={chapter.id} loader={notes[`./notes/${chapter.id}.md`]} />;
}

/**
 * 마크다운 노트 하나를 `## ` 제목 단위로 접어서 보여준다. 공부하기 > Spring Boot도 같은 화면을 쓴다.
 * @param noteKey 노트가 바뀌었는지 알아보는 값(챕터 id 등)
 * @param loader 노트 원문을 읽어 오는 함수(import.meta.glob 결과). 없으면 "준비 중"을 보여준다.
 */
export function MarkdownNotes({ noteKey, loader }: { noteKey: string; loader: (() => Promise<string>) | undefined }) {
  const [loaded, setLoaded] = useState<{ id: string; sections: Section[] } | null>(null);
  const [showAi, setShowAi] = useState(readShowAiPreference);
  const sections = loaded?.id === noteKey ? loaded.sections : null;

  useEffect(() => {
    let cancelled = false;
    if (loader) {
      loader().then((raw) => {
        if (!cancelled) setLoaded({ id: noteKey, sections: splitSections(raw) });
      });
    }
    return () => {
      cancelled = true;
    };
  }, [loader, noteKey]);

  if (!loader) {
    return <p className="status-text">이 챕터의 정리는 아직 준비 중입니다.</p>;
  }
  if (!sections) {
    return <p className="status-text">불러오는 중...</p>;
  }

  const aiCount = sections.reduce((sum, section) => sum + (section.body.match(/^> \*\*AI 코멘트\*\*/gm)?.length ?? 0), 0);

  function toggleAi(next: boolean) {
    setShowAi(next);
    try {
      localStorage.setItem(AI_PREF_KEY, next ? 'on' : 'off');
    } catch {
      // 저장하지 못해도 이번 화면에서는 선택이 적용된다.
    }
  }

  return (
    <div className={showAi ? 'notes-view' : 'notes-view hide-ai'}>
      {aiCount > 0 && (
        <label className="ai-toggle">
          <input type="checkbox" checked={showAi} onChange={(event) => toggleAi(event.target.checked)} />
          💬 AI 코멘트 보기 <span className="ai-toggle-count">({aiCount}개)</span>
        </label>
      )}
      {sections.map((section, index) =>
        section.title === null ? (
          <div className="notes-intro markdown" key={index}>
            <Markdown>{section.body}</Markdown>
          </div>
        ) : (
          // 첫 항목만 펼쳐 두고 나머지는 접어 둔다 — 눌러서 펼쳐 보며 공부하는 용도.
          <details className="notes-section" key={index} open={index === 0 || (index === 1 && sections[0].title === null)}>
            <summary>{renderTitle(section.title)}</summary>
            <div className="markdown">
              <Markdown>{section.body}</Markdown>
            </div>
          </details>
        ),
      )}
    </div>
  );
}
