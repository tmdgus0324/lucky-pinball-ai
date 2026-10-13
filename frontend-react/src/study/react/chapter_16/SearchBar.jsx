import { useState } from 'react';

// 검색창과 글쓰기 버튼. 입력 중인 글자는 이 조각만 알면 되므로 여기서 state로 가진다.
// 검색 버튼을 누르면 부모(BoardPage)에게 단어만 넘긴다(onSearch).
export default function SearchBar({ initialKeyword, onSearch, onWrite }) {
  const [keyword, setKeyword] = useState(initialKeyword);

  return (
    <form onSubmit={(e) => { e.preventDefault(); onSearch(keyword); }}>
      <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="제목·작성자" />
      <button type="submit">검색</button>
      <button type="button" onClick={onWrite}>글쓰기</button>
    </form>
  );
}
