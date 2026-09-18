package com.meowflow.common.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Webhook 签名验证器
 * <p>
 * 支持 HMAC-SHA256 签名校验，用于验证 Webhook 请求来源
 */
@Slf4j
@Component
public class WebhookSignatureVerifier {

    @Value("${webhook.secret:default-secret-key}")
    private String secret;

    /**
     * 验证签名
     *
     * @param payload   请求体
     * @param signature  签名（Base64 编码）
     * @param timestamp  时间戳
     * @param secretKey  密钥（可选，默认为配置的密钥）
     * @return true 如果签名验证通过
     */
    public boolean verify(String payload, String signature, String timestamp, String secretKey) {
        if (payload == null || signature == null || timestamp == null) {
            log.warn("Webhook 签名验证参数为空");
            return false;
        }

        // 时间戳校验（防止重放攻击，允许 5 分钟时间窗口）
        if (!isTimestampValid(timestamp)) {
            log.warn("Webhook 时间戳超出允许范围: {}", timestamp);
            return false;
        }

        String key = secretKey != null ? secretKey : secret;
        String expectedSignature = generateSignature(payload, timestamp, key);

        boolean valid = constantTimeEquals(expectedSignature, signature);

        if (!valid) {
            log.warn("Webhook 签名验证失败: expected={}, actual={}", expectedSignature, signature);
        }

        return valid;
    }

    /**
     * 验证签名（使用默认密钥）
     */
    public boolean verify(String payload, String signature, String timestamp) {
        return verify(payload, signature, timestamp, null);
    }

    /**
     * 生成签名
     *
     * @param payload  请求体
     * @param timestamp 时间戳
     * @param secretKey 密钥
     * @return Base64 编码的签名
     */
    public String generateSignature(String payload, String timestamp, String secretKey) {
        String key = secretKey != null ? secretKey : secret;
        String data = payload + "." + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("生成签名失败", e);
            return null;
        }
    }

    /**
     * 生成签名（使用默认密钥）
     */
    public String generateSignature(String payload, String timestamp) {
        return generateSignature(payload, timestamp, null);
    }

    /**
     * 验证时间戳是否在有效范围内
     *
     * @param timestamp 时间戳（秒）
     * @return true 如果有效
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis() / 1000;
            long diff = Math.abs(now - ts);
            return diff <= 300; // 5 分钟
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 常量时间比较，防止时序攻击
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    /**
     * 生成随机密钥
     */
    public static String generateSecret() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
