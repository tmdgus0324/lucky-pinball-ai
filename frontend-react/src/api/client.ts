/**
 * 백엔드(plan/02_api-spec.md) 호출용 얇은 fetch 래퍼 모음.
 * frontend/js/api.js를 타입만 입혀서 옮긴 것 — 함수 시그니처와 동작은 동일하다.
 */
// 배포 시 Vercel 환경변수 VITE_API_BASE(예: https://xxx.onrender.com)로 백엔드 주소를 바꿔
// 끼울 수 있게 했다. 로컬 개발은 값을 안 주면 기존과 동일하게 localhost:8080을 그대로 쓴다.
const API_BASE = `${import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'}/api`;

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    ...options,
  });

  if (!response.ok) {
    let message = `HTTP ${response.status}`;
    try {
      const body = await response.json();
      if (body && body.error) message = body.error;
    } catch {
      // 응답 바디가 JSON이 아닌 경우 상태 코드만 사용
    }
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) return null as T;
  return response.json() as Promise<T>;
}

export interface Player {
  playerId: number;
  name: string;
  birthDate: string | null;
}

export interface Buff {
  tier: number;
  startY: number;
}

export type FortuneSource = 'AI' | 'CACHE' | 'NONE';

export interface FortuneResponse {
  playerId: number;
  fortuneScore: number | null;
  luckyNumber: number | null;
  fortuneMessage: string | null;
  buff: Buff;
  source: FortuneSource;
}

export interface GameParticipant {
  playerId: number;
  name: string;
  fortuneScore: number | null;
  luckyNumber: number | null;
  fortuneMessage: string | null;
  buff: Buff;
}

export interface GameCreateResponse {
  gameId: string;
  participants: GameParticipant[];
}

export interface GameStartResponse {
  gameId: string;
  status: string;
  startedAt: string;
}

export interface RankEntry {
  rank: number;
  playerId: number;
  name: string;
}

export interface GameResultView {
  gameId: string;
  ranking: RankEntry[];
  selectedName: string;
  participantCount: number;
  createdAt: string;
}

export interface FortuneHistoryEntry {
  createdDate: string;
  fortuneScore: number;
}

export interface AdminPlayer {
  playerId: number;
  name: string;
  birthDate: string | null;
  fortuneHistory: FortuneHistoryEntry[];
}

export interface AdminGame {
  gameId: string;
  selectedName: string | null;
  participantCount: number;
  createdAt: string;
}

export interface AdminLog {
  path: string;
  message: string;
  timestamp: string;
}

export const api = {
  registerPlayer(name: string, birthDate: string | null): Promise<Player> {
    return request('/player', {
      method: 'POST',
      body: JSON.stringify({ name, birthDate }),
    });
  },

  getFortune(playerId: number): Promise<FortuneResponse> {
    return request('/fortune', {
      method: 'POST',
      body: JSON.stringify({ playerId }),
    });
  },

  createGame(playerIds: number[]): Promise<GameCreateResponse> {
    return request('/game/create', {
      method: 'POST',
      body: JSON.stringify({ playerIds }),
    });
  },

  startGame(gameId: string): Promise<GameStartResponse> {
    return request('/game/start', {
      method: 'POST',
      body: JSON.stringify({ gameId }),
    });
  },

  reportResult(gameId: string, finishOrder: number[]): Promise<GameResultView> {
    return request('/game/result', {
      method: 'POST',
      body: JSON.stringify({ gameId, finishOrder }),
    });
  },

  getResult(gameId: string): Promise<GameResultView> {
    return request(`/game/result/${gameId}`, { method: 'GET' });
  },

  adminGetPlayers(): Promise<AdminPlayer[]> {
    return request('/admin/players', { method: 'GET' });
  },

  adminGetGames(): Promise<AdminGame[]> {
    return request('/admin/games', { method: 'GET' });
  },

  adminGetLogs(): Promise<AdminLog[]> {
    return request('/admin/logs', { method: 'GET' });
  },

  adminOverrideFortune(playerId: number, overrideScore: number): Promise<void> {
    return request('/admin/fortune/override', {
      method: 'POST',
      body: JSON.stringify({ playerId, overrideScore }),
    });
  },
};
