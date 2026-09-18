/**
 * 用户管理 API（与后端 /api/v1/users 对齐）
 *
 * 注意：GET /api/v1/auth/me 已整合到 authApi.getCurrentUser()，
 * 此处 me() 仅作转发兼容，避免直接引用 UserDTO 类型。
 */

import http from './http';
import { authApi } from './auth';
import type { UserInfo } from '@/types/user';
import { serviceUrl } from './endpoints';

export const userApi = {
  /** GET /api/v1/auth/me — 转发 authApi.getCurrentUser() */
  me(): Promise<UserInfo> {
    return authApi.getCurrentUser() as Promise<UserInfo>;
  },

  /** PUT /api/v1/users/{id} — 更新用户资料 */
  updateProfile(id: string, payload: Partial<UserInfo>): Promise<UserInfo> {
    // 后端字段名为 nickName（非 nickname），需要映射
    const { nickname, ...rest } = payload;
    return http
      .put(serviceUrl('user', `/api/v1/users/${id}`), {
        ...rest,
        ...(nickname !== undefined ? { nickName: nickname } : {}),
      })
      .then((r: any) => r.data);
  },

  /** PUT /api/v1/users/{id}/password — 修改密码 */
  changePassword(id: string, oldPassword: string, newPassword: string): Promise<void> {
    return http
      .put(
        serviceUrl('user', `/api/v1/users/${id}/password`),
        { oldPassword, newPassword },
      )
      .then((r: any) => r.data);
  },

  /** GET /api/v1/users 分页 — 团队成员列表 */
  pageUsers(params: {
    current?: number;
    size?: number;
    page?: number;
    pageSize?: number;
    keyword?: string;
    status?: string;
    departmentId?: string;
  }): Promise<{ records: UserInfo[]; total: number; current: number; size: number }> {
    const query: Record<string, any> = {};
    if (params.keyword) query.keyword = params.keyword;
    if (params.status) query.status = params.status;
    if (params.departmentId) query.departmentId = params.departmentId;
    query.current = params.current ?? params.page ?? 1;
    query.size = params.size ?? params.pageSize ?? 20;
    return http
      .get(serviceUrl('user', '/api/v1/users'), { params: query })
      .then((r: any) => r.data);
  },

  /** POST /api/v1/users — 创建用户（邀请） */
  createUser(payload: {
    username: string;
    email: string;
    password: string;
    nickname?: string;
  }): Promise<UserInfo> {
    return http
      .post(serviceUrl('user', '/api/v1/users'), payload)
      .then((r: any) => r.data);
  },

  /** PUT /api/v1/users/{id}/status — 修改用户状态 */
  updateStatus(id: string, status: string): Promise<void> {
    return http
      .put(
        serviceUrl('user', `/api/v1/users/${id}/status`),
        null,
        { params: { status } },
      )
      .then((r: any) => r.data);
  },

  /** DELETE /api/v1/users/{id} — 移除用户 */
  removeUser(id: string): Promise<void> {
    return http
      .delete(serviceUrl('user', `/api/v1/users/${id}`))
      .then((r: any) => r.data);
  },
};
