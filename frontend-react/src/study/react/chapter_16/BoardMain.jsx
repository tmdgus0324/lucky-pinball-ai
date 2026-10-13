import { useEffect, useState } from 'react';
import { boardApi } from './boardApi';

// 한 파일 버전. main.jsp와 같은 배치로 썼다.
// 위쪽(state와 함수)은 main.jsp의 <script>, 아래쪽 return(JSX)은 <body>에 해당한다.

const PAGE_SIZE = 5;

export default function BoardMain() {
  // ===== 전역 변수 → state =====
  // main.jsp는 변수에 값을 넣은 뒤 $('#tbody').html()로 화면을 직접 고쳤다.
  // React는 state를 바꾸기만 하면 아래 화면이 그 값으로 다시 그려진다.
  const [list, setList] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [keyword, setKeyword] = useState('');                        // <input id="keyword">의 값
  const [area, setArea] = useState('list');                          // 보이는 영역: list | detail | write (show/hide 대신)
  const [detail, setDetail] = useState(null);                       // 선택한 글(<input type="hidden" id="seq"> 대신)
  const [form, setForm] = useState({ title: '', writer: '', content: '' }); // 글쓰기 입력값
  const [loading, setLoading] = useState(true);                      // 열리자마자 조회하므로 true로 시작(빈 목록이 잠깐 "결과 없음"으로 보이지 않게)

  // ===== 화면이 처음 열리면 → useEffect =====
  // $(document).ready()처럼 처음 한 번 실행된다.
  // main.jsp는 첫 목록을 서버가 <c:forEach>로 그려 보냈지만, React는 화면이 뜬 뒤 여기서 받아 온다.
  useEffect(() => {
    fnSearch(1);
  }, []);

  // ===== 목록 조회(검색, 페이지 이동) =====
  async function fnSearch(page) {
    setLoading(true);
    const res = await boardApi.list({ keyword, page, size: PAGE_SIZE });
    setList(res.items);             // $('#tbody').html(html) 대신 값만 넣는다
    setTotalCount(res.totalCount);  // 페이징 버튼도 이 값으로 다시 그려진다(fn_paging이 따로 필요 없음)
    setCurrentPage(page);
    setLoading(false);
  }

  // ===== 상세 =====
  async function fnDetail(seq) {
    setDetail(await boardApi.get(seq));
    setArea('detail');              // $('#listArea').hide(); $('#detailArea').show();
  }

  // ===== 영역 전환 =====
  function fnGoWrite() {
    setForm({ title: '', writer: '', content: '' });   // $('#writeForm')[0].reset();
    setArea('write');
  }

  function fnGoList() {
    setArea('list');
  }

  // ===== 저장 =====
  async function fnSave() {
    if (form.title.trim() === '') {
      alert('제목을 입력하세요.');
      return;
    }
    await boardApi.create(form);    // $('#writeForm').serialize() 대신 form state를 그대로 보낸다
    alert('저장되었습니다.');
    fnGoList();
    fnSearch(1);
  }

  // ===== 삭제 =====
  async function fnDelete() {
    if (!confirm('삭제하시겠습니까?')) return;
    await boardApi.remove(detail.seq);
    alert('삭제되었습니다.');
    fnGoList();
    fnSearch(currentPage);
  }

  const totalPage = Math.ceil(totalCount / PAGE_SIZE);

  // ===== 화면(main.jsp의 <body>) =====
  return (
    <div>
      <h2>게시판</h2>

      {/* 목록 영역 */}
      {area === 'list' && (
        <div>
          {/* 엔터를 누르거나 검색 버튼을 누르면 form의 onSubmit이 실행된다(keyup에서 keyCode 13을 보던 코드 대신) */}
          <form onSubmit={(e) => { e.preventDefault(); fnSearch(1); }}>
            <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="제목·작성자" />
            <button type="submit">검색</button>
            <button type="button" onClick={fnGoWrite}>글쓰기</button>
          </form>
          <table border="1" style={{ borderCollapse: 'collapse', width: '100%', marginTop: 8 }}>
            <thead>
              <tr><th>번호</th><th>제목</th><th>작성자</th><th>작성일</th></tr>
            </thead>
            <tbody>
              {loading && <tr><td colSpan={4}>불러오는 중...</td></tr>}
              {!loading && list.length === 0 && <tr><td colSpan={4}>검색 결과가 없습니다.</td></tr>}
              {/* <c:forEach items="${list}" var="b"> 대신 map. key는 행을 구분하는 고유한 값(10장) */}
              {!loading && list.map((b) => (
                <tr key={b.seq} onClick={() => fnDetail(b.seq)} style={{ cursor: 'pointer' }}>
                  <td>{b.seq}</td><td>{b.title}</td><td>{b.writer}</td><td>{b.regDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 8 }}>
            {Array.from({ length: totalPage }, (_, i) => i + 1).map((p) => (
              <button key={p} type="button" disabled={p === currentPage} onClick={() => fnSearch(p)}>{p}</button>
            ))}
          </div>
        </div>
      )}

      {/* 상세 영역: 조건이 맞을 때만 그려진다(display:none으로 숨겨 두는 것이 아님) */}
      {area === 'detail' && detail && (
        <div>
          <h3>{detail.title}</h3>
          <p>{detail.writer} · {detail.regDate}</p>
          <p>{detail.content}</p>
          <button type="button" onClick={fnGoList}>목록</button>
          <button type="button" onClick={fnDelete}>삭제</button>
        </div>
      )}

      {/* 글쓰기 영역: 입력값은 state에 있고, 입력할 때마다 onChange로 state를 고친다(11장 제어 컴포넌트) */}
      {area === 'write' && (
        <div>
          <p>제목 <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></p>
          <p>작성자 <input value={form.writer} onChange={(e) => setForm({ ...form, writer: e.target.value })} /></p>
          <p>내용 <textarea value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} /></p>
          <button type="button" onClick={fnSave}>저장</button>
          <button type="button" onClick={fnGoList}>취소</button>
        </div>
      )}
    </div>
  );
}
