import { useState } from 'react';
import BoardMain from './BoardMain';
import BoardPage from './BoardPage';

// 데모 화면. 같은 게시판을 두 가지 구성으로 바꿔 가며 볼 수 있다.
// 두 버전은 같은 가짜 서버(boardApi)를 쓰므로, 한쪽에서 쓴 글이 다른 쪽에도 보인다.
export default function BoardDemo() {
  const [version, setVersion] = useState('one');

  return (
    <div>
      <p>
        <label>
          <input type="radio" checked={version === 'one'} onChange={() => setVersion('one')} /> 한 파일 버전(BoardMain.jsx)
        </label>{' '}
        <label>
          <input type="radio" checked={version === 'split'} onChange={() => setVersion('split')} /> 나눈 버전(BoardPage.jsx)
        </label>
      </p>
      <hr />
      {version === 'one' ? <BoardMain /> : <BoardPage />}
    </div>
  );
}
