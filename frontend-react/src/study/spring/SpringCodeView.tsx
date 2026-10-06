import { useEffect, useState } from 'react';
import { HighlightedCode } from '../react/HighlightedCode';
import type { SpringChapter } from './chapters';

// 예제 파일을 그대로 읽어 온다(?raw). 이 파일들을 임시 프로젝트에 복사해 빌드·실행해서 확인했다.
const sources = import.meta.glob('./code/*/*', { query: '?raw', import: 'default' }) as Record<
  string,
  () => Promise<string>
>;

const OUTPUT_FILE = 'output.txt';

const LANGUAGE_BY_EXTENSION: Record<string, string> = {
  java: 'java',
  gradle: 'groovy',
  yml: 'yaml',
  yaml: 'yaml',
  xml: 'markup',
  html: 'markup',
  jsp: 'markup',
  properties: 'properties',
  sql: 'sql',
  json: 'json',
};

function languageOf(file: string): string | undefined {
  if (file === OUTPUT_FILE) return 'bash';
  return LANGUAGE_BY_EXTENSION[file.split('.').pop() ?? ''];
}

export function SpringCodeView({ chapter }: { chapter: SpringChapter }) {
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
            {file === OUTPUT_FILE ? '▶ 실행 결과' : file}
          </button>
        ))}
      </div>
      <HighlightedCode
        code={code ?? '불러오는 중...'}
        language={code === null ? undefined : languageOf(selected)}
        lineNumbers={code !== null && selected !== OUTPUT_FILE}
      />
    </div>
  );
}
