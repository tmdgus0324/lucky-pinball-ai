/**
 * 핀볼 맵(클래식 갈톤보드) 렌더링 + 물리 시뮬레이션.
 * 설계 근거: plan/04_pinball-map-design.md
 *
 * 물리엔진(Matter.js) 관련 코드는 전부 MatterAdapter 안에 가둬뒀다.
 * 나중에 Box2D 등으로 교체하고 싶어지면, 이 어댑터와 같은 메서드를
 * 제공하는 새 어댑터를 만들어 끼워 넣기만 하면 된다 (게임 로직은
 * getPosition()으로 위치만 물어보기 때문에 엔진에 종속되지 않는다).
 */

const CLASSIC_GALTON_MAP = {
  id: 'classic-galton',
  name: '클래식 갈톤보드',
  boardWidth: 560,
  boardHeight: 770,
  pegField: { rows: 12, colSpacing: 60, rowSpacing: 50, startY: 90, pegRadius: 7 }, // 8행 -> 12행으로 확장
  finishLineY: 670,
  funnelBottomMargin: 50, // 하단에서 좌우로 좁아지는 폭(한쪽 기준)
  ballRadius: 13,
  gravityScale: 0.8, // 기존(1.2) 대비 1.5배 느리게
  restitution: 0.45,
};

// "역전 연출": 결승선을 통과하지 못한 공이 이 인원 이하로 줄어들면, 그중 가장 뒤처진(=꼴찌 후보)
// 공을 따라 카메라를 확대한다. 처음부터(8명 전원) 추적하면 초반 혼전에서 카메라가 계속
// 흔들리기만 해서 오히려 산만해지므로, 순위가 어느 정도 추려진 막판에만 켠다.
const ZOOM_TRIGGER_REMAINING = 3;
const ZOOM_SCALE = 1.3;

/** Matter.js를 감싸는 얇은 어댑터. 이 파일 밖에서는 Matter.* 를 직접 쓰지 않는다. */
class MatterAdapter {
  async init() {
    this.engine = Matter.Engine.create();
    this.world = this.engine.world;
    return this;
  }

  setGravity(scale) {
    this.world.gravity.y = scale;
  }

  addStaticRect(cx, cy, width, height, angle = 0) {
    const body = Matter.Bodies.rectangle(cx, cy, width, height, { isStatic: true, angle, friction: 0 });
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

  getPosition(body) {
    return { x: body.position.x, y: body.position.y };
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
    this._tickHandler = onTick;
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
const PEG_WALL_CLEARANCE = 70;

function buildPegPositions(cfg) {
  const { rows, colSpacing, rowSpacing, startY } = cfg.pegField;
  const pegs = [];
  for (let row = 0; row < rows; row++) {
    const y = startY + row * rowSpacing;
    const offset = row % 2 === 1 ? colSpacing / 2 : 0;
    const { left, right } = playableBoundsAtY(y, cfg);
    for (let x = left + PEG_WALL_CLEARANCE + offset; x <= right - PEG_WALL_CLEARANCE; x += colSpacing) {
      pegs.push({ x, y });
    }
  }
  return pegs;
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
  // 버프 오프셋(0~60)을 낙하 시작 구역(못 1행 y=90보다 한참 위) 안에서만 움직이게 스케일링.
  return 12 + buffStartY * 0.6;
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

function buildBoard(adapter, sceneEl, cfg) {
  renderStaticElements(sceneEl, cfg);
  renderPegs(sceneEl, cfg);

  buildPegPositions(cfg).forEach((pos) => {
    adapter.addStaticCircle(pos.x, pos.y, cfg.pegField.pegRadius, 0.5);
  });

  const wallThickness = 16;
  const bottom = cfg.boardHeight;
  const rightMargin = cfg.funnelBottomMargin;

  // 좌우 벽: 위(폭 전체) -> 아래(중앙으로 좁아짐)로 이어지는 깔때기 모양.
  // Matter.Bodies.rectangle(cx,cy,length,thickness,{angle})는 angle=0일 때 length가 가로축과 나란하므로,
  // 벽의 두 끝점을 잇는 벡터의 각도를 그대로 angle로 넘기면 벽이 그 방향으로 정렬된다.
  const leftLen = Math.hypot(rightMargin, bottom);
  const leftAngle = Math.atan2(bottom, rightMargin);
  adapter.addStaticRect(rightMargin / 2, bottom / 2, leftLen, wallThickness, leftAngle);
  renderWallVisual(sceneEl, 0, 0, rightMargin, bottom, wallThickness);

  const rightLen = Math.hypot(rightMargin, bottom);
  const rightAngle = Math.atan2(bottom, -rightMargin);
  adapter.addStaticRect(cfg.boardWidth - rightMargin / 2, bottom / 2, rightLen, wallThickness, rightAngle);
  renderWallVisual(sceneEl, cfg.boardWidth, 0, cfg.boardWidth - rightMargin, bottom, wallThickness);

  // 바닥: 완주 후 구슬이 자연스럽게 멈춰 쌓이는 용도.
  adapter.addStaticRect(cfg.boardWidth / 2, bottom + 8, cfg.boardWidth, 16, 0);
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
  buildBoard(adapter, sceneEl, cfg);

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

    // "역전 연출": 결승선을 통과 못 한 공이 ZOOM_TRIGGER_REMAINING명 이하로 줄어들면,
    // 그중 가장 못 내려간(y가 가장 작은 = 현재 꼴찌 후보) 공을 계속 따라가며 확대한다.
    // 꼴찌 후보가 바뀔 때마다(=역전) 카메라가 새 대상 쪽으로 부드럽게 넘어간다
    // (CSS transition으로 처리 — 매 프레임 목표 좌표만 갱신하면 됨, style.css 참고).
    if (!allFinished && unfinished.length <= ZOOM_TRIGGER_REMAINING) {
      const trailing = unfinished.reduce((a, b) => (a.pos.y <= b.pos.y ? a : b));
      applyCamera(sceneEl, cfg, trailing.pos.x, trailing.pos.y, ZOOM_SCALE);
    } else if (!allFinished) {
      applyCamera(sceneEl, cfg, cfg.boardWidth / 2, cfg.boardHeight / 2, 1);
    }

    if (allFinished) {
      adapter.dispose();
      onComplete(finishOrder);
    }
  });
}
