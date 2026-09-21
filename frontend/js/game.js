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
  boardHeight: 640,
  pegField: { rows: 8, colSpacing: 60, rowSpacing: 50, startY: 90, pegRadius: 7 },
  finishLineY: 470,
  funnelBottomMargin: 50, // 하단에서 좌우로 좁아지는 폭(한쪽 기준)
  ballRadius: 13,
  gravityScale: 1.2,
  restitution: 0.45,
};

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
  const { left, right } = playableBoundsAtY(20, cfg);
  const usable = right - left;
  return left + (usable * (index + 0.5)) / total;
}

function spawnY(buffStartY) {
  // 버프 오프셋(0~60)을 낙하 시작 구역(못 1행 y=90보다 한참 위) 안에서만 움직이게 스케일링.
  return 12 + buffStartY * 0.6;
}

function renderStaticElements(boardEl, cfg) {
  boardEl.innerHTML = '';

  const dropLabel = document.createElement('div');
  dropLabel.className = 'zone-label';
  dropLabel.textContent = '낙하 시작 구역';
  boardEl.appendChild(dropLabel);

  const finishLine = document.createElement('div');
  finishLine.className = 'finish-line';
  finishLine.style.top = cfg.finishLineY + 'px';
  finishLine.innerHTML = '<span class="label">결승선</span>';
  boardEl.appendChild(finishLine);
}

function renderPegs(boardEl, cfg) {
  buildPegPositions(cfg).forEach((pos) => {
    const el = document.createElement('div');
    el.className = 'peg';
    el.style.left = pos.x + 'px';
    el.style.top = pos.y + 'px';
    boardEl.appendChild(el);
  });
}

function renderWallVisual(boardEl, x1, y1, x2, y2, thickness) {
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
  boardEl.appendChild(el);
}

function buildBoard(adapter, boardEl, cfg) {
  renderStaticElements(boardEl, cfg);
  renderPegs(boardEl, cfg);

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
  renderWallVisual(boardEl, 0, 0, rightMargin, bottom, wallThickness);

  const rightLen = Math.hypot(rightMargin, bottom);
  const rightAngle = Math.atan2(bottom, -rightMargin);
  adapter.addStaticRect(cfg.boardWidth - rightMargin / 2, bottom / 2, rightLen, wallThickness, rightAngle);
  renderWallVisual(boardEl, cfg.boardWidth, 0, cfg.boardWidth - rightMargin, bottom, wallThickness);

  // 바닥: 완주 후 구슬이 자연스럽게 멈춰 쌓이는 용도.
  adapter.addStaticRect(cfg.boardWidth / 2, bottom + 8, cfg.boardWidth, 16, 0);
}

function createBallEl(boardEl, name, colorIndex) {
  const palette = ['#6b7280', '#4ade80', '#38bdf8', '#a78bfa', '#ffd166', '#f472b6', '#fb923c', '#22d3ee'];
  const el = document.createElement('div');
  el.className = 'ball';
  el.style.background = palette[colorIndex % palette.length];
  el.textContent = name;
  boardEl.appendChild(el);
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

  buildBoard(adapter, boardEl, cfg);

  const ballStates = [];
  const pendingNames = new Set();

  participants.forEach((participant, index) => {
    const x = spawnX(index, participants.length, cfg);
    const y = spawnY(participant.buff.startY);
    const body = adapter.addBall(x, y, cfg.ballRadius, cfg.restitution);
    const el = createBallEl(boardEl, participant.name, index);
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

  adapter.run(() => {
    let allFinished = true;

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

    if (allFinished) {
      adapter.dispose();
      onComplete(finishOrder);
    }
  });
}
