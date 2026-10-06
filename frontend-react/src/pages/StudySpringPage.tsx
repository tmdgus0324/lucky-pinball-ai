import { Link } from 'react-router-dom';
import { PARTS, SPRING_CHAPTERS, isReady, type SpringChapter } from '../study/spring/chapters';

function chapterNo(chapter: SpringChapter): number {
  return Number(chapter.id.replace('ch', ''));
}

function ChapterCard({ chapter }: { chapter: SpringChapter }) {
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
    <Link className="study-card" to={`/study/spring/${chapter.id}`}>
      {content}
    </Link>
  ) : (
    <div className="study-card disabled">{content}</div>
  );
}

export function StudySpringPage() {
  return (
    <>
      <header className="top">
        <h2>📚 공부하기 · Spring Boot</h2>
        <p>
          1~2년차 개발자가 3주 정도 공부할 분량의 Spring Boot 기초입니다. "객실 예약 API" 예제 하나를 1장부터 키워 가며,
          챕터마다 정리, 실제로 실행해 본 예제 코드, 이 사이트의 핀볼 백엔드에서 쓰인 모습을 함께 봅니다.
        </p>
      </header>

      {PARTS.map((part) => (
        <section key={part} className="study-part">
          <h3 className="study-part-title">{part}</h3>
          <div className="study-grid">
            {SPRING_CHAPTERS.filter((chapter) => chapter.part === part).map((chapter) => (
              <ChapterCard chapter={chapter} key={chapter.id} />
            ))}
          </div>
        </section>
      ))}

      <p className="study-source">
        Spring Boot 4.1 공식 문서를 참고해 Claude가 작성했고, 예제는 직접 빌드·실행해 확인했습니다. 챕터마다 참고 자료 링크가 있습니다.
      </p>
    </>
  );
}
