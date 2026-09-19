/**
 * api/http.ts 拦截器测试
 *
 * 重点覆盖两类容易丢提示的路径：
 * - HTTP 200 + 响应体 code !== 200（业务错误走的是 **success** 分支，
 *   曾经不弹任何提示，用户只看到"没反应"）
 * - CAPTCHA_INVALID 这类非认证业务错误不得触发跳登录
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { AxiosInstance, AxiosResponse } from 'axios';
import { ErrorCode } from '@/types/error';

const messageError = vi.fn();
const messageWarning = vi.fn();

vi.mock('@/utils/notify', () => ({
  ElMessage: {
    error: (...args: unknown[]) => messageError(...args),
    warning: (...args: unknown[]) => messageWarning(...args),
    success: vi.fn(),
  },
}));

vi.mock('@/utils/auth', () => ({
  getAccessToken: vi.fn(() => null),
  getRefreshToken: vi.fn(() => null),
  setTokens: vi.fn(),
  clearAuth: vi.fn(),
}));

const replace = vi.fn();
vi.mock('@/router', () => ({
  default: { replace: (...args: unknown[]) => replace(...args) },
}));

/** 用自定义 adapter 替换掉真实网络层，直接决定响应内容 */
function mockResponse(http: AxiosInstance, body: unknown, status = 200) {
  http.defaults.adapter = async (config): Promise<AxiosResponse> =>
    ({
      data: body,
      status,
      statusText: String(status),
      headers: {},
      config,
    }) as AxiosResponse;
}

async function loadHttp() {
  const mod = await import('./http');
  return mod.default;
}

describe('http 响应拦截器', () => {
  beforeEach(() => {
    vi.resetModules();
    messageError.mockClear();
    messageWarning.mockClear();
    replace.mockClear();
  });

  it('HTTP 200 + code=1005：必须把后端消息提示出来', async () => {
    const http = await loadHttp();
    mockResponse(http, {
      code: ErrorCode.CAPTCHA_INVALID,
      message: '验证码错误或已过期',
      success: false,
    });

    await expect(http.get('/user/api/v1/captcha')).rejects.toThrow();

    expect(messageError).toHaveBeenCalledWith('验证码错误或已过期');
  });

  it('HTTP 200 + code=1005：不得跳转登录页（否则输错验证码会被反复重定向）', async () => {
    const http = await loadHttp();
    mockResponse(http, {
      code: ErrorCode.CAPTCHA_INVALID,
      message: '验证码错误或已过期',
      success: false,
    });

    await expect(http.get('/user/api/v1/captcha')).rejects.toThrow();

    expect(replace).not.toHaveBeenCalled();
  });

  it('HTTP 200 + code=4001：按错误码映射为"资源不存在"', async () => {
    const http = await loadHttp();
    mockResponse(http, { code: ErrorCode.DATA_NOT_FOUND, message: '数据不存在', success: false });

    await expect(http.get('/workflow/api/workflow/1')).rejects.toThrow();

    expect(messageError).toHaveBeenCalledWith('资源不存在');
  });

  it('HTTP 200 + code=2000/限流：用 warning 提示', async () => {
    const http = await loadHttp();
    mockResponse(http, { code: ErrorCode.RATE_LIMITED, message: 'too many', success: false });

    await expect(http.get('/workflow/api/workflow')).rejects.toThrow();

    expect(messageWarning).toHaveBeenCalled();
  });

  it('HTTP 200 + code=200：正常放行 data，不弹任何提示', async () => {
    const http = await loadHttp();
    mockResponse(http, { code: 200, message: '操作成功', data: { id: 7 }, success: true });

    const res = await http.get('/workflow/api/workflow/7');

    expect(res.data).toEqual({ id: 7 });
    expect(messageError).not.toHaveBeenCalled();
    expect(messageWarning).not.toHaveBeenCalled();
  });

  it('HTTP 200 + 认证类业务错误：不弹提示（避免登录页弹无意义提示）', async () => {
    const http = await loadHttp();
    mockResponse(http, {
      code: ErrorCode.TOKEN_EXPIRED,
      message: '登录已过期',
      success: false,
    });

    await expect(http.get('/workflow/api/workflow')).rejects.toThrow();

    // 说明：刷新 token / 跳登录（handleAuthError）只在 HTTP 401 或网络层错误时触发，
    // HTTP 200 不会走这条路 —— 后端对未登录/过期返回的是 401，不是 200。
    // 因此这里只断言"不弹业务错误提示"，避免登录页出现无意义弹窗。
    expect(messageError).not.toHaveBeenCalled();
  });

  it('HTTP 200 + 业务错误：reject 出去的 message 与后端一致（调用方可自行处理）', async () => {
    const http = await loadHttp();
    mockResponse(http, {
      code: ErrorCode.CAPTCHA_INVALID,
      message: '验证码错误或已过期',
      success: false,
    });

    await expect(http.get('/user/api/v1/captcha')).rejects.toMatchObject({
      message: '验证码错误或已过期',
    });
  });
});
