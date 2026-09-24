/**
 * 핀볼 맵(클래식 갈톤보드) 렌더링 + 물리 시뮬레이션.
 * 설계 근거: plan/04_pinball-map-design.md
 *
 * 물리엔진(Matter.js) 관련 코드는 전부 MatterAdapter 안에 가둬뒀다.
 * 나중에 Box2D 등으로 교체하고 싶어지면, 이 어댑터와 같은 메서드를
 * 제공하는 새 어댑터를 만들어 끼워 넣기만 하면 된다 (게임 로직은
 * getPosition()으로 위치만 물어보기 때문에 엔진에 종속되지 않는다).
 */

// 맵을 기존(770px, 12행) 대비 약 2배로 키우고, 중간에 핀휠 행을 하나 끼워 넣었다.
// 참고: lazygyu/roulette(https://lazygyu.github.io/roulette/)의 "Wheel of fortune" 맵이
// 핀 배열 중간에 회전 막대(핀휠)를 가로로 나란히 배치하는 구조를 이 프로젝트 방식(못 배열
// 기반)에 맞게 응용했다.
const CLASSIC_GALTON_MAP = {
  id: 'classic-galton',
  name: '클래식 갈톤보드',
  boardWidth: 560,
  boardHeight: 1500,
  // startY(280)는 devhelp/26에서 상대평가 도입 후 buff.startY 최대값이 200->260으로
  // 커지면서 340으로 올렸다 — spawnY 최대값(20+260=280)과 정확히 같으면 여유가 0이 되어
  // devhelp/25에서 고친 "공이 못 행과 겹쳐서 끼는" 문제가 재발한다. 60px 여유를 다시 확보.
  pegField: { rows: 20, colSpacing: 60, rowSpacing: 50, startY: 340, pegRadius: 7 },
  finishLineY: 1400,
  funnelBottomMargin: 50, // 하단에서 좌우로 좁아지는 폭(한쪽 기준)
  ballRadius: 13,
  gravityScale: 0.4, // 기존(0.8) 대비 2배 느리게
  restitution: 0.45,
  pinwheelRowIndex: 10,
  pinwheelBarLength: 100,
  pinwheelBarThickness: 10,
  pinwheelAngularSpeed: 0.05,
  zoomStartY: 340 + 5 * 50, // 못 5행을 통과한 뒤부터 카메라 추적 시작 (startY 변경에 맞춰 같이 조정)
};

// "선두 추적" 연출: 완주하지 못한 공 중 가장 앞선(=현재 1등) 공을 계속 따라가며 확대한다.
// 그 공이 결승선을 통과하면, 남은 공 중 새로운 1등(=원래 2등)으로 자연스럽게 추적 대상이
// 넘어간다(매 틱 "완주 안 한 공 중 y가 가장 큰 것"을 다시 찾을 뿐이라 별도 상태 관리가 필요 없다).
// 낙하 초반(zoomStartY 이전)에는 8개 공이 한꺼번에 엎치락뒤치락해 카메라가 계속 흔들리므로
// 그 구간은 확대하지 않고 넓은 시야를 유지한다(devhelp/12에서 겪은 문제의 재발 방지).
const ZOOM_SCALE = 1.3;

/** Matter.js를 감싸는 얇은 어댑터. 이 파일 밖에서는 Matter.* 를 직접 쓰지 않는다. */
class MatterAdapter {
  constructor() {
    this.kinematics = [];
  }

  async init() {
    this.engine = Matter.Engine.create();
    this.world = this.engine.world;
    return this;
  }

  setGravity(scale) {
    this.world.gravity.y = scale;
  }

  addStaticRect(cx, cy, width, height, angle = 0, restitution = 0) {
    const body = Matter.Bodies.rectangle(cx, cy, width, height, { isStatic: true, angle, restitution, friction: 0 });
    Matter.World.add(this.world, body);
    return body;
  }

  addStaticCircle(cx, cy, radius, restitution = 0.5) {
    const body = Matter.Bodies.circle(cx, cy, radius, { isStatic: true, restitution, friction: 0 });
    Matter.World.add(this.world, body);
    return body;
  }

  addBall(x, y, radius, restitution) {
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
  addKinematicRect(cx, cy, width, height, angularSpeed) {
    const body = Matter.Bodies.rectangle(cx, cy, width, height, { isStatic: true, restitution: 0.3, friction: 0 });
    Matter.World.add(this.world, body);
    this.kinematics.push({ body, angularSpeed });
    return body;
  }

  getPosition(body) {
    return { x: body.position.x, y: body.position.y };
  }

  getAngle(body) {
    return body.angle;
  }

  getSpeed(body) {
    return body.speed;
  }

  nudge(body) {
    const horizontalKick = (Math.random() - 0.5) * 6;
    Matter.Body.setVelocity(body, {
      x: body.velocity.x + horizontalKick,
      y: Math.max(body.velocity.y, 2),
    });
  }

  run(onTick) {
    this.runner = Matter.Runner.create();
    Matter.Runner.run(this.runner, this.engine);
    this._tickHandler = () => {
      for (const k of this.kinematics) {
        Matter.Body.setAngle(k.body, k.body.angle + k.angularSpeed);
      }
      onTick();
    };
    Matter.Events.on(this.engine, 'afterUpdate', this._tickHandler);
  }

  stop() {
    if (this.runner) Matter.Runner.stop(this.runner);
    if (this._tickHandler) Matter.Events.off(this.engine, 'afterUpdate', this._tickHandler);
  }

  dispose() {
    this.stop();
    Matter.World.clear(this.world, false);
    Matter.Engine.clear(this.engine);
  }
}

function playableBoundsAtY(y, cfg) {
  const t = Math.min(1, Math.max(0, y / cfg.boardHeight));
  const margin = cfg.funnelBottomMargin * t;
  return { left: margin, right: cfg.boardWidth - margin };
}

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

function buildPegPositions(cfg) {
  const { rows, colSpacing, rowSpacing, startY } = cfg.pegField;
  const pegs = [];
  for (let row = 0; row < rows; row++) {
    if (row === cfg.pinwheelRowIndex) continue; // 이 행은 못 대신 핀휠이 차지한다
    const y = startY + row * rowSpacing;
    const offset = row % 2 === 1 ? colSpacing / 2 : 0;
    const { left, right } = playableBoundsAtY(y, cfg);
    for (let x = left + PEG_WALL_CLEARANCE + offset; x <= right - PEG_WALL_CLEARANCE; x += colSpacing) {
      pegs.push({ x, y });
    }
  }
  return pegs;
}

function buildPinwheelPositions(cfg) {
  const y = cfg.pegField.startY + cfg.pinwheelRowIndex * cfg.pegField.rowSpacing;
  const { left, right } = playableBoundsAtY(y, cfg);
  const spacing = cfg.pegField.colSpacing * 2; // 못보다 큰 장애물이라 한 칸 건너 배치
  const fieldLeft = left + PEG_WALL_CLEARANCE + cfg.pinwheelBarLength / 2;
  const fieldRight = right - PEG_WALL_CLEARANCE - cfg.pinwheelBarLength / 2;

  const pinwheels = [];
  let index = 0;
  for (let x = fieldLeft; x <= fieldRight; x += spacing, index++) {
    const angularSpeed = index % 2 === 0 ? cfg.pinwheelAngularSpeed : -cfg.pinwheelAngularSpeed;
    pinwheels.push({ x, y, angularSpeed });
  }
  return pinwheels;
}

function spawnX(index, total, cfg) {
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

function spawnY(buffStartY) {
  // buffStartY(0~200, BuffCalculator 참고)를 그대로 낙하 시작 y좌표에 더한다 — 점수 1점
  // 차이가 항상 픽셀 단위로 드러나야 하므로 더는 구간별로 뭉개서 스케일링하지 않는다.
  // 드롭존 아래(pegField.startY)까지 충분한 여유가 있어 최대값(200)에서도 못 행과 겹치지 않는다.
  return 20 + buffStartY;
}

/**
 * 카메라(확대/이동) 연출을 위해 보드를 두 겹으로 나눈다:
 * boardEl(뷰포트, 크기 고정·overflow:hidden) 안에 sceneEl(실제 못/벽/공이 그려지는 레이어)을
 * 두고, sceneEl에만 scale/translate 변형을 걸어 "카메라"처럼 보이게 한다.
 */
function createScene(boardEl, cfg) {
  boardEl.innerHTML = '';
  boardEl.style.width = cfg.boardWidth + 'px';
  boardEl.style.height = cfg.boardHeight + 'px';

  const sceneEl = document.createElement('div');
  sceneEl.className = 'pinball-scene';
  sceneEl.style.width = cfg.boardWidth + 'px';
  sceneEl.style.height = cfg.boardHeight + 'px';
  boardEl.appendChild(sceneEl);
  return sceneEl;
}

/**
 * sceneEl을 (targetX, targetY)가 뷰포트 중앙에 오도록 scale배 확대한다.
 * scale=1, target=중앙이면 원래 상태(변형 없음)와 같다.
 */
function applyCamera(sceneEl, cfg, targetX, targetY, scale) {
  const dx = cfg.boardWidth / 2 - targetX * scale;
  const dy = cfg.boardHeight / 2 - targetY * scale;
  sceneEl.style.transform = `translate(${dx}px, ${dy}px) scale(${scale})`;
}

function renderStaticElements(sceneEl, cfg) {
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

function renderPegs(sceneEl, cfg) {
  buildPegPositions(cfg).forEach((pos) => {
    const el = document.createElement('div');
    el.className = 'peg';
    el.style.left = pos.x + 'px';
    el.style.top = pos.y + 'px';
    sceneEl.appendChild(el);
  });
}

function renderWallVisual(sceneEl, x1, y1, x2, y2, thickness) {
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

function renderPinwheelVisual(sceneEl, pos, cfg) {
  const el = document.createElement('div');
  el.className = 'spinner-bar';
  el.style.width = cfg.pinwheelBarLength + 'px';
  el.style.height = cfg.pinwheelBarThickness + 'px';
  el.style.left = pos.x - cfg.pinwheelBarLength / 2 + 'px';
  el.style.top = pos.y - cfg.pinwheelBarThickness / 2 + 'px';
  sceneEl.appendChild(el);
  return el;
}

function buildBoard(adapter, sceneEl, cfg) {
  renderStaticElements(sceneEl, cfg);
  renderPegs(sceneEl, cfg);

  buildPegPositions(cfg).forEach((pos) => {
    adapter.addStaticCircle(pos.x, pos.y, cfg.pegField.pegRadius, 0.5);
  });

  const spinners = buildPinwheelPositions(cfg).map((pos) => {
    const body = adapter.addKinematicRect(pos.x, pos.y, cfg.pinwheelBarLength, cfg.pinwheelBarThickness, pos.angularSpeed);
    const el = renderPinwheelVisual(sceneEl, pos, cfg);
    return { body, el };
  });

  const wallThickness = 16;
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

  // 바닥: 완주 후 구슬이 자연스럽게 멈춰 쌓이는 용도.
  adapter.addStaticRect(cfg.boardWidth / 2, bottom + 8, cfg.boardWidth, 16, 0);

  return spinners;
}

function createBallEl(sceneEl, name, colorIndex) {
  const palette = ['#6b7280', '#4ade80', '#38bdf8', '#a78bfa', '#ffd166', '#f472b6', '#fb923c', '#22d3ee'];
  const el = document.createElement('div');
  el.className = 'ball';
  el.style.background = palette[colorIndex % palette.length];
  el.textContent = name;
  sceneEl.appendChild(el);
  return el;
}

function renderRankPanel(rankListEl, finishedEntries, pendingNames) {
  rankListEl.innerHTML = '';
  const total = finishedEntries.length + pendingNames.size;

  finishedEntries.forEach((entry, index) => {
    const isLast = index === finishedEntries.length - 1 && pendingNames.size === 0;
    const item = document.createElement('div');
    item.className = 'rank-item' + (isLast ? ' selected' : '');
    item.innerHTML = `<span class="position">${index + 1}</span> ${entry.name}`;
    rankListEl.appendChild(item);
  });

  pendingNames.forEach((name) => {
    const item = document.createElement('div');
    item.className = 'rank-item pending';
    item.innerHTML = `<span class="position">?</span> ${name} (진행 중)`;
    rankListEl.appendChild(item);
  });

  if (total === 0) {
    rankListEl.innerHTML = '<div class="rank-item pending">게임을 시작하면 여기에 순위가 표시됩니다</div>';
  }
}

/**
 * @param {Object} opts
 * @param {HTMLElement} opts.boardEl - .pinball-board 컨테이너
 * @param {HTMLElement} opts.rankListEl - 순위를 표시할 컨테이너
 * @param {Array<{playerId:number,name:string,buff:{startY:number}}>} opts.participants
 * @param {(finishOrderPlayerIds:number[]) => void} opts.onComplete
 */
async function runPinballGame({ boardEl, rankListEl, participants, onComplete }) {
  const cfg = CLASSIC_GALTON_MAP;
  const adapter = new MatterAdapter();
  await adapter.init();
  adapter.setGravity(cfg.gravityScale);

  const sceneEl = createScene(boardEl, cfg);
  const spinners = buildBoard(adapter, sceneEl, cfg);

  const ballStates = [];
  const pendingNames = new Set();

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

  const finishOrder = [];
  const finishedEntries = [];
  renderRankPanel(rankListEl, finishedEntries, pendingNames);
  applyCamera(sceneEl, cfg, cfg.boardWidth / 2, cfg.boardHeight / 2, 1);

  adapter.run(() => {
    for (const spinner of spinners) {
      spinner.el.style.transform = `rotate(${adapter.getAngle(spinner.body)}rad)`;
    }

    let allFinished = true;
    const unfinished = [];

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
          // 관찰되어(공이 결승선에 영영 도달하지 못함) 안전장치로 넣은 로직 —
          // 일정 시간 이상 거의 정지해 있으면 살짝 흔들어서 다시 굴러가게 만든다.
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
        applyCamera(sceneEl, cfg, leading.pos.x, leading.pos.y, ZOOM_SCALE);
      } else {
        applyCamera(sceneEl, cfg, cfg.boardWidth / 2, cfg.boardHeight / 2, 1);
      }
    }

    if (allFinished) {
      adapter.dispose();
      onComplete(finishOrder);
    }
  });
}
