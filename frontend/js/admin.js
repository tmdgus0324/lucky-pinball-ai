/**
 * admin.html 전용 스크립트 — 조회 3종(참가자·게임·오류 로그).
 */
(function () {
  const playersBody = document.getElementById('playersTableBody');
  const gamesBody = document.getElementById('gamesTableBody');
  const logList = document.getElementById('logList');

  // 참가자 이름·당첨자 이름은 사용자 입력이라, innerHTML 템플릿에 그대로 넣으면 태그를
  // 심어 스크립트를 실행시킬 수 있다(XSS). 구조는 유지하고 이 값들만 이스케이프한다.
  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

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
          return `<tr><td>${player.playerId}</td><td>${escapeHtml(player.name)}</td><td>${player.birthDate || '미입력'}</td><td>${history}</td></tr>`;
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
            `<tr><td>${game.gameId}</td><td>${game.selectedName ? escapeHtml(game.selectedName) : '(진행 중)'}</td><td>${game.participantCount}</td><td>${game.createdAt}</td></tr>`
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
        .map(
          (log) =>
            `<div class="log-item"><span class="path">${escapeHtml(log.path)}</span>${escapeHtml(log.message)} · ${log.timestamp}</div>`,
        )
        .join('');
    } catch (error) {
      logList.innerHTML = `<div class="log-item">불러오기 실패: ${error.message}</div>`;
    }
  }

  loadPlayers();
  loadGames();
  loadLogs();
})();
