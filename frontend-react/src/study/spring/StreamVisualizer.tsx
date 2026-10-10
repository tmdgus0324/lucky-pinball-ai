import { useEffect, useRef, useState } from 'react';
import { HighlightedCode } from '../react/HighlightedCode';

/**
 * 18장 "Stream 시각화" 탭. 브라우저에서는 Java를 실행할 수 없어서, Stream이 요소를 처리하는 순서를
 * 같은 데이터로 한 단계씩 보여 준다. 순서는 code/ch18/StreamOrder.java를 실제로 실행한 결과와 맞췄다
 * (화면에만 있는 단계는 extra로 표시하고 기록에서 흐리게 보여 준다).
 */

type Status = 'CONFIRMED' | 'CANCELED';

interface Reservation {
  id: number;
  guestName: string;
  room: string;
  nights: number;
  status: Status;
}

// StreamBasics.java, StreamOrder.java와 같은 예약 6건
const RESERVATIONS: Reservation[] = [
  { id: 1, guestName: '김민수', room: '오션뷰', nights: 2, status: 'CONFIRMED' },
  { id: 2, guestName: '박지영', room: '스탠다드', nights: 1, status: 'CANCELED' },
  { id: 3, guestName: '이수진', room: '오션뷰', nights: 3, status: 'CONFIRMED' },
  { id: 4, guestName: '최현우', room: '스위트', nights: 2, status: 'CONFIRMED' },
  { id: 5, guestName: '정다은', room: '오션뷰', nights: 4, status: 'CANCELED' },
  { id: 6, guestName: '한지민', room: '스탠다드', nights: 1, status: 'CONFIRMED' },
];

type ItemState = 'waiting' | 'dropped' | 'held' | 'passed' | 'skipped';

const STATE_LABELS: Record<ItemState, string> = {
  waiting: '대기',
  dropped: '걸러짐',
  held: '모아 둠',
  passed: '결과로',
  skipped: '안 꺼냄',
};

interface Step {
  /** 이 단계에서 다루는 예약 번호. 정렬·멈춤처럼 특정 요소가 없는 단계는 null */
  itemId: number | null;
  /** 지금 일하는 연산의 위치(stages의 인덱스) */
  stage: number;
  /** 기록에 남는 한 줄. StreamOrder.java가 찍는 줄과 같은 문구다 */
  text: string;
  /** 이 단계에서 상태가 바뀌는 요소 */
  states?: Record<number, ItemState>;
  /** 결과 리스트에 넣을 값 */
  add?: string;
  /** groupingBy에서 1 늘릴 키 */
  count?: string;
  /** Java 실행에서는 찍히지 않고 화면에서만 보여 주는 단계 */
  extra?: boolean;
}

interface Preset {
  id: string;
  label: string;
  code: string;
  stages: string[];
  steps: Step[];
  /** 결과가 Map이면 true */
  isMap?: boolean;
  point: string;
}

const label = (r: Reservation) => `${r.id}번 ${r.guestName}`;

function filterMapPreset(): Preset {
  const steps: Step[] = [];
  const result: string[] = [];
  for (const r of RESERVATIONS) {
    steps.push({ itemId: r.id, stage: 0, text: `${label(r)} 꺼냄` });
    const ok = r.status === 'CONFIRMED';
    steps.push({ itemId: r.id, stage: 1, text: `  filter 확정? ${ok ? '통과' : '걸러짐'}`, states: ok ? undefined : { [r.id]: 'dropped' } });
    if (!ok) continue;
    steps.push({ itemId: r.id, stage: 2, text: `  map → ${r.guestName}` });
    steps.push({ itemId: r.id, stage: 3, text: '  toList에 담음', add: r.guestName, states: { [r.id]: 'passed' }, extra: true });
    result.push(r.guestName);
  }
  steps.push({ itemId: null, stage: 3, text: `결과 [${result.join(', ')}]` });
  return {
    id: 'filter-map',
    label: 'filter → map',
    code: `List<String> names = reservations.stream()
        .filter(r -> r.status() == Status.CONFIRMED)
        .map(Reservation::guestName)
        .toList();`,
    stages: ['stream()', 'filter(확정?)', 'map(이름)', 'toList()'],
    steps,
    point: 'filter를 6번 다 한 뒤에 map을 하는 것이 아닙니다. 요소 하나가 끝까지 지나간 뒤 다음 요소를 꺼냅니다.',
  };
}

function limitPreset(): Preset {
  const steps: Step[] = [];
  const result: string[] = [];
  for (const r of RESERVATIONS) {
    steps.push({ itemId: r.id, stage: 0, text: `${label(r)} 꺼냄` });
    const ok = r.nights >= 2;
    steps.push({ itemId: r.id, stage: 1, text: `  filter 2박 이상? ${ok ? '통과' : '걸러짐'}`, states: ok ? undefined : { [r.id]: 'dropped' } });
    if (!ok) continue;
    steps.push({ itemId: r.id, stage: 2, text: `  map → ${r.guestName}` });
    result.push(r.guestName);
    steps.push({ itemId: r.id, stage: 3, text: `  limit(2) ${result.length}번째`, extra: true });
    steps.push({ itemId: r.id, stage: 4, text: '  toList에 담음', add: r.guestName, states: { [r.id]: 'passed' }, extra: true });
    if (result.length === 2) {
      const rest = RESERVATIONS.filter((x) => x.id > r.id);
      steps.push({
        itemId: null,
        stage: 3,
        text: `limit(2)가 다 차서 멈춤. ${rest.map((x) => x.id).join(', ')}번은 꺼내지 않음`,
        states: Object.fromEntries(rest.map((x) => [x.id, 'skipped' as ItemState])),
        extra: true,
      });
      break;
    }
  }
  steps.push({ itemId: null, stage: 4, text: `결과 [${result.join(', ')}]` });
  return {
    id: 'limit',
    label: 'limit(2)',
    code: `List<String> names = reservations.stream()
        .filter(r -> r.nights() >= 2)
        .map(Reservation::guestName)
        .limit(2)
        .toList();`,
    stages: ['stream()', 'filter(2박 이상?)', 'map(이름)', 'limit(2)', 'toList()'],
    steps,
    point: '필요한 개수가 차면 바로 멈춥니다. 뒤쪽 요소는 꺼내지도 않습니다.',
  };
}

function sortedPreset(): Preset {
  const steps: Step[] = [];
  for (const r of RESERVATIONS) {
    steps.push({ itemId: r.id, stage: 1, text: `${label(r)} 꺼냄 → sorted가 모아 둠`, states: { [r.id]: 'held' } });
  }
  // Java의 정렬처럼 숙박이 같으면 원래 순서를 지킨다(Array.prototype.sort도 안정 정렬)
  const sorted = [...RESERVATIONS].sort((a, b) => a.nights - b.nights);
  steps.push({ itemId: null, stage: 1, text: `  sorted: 숙박 순으로 정렬 → ${sorted.map((r) => r.guestName).join(', ')}`, extra: true });
  for (const r of sorted) {
    steps.push({ itemId: r.id, stage: 2, text: `  map → ${r.guestName}(${r.nights}박)` });
    steps.push({ itemId: r.id, stage: 3, text: '  toList에 담음', add: r.guestName, states: { [r.id]: 'passed' }, extra: true });
  }
  steps.push({ itemId: null, stage: 3, text: `결과 [${sorted.map((r) => r.guestName).join(', ')}]` });
  return {
    id: 'sorted',
    label: 'sorted',
    code: `List<String> names = reservations.stream()
        .sorted(Comparator.comparing(Reservation::nights))
        .map(Reservation::guestName)
        .toList();`,
    stages: ['stream()', 'sorted(숙박)', 'map(이름)', 'toList()'],
    steps,
    point: 'sorted는 전부 받아야 순서를 정할 수 있어서, 6개를 모두 모은 뒤에야 다음 단계로 보냅니다.',
  };
}

function groupingPreset(): Preset {
  const steps: Step[] = [];
  const counts = new Map<string, number>();
  for (const r of RESERVATIONS) {
    steps.push({ itemId: r.id, stage: 1, text: `${label(r)} 꺼냄 → ${r.room} 칸 +1`, count: r.room, states: { [r.id]: 'passed' } });
    counts.set(r.room, (counts.get(r.room) ?? 0) + 1);
  }
  const text = [...counts].map(([room, n]) => `${room}=${n}`).join(', ');
  steps.push({ itemId: null, stage: 1, text: `결과 {${text}}` });
  return {
    id: 'grouping',
    label: 'groupingBy',
    code: `Map<String, Long> countByRoom = reservations.stream()
        .collect(Collectors.groupingBy(Reservation::room, Collectors.counting()));`,
    stages: ['stream()', 'groupingBy(객실, counting)'],
    steps,
    isMap: true,
    point: '요소마다 키(객실)를 보고 그 칸의 개수를 1씩 늘립니다. SQL의 GROUP BY와 COUNT를 떠올리면 됩니다.',
  };
}

const PRESETS: Preset[] = [filterMapPreset(), limitPreset(), sortedPreset(), groupingPreset()];

/** 처음부터 index번째 단계까지 적용한 화면 상태 */
function replay(steps: Step[], index: number) {
  const states: Record<number, ItemState> = {};
  const list: string[] = [];
  const counts = new Map<string, number>();
  for (let i = 0; i <= index; i++) {
    const step = steps[i];
    Object.assign(states, step.states);
    if (step.add !== undefined) list.push(step.add);
    if (step.count !== undefined) counts.set(step.count, (counts.get(step.count) ?? 0) + 1);
  }
  return { states, list, counts };
}

const AUTO_PLAY_MS = 700;

export function StreamVisualizer() {
  const [presetId, setPresetId] = useState(PRESETS[0].id);
  // -1은 아직 아무것도 하지 않은 상태
  const [index, setIndex] = useState(-1);
  const [playing, setPlaying] = useState(false);
  const logRef = useRef<HTMLOListElement>(null);

  const preset = PRESETS.find((p) => p.id === presetId) ?? PRESETS[0];
  const last = preset.steps.length - 1;
  const current = index >= 0 ? preset.steps[index] : null;
  const { states, list, counts } = replay(preset.steps, index);
  // 마지막 단계에 닿으면 자동 재생은 저절로 멈춘다
  const autoPlaying = playing && index < last;

  useEffect(() => {
    if (!autoPlaying) return;
    const timer = setTimeout(() => setIndex((i) => i + 1), AUTO_PLAY_MS);
    return () => clearTimeout(timer);
  }, [autoPlaying, index]);

  // 기록은 최신 줄이 보이게 아래로 내린다
  useEffect(() => {
    const el = logRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [index, presetId]);

  function choose(id: string) {
    setPresetId(id);
    setIndex(-1);
    setPlaying(false);
  }

  return (
    <div className="stream-viz">
      <p className="desc">
        예약 6건이 Stream을 어떤 순서로 지나가는지 한 단계씩 봅니다. 순서는 코드 탭의 StreamOrder.java를 실제로 실행한 결과와
        같습니다.
      </p>

      <div className="stream-presets" role="group" aria-label="예제 고르기">
        {PRESETS.map((p) => (
          <button
            type="button"
            key={p.id}
            className={p.id === preset.id ? 'file-tab active' : 'file-tab'}
            aria-pressed={p.id === preset.id}
            onClick={() => choose(p.id)}
          >
            {p.label}
          </button>
        ))}
      </div>

      <HighlightedCode code={preset.code} language="java" />

      <ol className="stream-pipeline" aria-label="연산 순서">
        {preset.stages.map((stage, i) => (
          <li key={stage} className={current && current.stage === i ? 'on' : undefined}>
            {stage}
          </li>
        ))}
      </ol>

      <p className="stream-now" aria-live="polite">
        {current ? current.text.trim() : '[다음]을 누르면 첫 요소를 꺼냅니다.'}
      </p>

      <div className="stream-controls">
        <button type="button" className="secondary" disabled={index < 0} onClick={() => { setPlaying(false); setIndex(-1); }}>
          처음
        </button>
        <button type="button" className="secondary" disabled={index < 0} onClick={() => { setPlaying(false); setIndex((i) => i - 1); }}>
          이전
        </button>
        <button type="button" disabled={index >= last} onClick={() => { setPlaying(false); setIndex((i) => i + 1); }}>
          다음
        </button>
        <button type="button" className="secondary" disabled={index >= last} onClick={() => setPlaying(!autoPlaying)}>
          {autoPlaying ? '멈춤' : '자동 재생'}
        </button>
        <span className="stream-counter">
          {index + 1} / {preset.steps.length}
        </span>
      </div>

      <div className="stream-board">
        <div>
          <h4>원본 리스트</h4>
          <ul className="stream-items">
            {RESERVATIONS.map((r) => {
              const state = states[r.id] ?? 'waiting';
              const isCurrent = current?.itemId === r.id;
              // 지금 다루는 요소는 "처리 중". 단, 이번 단계에서 막 상태가 정해졌으면(걸러짐, 모아 둠, 결과로) 그것을 보여 준다
              const working = isCurrent && current?.states?.[r.id] === undefined;
              return (
                <li key={r.id} className={`stream-item ${state}${isCurrent ? ' current' : ''}`}>
                  <span className="stream-item-name">{label(r)}</span>
                  <span className="stream-item-info">
                    {r.room} · {r.nights}박 · {r.status === 'CONFIRMED' ? '확정' : '취소'}
                  </span>
                  <span className="stream-item-state">{working ? '처리 중' : STATE_LABELS[state]}</span>
                </li>
              );
            })}
          </ul>
        </div>
        <div>
          <h4>{preset.isMap ? '결과 Map' : '결과 List'}</h4>
          {preset.isMap ? (
            <ul className="stream-map">
              {[...counts].map(([room, n]) => (
                <li key={room}>
                  <span>{room}</span>
                  <span className="stream-bar" style={{ width: `${n * 28}px` }} />
                  <strong>{n}</strong>
                </li>
              ))}
              {counts.size === 0 && <li className="stream-empty">아직 비어 있음</li>}
            </ul>
          ) : (
            <ol className="stream-result">
              {list.map((name) => (
                <li key={name}>{name}</li>
              ))}
              {list.length === 0 && <li className="stream-empty">아직 비어 있음</li>}
            </ol>
          )}
          <p className="stream-point">{preset.point}</p>
        </div>
      </div>

      <h4>기록</h4>
      <ol className="stream-log" ref={logRef}>
        {preset.steps.slice(0, index + 1).map((step, i) => (
          <li key={i} className={step.extra ? 'extra' : undefined}>
            {step.text}
          </li>
        ))}
      </ol>
      <p className="status-text">흐린 줄은 이해를 돕기 위해 화면에만 넣은 단계입니다. 나머지 줄은 Java 실행 결과와 같습니다.</p>
    </div>
  );
}
