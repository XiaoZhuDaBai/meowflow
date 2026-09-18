package com.meowflow.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    BIZ_ERROR(1000, "业务处理失败"),
    NODE_EXECUTE_ERROR(1001, "节点执行失败"),
    WORKFLOW_NOT_FOUND(1002, "工作流不存在"),
    WORKFLOW_EXECUTE_ERROR(1003, "工作流执行失败"),
    PARAM_ERROR(1004, "参数错误"),
    CAPTCHA_INVALID(1005, "验证码错误或已过期"),
    EMAIL_CODE_INVALID(1006, "邮箱验证码错误或已过期"),

    RATE_LIMITED(2001, "请求过于频繁"),
    CIRCUIT_BREAKER_OPEN(2002, "服务熔断中，请稍后重试"),
    REQUEST_REPEAT(2003, "请求重复，请勿重复提交"),

    USERNAME_EXISTS(3001, "用户名已存在"),
    USER_NOT_FOUND(3002, "用户不存在"),
    PASSWORD_ERROR(3003, "密码错误"),
    TOKEN_INVALID(3004, "Token 无效"),
    TOKEN_EXPIRED(3005, "Token 已过期"),
    ACCOUNT_LOCKED(3006, "账号已被锁定"),
    NOT_LOGGED_IN(3007, "未登录"),

    DATA_NOT_FOUND(4001, "数据不存在"),
    DATA_ALREADY_EXISTS(4002, "数据已存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}