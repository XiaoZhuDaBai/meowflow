import { STORAGE_KEYS } from './constants';

// =============================================================================
// 兼容旧版：保持原有 `meowflow.token` 单 token 接口
// =============================================================================

export function getToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.TOKEN);
}

export function setToken(token: string): void {
  localStorage.setItem(STORAGE_KEYS.TOKEN, token);
}

export function clearToken(): void {
  localStorage.removeItem(STORAGE_KEYS.TOKEN);
}

// =============================================================================
// SaToken 双 token 接口（与后端对齐）
// =============================================================================

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

export function getAccessToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
  localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
  // 兼容旧 token 取值入口
  localStorage.setItem(STORAGE_KEYS.TOKEN, accessToken);
}

export function clearAuth(): void {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
  localStorage.removeItem(STORAGE_KEYS.TOKEN);
  localStorage.removeItem(STORAGE_KEYS.USER);
}

export function getStoredJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function setStoredJson(key: string, value: unknown): void {
  localStorage.setItem(key, JSON.stringify(value));
}

export function resetAllMockStorage(): void {
  Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k));
}

/** Generate a JWT-like mock token so the API layer is happy. */
export function makeMockToken(prefix = 'mock-token'): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}
