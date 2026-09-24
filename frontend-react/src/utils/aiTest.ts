/**
 * "AI TEST" 버튼 전용 — 실제 이름/생년월일을 손으로 입력하지 않고도 진짜 AI 호출·캐시
 * 재사용 흐름을 시연/테스트할 수 있도록 임의의 한글 3글자 이름과 생년월일을 만든다.
 * 빠른 추가(quickAddBuff.ts)와 달리 이건 실제 생년월일을 서버에 보내서 진짜 AI를
 * 호출시키는 용도다 — 관리자 화면의 "AI 호출" 컬럼으로 결과를 확인할 수 있다.
 */
const SURNAMES = ['김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '서', '신', '권'];
const GIVEN_SYLLABLES = ['민', '서', '준', '지', '현', '우', '수', '영', '은', '도', '하', '유', '성', '재', '원'];

export function randomKoreanName(): string {
  const surname = SURNAMES[Math.floor(Math.random() * SURNAMES.length)];
  const g1 = GIVEN_SYLLABLES[Math.floor(Math.random() * GIVEN_SYLLABLES.length)];
  const g2 = GIVEN_SYLLABLES[Math.floor(Math.random() * GIVEN_SYLLABLES.length)];
  return `${surname}${g1}${g2}`;
}

export function randomBirthDateIso(): string {
  const year = 1970 + Math.floor(Math.random() * 41); // 1970~2010
  const month = 1 + Math.floor(Math.random() * 12);
  const day = 1 + Math.floor(Math.random() * 28); // 28일까지만 써서 월별 일수 문제를 피함
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}
