/**
 * stores/user.spec.ts — 用户状态管理单元测试
 *
 * 覆盖：bootstrap、loginWithCredentials、logout、hasPermission
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useUserStore } from '../stores/user';
import * as authUtils from '../utils/auth';
import * as notifyUtils from '../utils/notify';

// Mock modules
vi.mock('../utils/auth', () => ({
  getAccessToken: vi.fn(),
  getStoredJson: vi.fn(),
  setStoredJson: vi.fn(),
  setTokens: vi.fn(),
  clearAuth: vi.fn(),
  makeMockToken: vi.fn(() => 'mock-token-xxx'),
}));

vi.mock('../utils/notify', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('../api/auth', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
    register: vi.fn(),
    sendEmailCode: vi.fn(),
    resetPassword: vi.fn(),
  },
}));

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe('bootstrap', () => {
    it('should clear user when no token exists', async () => {
      vi.mocked(authUtils.getAccessToken).mockReturnValue(null);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.user).toBeNull();
      expect(store.roles).toEqual([]);
      expect(store.permissions).toEqual([]);
    });

    it('should use cached user when token exists', async () => {
      const cachedUser = {
        id: 'u-001',
        username: 'test',
        nickname: '测试用户',
        roles: ['admin'],
        permissions: ['workflow:view'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('valid-token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.user?.nickname).toBe('测试用户');
      expect(store.roles).toContain('admin');
    });

    it('should fetch user when token exists but no cached user', async () => {
      const fetchedUser = {
        id: 'u-002',
        username: 'fetched',
        nickname: '已获取用户',
        roles: ['common'],
        permissions: ['log:view'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('valid-token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(null);
      const { authApi } = await import('../api/auth');
      vi.mocked(authApi.getCurrentUser).mockResolvedValue(fetchedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.user?.nickname).toBe('已获取用户');
      expect(store.permissions).toContain('log:view');
    });
  });

  describe('hasPermission / hasRole', () => {
    it('should check permissions correctly', async () => {
      const cachedUser = {
        id: 'u-003',
        username: 'perm-test',
        nickname: '权限测试',
        roles: ['admin'],
        permissions: ['workflow:view', 'workflow:edit'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.hasPermission('workflow:view')).toBe(true);
      expect(store.hasPermission('workflow:publish')).toBe(false);
      expect(store.hasAnyPermission(['workflow:view', 'log:view'])).toBe(true);
      expect(store.hasAnyPermission(['dashboard:view'])).toBe(false);
      expect(store.hasAllPermissions(['workflow:view', 'workflow:edit'])).toBe(true);
      expect(store.hasAllPermissions(['workflow:view', 'workflow:publish'])).toBe(false);
    });

    it('should check roles correctly', async () => {
      const cachedUser = {
        id: 'u-004',
        username: 'role-test',
        nickname: '角色测试',
        roles: ['admin', 'common'],
        permissions: ['dashboard'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.hasRole('admin')).toBe(true);
      expect(store.hasRole('super_admin')).toBe(false);
    });
  });

  describe('isAdmin', () => {
    it('should return true for admin role', async () => {
      const cachedUser = {
        id: 'u-005',
        username: 'admin',
        nickname: '管理员',
        roles: ['admin'],
        permissions: ['dashboard'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.isAdmin).toBe(true);
    });

    it('should return true for super_admin role', async () => {
      const cachedUser = {
        id: 'u-006',
        username: 'super',
        nickname: '超级管理员',
        roles: ['super_admin'],
        permissions: ['dashboard'],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.isAdmin).toBe(true);
    });

    it('should return false for common role', async () => {
      const cachedUser = {
        id: 'u-007',
        username: 'normal',
        nickname: '普通用户',
        roles: ['common'],
        permissions: [],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const store = useUserStore();
      await store.bootstrap();
      expect(store.isAdmin).toBe(false);
    });
  });

  describe('logout', () => {
    it('should clear user and dispatch event', async () => {
      const cachedUser = {
        id: 'u-008',
        username: 'logout-test',
        nickname: '登出测试',
        roles: ['common'],
        permissions: [],
      };
      vi.mocked(authUtils.getAccessToken).mockReturnValue('token');
      vi.mocked(authUtils.getStoredJson).mockReturnValue(cachedUser);
      const { authApi } = await import('../api/auth');
      vi.mocked(authApi.logout).mockResolvedValue(undefined);
      const store = useUserStore();
      await store.bootstrap();
      const eventSpy = vi.spyOn(window, 'dispatchEvent');
      await store.logout();
      expect(authUtils.clearAuth).toHaveBeenCalled();
      expect(store.user).toBeNull();
      expect(eventSpy).toHaveBeenCalledWith(expect.objectContaining({ type: 'app:auth-cleared' }));
    });
  });
});
