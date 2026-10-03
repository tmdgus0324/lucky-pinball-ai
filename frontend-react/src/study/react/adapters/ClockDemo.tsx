import { useEffect, useState } from 'react';
import Clock from '../chapter_04/Clock';

/**
 * 원본(index.js)은 setInterval로 root.render(<Clock />)를 1초마다 다시 호출했다.
 * 웹 데모에서는 루트를 다시 만들 수 없으니, 같은 효과(1초마다 Clock을 다시 렌더링)를 내는 래퍼를 둔다.
 * Clock 자체는 원본 그대로이고, 렌더링될 때마다 new Date()로 현재 시간을 계산한다.
 */
export default function ClockDemo() {
  const [, setTick] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setTick((n) => n + 1), 1000);
    return () => clearInterval(timer);
  }, []);

  return <Clock />;
}
