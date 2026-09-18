/**
 * 用户状态管理（Pinia Store）
 *
 * 设计原则：
 * - 单一真相：user/roles/permissions 从登录/注册/刷新后端一次性带回，写入 store + localStorage。
 * - 启动引导：bootstrap() 读取 token，有 token 但无 user 时调 /auth/me 回填。
 * - 权限消费：hasRole / hasPermission / hasAnyPermission 供 v-permission 指令和组件调用。
 * - 退出：清 tokens、user、STORAGE_KEYS.USER；广播 app:auth-cleared 事件给其他 store。
 */

import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  getAccessToken,
  getStoredJson,
  setStoredJson,
  setTokens,
  clearAuth,
  makeMockToken,
} from '@/utils/auth';
import { STORAGE_KEYS } from '@/utils/constants';
import { ElMessage } from '@/utils/notify';
import { authApi } from '@/api/auth';
import { MOCK_USER } from '@/mock/users';
import type { UserInfo } from '@/types/user';
import type { UserDTO } from '@/api/auth';

// mock 模式 = 启动时通过 VITE_USE_MOCK 控制
const USE_MOCK_LOGIN = import.meta.env.VITE_USE_MOCK === 'true';

export const useUserStore = defineStore('user', () => {
  // ---------- 状态 ----------
  const user = ref<UserInfo | null>(null);
  const roles = ref<string[]>([]);
  const permissions = ref<string[]>([]);
  // 当前操作的组织 id（默认 = 用户的个人组织）。
  // 切换团队视角时由 UI 调用 switchOrg 更新。
  const currentOrgId = ref<number | null>(null);

  // ---------- 计算属性 ----------
  const isAdmin = computed(
    () => roles.value.includes('admin') || roles.value.includes('super_admin'),
  );
  const isLoggedIn = computed(() => !!user.value && !!getAccessToken());

  // ---------- 内部工具 ----------
  function normalizePermissions(rawPerms: unknown): string[] {
    if (!Array.isArray(rawPerms)) return [];
    return rawPerms.filter(
      (p): p is string => typeof p === 'string' && /^[\w:.-]+$/.test(p),
    );
  }

  function persistAndSetUser(raw: UserDTO) {
    const perms = normalizePermissions(raw.permissions);
    const info: UserInfo = {
      id: String(raw.id ?? (raw as any).userId ?? ''),
      username: raw.username ?? '',
      nickname: raw.nickname ?? (raw as any).nickName ?? raw.username ?? '',
      email: raw.email ?? '',
      avatar: raw.avatar,
      role: primaryRole(raw.roles),
      roles: raw.roles ?? [],
      permissions: perms,
      orgId: raw.orgId != null ? Number(raw.orgId) : undefined,
      orgIds: (raw.orgIds ?? []).map((n) => Number(n)),
    };
    user.value = info;
    roles.value = raw.roles ?? [];
    permissions.value = perms;
    // 默认进入个人组织
    if (currentOrgId.value == null && info.orgId != null) {
      currentOrgId.value = info.orgId;
    }
    setStoredJson(STORAGE_KEYS.USER, info);
    return info;
  }

  function primaryRole(rolesList?: string[]): string {
    if (!rolesList?.length) return 'user';
    if (rolesList.includes('super_admin')) return 'admin';
    if (rolesList.includes('admin')) return 'admin';
    return rolesList[0] ?? 'user';
  }

  // ---------- 公开方法 ----------

  /**
   * 启动引导：读取本地缓存 + token，必要时调 /auth/me 回填用户信息。
   * 由 main.ts router.isReady().then() 调用。
   */
  async function bootstrap() {
    const token = getAccessToken();
    if (!token) {
      user.value = null;
      roles.value = [];
      permissions.value = [];
      return;
    }
    const cached = getStoredJson<UserInfo | null>(STORAGE_KEYS.USER, null);
    // 缓存中的 permissions 必须有 ≥1 个像权限 code 的条目才视为合法
    // (过滤掉中文 label / 损坏数据)
    const cachedCodePerms = normalizePermissions(cached?.permissions);
    if (cached && cachedCodePerms.length > 0) {
      persistAndSetUser(cached as unknown as UserDTO);
      return;
    }
    // 缓存缺失/无有效 permissions → 回退
    try {
      if (USE_MOCK_LOGIN) {
        persistAndSetUser(MOCK_USER as unknown as UserDTO);
      } else {
        const resp = await authApi.getCurrentUser();
        persistAndSetUser(resp);
      }
    } catch {
      clearAuth();
      user.value = null;
      roles.value = [];
      permissions.value = [];
    }
  }

  /**
   * 用户名密码登录（可选图形验证码）
   */
  async function loginWithCredentials(
    username: string,
    password: string,
    code?: string,
    uuid?: string,
  ) {
    const resp = await authApi.login({ username, password, code, uuid });
    setTokens(resp.accessToken, resp.refreshToken);
    persistAndSetUser(resp.user);
    ElMessage.success('登录成功');
  }

  /**
   * Mock 一键登录（开发 / 演示用）
   */
  async function login() {
    const access = makeMockToken('access-token');
    const refresh = makeMockToken('refresh-token');
    setTokens(access, refresh);
    persistAndSetUser(MOCK_USER as unknown as UserDTO);
    ElMessage.success('已自动登录 (Mock)');
  }

  /**
   * 登出：调用后端 logout → 清本地缓存 → 广播 app:auth-cleared 事件。
   * 返回 true，由调用方决定是否跳转。
   */
  async function logout(): Promise<boolean> {
    try {
      await authApi.logout();
    } catch {
      // ignore
    }
    clearAuth();
    user.value = null;
    roles.value = [];
    permissions.value = [];
    currentOrgId.value = null;
    window.dispatchEvent(new CustomEvent('app:auth-cleared'));
    return true;
  }

  /**
   * 更新本地 user（编辑资料后调用）
   */
  function updateProfile(payload: Partial<UserInfo>) {
    if (!user.value) return;
    const updated = { ...user.value, ...payload };
    user.value = updated;
    setStoredJson(STORAGE_KEYS.USER, updated);
  }

  /**
   * 用户注册（成功后直接登录）
   */
  async function register(payload: {
    username: string;
    password: string;
    nickName: string;
    email: string;
    phone?: string;
    code: string;
    uuid: string;
    emailCode: string;
    agree: boolean;
  }) {
    const resp = await authApi.register({
      username: payload.username,
      password: payload.password,
      nickName: payload.nickName,
      email: payload.email,
      phone: payload.phone,
      captchaCode: payload.code,
      uuid: payload.uuid,
      emailCode: payload.emailCode,
      agree: payload.agree,
    });
    setTokens(resp.accessToken, resp.refreshToken);
    persistAndSetUser(resp.user);
    ElMessage.success('注册成功');
  }

  /**
   * 发送邮箱验证码（type: 'REGISTER' | 'RESET_PWD'）
   */
  async function sendEmailCode(email: string, type: 'REGISTER' | 'RESET_PWD') {
    await authApi.sendEmailCode({ email, type });
    ElMessage.success('验证码已发送');
  }

  /**
   * 通过邮箱验证码重置密码（含图形验证码）
   */
  async function resetPasswordByEmail(
    email: string,
    emailCode: string,
    newPassword: string,
    captchaCode?: string,
    uuid?: string,
  ) {
    await authApi.resetPassword({
      email,
      code: emailCode,
      newPassword,
      captchaCode,
      uuid,
    });
    ElMessage.success('密码重置成功，请使用新密码登录');
  }

  // ---------- 权限检查 ----------
  function hasRole(role: string): boolean {
    return roles.value.includes(role);
  }

  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm);
  }

  function hasAnyPermission(perms: string[]): boolean {
    if (!perms?.length) return true;
    return perms.some((p) => permissions.value.includes(p));
  }

  function hasAllPermissions(perms: string[]): boolean {
    if (!perms?.length) return true;
    return perms.every((p) => permissions.value.includes(p));
  }

  /**
   * 切换当前操作组织（个人 ↔ 团队）。
   * 应当只切换到 user.orgIds 中存在的 id；
   * 非法值会被忽略,以避免误操作。
   */
  function switchOrg(orgId: number | null) {
    if (orgId == null) {
      // 切回个人组织
      currentOrgId.value = user.value?.orgId ?? null;
      return;
    }
    if (user.value?.orgIds?.includes(orgId)) {
      currentOrgId.value = orgId;
    } else {
      // 静默回退
      currentOrgId.value = user.value?.orgId ?? null;
    }
  }

  // ---------- 导出 ----------
  return {
    user,
    roles,
    permissions,
    currentOrgId,
    isAdmin,
    isLoggedIn,
    bootstrap,
    login,
    loginWithCredentials,
    logout,
    updateProfile,
    register,
    sendEmailCode,
    resetPasswordByEmail,
    hasRole,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    switchOrg,
  };
});
