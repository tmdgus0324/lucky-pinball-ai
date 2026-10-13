// 목록 표. 받은 list를 그리기만 하고, 데이터를 직접 조회하지 않는다.
// 행을 누르면 어떤 글인지만 부모에게 알린다(onSelect). 무엇을 할지는 부모가 정한다.
export default function BoardTable({ list, loading, onSelect }) {
  return (
    <table border="1" style={{ borderCollapse: 'collapse', width: '100%', marginTop: 8 }}>
      <thead>
        <tr><th>번호</th><th>제목</th><th>작성자</th><th>작성일</th></tr>
      </thead>
      <tbody>
        {loading && <tr><td colSpan={4}>불러오는 중...</td></tr>}
        {!loading && list.length === 0 && <tr><td colSpan={4}>검색 결과가 없습니다.</td></tr>}
        {!loading && list.map((b) => (
          <tr key={b.seq} onClick={() => onSelect(b.seq)} style={{ cursor: 'pointer' }}>
            <td>{b.seq}</td><td>{b.title}</td><td>{b.writer}</td><td>{b.regDate}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
