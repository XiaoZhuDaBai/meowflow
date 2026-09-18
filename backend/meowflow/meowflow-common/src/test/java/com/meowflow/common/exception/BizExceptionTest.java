package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BizExceptionTest {

    @Test
    void constructor_withResultCode_shouldSetCorrectValues() {
        BizException ex = new BizException(ResultCode.BAD_REQUEST);
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals(ResultCode.BAD_REQUEST.name(), ex.getErrorCode());
        assertEquals(ResultCode.BAD_REQUEST.getMessage(), ex.getMessage());
    }

    @Test
    void constructor_withResultCodeAndMessage_shouldOverrideMessage() {
        BizException ex = new BizException(ResultCode.BAD_REQUEST, "自定义错误消息");
        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals(ResultCode.BAD_REQUEST.name(), ex.getErrorCode());
        assertEquals("自定义错误消息", ex.getMessage());
    }

    @Test
    void constructor_withCodeAndMessage_shouldSetValues() {
        BizException ex = new BizException(9999, "自定义错误");
        assertEquals(9999, ex.getCode());
        assertEquals("9999", ex.getErrorCode());
        assertEquals("自定义错误", ex.getMessage());
    }

    @Test
    void addDetail_shouldAddKeyValuePair() {
        BizException ex = new BizException(ResultCode.BAD_REQUEST);
        ex.addDetail("field", "username");
        ex.addDetail("reason", "too short");
        assertEquals("username", ex.getDetails().get("field"));
        assertEquals("too short", ex.getDetails().get("reason"));
    }

    @Test
    void addDetail_shouldReturnThisForChaining() {
        BizException ex = new BizException(ResultCode.BAD_REQUEST);
        BizException result = ex.addDetail("key", "value");
        assertSame(ex, result);
    }

    @Test
    void exception_shouldBeRuntimeException() {
        BizException ex = new BizException(ResultCode.BAD_REQUEST);
        assertTrue(ex instanceof RuntimeException);
    }
}
