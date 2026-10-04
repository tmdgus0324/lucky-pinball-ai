/**
 * 백엔드(plan/02_api-spec.md) 호출용 얇은 fetch 래퍼 모음.
 * 이 파일만 보면 프론트엔드가 서버와 어떤 계약(요청/응답 모양)을 주고받는지 한눈에 알 수 있다.
 */
const API_BASE = 'http://localhost:8080/api';

async function request(path, options) {
  const response = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    ...options,
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try {
      const body = await response.json();
      if (body && body.error) message = body.error;
    } catch (ignored) {
      // 응답 바디가 JSON이 아닌 경우 상태 코드만 사용
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

const api = {
  registerPlayer(name, birthDate) {
    return request('/player', {
      method: 'POST',
      body: JSON.stringify({ name, birthDate }),
    });
  },

  getFortune(playerId) {
    return request('/fortune', {
      method: 'POST',
      body: JSON.stringify({ playerId }),
    });
  },

  createGame(playerIds) {
    return request('/game/create', {
      method: 'POST',
      body: JSON.stringify({ playerIds }),
    });
  },

  startGame(gameId) {
    return request('/game/start', {
      method: 'POST',
      body: JSON.stringify({ gameId }),
    });
  },

  reportResult(gameId, finishOrder) {
    return request('/game/result', {
      method: 'POST',
      body: JSON.stringify({ gameId, finishOrder }),
    });
  },

  getResult(gameId) {
    return request(`/game/result/${gameId}`, { method: 'GET' });
  },

  adminGetPlayers() {
    return request('/admin/players', { method: 'GET' });
  },

  adminGetGames() {
    return request('/admin/games', { method: 'GET' });
  },

  adminGetLogs() {
    return request('/admin/logs', { method: 'GET' });
  },
};
