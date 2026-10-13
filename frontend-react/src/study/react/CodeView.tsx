import { useEffect, useState } from 'react';
import type { Chapter } from './chapters';
import { HighlightedCode } from './HighlightedCode';

// 화면에 보여주는 코드는 실제로 실행되는 파일을 그대로 읽어 온다(?raw) — 보여주는 코드와 동작하는 코드가 어긋날 수 없다.
// 16장은 비교용 main.jsp와 화면이 없는 .js 파일(가짜 API, 훅)도 함께 보여준다.
const sources = import.meta.glob('./chapter_*/*.{jsx,js,jsp}', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

/** 챕터에 파일 순서(files)가 있으면 그 순서로, 없으면 이름순으로 */
function filesOf(chapter: Chapter): string[] {
  if (chapter.files) {
    return chapter.files.map((name) => `./${chapter.folder}/${name}`).filter((path) => path in sources);
  }
  return Object.keys(sources)
    .filter((path) => path.startsWith(`./${chapter.folder}/`))
    .sort();
}

/** JSP는 HTML 하이라이트로, 나머지(.jsx, .js)는 JSX 하이라이트로 보여준다 */
function languageOf(path: string): string {
  return path.endsWith('.jsp') ? 'markup' : 'jsx';
}

export function CodeView({ chapter }: { chapter: Chapter }) {
  const files = filesOf(chapter);
  const [selected, setSelected] = useState(files[0]);
  const [loaded, setLoaded] = useState<{ path: string; text: string } | null>(null);
  const code = loaded?.path === selected ? loaded.text : null;

  useEffect(() => {
    let cancelled = false;
    if (selected) {
      sources[selected]().then((text) => {
        if (!cancelled) setLoaded({ path: selected, text });
      });
    }
    return () => {
      cancelled = true;
    };
  }, [selected]);

  return (
    <div className="code-view">
      <div className="file-tabs" role="tablist">
        {files.map((path) => (
          <button
            type="button"
            role="tab"
            key={path}
            aria-selected={path === selected}
            className={path === selected ? 'file-tab active' : 'file-tab'}
            onClick={() => setSelected(path)}
          >
            {path.split('/').pop()}
          </button>
        ))}
      </div>
      <HighlightedCode code={code ?? '불러오는 중...'} language={code === null ? undefined : languageOf(selected)} lineNumbers={code !== null} />
    </div>
  );
}
