import { ballColorAt } from '../game/ballColors';

/** 참가자 순서(index)에 배정된 공 색을 보여주는 작은 점. 마우스를 올리면 색 이름이 보인다. */
export function BallDot({ index }: { index: number }) {
  const color = ballColorAt(index);
  return (
    <span
      className="ball-dot"
      style={{ background: color.hex }}
      title={`${color.name} 공`}
      role="img"
      aria-label={`${color.name} 공`}
    />
  );
}
