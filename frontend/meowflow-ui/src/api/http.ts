/**
 * Axios 实例与拦截器
 *
 * 契约：后端所有接口返回统一 envelope：
 *   { code: 200, message: "ok", data: {...}, timestamp, traceId }
 *   { code: 3xxx, message: "业务错误", data: null, ... }
 *
 * - 响应拦截器：仅当 code === 200 时放行业务 data；其余走错误分支。
 * - 401 处理：尝试 refreshAccessToken()，成功后重放请求；失败清 tokens 并跳转登录页。
 * - 队列机制：多个并发 401 共享一次刷新。
 */

import axios, {
  AxiosError,
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from '@/utils/notify';
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearAuth,
} from '@/utils/auth';
import { ErrorCode, isAuthError } from '@/types/error';
import { serviceUrl } from './endpoints';
import router from '@/router';

// =============================================================================
// 环境配置
// =============================================================================

/** 网关地址。所有 API 调用都会通过 gateway 转发到对应微服务。 */
const BASE_URL = import.meta.env.VITE_GATEWAY_BASE_URL || '';

const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

// =============================================================================
// 业务错误提示
// =============================================================================

/**
 * 把后端返回的业务错误码映射成用户可读的提示。
 *
 * <p>后端有两种失败表达方式：
 * - HTTP 4xx/5xx（走响应拦截器的 error 分支）
 * - **HTTP 200 + 响应体里的 code !== 200**（业务错误，走 success 分支）
 *
 * 两处都必须调用本函数，否则后一种情况的 message 会被丢掉 —— 表现就是
 * 接口明明返回了"验证码错误或已过期"，页面上却什么都不提示
 * （登录接口校验验证码失败时正是这条路径）。
 */
function notifyBusinessError(code: number, message?: string): void {
  const msg = message || '操作失败';
  if (code === ErrorCode.FORBIDDEN) {
    ElMessage.error('没有权限访问该资源');
  } else if (
    code === ErrorCode.NOT_FOUND ||
    code === ErrorCode.USER_NOT_FOUND ||
    code === ErrorCode.DATA_NOT_FOUND ||
    code === ErrorCode.WORKFLOW_NOT_FOUND
  ) {
    ElMessage.error('资源不存在');
  } else if (code === ErrorCode.RATE_LIMITED) {
    ElMessage.warning('请求过于频繁，请稍后再试');
  } else if (code === ErrorCode.CIRCUIT_BREAKER_OPEN) {
    ElMessage.warning('服务熔断中，请稍后重试');
  } else if (code === ErrorCode.ACCOUNT_LOCKED) {
    ElMessage.error('账号已被锁定，请联系管理员');
  } else {
    // 其余业务错误（含 CAPTCHA_INVALID / EMAIL_CODE_INVALID / 参数校验等）
    // 直接显示后端消息，后端文案已经是面向用户的。
    ElMessage.error(msg);
  }
}

// =============================================================================
// Token 刷新队列
// =============================================================================

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

function subscribeTokenRefresh(cb: (token: string) => void): void {
  refreshSubscribers.push(cb);
}

function onTokenRefreshed(newAccessToken: string): void {
  refreshSubscribers.forEach((cb) => cb(newAccessToken));
  refreshSubscribers = [];
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('no refresh token');
  const { data } = await axios.post(
    BASE_URL + serviceUrl('user', '/api/v1/auth/refresh', '/auth/refresh'),
    null,
    { params: { refreshToken } },
  );
  // 后端返回 { code: 200, data: LoginResponse }
  const payload = data?.data ?? data;
  const newAccess = payload?.accessToken;
  const newRefresh = payload?.refreshToken;
  if (!newAccess || !newRefresh) throw new Error('invalid refresh response');
  setTokens(newAccess, newRefresh);
  return newAccess;
}

// =============================================================================
// 401 处理：刷新 + 重放
// =============================================================================

function redirectToLogin() {
  clearAuth();
  router.replace({
    name: 'Login',
    query: { redirect: router.currentRoute.value.fullPath },
  });
}

async function handleAuthError(error: AxiosError): Promise<unknown> {
  const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
  const businessCode = (error.response?.data as any)?.code;

  const needsRefresh =
    (error.response?.status === 401 && !!getRefreshToken()) ||
    (error.response?.status === undefined &&
      businessCode !== undefined &&
      isAuthError(businessCode) &&
      !!getRefreshToken());

  if (!needsRefresh) {
    redirectToLogin();
    return Promise.reject(error);
  }

  if (!config || config._retry) {
    redirectToLogin();
    return Promise.reject(error);
  }

  config._retry = true;

  if (!isRefreshing) {
    isRefreshing = true;
    try {
      const newAccess = await refreshAccessToken();
      isRefreshing = false;
      onTokenRefreshed(newAccess);
      (config.headers as any).Authorization = `Bearer ${newAccess}`;
      return http(config);
    } catch {
      isRefreshing = false;
      refreshSubscribers = [];
      redirectToLogin();
      return Promise.reject(error);
    }
  }

  return new Promise((resolve, reject) => {
    subscribeTokenRefresh((newToken: string) => {
      try {
        (config.headers as any).Authorization = `Bearer ${newToken}`;
        resolve(http(config));
      } catch (e) {
        reject(e);
      }
    });
  });
}

// =============================================================================
// 拦截器
// =============================================================================

http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token) {
      config.headers.set?.('Authorization', `Bearer ${token}`);
      (config.headers as any).Authorization = `Bearer ${token}`;
    }
    (config.headers as any)['X-Trace-ID'] =
      `trace-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const envelope = response.data;
    // 后端统一信封：成功是 { code, message, data, timestamp, traceId, success }，
    // 而**失败响应里没有 data 字段**（只有 code/message/timestamp/traceId/success）。
    // 因此这里只能用 code 判断是不是信封 —— 曾经的判断条件带了 `'data' in envelope`，
    // 导致所有业务错误都不匹配：既不提示消息、也不 reject，
    // 调用方拿到的是原始信封而不是数据（读 data 全是 undefined）。
    if (
      envelope &&
      typeof envelope === 'object' &&
      typeof envelope.code === 'number'
    ) {
      if (envelope.code !== 200) {
        // 认证类错误交给下面的 error 分支去刷新 / 跳登录，这里只负责其余业务错误的提示。
        // 注意：CAPTCHA_INVALID(1005) 等不属于认证错误，绝不能触发跳登录，
        // 否则用户在登录页输错验证码就会被反复重定向。
        if (!isAuthError(envelope.code)) {
          notifyBusinessError(envelope.code, envelope.message);
        }
        const err = new AxiosError(
          envelope.message || '请求失败',
          'ERR_BAD_RESPONSE',
          response.config,
          response.request,
          response,
        );
        (err as any).response = response;
        return Promise.reject(err);
      }
      response.data = envelope.data;
    }
    return response;
  },
  async (error: AxiosError) => {
    const status = error.response?.status;
    const businessCode = (error.response?.data as any)?.code;
    const businessMessage = (error.response?.data as any)?.message;

    if (
      status === 401 ||
      (status === undefined &&
        businessCode !== undefined &&
        isAuthError(businessCode))
    ) {
      return handleAuthError(error);
    }

    if (businessCode !== undefined) {
      if (businessCode !== ErrorCode.SUCCESS) {
        notifyBusinessError(businessCode, businessMessage);
      }
    } else if (status && status >= 500) {
      ElMessage.error('服务器异常，请稍后再试');
    }

    return Promise.reject(error);
  },
);

export default http;
