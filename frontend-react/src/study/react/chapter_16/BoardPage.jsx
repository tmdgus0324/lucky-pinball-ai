import { useState } from 'react';
import { boardApi } from './boardApi';
import { useBoards } from './useBoards';
import SearchBar from './SearchBar';
import BoardTable from './BoardTable';
import Pagination from './Pagination';
import BoardDetail from './BoardDetail';
import BoardForm from './BoardForm';

// 나눈 버전의 "main". 화면 조각(컴포넌트)을 조립하고, 어느 영역을 보여 줄지 정한다.
// main.jsp 한 파일이 하던 일을 여러 파일이 나눠 맡고, 이 파일은 배치와 흐름만 가진다.
//
// 데이터 흐름(12장 State 끌어올리기):
//   - 여러 조각이 함께 쓰는 값(목록, 현재 페이지, 보이는 영역)은 여기서 가진다.
//   - 조각에게는 props로 값을 내려 주고(list={...}),
//   - 조각에서 일어난 일은 함수로 돌려받는다(onSelect={openDetail}).

const PAGE_SIZE = 5;

export default function BoardPage() {
  const boards = useBoards(PAGE_SIZE);
  const [area, setArea] = useState('list');   // list | detail | write
  const [detail, setDetail] = useState(null);

  async function openDetail(seq) {
    setDetail(await boardApi.get(seq));
    setArea('detail');
  }

  async function save(form) {
    await boardApi.create(form);
    alert('저장되었습니다.');
    setArea('list');
    boards.search(1);
  }

  async function remove(seq) {
    if (!confirm('삭제하시겠습니까?')) return;
    await boardApi.remove(seq);
    alert('삭제되었습니다.');
    setArea('list');
    boards.search(boards.currentPage);
  }

  return (
    <div>
      <h2>게시판</h2>

      {area === 'list' && (
        <>
          <SearchBar initialKeyword={boards.keyword} onSearch={(word) => boards.search(1, word)} onWrite={() => setArea('write')} />
          <BoardTable list={boards.list} loading={boards.loading} onSelect={openDetail} />
          <Pagination page={boards.currentPage} totalPage={boards.totalPage} onChange={(page) => boards.search(page)} />
        </>
      )}

      {area === 'detail' && detail && (
        <BoardDetail board={detail} onBack={() => setArea('list')} onDelete={() => remove(detail.seq)} />
      )}

      {area === 'write' && <BoardForm onSave={save} onCancel={() => setArea('list')} />}
    </div>
  );
}
