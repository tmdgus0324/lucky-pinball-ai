// 페이지 번호 버튼. JSP라면 <jsp:include page="paging.jsp"/>로 붙이던 공통 조각에 해당한다.
// 다른 목록 화면에서도 page, totalPage, onChange만 넘기면 그대로 쓸 수 있다.
export default function Pagination({ page, totalPage, onChange }) {
  return (
    <div style={{ marginTop: 8 }}>
      {Array.from({ length: totalPage }, (_, i) => i + 1).map((p) => (
        <button key={p} type="button" disabled={p === page} onClick={() => onChange(p)}>{p}</button>
      ))}
    </div>
  );
}
