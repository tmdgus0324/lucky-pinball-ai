// 서버 대신 쓰는 가짜 API.
// 실제 시스템이라면 이 자리에서 fetch('/api/boards?...')로 Spring 서버(@RestController)를 부르고 JSON을 받는다.
// 응답을 0.3초 늦게 줘서 "불러오는 중" 상태도 보이게 했다.
const DELAY_MS = 300;

const TITLES = [
  '10월 정기 점검 안내', '주차 등록은 어디서 하나요?', '조식 운영 시간 변경', '객실 예약 취소 문의',
  '수영장 이용 수칙', '분실물 보관 안내', '단체 예약 할인 문의', '체크인 시간 문의',
  '반려동물 동반 가능 여부', '셔틀버스 시간표', '영수증 재발급 요청', '11월 이벤트 안내',
  '와이파이 접속 방법', '연박 할인 문의', '객실 청소 요청 방법', '레스토랑 예약 문의',
  '결제 오류 문의', '회원 등급 안내', '얼리 체크인 가능 여부', '키즈클럽 운영 안내',
  '시설 공사 안내', '포인트 적립 문의', '고객센터 운영 시간',
];
const WRITERS = ['관리자', '김민수', '박지영', '이수진', '최현우'];

let boards = TITLES.map((title, i) => ({
  seq: i + 1,
  title,
  writer: WRITERS[i % WRITERS.length],
  content: `${title}에 대한 본문입니다.`,
  regDate: `2026-09-${String(i + 1).padStart(2, '0')}`,
}));
let nextSeq = boards.length + 1;

// 서버가 JSON으로 보내 주는 것처럼, 화면이 받은 객체를 고쳐도 원본 데이터가 바뀌지 않게 복사해서 준다.
function respond(value) {
  return new Promise((resolve) => setTimeout(() => resolve(JSON.parse(JSON.stringify(value))), DELAY_MS));
}

export const boardApi = {
  // GET /api/boards?keyword=&page=&size=  목록은 본문(content) 없이 보낸다
  list({ keyword = '', page = 1, size = 5 }) {
    const matched = boards
      .filter((b) => b.title.includes(keyword) || b.writer.includes(keyword))
      .sort((a, b) => b.seq - a.seq);
    const start = (page - 1) * size;
    const items = matched.slice(start, start + size).map(({ seq, title, writer, regDate }) => ({ seq, title, writer, regDate }));
    return respond({ items, totalCount: matched.length });
  },

  // GET /api/boards/{seq}
  get(seq) {
    return respond(boards.find((b) => b.seq === seq) ?? null);
  },

  // POST /api/boards
  create({ title, writer, content }) {
    const board = { seq: nextSeq++, title, writer, content, regDate: new Date().toISOString().slice(0, 10) };
    boards = [...boards, board];
    return respond(board);
  },

  // DELETE /api/boards/{seq}
  remove(seq) {
    boards = boards.filter((b) => b.seq !== seq);
    return respond({ deleted: seq });
  },
};
