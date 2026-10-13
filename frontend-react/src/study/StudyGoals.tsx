/**
 * 챕터 제목 아래, 탭 위에 보여 주는 학습 목표와 학습 결과.
 * 탭을 바꿔도 계속 보이게 탭 밖에 둔다. 공부하기 > React와 Spring Boot가 같이 쓴다.
 */
export function StudyGoals({ goal, outcome }: { goal: string; outcome: string }) {
  return (
    <dl className="study-goals">
      <div>
        <dt>학습 목표</dt>
        <dd>{goal}</dd>
      </div>
      <div>
        <dt>학습 결과</dt>
        <dd>{outcome}</dd>
      </div>
    </dl>
  );
}
