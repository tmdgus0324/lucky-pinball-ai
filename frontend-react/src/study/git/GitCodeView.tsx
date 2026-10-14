import { useEffect, useState } from 'react';
import { HighlightedCode } from '../react/HighlightedCode';
import type { GitChapter } from './chapters';

// 명령 모음(commands.sh)과 실행 결과(output.txt)를 그대로 읽어 온다(?raw).
// 명령은 임시 폴더의 새 저장소에서 실제로 실행했고, output.txt는 그때의 출력이다.
const sources = import.meta.glob('./code/*/*', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

const OUTPUT_FILE = 'output.txt';

const LABELS: Record<string, string> = {
  'commands.sh': '명령',
  [OUTPUT_FILE]: '▶ 실행 결과',
};

export function GitCodeView({ chapter }: { chapter: GitChapter }) {
  const files = chapter.files.filter((file) => sources[`./code/${chapter.id}/${file}`]);
  const [selected, setSelected] = useState(files[0]);
  const [loaded, setLoaded] = useState<{ file: string; text: string } | null>(null);
  const code = loaded?.file === selected ? loaded.text : null;

  useEffect(() => {
    let cancelled = false;
    const loader = selected ? sources[`./code/${chapter.id}/${selected}`] : undefined;
    loader?.().then((text) => {
      if (!cancelled) setLoaded({ file: selected, text });
    });
    return () => {
      cancelled = true;
    };
  }, [chapter.id, selected]);

  return (
    <div className="code-view">
      <div className="file-tabs" role="tablist">
        {files.map((file) => (
          <button
            type="button"
            role="tab"
            key={file}
            aria-selected={file === selected}
            className={file === selected ? 'file-tab active' : 'file-tab'}
            onClick={() => setSelected(file)}
          >
            {LABELS[file] ?? file}
          </button>
        ))}
      </div>
      <HighlightedCode
        code={code ?? '불러오는 중...'}
        language={code === null ? undefined : 'bash'}
        lineNumbers={code !== null && selected !== OUTPUT_FILE}
      />
    </div>
  );
}
