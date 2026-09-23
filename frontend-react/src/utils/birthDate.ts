/**
 * "880324" 같은 6자리(YYMMDD) 문자열을 "1988-03-24" 형태로 변환한다.
 * 두 자리 연도(YY)의 세기 판단 기준: 50 이상이면 1900년대, 50 미만이면 2000년대
 * (주민등록번호 앞자리와 같은 흔한 관례). 형식이 맞지 않으면 null을 반환한다.
 */
export function parseBirthDateShorthand(raw: string): string | null {
  const digits = raw.replace(/\D/g, '');
  if (digits.length !== 6) return null;

  const yy = parseInt(digits.slice(0, 2), 10);
  const month = parseInt(digits.slice(2, 4), 10);
  const day = parseInt(digits.slice(4, 6), 10);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;

  const year = (yy >= 50 ? 1900 : 2000) + yy;
  const mm = String(month).padStart(2, '0');
  const dd = String(day).padStart(2, '0');
  return `${year}-${mm}-${dd}`;
}
