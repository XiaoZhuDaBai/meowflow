package com.meowflow.user.service;

import com.wf.captcha.SpecCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务。
 * <p>
 * 验证码以 UUID 为 key、答案为 value 存入 Redis，
 * 有效期 5 分钟，支持一次性使用（验证后立即删除）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * 生成验证码图片。
     *
     * @return CaptchaVO 包含 base64 图片和 uuid
     * @throws IOException 如果图片生成失败
     */
    public CaptchaVO generate() throws IOException {
        SpecCaptcha captcha = new SpecCaptcha(120, 40, 4);
        String code = captcha.text();
        String uuid = UUID.randomUUID().toString();
        String key = CAPTCHA_KEY_PREFIX + uuid;

        redisTemplate.opsForValue().set(key, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        String base64 = captcha.toBase64();
        log.debug("Generated captcha, uuid={}, code={}", uuid, code);

        return new CaptchaVO(uuid, base64);
    }

    /**
     * 返回指定 uuid 对应的验证码答案（仅供测试/开发环境使用）。
     * 生产环境不应暴露此接口。
     */
    public String getAnswer(String uuid) {
        String key = CAPTCHA_KEY_PREFIX + uuid;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * 验证验证码是否正确。
     * <p>
     * 验证成功后自动从 Redis 删除该验证码（一次性）。
     *
     * @param uuid      验证码 UUID
     * @param userInput 用户输入的验证码
     * @return true 表示验证通过
     */
    public boolean verify(String uuid, String userInput) {
        if (uuid == null || userInput == null) {
            return false;
        }
        String key = CAPTCHA_KEY_PREFIX + uuid;
        Object stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            log.debug("Captcha expired or not found, uuid={}", uuid);
            return false;
        }
        boolean matched = stored.toString().equalsIgnoreCase(userInput.trim());
        if (matched) {
            redisTemplate.delete(key);
            log.debug("Captcha verified successfully, uuid={}", uuid);
        } else {
            log.debug("Captcha mismatch, uuid={}, input={}, stored={}", uuid, userInput, stored);
        }
        return matched;
    }

    /**
     * 移除验证码（登出/刷新时允许重新生成）。
     */
    public void remove(String uuid) {
        if (uuid != null) {
            redisTemplate.delete(CAPTCHA_KEY_PREFIX + uuid);
        }
    }

    /**
     * 验证码 VO。
     */
    public record CaptchaVO(String uuid, String img) {
    }
}
