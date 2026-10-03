import { Link } from 'react-router-dom';
import { CHAPTERS } from '../study/react/chapters';

export function StudyReactPage() {
  return (
    <>
      <header className="top">
        <h2>📚 공부하기 · React</h2>
        <p>
          예전에 React를 공부하며 정리한 자료입니다. 챕터를 누르면 예제를 바로 실행해 보고, 코드와 정리 내용을 함께 볼 수 있습니다.
        </p>
      </header>

      <div className="study-grid">
        {CHAPTERS.map((chapter) => (
          <Link className="study-card" to={`/study/react/${chapter.id}`} key={chapter.id}>
            <span className="study-card-no">{chapter.id.replace('ch', '')}장</span>
            <h3>{chapter.title}</h3>
            <p>{chapter.summary}</p>
            <div className="study-keywords">
              {chapter.keywords.map((keyword) => (
                <span className="badge" key={keyword}>
                  {keyword}
                </span>
              ))}
            </div>
          </Link>
        ))}
      </div>

      <p className="study-source">
        출처:{' '}
        <a href="https://github.com/tmdgus0324/React_basic" target="_blank" rel="noreferrer">
          github.com/tmdgus0324/React_basic
        </a>{' '}
        (예제 코드는 원본을 그대로 옮겼고, 웹에서 실행하기 위해 바꾼 부분은 각 챕터 아래에 적어 두었습니다)
      </p>
    </>
  );
}
