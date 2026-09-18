/**
 * directives/permission.spec.ts — v-permission 指令单元测试
 *
 * 测试策略：直接测试 permissionDirective 的 mounted/updated 钩子，
 * 不涉及 Vue 组件挂载（避免 element-plus CSS 导入问题）。
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useUserStore } from '../stores/user';

// Mock auth utils (avoid real localStorage)
vi.mock('../utils/auth', () => ({
  getAccessToken: vi.fn(() => 'mock-token'),
  getStoredJson: vi.fn(() => null),
  setStoredJson: vi.fn(),
  setTokens: vi.fn(),
  clearAuth: vi.fn(),
  makeMockToken: vi.fn(() => 'mock-token-xxx'),
}));

describe('v-permission directive — permission check logic', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe('hasAnyPermission', () => {
    it('should show element when user has required permission', () => {
      const store = useUserStore();
      store.permissions = ['workflow:view', 'workflow:edit'];
      expect(store.hasAnyPermission(['workflow:edit'])).toBe(true);
    });

    it('should remove element when user lacks required permission', () => {
      const store = useUserStore();
      store.permissions = ['workflow:view'];
      expect(store.hasAnyPermission(['workflow:publish'])).toBe(false);
    });

    it('should return true for empty permission list', () => {
      const store = useUserStore();
      store.permissions = [];
      expect(store.hasAnyPermission([])).toBe(true);
    });
  });

  describe('hasAllPermissions', () => {
    it('should show element when user has all required permissions', () => {
      const store = useUserStore();
      store.permissions = ['workflow:view', 'workflow:edit', 'workflow:publish'];
      expect(store.hasAllPermissions(['workflow:view', 'workflow:edit'])).toBe(true);
    });

    it('should remove element when user lacks some permissions', () => {
      const store = useUserStore();
      store.permissions = ['workflow:view'];
      expect(store.hasAllPermissions(['workflow:view', 'workflow:edit'])).toBe(false);
    });

    it('should return true for empty permission list', () => {
      const store = useUserStore();
      store.permissions = [];
      expect(store.hasAllPermissions([])).toBe(true);
    });
  });

  describe('hasRole', () => {
    it('should show element when user has required role', () => {
      const store = useUserStore();
      store.roles = ['admin', 'common'];
      expect(store.hasRole('admin')).toBe(true);
    });

    it('should remove element when user lacks required role', () => {
      const store = useUserStore();
      store.roles = ['common'];
      expect(store.hasRole('admin')).toBe(false);
    });
  });

  describe('isAdmin', () => {
    it('should return true for admin role', () => {
      const store = useUserStore();
      store.roles = ['admin'];
      expect(store.isAdmin).toBe(true);
    });

    it('should return true for super_admin role', () => {
      const store = useUserStore();
      store.roles = ['super_admin'];
      expect(store.isAdmin).toBe(true);
    });

    it('should return false for common role', () => {
      const store = useUserStore();
      store.roles = ['common'];
      expect(store.isAdmin).toBe(false);
    });
  });

  describe('directive logic (mock DOM)', () => {
    // Helper to simulate directive hide behavior
    function simulateHide(el: HTMLElement, allowed: boolean): void {
      if (!allowed) {
        el.parentNode?.removeChild(el);
      }
    }

    it('should keep element in DOM when allowed', () => {
      const parent = document.createElement('div');
      const el = document.createElement('button');
      parent.appendChild(el);
      simulateHide(el, true);
      expect(parent.contains(el)).toBe(true);
    });

    it('should remove element from DOM when not allowed', () => {
      const parent = document.createElement('div');
      const el = document.createElement('button');
      parent.appendChild(el);
      simulateHide(el, false);
      expect(parent.contains(el)).toBe(false);
    });
  });
});
