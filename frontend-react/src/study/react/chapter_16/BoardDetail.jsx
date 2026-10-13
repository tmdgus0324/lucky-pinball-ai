// 상세 화면. 보여 줄 글(board)을 받아 그리고, 버튼은 부모에게 알리기만 한다.
// main.jsp의 $('#detailTitle').text(b.title) 같은 코드가 {board.title} 한 줄로 바뀐다.
export default function BoardDetail({ board, onBack, onDelete }) {
  return (
    <div>
      <h3>{board.title}</h3>
      <p>{board.writer} · {board.regDate}</p>
      <p>{board.content}</p>
      <button type="button" onClick={onBack}>목록</button>
      <button type="button" onClick={onDelete}>삭제</button>
    </div>
  );
}
