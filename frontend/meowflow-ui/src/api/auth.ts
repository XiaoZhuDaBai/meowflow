/**
 * 认证 API（与后端 /api/v1/auth 对齐）
 *
 * 后端契约：
 * - LoginResponse: { accessToken, refreshToken, tokenType, expiresIn, user }
 * - RegisterRequest: { username, password, nickName, email, phone?, captchaCode, uuid, emailCode, agree }
 * - ResetPasswordRequest: { email, code(邮箱验证码), newPassword, captchaCode?, uuid? }
 */

import http from './http';
import { serviceUrl } from './endpoints';

// ---------------------------------------------------------------------------
// DTOs
// ---------------------------------------------------------------------------

export interface LoginRequest {
  username: string;
  password: string;
  code?: string;   // 图形验证码答案
  uuid?: string;   // 图形验证码 UUID
}

/** 登录/注册成功后 data（后端 LoginResponse） */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDTO;
}

/** 刷新成功后 data（与 LoginResponse 同结构） */
export type RefreshResponse = LoginResponse;

/** 当前用户 DTO（与 LoginResponse.user 同结构） */
export interface UserDTO {
  id: number | string;
  username: string;
  nickName?: string;
  nickname?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  sex?: string;
  status?: string;
  orgId?: number | string;
  orgName?: string;
  postId?: number | string;
  postName?: string;
  loginIp?: string;
  loginAt?: string;
  loginDate?: string;
  loginCount?: number;
  roles?: string[];
  permissions?: string[];
  /** 用户所属组织 id 集合（个人 + 加入的团队） */
  orgIds?: number[];
  createTime?: string;
  remark?: string;
}

/** 注册请求 */
export interface RegisterRequest {
  username: string;
  password: string;
  nickName: string;
  email: string;
  phone?: string;
  sex?: string;
  captchaCode: string;
  uuid: string;
  emailCode: string;
  agree: boolean;
}

/** 发送邮箱验证码请求 */
export interface VerifyCodeRequest {
  email: string;
  type: 'REGISTER' | 'RESET_PWD';
}

/** 通过邮箱重置密码请求 */
export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
  captchaCode?: string;
  uuid?: string;
}

// ---------------------------------------------------------------------------
// API
// ---------------------------------------------------------------------------

export const authApi = {
  login(data: LoginRequest): Promise<LoginResponse> {
    return http
      .post(serviceUrl('user', '/api/v1/auth/login', '/auth/login'), data)
      .then((r: any) => r.data);
  },

  refreshToken(refreshToken: string): Promise<RefreshResponse> {
    return http
      .post(
        serviceUrl('user', '/api/v1/auth/refresh', '/auth/refresh'),
        null,
        { params: { refreshToken } },
      )
      .then((r: any) => r.data);
  },

  logout(): Promise<void> {
    return http
      .post(serviceUrl('user', '/api/v1/auth/logout', '/auth/logout'))
      .then((r: any) => r.data);
  },

  getCurrentUser(): Promise<UserDTO> {
    return http
      .get(serviceUrl('user', '/api/v1/auth/me', '/auth/me'))
      .then((r: any) => r.data);
  },

  register(data: RegisterRequest): Promise<LoginResponse> {
    return http
      .post(serviceUrl('user', '/api/v1/auth/register', '/auth/register'), data)
      .then((r: any) => r.data);
  },

  sendEmailCode(data: VerifyCodeRequest): Promise<void> {
    return http
      .post(serviceUrl('user', '/api/v1/auth/email-code', '/auth/email-code'), data)
      .then((r: any) => r.data);
  },

  resetPassword(data: ResetPasswordRequest): Promise<void> {
    return http
      .post(serviceUrl('user', '/api/v1/auth/password/reset', '/auth/password/reset'), data)
      .then((r: any) => r.data);
  },
};
