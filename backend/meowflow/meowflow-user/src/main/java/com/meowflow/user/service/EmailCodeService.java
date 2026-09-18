package com.meowflow.user.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务。
 * <ul>
 *   <li>验证码存储在 Redis，TTL 10 分钟，一次性使用（验证后自动删除）。</li>
 *   <li>发送冷却期 60 秒（同一邮箱同一类型 60 秒内不可重复发送）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MailSender mailSender;
    private final UserRepository userRepository;

    private static final String CODE_KEY_PREFIX = "email:code:";
    private static final String COOLDOWN_KEY_PREFIX = "email:cooldown:";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 10;
    private static final int COOLDOWN_SECONDS = 60;

    @Value("${meowflow.mail.from:no-reply@meowflow.local}")
    private String mailFrom;

    /**
     * 发送验证码。
     *
     * @param email 收件邮箱
     * @param type  类型：REGISTER | RESET_PWD
     * @param username 可选，用于注册场景的用户名（模板引用）
     */
    public void sendCode(String email, String type, String username) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + type + ":" + email;
        Boolean cooldownHit = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(cooldownHit)) {
            throw new BizException(ResultCode.RATE_LIMITED, "发送太频繁，请稍后再试");
        }

        String code = generateCode();
        String codeKey = CODE_KEY_PREFIX + type + ":" + email;

        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        String subject = "REGISTER".equals(type) ? "喵流 - 注册验证码" : "喵流 - 密码重置验证码";
        String htmlBody = buildHtmlEmail(code, type, username);
        mailSender.send(email, subject, htmlBody);

        log.debug("Email code sent to {}, type={}, code={}", email, type, code);
    }

    /**
     * 验证验证码。
     * 验证成功后自动删除验证码（一次性）。
     *
     * @param email 收件邮箱
     * @param code  用户输入的验证码
     * @param type  类型：REGISTER | RESET_PWD
     * @return true 验证通过
     */
    public boolean verify(String email, String code, String type) {
        String codeKey = CODE_KEY_PREFIX + type + ":" + email;
        Object stored = redisTemplate.opsForValue().get(codeKey);
        if (stored == null) {
            log.debug("Email code expired or not found, email={}, type={}", email, type);
            return false;
        }
        boolean matched = stored.toString().equals(code.trim());
        if (matched) {
            redisTemplate.delete(codeKey);
            log.debug("Email code verified successfully, email={}, type={}", email, type);
        } else {
            log.debug("Email code mismatch, email={}, type={}", email, type);
        }
        return matched;
    }

    /**
     * 检查邮箱是否已被注册（用于忘记密码场景）。
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    private String generateCode() {
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }

    private String buildHtmlEmail(String code, String type, String username) {
        String title = "REGISTER".equals(type) ? "注册验证码" : "密码重置验证码";
        String greeting = username != null ? "尊敬的用户 " + username : "尊敬的用户";
        String action = "REGISTER".equals(type) ? "注册账号" : "重置密码";

        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                  <div style="background: #4A90E2; color: white; padding: 16px; text-align: center; font-size: 20px; font-weight: bold; border-radius: 8px 8px 0 0;">
                    喵流 - MeowFlow
                  </div>
                  <div style="padding: 24px; background: #f9f9f9;">
                    <p style="font-size: 15px; color: #333;">%s，您好！</p>
                    <p style="font-size: 15px; color: #333;">您正在使用邮箱进行%s，请在 10 分钟内输入以下验证码：</p>
                    <div style="background: white; border: 2px dashed #4A90E2; border-radius: 6px; text-align: center; padding: 16px; margin: 20px 0;">
                      <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #4A90E2;">%s</span>
                    </div>
                    <p style="font-size: 13px; color: #999;">如果这不是您的操作，请忽略此邮件。</p>
                  </div>
                  <div style="padding: 12px; text-align: center; font-size: 12px; color: #999;">
                    喵流 · 自动化工作流平台
                  </div>
                </div>
                """.formatted(greeting, action, code);
    }
}
