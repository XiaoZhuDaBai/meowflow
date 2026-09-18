package com.meowflow.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("CaptchaService 需要 Nashorn JavaScript 引擎支持 ArithmeticCaptcha，在 JDK 17 默认环境不可用")
class CaptchaServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        captchaService = new CaptchaService(redisTemplate);
    }

    @Test
    void getAnswer_shouldReturnStoredCode() {
        String uuid = "test-uuid";
        String expectedCode = "1234";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(expectedCode);

        String result = captchaService.getAnswer(uuid);

        assertEquals(expectedCode, result);
    }

    @Test
    void getAnswer_shouldReturnNullForNonExistentKey() {
        String uuid = "non-existent-uuid";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(null);

        String result = captchaService.getAnswer(uuid);

        assertNull(result);
    }

    @Test
    void verify_shouldReturnTrueForCorrectCode() {
        String uuid = "test-uuid";
        String code = "1234";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(code);

        boolean result = captchaService.verify(uuid, code);

        assertTrue(result);
        verify(redisTemplate).delete("captcha:" + uuid);
    }

    @Test
    void verify_shouldReturnFalseForIncorrectCode() {
        String uuid = "test-uuid";
        String storedCode = "1234";
        String wrongCode = "5678";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(storedCode);

        boolean result = captchaService.verify(uuid, wrongCode);

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void verify_shouldReturnFalseForNullUuid() {
        boolean result = captchaService.verify(null, "1234");

        assertFalse(result);
    }

    @Test
    void verify_shouldReturnFalseForNullCode() {
        boolean result = captchaService.verify("test-uuid", null);

        assertFalse(result);
    }

    @Test
    void verify_shouldReturnFalseForExpiredCode() {
        String uuid = "test-uuid";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(null);

        boolean result = captchaService.verify(uuid, "1234");

        assertFalse(result);
    }

    @Test
    void verify_shouldBeCaseInsensitive() {
        String uuid = "test-uuid";
        String storedCode = "AbCd";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(storedCode);

        assertTrue(captchaService.verify(uuid, "abcd"));
        assertTrue(captchaService.verify(uuid, "ABCD"));
        assertTrue(captchaService.verify(uuid, "AbCd"));
    }

    @Test
    void verify_shouldTrimWhitespace() {
        String uuid = "test-uuid";
        String storedCode = "1234";
        when(valueOperations.get("captcha:" + uuid)).thenReturn(storedCode);

        assertTrue(captchaService.verify(uuid, " 1234 "));
    }

    @Test
    void remove_shouldDeleteFromRedis() {
        String uuid = "test-uuid";

        captchaService.remove(uuid);

        verify(redisTemplate).delete("captcha:" + uuid);
    }

    @Test
    void remove_shouldHandleNullUuid() {
        captchaService.remove(null);

        verify(redisTemplate, never()).delete(anyString());
    }
}
