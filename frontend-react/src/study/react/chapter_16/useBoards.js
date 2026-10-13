import { useEffect, useState } from 'react';
import { boardApi } from './boardApi';

// 목록 조회에 필요한 state와 조회 함수를 묶은 Custom Hook(7장).
// BoardMain.jsx 위쪽의 목록 관련 코드(list, totalCount, currentPage, fnSearch)를 그대로 옮겨 왔다.
// 화면(JSX)이 없어서, 같은 목록 조회가 필요한 다른 화면에서도 다시 쓸 수 있다.
export function useBoards(pageSize) {
  const [list, setList] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [keyword, setKeyword] = useState('');   // 마지막으로 검색한 단어(페이지를 옮겨도 유지)
  const [loading, setLoading] = useState(true);   // 열리자마자 조회하므로 true로 시작(빈 목록이 잠깐 "결과 없음"으로 보이지 않게)

  async function search(page, word = keyword) {
    setLoading(true);
    const res = await boardApi.list({ keyword: word, page, size: pageSize });
    setList(res.items);
    setTotalCount(res.totalCount);
    setCurrentPage(page);
    setKeyword(word);
    setLoading(false);
  }

  // 화면이 처음 열리면 1페이지를 조회한다($(document).ready)
  useEffect(() => {
    search(1, '');
  }, []);

  return {
    list,
    keyword,
    currentPage,
    loading,
    totalPage: Math.ceil(totalCount / pageSize),
    search,
  };
}
