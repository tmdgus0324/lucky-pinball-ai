import { Link } from 'react-router-dom';
import { GIT_CHAPTERS, PARTS, isReady, type GitChapter } from '../study/git/chapters';

function chapterNo(chapter: GitChapter): number {
  return Number(chapter.id.replace('ch', ''));
}

function ChapterCard({ chapter }: { chapter: GitChapter }) {
  const content = (
    <>
      <span className="study-card-no">
        {chapterNo(chapter)}장{!isReady(chapter) && <span className="badge study-soon">준비 중</span>}
      </span>
      <h3>{chapter.title}</h3>
      <p>{chapter.summary}</p>
      <div className="study-keywords">
        {chapter.keywords.map((keyword) => (
          <span className="badge" key={keyword}>
            {keyword}
          </span>
        ))}
      </div>
    </>
  );

  return isReady(chapter) ? (
    <Link className="study-card" to={`/study/git/${chapter.id}`}>
      {content}
    </Link>
  ) : (
    <div className="study-card disabled">{content}</div>
  );
}

export function StudyGitPage() {
  return (
    <>
      <header className="top">
        <h2>📚 공부하기 · Git</h2>
        <p>
          SVN을 써 온 사람이 Git으로 시스템을 운영할 때 필요한 내용입니다. 기본 명령부터 브랜치·협업, 되돌리기와 장애 원인 커밋
          찾기까지, 챕터마다 정리, 실제로 실행해 본 명령과 결과, 이 사이트 저장소에서 쓰인 모습을 함께 봅니다.
        </p>
      </header>

      {PARTS.map((part) => (
        <section key={part} className="study-part">
          <h3 className="study-part-title">{part}</h3>
          <div className="study-grid">
            {GIT_CHAPTERS.filter((chapter) => chapter.part === part).map((chapter) => (
              <ChapterCard chapter={chapter} key={chapter.id} />
            ))}
          </div>
        </section>
      ))}

      <p className="study-source">
        Git 공식 문서(git-scm.com)를 참고해 Claude가 작성했고, 명령은 새 저장소에서 직접 실행해 확인했습니다. 챕터마다 참고 자료 링크가 있습니다.
      </p>
    </>
  );
}
