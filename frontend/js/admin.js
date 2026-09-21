/**
 * admin.html 전용 스크립트 — 조회 3종 + 가중치 조정 스텁 호출.
 */
(function () {
  const playersBody = document.getElementById('playersTableBody');
  const gamesBody = document.getElementById('gamesTableBody');
  const logList = document.getElementById('logList');
  const overrideForm = document.getElementById('overrideForm');
  const overrideStatus = document.getElementById('overrideStatus');

  async function loadPlayers() {
    try {
      const players = await api.adminGetPlayers();
      if (players.length === 0) {
        playersBody.innerHTML = '<tr><td colspan="4">등록된 참가자가 없습니다.</td></tr>';
        return;
      }
      playersBody.innerHTML = players
        .map((player) => {
          const history = player.fortuneHistory.length
            ? player.fortuneHistory.map((h) => `${h.createdDate} · ${h.fortuneScore}`).join(' &nbsp;→&nbsp; ')
            : '이력 없음';
          return `<tr><td>${player.playerId}</td><td>${player.name}</td><td>${player.birthDate}</td><td>${history}</td></tr>`;
        })
        .join('');
    } catch (error) {
      playersBody.innerHTML = `<tr><td colspan="4">불러오기 실패: ${error.message}</td></tr>`;
    }
  }

  async function loadGames() {
    try {
      const games = await api.adminGetGames();
      if (games.length === 0) {
        gamesBody.innerHTML = '<tr><td colspan="4">아직 진행된 게임이 없습니다.</td></tr>';
        return;
      }
      gamesBody.innerHTML = games
        .map(
          (game) =>
            `<tr><td>${game.gameId}</td><td>${game.selectedName || '(진행 중)'}</td><td>${game.participantCount}</td><td>${game.createdAt}</td></tr>`
        )
        .join('');
    } catch (error) {
      gamesBody.innerHTML = `<tr><td colspan="4">불러오기 실패: ${error.message}</td></tr>`;
    }
  }

  async function loadLogs() {
    try {
      const logs = await api.adminGetLogs();
      if (logs.length === 0) {
        logList.innerHTML = '<div class="log-item">기록된 오류가 없습니다.</div>';
        return;
      }
      logList.innerHTML = logs
        .map((log) => `<div class="log-item"><span class="path">${log.path}</span>${log.message} · ${log.timestamp}</div>`)
        .join('');
    } catch (error) {
      logList.innerHTML = `<div class="log-item">불러오기 실패: ${error.message}</div>`;
    }
  }

  overrideForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const playerId = Number(overrideForm.playerId.value);
    const overrideScore = Number(overrideForm.overrideScore.value);

    overrideStatus.classList.remove('error');
    overrideStatus.textContent = '요청 중...';
    try {
      await api.adminOverrideFortune(playerId, overrideScore);
      // 501 응답은 api.js에서 에러로 처리되므로 여기 도달하면 실제로 구현된 것 — 지금은 도달할 일이 없음.
      overrideStatus.textContent = '적용되었습니다.';
    } catch (error) {
      overrideStatus.classList.add('error');
      overrideStatus.textContent =
        error.status === 501 ? `아직 준비 중인 기능입니다 (${error.message})` : `실패: ${error.message}`;
      loadLogs();
    }
  });

  loadPlayers();
  loadGames();
  loadLogs();
})();
