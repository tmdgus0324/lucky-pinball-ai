import ConfirmButton from '../chapter_08/ConfirmButton';
import ConfirmButton2 from '../chapter_08/ConfirmButton2';

/** 원본에서는 둘 중 하나를 골라 index.js에 연결했다. 비교하기 쉽게 두 방식을 나란히 보여준다. */
export default function ConfirmButtonsDemo() {
  return (
    <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
      <div>
        <p style={{ margin: '0 0 8px', fontSize: 13 }}>클래스 컴포넌트 (ConfirmButton)</p>
        <ConfirmButton />
      </div>
      <div>
        <p style={{ margin: '0 0 8px', fontSize: 13 }}>함수 컴포넌트 (ConfirmButton2)</p>
        <ConfirmButton2 />
      </div>
    </div>
  );
}
