import http from './http';
import { serviceUrl } from './endpoints';
import type { UserInfo } from '@/types/user';

export interface TeamMember extends UserInfo {
  joinedAt: string;
  lastActive?: string;
  status?: string;
  displayName?: string;
}

export interface TeamRole {
  id: string;
  name: string;
  code?: string;
  status?: string;
}

export interface TeamPageResult {
  records: TeamMember[];
  total: number;
  current: number;
  size: number;
}

export const teamApi = {
  /** GET /user/api/v1/users 分页 — 团队成员列表 */
  pageMembers(params: {
    current?: number;
    size?: number;
    keyword?: string;
    status?: string;
  }): Promise<TeamPageResult> {
    return http
      .get(serviceUrl('user', '/api/v1/users'), { params })
      .then((r: any) => r.data);
  },

  /** GET /user/api/v1/roles/all — 角色下拉列表 */
  listRoles(): Promise<TeamRole[]> {
    return http
      .get(serviceUrl('user', '/api/v1/roles/all'))
      .then((r: any) => (r.data ?? []).map((role: any) => ({
        id: String(role.id),
        name: role.name ?? role.code ?? String(role.id),
        code: role.code,
        status: role.status,
      })))
      .catch(() => [
        { id: '1', name: '管理员', code: 'admin' },
        { id: '2', name: '成员', code: 'common' },
      ]);
  },

  /** POST /user/api/v1/users — 创建用户（邀请） */
  inviteMember(payload: {
    username: string;
    email: string;
    password: string;
    displayName?: string;
  }): Promise<TeamMember> {
    return http
      .post(serviceUrl('user', '/api/v1/users'), {
        username: payload.username,
        email: payload.email,
        password: payload.password,
        nickName: payload.displayName, // 后端字段名为 nickName
      })
      .then((r: any) => r.data);
  },

  /** PUT /user/api/v1/users/{id}/status — 修改用户状态 */
  updateMemberStatus(id: string, status: '1' | '0'): Promise<void> {
    return http
      .put(serviceUrl('user', `/api/v1/users/${id}/status`), null, { params: { status } })
      .then((r: any) => r.data);
  },

  /** PUT /user/api/v1/users/{id}/roles — 修改用户角色 */
  updateMemberRole(id: string, roleIds: string[]): Promise<void> {
    return http
      .put(serviceUrl('user', `/api/v1/users/${id}/roles`), roleIds)
      .then((r: any) => r.data);
  },

  /** DELETE /user/api/v1/users/{id} — 移除用户 */
  removeMember(id: string): Promise<void> {
    return http.delete(serviceUrl('user', `/api/v1/users/${id}`)).then((r: any) => r.data);
  },
};
