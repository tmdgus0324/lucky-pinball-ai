/**
 * index.html 화면 전체를 잇는 진행 흐름: 등록 -> 운세 확인 -> 게임 시작 -> 결과.
 * DOM 조작/이벤트 처리만 담당하고, 서버 호출은 api.js, 물리 게임은 game.js에 맡긴다.
 */
(function () {
  const state = {
    players: [], // {playerId, name, birthDate}
    fortunes: new Map(), // playerId -> {fortuneScore, luckyNumber, fortuneMessage, buff}
  };

  const els = {
    form: document.getElementById('regForm'),
    nameInput: document.querySelector('#regForm input[name="name"]'),
    birthInput: document.querySelector('#regForm input[name="birthDate"]'),
    playerList: document.getElementById('playerList'),
    playerStatus: document.getElementById('playerStatus'),
    fortuneBtn: document.getElementById('fortuneBtn'),
    fortuneGrid: document.getElementById('fortuneGrid'),
    fortuneStatus: document.getElementById('fortuneStatus'),
    startGameBtn: document.getElementById('startGameBtn'),
    pinballBoard: document.getElementById('pinballBoard'),
    rankList: document.getElementById('rankList'),
    resultSection: document.getElementById('resultSection'),
    resultBanner: document.getElementById('resultBanner'),
  };

  function setStatus(el, message, isError) {
    el.textContent = message || '';
    el.classList.toggle('error', Boolean(isError));
  }

  /**
   * "880324" 같은 6자리(YYMMDD) 문자열을 "1988-03-24" 형태로 변환한다.
   * 두 자리 연도(YY)의 세기 판단 기준: 50 이상이면 1900년대, 50 미만이면 2000년대
   * (주민등록번호 앞자리와 같은 흔한 관례). 형식이 맞지 않으면 null을 반환한다.
   */
  function parseBirthDateShorthand(raw) {
    const digits = raw.replace(/\D/g, '');
    if (digits.length !== 6) return null;

    const yy = parseInt(digits.slice(0, 2), 10);
    const month = parseInt(digits.slice(2, 4), 10);
    const day = parseInt(digits.slice(4, 6), 10);
    if (month < 1 || month > 12 || day < 1 || day > 31) return null;

    const year = (yy >= 50 ? 1900 : 2000) + yy;
    const mm = String(month).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
    return `${year}-${mm}-${dd}`;
  }

  function renderPlayerList() {
    els.playerList.innerHTML = '';
    state.players.forEach((player) => {
      const chip = document.createElement('span');
      chip.className = 'player-chip';
      chip.innerHTML = `${player.name} · ${player.birthDate || '생년월일 미입력'} `;
      const removeBtn = document.createElement('button');
      removeBtn.type = 'button';
      removeBtn.className = 'remove';
      removeBtn.textContent = '✕';
      removeBtn.addEventListener('click', () => {
        state.players = state.players.filter((p) => p.playerId !== player.playerId);
        renderPlayerList();
        updateFortuneButtonState();
      });
      chip.appendChild(removeBtn);
      els.playerList.appendChild(chip);
    });
    setStatus(els.playerStatus, `${state.players.length}/8명 등록됨`);
  }

  function updateFortuneButtonState() {
    els.fortuneBtn.disabled = state.players.length < 2 || state.players.length > 8;
  }

  els.form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const name = els.nameInput.value.trim();
    const rawBirth = els.birthInput.value.trim();

    if (!name) {
      setStatus(els.playerStatus, '이름을 입력해주세요.', true);
      return;
    }

    let birthDate = null; // 비워두면 버프 없이 참여(AI 호출 없음)
    if (rawBirth) {
      birthDate = parseBirthDateShorthand(rawBirth);
      if (!birthDate) {
        setStatus(els.playerStatus, '생년월일은 6자리 숫자로 입력해주세요 (예: 880324).', true);
        return;
      }
    }
    if (state.players.length >= 8) {
      setStatus(els.playerStatus, '참가자는 최대 8명까지 등록할 수 있습니다.', true);
      return;
    }

    try {
      const player = await api.registerPlayer(name, birthDate);
      state.players.push(player);
      els.nameInput.value = '';
      els.birthInput.value = '';
      renderPlayerList();
      updateFortuneButtonState();
    } catch (error) {
      setStatus(els.playerStatus, `등록 실패: ${error.message}`, true);
    }
  });

  els.fortuneBtn.addEventListener('click', async () => {
    els.fortuneBtn.disabled = true;
    setStatus(els.fortuneStatus, '운세를 확인하는 중...');
    els.fortuneGrid.innerHTML = '';

    // Promise.all이 아니라 allSettled를 쓴 이유: 생년월일을 입력한 참가자 중 한 명이라도
    // AI 호출에 실패하면(예: API 키 미설정) Promise.all은 전체를 실패시켜서 생년월일 없는
    // 참가자(원래 실패할 이유가 없는)의 카드까지 못 보여준다 — 참가자별로 독립적으로 처리한다.
    const settled = await Promise.allSettled(state.players.map((player) => api.getFortune(player.playerId)));

    const results = [];
    const errors = [];
    settled.forEach((outcome, index) => {
      if (outcome.status === 'fulfilled') {
        state.fortunes.set(outcome.value.playerId, outcome.value);
        results.push(outcome.value);
      } else {
        errors.push(`${state.players[index].name}: ${outcome.reason.message}`);
      }
    });

    renderFortuneGrid(results);

    if (errors.length === 0) {
      setStatus(els.fortuneStatus, '');
      els.startGameBtn.disabled = false;
    } else {
      setStatus(els.fortuneStatus, `일부 운세 조회 실패 - ${errors.join(' / ')}`, true);
      els.fortuneBtn.disabled = false; // 실패한 참가자가 있으니 재시도할 수 있게 다시 활성화
    }
  });

  const SOURCE_LABEL = { AI: 'AI 분석 (1회차)', CACHE: '이전 기록 재사용', NONE: '생년월일 미입력' };

  function renderFortuneGrid(results) {
    results.forEach((fortune) => {
      const player = state.players.find((p) => p.playerId === fortune.playerId);
      const card = document.createElement('div');
      card.className = `fortune-card tier-${fortune.buff.tier}`;
      const buffLabel = fortune.buff.tier === 0 ? '버프 없음' : `버프 ${fortune.buff.tier}`;
      const sourceLabel = SOURCE_LABEL[fortune.source] || fortune.source;

      const scoreBlock =
        fortune.source === 'NONE'
          ? `<div class="message">생년월일을 입력하지 않아 운세 없이 참여합니다</div>`
          : `<div class="score">${fortune.fortuneScore}<span style="font-size:12px;color:var(--muted)">점</span></div>
             <div class="message">"${fortune.fortuneMessage}"</div>`;

      card.innerHTML = `
        <div class="name">${player ? player.name : fortune.playerId}</div>
        ${scoreBlock}
        <div class="stats"><span>${buffLabel}</span><span>시작 높이 +${fortune.buff.startY}</span><span>${sourceLabel}</span></div>
      `;
      els.fortuneGrid.appendChild(card);
    });
  }

  els.startGameBtn.addEventListener('click', async () => {
    els.startGameBtn.disabled = true;
    els.resultSection.hidden = true;

    try {
      const playerIds = state.players.map((p) => p.playerId);
      const game = await api.createGame(playerIds);
      await api.startGame(game.gameId);

      await runPinballGame({
        boardEl: els.pinballBoard,
        rankListEl: els.rankList,
        participants: game.participants,
        onComplete: async (finishOrder) => {
          try {
            const result = await api.reportResult(game.gameId, finishOrder);
            showResult(result);
          } catch (error) {
            setStatus(els.fortuneStatus, `결과 보고 실패: ${error.message}`, true);
          } finally {
            // 몇 번이든 다시 시작할 수 있게 버튼을 다시 활성화한다 — 매번 새 게임(새 gameId)이
            // 만들어지고 물리 시뮬레이션도 다시 도니 이전 판과 같은 결과가 나오지 않는다.
            els.startGameBtn.disabled = false;
          }
        },
      });
    } catch (error) {
      setStatus(els.fortuneStatus, `게임 시작 실패: ${error.message}`, true);
      els.startGameBtn.disabled = false;
    }
  });

  function showResult(result) {
    els.resultSection.hidden = false;
    els.resultBanner.innerHTML = `
      <div class="bell">🔔</div>
      <div class="selected-name">${result.selectedName} 님 당첨!</div>
      <div class="sub">참가자 ${result.participantCount}명 중 가장 마지막으로 결승선 통과 · 게임 ID: ${result.gameId}</div>
    `;
    els.resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  renderPlayerList();
  updateFortuneButtonState();
})();
