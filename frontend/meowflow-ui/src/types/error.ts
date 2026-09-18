/**
 * 前后端错误码对齐定义
 *
 * 与后端 `com.meowflow.common.result.ResultCode` 一一对应。
 * 详见: backend/meowflow/meowflow-common/src/main/java/com/meowflow/common/result/ResultCode.java
 */

export enum ErrorCode {
  // ============ 成功 ============
  SUCCESS = 200,

  // ============ 客户端错误 4xx ============
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  METHOD_NOT_ALLOWED = 405,

  // ============ 服务端错误 5xx ============
  INTERNAL_ERROR = 500,
  SERVICE_UNAVAILABLE = 503,

  // ============ 业务错误 1xxx ============
  BIZ_ERROR = 1000,
  NODE_EXECUTE_ERROR = 1001,
  WORKFLOW_NOT_FOUND = 1002,
  WORKFLOW_EXECUTE_ERROR = 1003,
  PARAM_ERROR = 1004,
  CAPTCHA_INVALID = 1005,
  EMAIL_CODE_INVALID = 1006,

  // ============ 限流熔断 2xxx ============
  RATE_LIMITED = 2001,
  CIRCUIT_BREAKER_OPEN = 2002,
  REQUEST_REPEAT = 2003,

  // ============ 用户/认证 3xxx ============
  USERNAME_EXISTS = 3001,
  USER_NOT_FOUND = 3002,
  PASSWORD_ERROR = 3003,
  TOKEN_INVALID = 3004,
  TOKEN_EXPIRED = 3005,
  ACCOUNT_LOCKED = 3006,
  NOT_LOGGED_IN = 3007,

  // ============ 数据 4xxx ============
  DATA_NOT_FOUND = 4001,
  DATA_ALREADY_EXISTS = 4002,
}

export interface ApiError {
  code: number;
  message: string;
  errorCode?: string;
  details?: Record<string, any>;
  traceId?: string;
  timestamp?: number;
}

export function isSuccess(code: number): boolean {
  return code === ErrorCode.SUCCESS;
}

export function isAuthError(code: number): boolean {
  return (
    code === ErrorCode.UNAUTHORIZED ||
    code === ErrorCode.NOT_LOGGED_IN ||
    code === ErrorCode.TOKEN_INVALID ||
    code === ErrorCode.TOKEN_EXPIRED
  );
}

export function isPermissionError(code: number): boolean {
  return code === ErrorCode.FORBIDDEN;
}
