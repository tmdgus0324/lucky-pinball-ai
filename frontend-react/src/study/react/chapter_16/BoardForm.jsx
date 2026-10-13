import { useState } from 'react';

// 글쓰기 폼. 입력값은 이 조각 안의 state에 있고(11장 제어 컴포넌트),
// 저장 버튼을 누르면 검사한 뒤 입력값 묶음만 부모에게 넘긴다(onSave).
export default function BoardForm({ onSave, onCancel }) {
  const [form, setForm] = useState({ title: '', writer: '', content: '' });

  function change(e) {
    setForm({ ...form, [e.target.name]: e.target.value });   // name 속성으로 어느 칸인지 구분한다
  }

  function submit() {
    if (form.title.trim() === '') {
      alert('제목을 입력하세요.');
      return;
    }
    onSave(form);
  }

  return (
    <div>
      <p>제목 <input name="title" value={form.title} onChange={change} /></p>
      <p>작성자 <input name="writer" value={form.writer} onChange={change} /></p>
      <p>내용 <textarea name="content" value={form.content} onChange={change} /></p>
      <button type="button" onClick={submit}>저장</button>
      <button type="button" onClick={onCancel}>취소</button>
    </div>
  );
}
