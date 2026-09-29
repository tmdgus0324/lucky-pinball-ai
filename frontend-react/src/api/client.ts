/**
 * 백엔드(plan/02_api-spec.md) 호출용 얇은 fetch 래퍼 모음.
 * frontend/js/api.js를 타입만 입혀서 옮긴 것 — 함수 시그니처와 동작은 동일하다.
 */
// 배포 시 Vercel 환경변수 VITE_API_BASE(예: https://xxx.onrender.com)로 백엔드 주소를 바꿔
// 끼울 수 있게 했다. 로컬 개발은 값을 안 주면 기존과 동일하게 localhost:8080을 그대로 쓴다.
const API_BASE = `${import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'}/api`;

// 관리자 로그인 토큰. 쿠키 대신 헤더로 보내는 이유는 AdminAuthFilter 주석 참고 —
// 프론트(Vercel)·백엔드(Render)가 서로 다른 도메인이라 쿠키 세션보다 훨씬 단순하다.
// localStorage에 저장해서 새로고침해도 로그인이 유지되게 했다(단, XSS에 뚫리면 토큰도
// 같이 탈취될 수 있다는 절충 — 이 프로젝트는 innerHTML을 안 쓰고 React가 기본적으로
// 이스케이프해서 그 위험을 낮춰뒀다, devhelp/29 참고).
const ADMIN_TOKEN_KEY = 'adminToken';

function getAdminToken(): string | null {
  try {
    return localStorage.getItem(ADMIN_TOKEN_KEY);
  } catch {
    return null;
  }
}

function setAdminToken(token: string | null) {
  try {
    if (token) localStorage.setItem(ADMIN_TOKEN_KEY, token);
    else localStorage.removeItem(ADMIN_TOKEN_KEY);
  } catch {
    // 프라이빗 브라우징 등으로 localStorage를 못 쓰면, 이번 탭에서만 로그인이 유지 안 될 뿐
    // 기능 자체가 죽어서는 안 된다 — 조용히 넘어간다.
  }
}

export class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getAdminToken();
  const response = await fetch(API_BASE + path, {
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
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
  source: FortuneSource;
}

export interface AdminPlayer {
  playerId: number;
  name: string;
  birthDate: string | null;
  fortuneHistory: FortuneHistoryEntry[];
  /** 이 참가자에서 AI를 호출한 적이 있으면 'AI', 기존 데이터만 썼으면 'CACHE', 아직 조회한 적 없으면 null. */
  fortuneSource: FortuneSource | null;
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

export interface ReusablePlayer {
  playerId: number;
  name: string;
  birthDate: string;
  fortuneSource: FortuneSource;
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

  getReusablePlayers(): Promise<ReusablePlayer[]> {
    return request('/players/reusable', { method: 'GET' });
  },

  isAdminLoggedIn(): boolean {
    return getAdminToken() != null;
  },

  async adminLogin(username: string, password: string): Promise<void> {
    const { token } = await request<{ token: string }>('/admin/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    setAdminToken(token);
  },

  async adminLogout(): Promise<void> {
    try {
      await request('/admin/logout', { method: 'POST' });
    } finally {
      // 서버 호출이 실패하더라도(네트워크 오류 등) 이 브라우저에서는 확실히 로그아웃 처리한다.
      setAdminToken(null);
    }
  },
};
