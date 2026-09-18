package com.meowflow.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultCodeTest {

    @Test
    void success_shouldHaveCorrectCodeAndMessage() {
        assertEquals(200, ResultCode.SUCCESS.getCode());
        assertEquals("操作成功", ResultCode.SUCCESS.getMessage());
    }

    @Test
    void badRequest_shouldHaveCorrectCodeAndMessage() {
        assertEquals(400, ResultCode.BAD_REQUEST.getCode());
        assertEquals("请求参数错误", ResultCode.BAD_REQUEST.getMessage());
    }

    @Test
    void unauthorized_shouldHaveCorrectCodeAndMessage() {
        assertEquals(401, ResultCode.UNAUTHORIZED.getCode());
        assertEquals("未授权", ResultCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void forbidden_shouldHaveCorrectCodeAndMessage() {
        assertEquals(403, ResultCode.FORBIDDEN.getCode());
        assertEquals("禁止访问", ResultCode.FORBIDDEN.getMessage());
    }

    @Test
    void notFound_shouldHaveCorrectCodeAndMessage() {
        assertEquals(404, ResultCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", ResultCode.NOT_FOUND.getMessage());
    }

    @Test
    void internalError_shouldHaveCorrectCodeAndMessage() {
        assertEquals(500, ResultCode.INTERNAL_ERROR.getCode());
        assertEquals("服务器内部错误", ResultCode.INTERNAL_ERROR.getMessage());
    }

    @Test
    void bizError_shouldHaveCorrectCodeAndMessage() {
        assertEquals(1000, ResultCode.BIZ_ERROR.getCode());
        assertEquals("业务处理失败", ResultCode.BIZ_ERROR.getMessage());
    }

    @Test
    void workflowErrors_shouldHaveCorrectCodes() {
        assertEquals(1002, ResultCode.WORKFLOW_NOT_FOUND.getCode());
        assertEquals(1003, ResultCode.WORKFLOW_EXECUTE_ERROR.getCode());
    }

    @Test
    void rateLimitErrors_shouldHaveCorrectCodes() {
        assertEquals(2001, ResultCode.RATE_LIMITED.getCode());
        assertEquals(2002, ResultCode.CIRCUIT_BREAKER_OPEN.getCode());
        assertEquals(2003, ResultCode.REQUEST_REPEAT.getCode());
    }

    @Test
    void userErrors_shouldHaveCorrectCodes() {
        assertEquals(3001, ResultCode.USERNAME_EXISTS.getCode());
        assertEquals(3002, ResultCode.USER_NOT_FOUND.getCode());
        assertEquals(3003, ResultCode.PASSWORD_ERROR.getCode());
        assertEquals(3004, ResultCode.TOKEN_INVALID.getCode());
        assertEquals(3005, ResultCode.TOKEN_EXPIRED.getCode());
    }

    @Test
    void dataErrors_shouldHaveCorrectCodes() {
        assertEquals(4001, ResultCode.DATA_NOT_FOUND.getCode());
        assertEquals(4002, ResultCode.DATA_ALREADY_EXISTS.getCode());
    }
}
