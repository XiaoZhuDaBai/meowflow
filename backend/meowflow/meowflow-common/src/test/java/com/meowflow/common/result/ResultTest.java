package com.meowflow.common.result;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_shouldCreateResultWithNullData() {
        Result<String> result = Result.success();
        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void success_shouldCreateResultWithData() {
        Result<String> result = Result.success("test data");
        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), result.getMessage());
        assertEquals("test data", result.getData());
    }

    @Test
    void success_shouldCreateResultWithDataAndMessage() {
        Result<String> result = Result.success("test data", "自定义成功消息");
        assertNotNull(result);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("自定义成功消息", result.getMessage());
        assertEquals("test data", result.getData());
    }

    @Test
    void error_shouldCreateErrorResultFromResultCode() {
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST);
        assertNotNull(result);
        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void error_shouldCreateErrorResultWithCodeAndMessage() {
        Result<Void> result = Result.error(500, "服务器内部错误");
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误", result.getMessage());
    }

    @Test
    void error_shouldCreateErrorResultFromResultCodeWithMessage() {
        Result<Void> result = Result.error(ResultCode.NOT_FOUND, "用户不存在");
        assertNotNull(result);
        assertEquals(ResultCode.NOT_FOUND.getCode(), result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void isSuccess_shouldReturnTrueForSuccessCode() {
        Result<Void> result = Result.success();
        assertTrue(result.isSuccess());
    }

    @Test
    void isSuccess_shouldReturnFalseForErrorCode() {
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST);
        assertFalse(result.isSuccess());
    }

    @Test
    void success_shouldSetTimestamp() {
        long before = System.currentTimeMillis() - 100;
        Result<Void> result = Result.success();
        long after = System.currentTimeMillis() + 100;
        assertTrue(result.getTimestamp() >= before);
        assertTrue(result.getTimestamp() <= after);
    }

    @Test
    void success_withComplexData_shouldWork() {
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("list", List.of(1, 2, 3));
        complexData.put("nested", Map.of("key", "value"));

        Result<Map<String, Object>> result = Result.success(complexData);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(List.of(1, 2, 3), result.getData().get("list"));
        assertNotNull(result.getData().get("nested"));
    }
}
