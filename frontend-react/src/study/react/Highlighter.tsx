import { Highlight, Prism, themes } from 'prism-react-renderer';
// prism-react-renderer에 기본으로 없는 언어(공부하기 > Spring Boot용). prismGlobal이 먼저 와야 한다.
import './prismGlobal';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-groovy';
import 'prismjs/components/prism-properties';
import 'prismjs/components/prism-bash';

interface HighlighterProps {
  code: string;
  language?: string;
  lineNumbers?: boolean;
}

/**
 * 실제로 색을 입히는 부분. prism-react-renderer가 커서(수십 kB) HighlightedCode가 lazy로만 불러온다 —
 * 하이라이터가 도착하기 전에는 색 없는 코드가 먼저 보이고, 도착하면 색이 입혀진다.
 * 등록되지 않은 언어(dockerfile 등)는 색 없이 그대로 보여준다.
 */
export default function Highlighter({ code, language, lineNumbers }: HighlighterProps) {
  const known = language && Prism.languages[language] ? language : 'plain';

  return (
    <Highlight theme={themes.vsDark} code={code} language={known}>
      {({ tokens, getLineProps, getTokenProps }) => (
        <pre className={lineNumbers ? 'code-block with-lines' : 'code-block'}>
          <code>
            {tokens.map((line, index) => (
              <div key={index} {...getLineProps({ line })} className="code-line">
                {lineNumbers && <span className="line-no">{index + 1}</span>}
                <span className="line-text">
                  {line.map((token, tokenIndex) => (
                    <span key={tokenIndex} {...getTokenProps({ token })} />
                  ))}
                  {line.length === 0 || (line.length === 1 && line[0].empty) ? '​' : null}
                </span>
              </div>
            ))}
          </code>
        </pre>
      )}
    </Highlight>
  );
}
