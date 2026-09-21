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

  function renderPlayerList() {
    els.playerList.innerHTML = '';
    state.players.forEach((player) => {
      const chip = document.createElement('span');
      chip.className = 'player-chip';
      chip.innerHTML = `${player.name} · ${player.birthDate} `;
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
    const birthDate = els.birthInput.value;

    if (!name || !birthDate) {
      setStatus(els.playerStatus, '이름과 생년월일을 모두 입력해주세요.', true);
      return;
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

    try {
      const results = await Promise.all(
        state.players.map((player) => api.getFortune(player.playerId))
      );
      results.forEach((fortune) => state.fortunes.set(fortune.playerId, fortune));
      renderFortuneGrid(results);
      setStatus(els.fortuneStatus, '');
      els.startGameBtn.disabled = false;
    } catch (error) {
      setStatus(els.fortuneStatus, `운세 조회 실패: ${error.message}`, true);
      els.fortuneBtn.disabled = false;
    }
  });

  function renderFortuneGrid(results) {
    results.forEach((fortune) => {
      const player = state.players.find((p) => p.playerId === fortune.playerId);
      const card = document.createElement('div');
      card.className = `fortune-card tier-${fortune.buff.tier}`;
      const buffLabel = fortune.buff.tier === 0 ? '버프 없음' : `버프 ${fortune.buff.tier}`;
      card.innerHTML = `
        <div class="name">${player ? player.name : fortune.playerId}</div>
        <div class="score">${fortune.fortuneScore}<span style="font-size:12px;color:var(--muted)">점</span></div>
        <div class="message">"${fortune.fortuneMessage}"</div>
        <div class="stats"><span>${buffLabel}</span><span>시작 높이 +${fortune.buff.startY}</span></div>
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
