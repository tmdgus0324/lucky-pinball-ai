/**
 * 핀볼 맵(클래식 갈톤보드) 렌더링 + 물리 시뮬레이션.
 * frontend/js/game.js를 거의 그대로 옮긴 것 — React는 여기에 DOM 요소만 넘겨주는 얇은
 * 경계 역할만 한다 (devhelp/21 참고). 설계 근거: plan/04_pinball-map-design.md
 *
 * 물리엔진(Matter.js) 관련 코드는 전부 MatterAdapter 안에 가둬뒀다.
 * 나중에 Box2D 등으로 교체하고 싶어지면, 이 어댑터와 같은 메서드를
 * 제공하는 새 어댑터를 만들어 끼워 넣기만 하면 된다 (게임 로직은
 * getPosition()으로 위치만 물어보기 때문에 엔진에 종속되지 않는다).
 */
import * as Matter from 'matter-js';
import type { GameParticipant } from '../api/client';

interface MapConfig {
  id: string;
  name: string;
  boardWidth: number;
  boardHeight: number;
  pegField: { rows: number; colSpacing: number; rowSpacing: number; startY: number; pegRadius: number };
  finishLineY: number;
  funnelBottomMargin: number;
  ballRadius: number;
  gravityScale: number;
  restitution: number;
  /** 이 못 행(row index) 목록은 못 대신 회전 핀휠 장애물로 대체한다. */
  pinwheelRowIndices: number[];
  pinwheelBarLength: number;
  pinwheelBarThickness: number;
  /** 라디안/틱. 인접한 핀휠끼리는 부호를 번갈아 반대로 돈다(lazygyu/roulette 참고). */
  pinwheelAngularSpeed: number;
  /** 이 못 행(row index)의 높이에 양쪽 벽 삼각 선반을 붙인다 — 벽을 타고 내려오는 빈 통로를 막는다. */
  wedgeRowIndices: number[];
  /** 선반이 벽 안쪽 면에서 중앙으로 튀어나오는 폭 = 높이(45° 경사). */
  wedgeSize: number;
  /** 카메라가 선두 공 추적을 시작하는 y좌표 — 낙하 초반 혼전 구간은 제외한다. */
  zoomStartY: number;
}

// 맵을 기존(770px, 12행) 대비 약 2배로 키우고, 중간에 핀휠 행을 하나 끼워 넣었다.
// 참고: lazygyu/roulette(https://lazygyu.github.io/roulette/)의 "Wheel of fortune" 맵이
// 핀 배열 중간에 회전 막대(핀휠)를 가로로 나란히 배치하는 구조를 이 프로젝트 방식(못 배열
// 기반)에 맞게 응용했다.
const CLASSIC_GALTON_MAP: MapConfig = {
  id: 'classic-galton',
  name: '클래식 갈톤보드',
  boardWidth: 560,
  boardHeight: 1500,
  // startY(280)는 devhelp/26에서 상대평가 도입 후 buff.startY 최대값이 200->260으로
  // 커지면서 340으로 올렸다 — spawnY 최대값(20+260=280)과 정확히 같으면 여유가 0이 되어
  // devhelp/25에서 고친 "공이 못 행과 겹쳐서 끼는" 문제가 재발한다. 60px 여유를 다시 확보.
  pegField: { rows: 20, colSpacing: 60, rowSpacing: 50, startY: 340, pegRadius: 7 },
  finishLineY: 1400,
  funnelBottomMargin: 50,
  ballRadius: 13,
  gravityScale: 0.4, // 기존(0.8) 대비 2배 느리게
  restitution: 0.45,
  // 기존 10행(중간) 한 줄 → 5행/15행 두 줄로 늘렸다가, 두 줄 사이(9행, 450px)가 여전히
  // 완전히 뚫려있어서 4행 간격으로 균등하게 재배치했다 — 통로 전체에서 핀휠과 마주치는
  // 간격을 일정하게 유지하기 위함 (devhelp/31 참고).
  pinwheelRowIndices: [4, 8, 12, 16],
  pinwheelBarLength: 100,
  pinwheelBarThickness: 10,
  pinwheelAngularSpeed: 0.05,
  // 핀휠 행(4/8/12/16) 사이의 행마다 벽 선반을 둔다 — devhelp/36 참고.
  wedgeRowIndices: [2, 6, 10, 14, 18],
  wedgeSize: 40,
  zoomStartY: 340 + 5 * 50, // 못 5행을 통과한 뒤부터 카메라 추적 시작 (startY 변경에 맞춰 같이 조정)
};

// "선두 추적" 연출: 완주하지 못한 공 중 가장 앞선(=현재 1등) 공을 계속 따라가며 확대한다.
// 그 공이 결승선을 통과하면, 남은 공 중 새로운 1등(=원래 2등)으로 자연스럽게 추적 대상이
// 넘어간다(매 틱 "완주 안 한 공 중 y가 가장 큰 것"을 다시 찾을 뿐이라 별도 상태 관리가 필요 없다).
// 낙하 초반(zoomStartY 이전)에는 8개 공이 한꺼번에 엎치락뒤치락해 카메라가 계속 흔들리므로
// 그 구간은 확대하지 않고 넓은 시야를 유지한다(devhelp/12에서 겪은 문제의 재발 방지).
const ZOOM_SCALE = 1.3;

// 못을 벽에 너무 가깝게 두면, 벽과 못 사이의 좁은 틈에 공이 반복적으로 끼었다 빠지기를
// 되풀이하며 "덜컹거리며 내려오는" 부자연스러운 움직임이 생긴다(실제 테스트로 확인됨,
// devhelp/11 참고). 그래서 벽 근처에는 못을 아예 배치하지 않는 여유 구간을 넉넉히 둔다.
//
// devhelp/25에서 이 값을 30까지 줄여서(벽에 지그재그/돌기를 붙여 빈 통로를 메우려는
// 시도) 8명 플레이 중 2개의 공이 영영 못 빠져나오는 심각한 재발을 두 번(지그재그 버전,
// 돌기 버전 둘 다) 확인했다 — devhelp/11이 70을 고른 이유가 정확히 이거였다. 새 장애물의
// 모양(뾰족한 지그재그 vs 둥근 돌기) 문제가 아니라, 이 여유 구간 자체를 줄이면 재발한다는
// 게 두 번의 실측으로 확인됐으므로 값은 70으로 되돌리고, "빈 통로" 문제는 벽 자체의
// 반발력(restitution)을 높여 다른 방식으로 접근한다(아래 WALL_RESTITUTION).
const PEG_WALL_CLEARANCE = 70;

// 벽에 맞은 공이 매끄럽게 미끄러지지 않고 안쪽(못 쪽)으로 튕겨 돌아오도록 반발력을 준다.
// 벽 형태나 여유 구간은 devhelp/11에서 이미 검증된 값 그대로 두고, 반발력만 0(원래 값)에서
// 올렸다 — 새 장애물을 추가하는 것보다 훨씬 낮은 리스크로 "벽을 따라 끝까지 미끄러지는"
// 문제를 완화한다.
const WALL_RESTITUTION = 0.7;

// 위 PEG_WALL_CLEARANCE는 "고정된" 못 기준으로 검증된 값이라 그대로 둔다. 핀휠은 계속
// 회전하는 장애물이라 같은 틈에서도 위치가 끊임없이 바뀌어서, 못처럼 좁은 틈에 공이 고정
// 상태로 끼는 것과는 다른 상황일 가능성이 있다는 가설로 핀휠만 벽에 조금 더 가깝게
// (70 → 50) 배치했다 — 8명 게임 10회 반복으로 안전함을 확인했다.
//
// 이후 "벽에 거의 붙을 정도로" 15까지 줄여봤지만, 5번 중 4번이 120초 안에 못 끝나고
// 타임아웃됐다(정상은 15~20초) — 회전 장애물도 벽에 너무 가까우면 못과 똑같이 위험하다는
// 뜻이다. "회전하니까 안전할 것"이라는 가설은 정도의 차이일 뿐 무한정 적용되지 않는다.
// 그래서 검증된 50으로 되돌렸다(devhelp/31, 확인 방식은 devhelp/25와 동일).
const PINWHEEL_WALL_CLEARANCE = 50;

const WALL_THICKNESS = 16;

// 벽 선반(wedge) 설계 규칙 — devhelp/25·31의 실패 경험에서 뽑은 것(devhelp/36):
//  R1. 장애물끼리(또는 장애물과 벽) 틈은 0(접촉)이거나 이 값 이상이어야 한다. 그 사이의 좁은
//      틈은 공이 끼는 함정이 된다(30·15는 실패, 50·63·70은 통과). 선반 팁에 이 값보다 가까운
//      못은 아예 배치하지 않는다.
//  R2. 선반 윗면은 중앙 쪽으로 내려가는 경사(벽과의 각 135°)라 오목한 웅덩이가 생기지 않는다.
const MIN_OBSTACLE_GAP = 50;

/** Matter.js를 감싸는 얇은 어댑터. 이 파일 밖에서는 Matter.* 를 직접 쓰지 않는다. */
class MatterAdapter {
  private engine!: Matter.Engine;
  private world!: Matter.World;
  private runner?: Matter.Runner;
  private tickHandler?: () => void;
  private kinematics: { body: Matter.Body; angularSpeed: number }[] = [];

  async init(): Promise<this> {
    this.engine = Matter.Engine.create();
    this.world = this.engine.world;
    return this;
  }

  setGravity(scale: number) {
    this.world.gravity.y = scale;
  }

  addStaticRect(cx: number, cy: number, width: number, height: number, angle = 0, restitution = 0): Matter.Body {
    const body = Matter.Bodies.rectangle(cx, cy, width, height, { isStatic: true, angle, restitution, friction: 0 });
    Matter.World.add(this.world, body);
    return body;
  }

  /** 볼록 다각형(꼭짓점은 씬 좌표). fromVertices는 넘긴 좌표를 무게중심으로 삼으므로 중심을 직접 구해서 준다. */
  addStaticPolygon(vertices: { x: number; y: number }[], restitution = 0): Matter.Body {
    const centre = Matter.Vertices.centre(vertices);
    const body = Matter.Bodies.fromVertices(centre.x, centre.y, [vertices], { isStatic: true, restitution, friction: 0 });
    Matter.World.add(this.world, body);
    return body;
  }

  addStaticCircle(cx: number, cy: number, radius: number, restitution = 0.5): Matter.Body {
    const body = Matter.Bodies.circle(cx, cy, radius, { isStatic: true, restitution, friction: 0 });
    Matter.World.add(this.world, body);
    return body;
  }

  addBall(x: number, y: number, radius: number, restitution: number): Matter.Body {
    const body = Matter.Bodies.circle(x, y, radius, {
      restitution,
      friction: 0,
      frictionAir: 0.001,
    });
    Matter.World.add(this.world, body);
    return body;
  }

  /**
   * 제자리에서 계속 회전하는 장애물(핀휠). Box2D의 kinematic 바디에 해당 —
   * 물리적으로는 static이라 공에 부딪혀도 밀리지 않지만, 매 틱 각도를 직접 갱신해서
   * 계속 돌아가게 만든다(lazygyu/roulette의 회전 막대 장애물과 같은 방식).
   */
  addKinematicRect(cx: number, cy: number, width: number, height: number, angularSpeed: number): Matter.Body {
    const body = Matter.Bodies.rectangle(cx, cy, width, height, { isStatic: true, restitution: 0.3, friction: 0 });
    Matter.World.add(this.world, body);
    this.kinematics.push({ body, angularSpeed });
    return body;
  }

  getPosition(body: Matter.Body): { x: number; y: number } {
    return { x: body.position.x, y: body.position.y };
  }

  getAngle(body: Matter.Body): number {
    return body.angle;
  }

  getSpeed(body: Matter.Body): number {
    return body.speed;
  }

  nudge(body: Matter.Body) {
    const horizontalKick = (Math.random() - 0.5) * 6;
    Matter.Body.setVelocity(body, {
      x: body.velocity.x + horizontalKick,
      y: Math.max(body.velocity.y, 2),
    });
  }

  run(onTick: () => void) {
    this.runner = Matter.Runner.create();
    Matter.Runner.run(this.runner, this.engine);
    this.tickHandler = () => {
      for (const k of this.kinematics) {
        Matter.Body.setAngle(k.body, k.body.angle + k.angularSpeed);
      }
      onTick();
    };
    Matter.Events.on(this.engine, 'afterUpdate', this.tickHandler);
  }

  stop() {
    if (this.runner) Matter.Runner.stop(this.runner);
    if (this.tickHandler) Matter.Events.off(this.engine, 'afterUpdate', this.tickHandler);
  }

  dispose() {
    this.stop();
    Matter.World.clear(this.world, false);
    Matter.Engine.clear(this.engine);
  }
}

function playableBoundsAtY(y: number, cfg: MapConfig) {
  const t = Math.min(1, Math.max(0, y / cfg.boardHeight));
  const margin = cfg.funnelBottomMargin * t;
  return { left: margin, right: cfg.boardWidth - margin };
}

interface WedgeSpec {
  /** 볼록 사각형 꼭짓점(씬 좌표). 벽 쪽 두 점은 벽 안으로 4px 파고들어 이음새 틈을 없앤다. */
  vertices: { x: number; y: number }[];
}

function buildWedgeSpecs(cfg: MapConfig): WedgeSpec[] {
  const specs: WedgeSpec[] = [];
  const half = cfg.wedgeSize / 2;
  const embed = 4;

  cfg.wedgeRowIndices.forEach((rowIndex) => {
    const y = cfg.pegField.startY + rowIndex * cfg.pegField.rowSpacing;
    const { left, right } = playableBoundsAtY(y, cfg);
    const innerLeft = left + WALL_THICKNESS / 2; // 벽 중심선 + 반 두께 = 벽 안쪽 면
    const innerRight = right - WALL_THICKNESS / 2;

    // 꼭대기가 벽에 붙고 아래쪽 끝(팁)이 중앙 쪽으로 튀어나온다 — 윗면이 중앙으로 내려가는 45° 경사.
    specs.push({
      vertices: [
        { x: innerLeft - embed, y: y - half },
        { x: innerLeft, y: y - half },
        { x: innerLeft + cfg.wedgeSize, y: y + half },
        { x: innerLeft - embed, y: y + half },
      ],
    });
    specs.push({
      vertices: [
        { x: innerRight + embed, y: y - half },
        { x: innerRight, y: y - half },
        { x: innerRight - cfg.wedgeSize, y: y + half },
        { x: innerRight + embed, y: y + half },
      ],
    });
  });
  return specs;
}

function distanceToSegment(px: number, py: number, ax: number, ay: number, bx: number, by: number): number {
  const abx = bx - ax;
  const aby = by - ay;
  const t = Math.max(0, Math.min(1, ((px - ax) * abx + (py - ay) * aby) / (abx * abx + aby * aby)));
  return Math.hypot(px - (ax + t * abx), py - (ay + t * aby));
}

function distanceToPolygon(px: number, py: number, vertices: { x: number; y: number }[]): number {
  let min = Infinity;
  for (let i = 0; i < vertices.length; i++) {
    const a = vertices[i];
    const b = vertices[(i + 1) % vertices.length];
    min = Math.min(min, distanceToSegment(px, py, a.x, a.y, b.x, b.y));
  }
  return min;
}

function buildPegPositions(cfg: MapConfig): { x: number; y: number }[] {
  const { rows, colSpacing, rowSpacing, startY, pegRadius } = cfg.pegField;
  const wedges = buildWedgeSpecs(cfg);
  const pegs: { x: number; y: number }[] = [];
  for (let row = 0; row < rows; row++) {
    if (cfg.pinwheelRowIndices.includes(row)) continue; // 이 행들은 못 대신 핀휠이 차지한다
    const y = startY + row * rowSpacing;
    const offset = row % 2 === 1 ? colSpacing / 2 : 0;
    const { left, right } = playableBoundsAtY(y, cfg);
    for (let x = left + PEG_WALL_CLEARANCE + offset; x <= right - PEG_WALL_CLEARANCE; x += colSpacing) {
      // R1: 선반 표면과의 틈이 MIN_OBSTACLE_GAP보다 좁아지는 못은 두지 않는다.
      const tooCloseToWedge = wedges.some((w) => distanceToPolygon(x, y, w.vertices) - pegRadius < MIN_OBSTACLE_GAP);
      if (tooCloseToWedge) continue;
      pegs.push({ x, y });
    }
  }
  return pegs;
}

interface PinwheelSpec {
  x: number;
  y: number;
  angularSpeed: number;
}

function buildPinwheelPositions(cfg: MapConfig): PinwheelSpec[] {
  const spacing = cfg.pegField.colSpacing * 2; // 못보다 큰 장애물이라 한 칸 건너 배치
  const pinwheels: PinwheelSpec[] = [];

  cfg.pinwheelRowIndices.forEach((rowIndex) => {
    const y = cfg.pegField.startY + rowIndex * cfg.pegField.rowSpacing;
    const { left, right } = playableBoundsAtY(y, cfg);
    const fieldLeft = left + PINWHEEL_WALL_CLEARANCE + cfg.pinwheelBarLength / 2;
    const fieldRight = right - PINWHEEL_WALL_CLEARANCE - cfg.pinwheelBarLength / 2;
    const usable = fieldRight - fieldLeft;

    // 왼쪽부터 그리디하게 채우면 spacing으로 안 나눠떨어지는 나머지가 전부 오른쪽에
    // 몰려서 좌우가 비대칭으로 보인다 — 실제로 들어갈 개수를 먼저 구하고, 그 묶음
    // 전체를 fieldLeft~fieldRight 가운데에 정렬한다.
    const count = Math.max(1, Math.floor(usable / spacing) + 1);
    const totalSpan = (count - 1) * spacing;
    const startX = fieldLeft + (usable - totalSpan) / 2;

    for (let i = 0; i < count; i++) {
      const x = startX + i * spacing;
      const angularSpeed = i % 2 === 0 ? cfg.pinwheelAngularSpeed : -cfg.pinwheelAngularSpeed;
      pinwheels.push({ x, y, angularSpeed });
    }
  });

  return pinwheels;
}

function spawnX(index: number, total: number, cfg: MapConfig): number {
  // 양 끝(벽 바로 옆)에서 출발하면 못 하나 건드리지 않고 벽을 따라 있는 빈 통로로
  // 바로 낙하해버려 재미가 없다 — 그래서 출발 위치를 "못 배열이 실제로 있는 범위"
  // 안쪽으로 한정하고, 정확히 같은 자리에서 매번 출발하지 않도록 약간의 랜덤 오차를 준다.
  const { left, right } = playableBoundsAtY(20, cfg);
  const fieldLeft = left + PEG_WALL_CLEARANCE;
  const fieldRight = right - PEG_WALL_CLEARANCE;
  const usable = fieldRight - fieldLeft;
  const base = fieldLeft + (usable * (index + 0.5)) / total;
  const jitter = (Math.random() - 0.5) * (usable / total) * 0.5;
  return base + jitter;
}

function spawnY(buffStartY: number): number {
  // buffStartY(0~200, BuffCalculator 참고)를 그대로 낙하 시작 y좌표에 더한다 — 점수 1점
  // 차이가 항상 픽셀 단위로 드러나야 하므로 더는 구간별로 뭉개서 스케일링하지 않는다.
  // 드롭존 아래(pegField.startY)까지 충분한 여유가 있어 최대값(200)에서도 못 행과 겹치지 않는다.
  return 20 + buffStartY;
}

/**
 * 카메라(확대/이동) 연출을 위해 보드를 두 겹으로 나눈다:
 * boardEl(뷰포트, 크기 고정·overflow:hidden) 안에 sceneEl(실제 못/벽/공이 그려지는 레이어)을
 * 두고, sceneEl에만 scale/translate 변형을 걸어 "카메라"처럼 보이게 한다.
 *
 * fitScale: 모바일처럼 화면이 좁아서 CSS(`max-width:100%`)가 boardEl을 cfg.boardWidth보다
 * 작게 강제로 줄이는 경우, 그 실제 렌더 폭에 맞춰 scene 전체를 같은 비율로 축소한다.
 * 안 하면 scene은 여전히 560px 그대로라 overflow:hidden에 의해 오른쪽 절반 가까이가
 * 통째로 잘려서 안 보이게 된다(실기기 테스트로 확인됨).
 */
function createScene(boardEl: HTMLElement, cfg: MapConfig): { sceneEl: HTMLDivElement; fitScale: number } {
  boardEl.innerHTML = '';
  boardEl.style.width = cfg.boardWidth + 'px';
  boardEl.style.height = cfg.boardHeight + 'px';

  const renderedWidth = boardEl.getBoundingClientRect().width;
  const fitScale = renderedWidth > 0 ? Math.min(1, renderedWidth / cfg.boardWidth) : 1;
  boardEl.style.height = cfg.boardHeight * fitScale + 'px';

  const sceneEl = document.createElement('div');
  sceneEl.className = 'pinball-scene';
  sceneEl.style.width = cfg.boardWidth + 'px';
  sceneEl.style.height = cfg.boardHeight + 'px';
  boardEl.appendChild(sceneEl);
  return { sceneEl, fitScale };
}

/**
 * sceneEl을 (targetX, targetY)가 뷰포트 중앙에 오도록 cameraScale배 확대한다.
 * fitScale은 위 createScene에서 구한 "화면에 맞추기" 배율과 곱해져서 최종 배율이 된다 —
 * cameraScale=1, fitScale=1, target=중앙이면 원래 상태(변형 없음)와 같다.
 */
function applyCamera(
  sceneEl: HTMLElement,
  cfg: MapConfig,
  targetX: number,
  targetY: number,
  cameraScale: number,
  fitScale: number,
) {
  const totalScale = fitScale * cameraScale;
  const viewportWidth = cfg.boardWidth * fitScale;
  const viewportHeight = cfg.boardHeight * fitScale;
  const dx = viewportWidth / 2 - targetX * totalScale;
  const dy = viewportHeight / 2 - targetY * totalScale;
  sceneEl.style.transform = `translate(${dx}px, ${dy}px) scale(${totalScale})`;
}

function renderStaticElements(sceneEl: HTMLElement, cfg: MapConfig) {
  const dropLabel = document.createElement('div');
  dropLabel.className = 'zone-label';
  dropLabel.textContent = '낙하 시작 구역';
  sceneEl.appendChild(dropLabel);

  const finishLine = document.createElement('div');
  finishLine.className = 'finish-line';
  finishLine.style.top = cfg.finishLineY + 'px';
  finishLine.innerHTML = '<span class="label">결승선</span>';
  sceneEl.appendChild(finishLine);
}

function renderPegs(sceneEl: HTMLElement, cfg: MapConfig) {
  buildPegPositions(cfg).forEach((pos) => {
    const el = document.createElement('div');
    el.className = 'peg';
    el.style.left = pos.x + 'px';
    el.style.top = pos.y + 'px';
    sceneEl.appendChild(el);
  });
}

function renderWallVisual(sceneEl: HTMLElement, x1: number, y1: number, x2: number, y2: number, thickness: number) {
  const dx = x2 - x1;
  const dy = y2 - y1;
  const length = Math.sqrt(dx * dx + dy * dy);
  const angle = Math.atan2(dy, dx);

  const el = document.createElement('div');
  el.className = 'wall-visual';
  el.style.width = length + 'px';
  el.style.height = thickness + 'px';
  el.style.left = x1 + 'px';
  el.style.top = y1 - thickness / 2 + 'px';
  el.style.transform = `rotate(${angle}rad)`;
  sceneEl.appendChild(el);
}

function renderWedgeVisual(sceneEl: HTMLElement, spec: WedgeSpec) {
  const xs = spec.vertices.map((v) => v.x);
  const ys = spec.vertices.map((v) => v.y);
  const minX = Math.min(...xs);
  const minY = Math.min(...ys);
  const width = Math.max(...xs) - minX;
  const height = Math.max(...ys) - minY;
  // 꼭짓점을 요소 크기 기준 퍼센트로 바꿔 clip-path로 오려낸다(물리 바디와 같은 모양).
  const points = spec.vertices
    .map((v) => `${(((v.x - minX) / width) * 100).toFixed(2)}% ${(((v.y - minY) / height) * 100).toFixed(2)}%`)
    .join(', ');

  const el = document.createElement('div');
  el.className = 'wall-wedge';
  el.style.left = minX + 'px';
  el.style.top = minY + 'px';
  el.style.width = width + 'px';
  el.style.height = height + 'px';
  el.style.clipPath = `polygon(${points})`;
  sceneEl.appendChild(el);
}

function renderPinwheelVisual(sceneEl: HTMLElement, pos: PinwheelSpec, cfg: MapConfig): HTMLDivElement {
  const el = document.createElement('div');
  el.className = 'spinner-bar';
  el.style.width = cfg.pinwheelBarLength + 'px';
  el.style.height = cfg.pinwheelBarThickness + 'px';
  el.style.left = pos.x - cfg.pinwheelBarLength / 2 + 'px';
  el.style.top = pos.y - cfg.pinwheelBarThickness / 2 + 'px';
  sceneEl.appendChild(el);
  return el;
}

interface SpinnerState {
  body: Matter.Body;
  el: HTMLDivElement;
}

function buildBoard(adapter: MatterAdapter, sceneEl: HTMLElement, cfg: MapConfig): SpinnerState[] {
  renderStaticElements(sceneEl, cfg);
  renderPegs(sceneEl, cfg);

  buildPegPositions(cfg).forEach((pos) => {
    adapter.addStaticCircle(pos.x, pos.y, cfg.pegField.pegRadius, 0.5);
  });

  const spinners: SpinnerState[] = buildPinwheelPositions(cfg).map((pos) => {
    const body = adapter.addKinematicRect(pos.x, pos.y, cfg.pinwheelBarLength, cfg.pinwheelBarThickness, pos.angularSpeed);
    const el = renderPinwheelVisual(sceneEl, pos, cfg);
    return { body, el };
  });

  const wallThickness = WALL_THICKNESS;
  const bottom = cfg.boardHeight;
  const rightMargin = cfg.funnelBottomMargin;

  // 좌우 벽: 위(폭 전체) -> 아래(중앙으로 좁아짐)로 이어지는 깔때기 모양.
  // restitution을 줘서 벽에 닿은 공이 매끄럽게 미끄러지지 않고 안쪽으로 튕겨 돌아오게 한다.
  const leftLen = Math.hypot(rightMargin, bottom);
  const leftAngle = Math.atan2(bottom, rightMargin);
  adapter.addStaticRect(rightMargin / 2, bottom / 2, leftLen, wallThickness, leftAngle, WALL_RESTITUTION);
  renderWallVisual(sceneEl, 0, 0, rightMargin, bottom, wallThickness);

  const rightLen = Math.hypot(rightMargin, bottom);
  const rightAngle = Math.atan2(bottom, -rightMargin);
  adapter.addStaticRect(cfg.boardWidth - rightMargin / 2, bottom / 2, rightLen, wallThickness, rightAngle, WALL_RESTITUTION);
  renderWallVisual(sceneEl, cfg.boardWidth, 0, cfg.boardWidth - rightMargin, bottom, wallThickness);

  // 벽 삼각 선반 — 벽과 같은 반발력. 벽을 타고 내려오던 공이 윗면(중앙 쪽 경사)에 닿아 안쪽으로 밀려난다.
  buildWedgeSpecs(cfg).forEach((spec) => {
    adapter.addStaticPolygon(spec.vertices, WALL_RESTITUTION);
    renderWedgeVisual(sceneEl, spec);
  });

  // 바닥: 완주 후 구슬이 자연스럽게 멈춰 쌓이는 용도.
  adapter.addStaticRect(cfg.boardWidth / 2, bottom + 8, cfg.boardWidth, 16, 0);

  return spinners;
}

function createBallEl(sceneEl: HTMLElement, name: string, colorIndex: number): HTMLDivElement {
  const palette = ['#6b7280', '#4ade80', '#38bdf8', '#a78bfa', '#ffd166', '#f472b6', '#fb923c', '#22d3ee'];
  const el = document.createElement('div');
  el.className = 'ball';
  el.style.background = palette[colorIndex % palette.length];
  el.textContent = name;
  sceneEl.appendChild(el);
  return el;
}

interface FinishedEntry {
  name: string;
}

function renderRankPanel(rankListEl: HTMLElement, finishedEntries: FinishedEntry[], pendingNames: Set<string>) {
  rankListEl.innerHTML = '';
  const total = finishedEntries.length + pendingNames.size;

  // 참가자 이름은 사용자가 입력한 값이라, innerHTML로 넣으면 이름에 태그를 넣어 스크립트를
  // 실행시킬 수 있다(XSS). textContent/append는 항상 텍스트로만 취급해서 안전하다.
  finishedEntries.forEach((entry, index) => {
    const isLast = index === finishedEntries.length - 1 && pendingNames.size === 0;
    const item = document.createElement('div');
    item.className = 'rank-item' + (isLast ? ' selected' : '');
    const position = document.createElement('span');
    position.className = 'position';
    position.textContent = String(index + 1);
    item.appendChild(position);
    item.append(' ' + entry.name);
    rankListEl.appendChild(item);
  });

  pendingNames.forEach((name) => {
    const item = document.createElement('div');
    item.className = 'rank-item pending';
    const position = document.createElement('span');
    position.className = 'position';
    position.textContent = '?';
    item.appendChild(position);
    item.append(` ${name} (진행 중)`);
    rankListEl.appendChild(item);
  });

  if (total === 0) {
    rankListEl.innerHTML = '<div class="rank-item pending">게임을 시작하면 여기에 순위가 표시됩니다</div>';
  }
}

interface BallState {
  playerId: number;
  name: string;
  body: Matter.Body;
  el: HTMLDivElement;
  finished: boolean;
  stallTicks: number;
}

export interface RunPinballGameOptions {
  boardEl: HTMLElement;
  rankListEl: HTMLElement;
  participants: GameParticipant[];
  onComplete: (finishOrder: number[]) => void;
}

/**
 * @returns dispose 함수 — 게임이 끝나기 전에 컴포넌트가 언마운트되는 경우
 * (StrictMode 이중 실행, 라우터 이동 등) 호출해서 물리 엔진을 확실히 정리한다.
 * (devhelp/18의 StrictMode 이중 실행 항목, devhelp/21 참고)
 */
export function runPinballGame({ boardEl, rankListEl, participants, onComplete }: RunPinballGameOptions): () => void {
  const cfg = CLASSIC_GALTON_MAP;
  const adapter = new MatterAdapter();
  let disposed = false;

  adapter.init().then(() => {
    if (disposed) return;
    adapter.setGravity(cfg.gravityScale);

    const { sceneEl, fitScale } = createScene(boardEl, cfg);
    const spinners = buildBoard(adapter, sceneEl, cfg);

    const ballStates: BallState[] = [];
    const pendingNames = new Set<string>();

    participants.forEach((participant, index) => {
      const x = spawnX(index, participants.length, cfg);
      const y = spawnY(participant.buff.startY);
      const body = adapter.addBall(x, y, cfg.ballRadius, cfg.restitution);
      const el = createBallEl(sceneEl, participant.name, index);
      ballStates.push({
        playerId: participant.playerId,
        name: participant.name,
        body,
        el,
        finished: false,
        stallTicks: 0,
      });
      pendingNames.add(participant.name);
    });

    const finishOrder: number[] = [];
    const finishedEntries: FinishedEntry[] = [];
    renderRankPanel(rankListEl, finishedEntries, pendingNames);
    applyCamera(sceneEl, cfg, cfg.boardWidth / 2, cfg.boardHeight / 2, 1, fitScale);

    adapter.run(() => {
      for (const spinner of spinners) {
        spinner.el.style.transform = `rotate(${adapter.getAngle(spinner.body)}rad)`;
      }

      let allFinished = true;
      const unfinished: { state: BallState; pos: { x: number; y: number } }[] = [];

      for (const state of ballStates) {
        const pos = adapter.getPosition(state.body);
        state.el.style.left = pos.x + 'px';
        state.el.style.top = pos.y + 'px';

        if (!state.finished) {
          if (pos.y >= cfg.finishLineY) {
            state.finished = true;
            state.el.classList.add('finished');
            finishOrder.push(state.playerId);
            finishedEntries.push({ name: state.name });
            pendingNames.delete(state.name);
            renderRankPanel(rankListEl, finishedEntries, pendingNames);
          } else {
            allFinished = false;
            unfinished.push({ state, pos });

            // 못/벽 사이의 기하학적 틈에 구슬이 물리적으로 끼어 멈춰버리는 경우가 실제로
            // 관찰되어(공이 결승선에 영영 도달하지 못함) 안전장치로 넣은 로직.
            if (adapter.getSpeed(state.body) < 0.05) {
              state.stallTicks++;
              if (state.stallTicks > 45) {
                adapter.nudge(state.body);
                state.stallTicks = 0;
              }
            } else {
              state.stallTicks = 0;
            }
          }
        }
      }

      // "선두 추적" 연출: 완주 안 한 공 중 가장 앞선(y가 가장 큰) 공을 계속 따라간다.
      // 그 공이 완주하면 다음 틱엔 자동으로 새로운 선두(원래 2등)가 이 계산에 잡힌다.
      // 낙하 초반(zoomStartY 이전)은 혼전이라 확대하지 않고 넓은 시야를 유지한다.
      if (!allFinished) {
        const leading = unfinished.reduce((a, b) => (a.pos.y >= b.pos.y ? a : b));
        if (leading.pos.y >= cfg.zoomStartY) {
          applyCamera(sceneEl, cfg, leading.pos.x, leading.pos.y, ZOOM_SCALE, fitScale);
        } else {
          applyCamera(sceneEl, cfg, cfg.boardWidth / 2, cfg.boardHeight / 2, 1, fitScale);
        }
      }

      // 게임 시작 전 "빈 맵 미리보기"(참가자 0명)에서는 공이 하나도 없어서 allFinished가
      // 초기값 true 그대로 유지된다 — 그걸 "전원 완주"로 오판해 즉시 onComplete가 불려버리는
      // 걸 막는다. 공이 1개 이상 있을 때만 완료 판정한다.
      if (allFinished && ballStates.length > 0) {
        adapter.dispose();
        onComplete(finishOrder);
      }
    });
  });

  return () => {
    disposed = true;
    adapter.dispose();
  };
}
