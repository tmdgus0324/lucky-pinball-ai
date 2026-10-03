import { useEffect, useState } from 'react';
import type { Chapter } from './chapters';

// 화면에 보여주는 코드는 실제로 실행되는 파일을 그대로 읽어 온다(?raw) — 보여주는 코드와 동작하는 코드가 어긋날 수 없다.
const sources = import.meta.glob('./chapter_*/*.jsx', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

function filesOf(chapter: Chapter): string[] {
  return Object.keys(sources)
    .filter((path) => path.startsWith(`./${chapter.folder}/`))
    .sort();
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
      <pre className="code-block">
        <code>{code ?? '불러오는 중...'}</code>
      </pre>
    </div>
  );
}
