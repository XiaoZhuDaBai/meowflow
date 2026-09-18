package com.meowflow.common.webhook;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Webhook 签名验证拦截器
 * <p>
 * 提供便捷的 HTTP 请求签名验证方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookInterceptor {

    private final WebhookSignatureVerifier verifier;

    /**
     * 从请求中提取签名信息并验证
     *
     * @param secretKey 密钥
     * @return true 如果验证通过
     */
    public boolean verifyRequest(String secretKey) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文");
            return false;
        }

        HttpServletRequest request = attributes.getRequest();

        String signature = request.getHeader("X-Webhook-Signature");
        String timestamp = request.getHeader("X-Webhook-Timestamp");

        if (signature == null || timestamp == null) {
            log.warn("Webhook 签名头缺失: signature={}, timestamp={}", signature, timestamp);
            return false;
        }

        String payload = getRequestBody(request);
        if (payload == null) {
            log.warn("无法获取请求体");
            return false;
        }

        return verifier.verify(payload, signature, timestamp, secretKey);
    }

    /**
     * 使用默认密钥验证请求
     */
    public boolean verifyRequest() {
        return verifyRequest(null);
    }

    /**
     * 获取请求体
     */
    private String getRequestBody(HttpServletRequest request) {
        try {
            // 尝试从请求属性中获取（如果之前已经读取过）
            String cachedBody = (String) request.getAttribute("_cachedRequestBody");
            if (cachedBody != null) {
                return cachedBody;
            }

            // 读取请求体
            StringBuilder body = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            String result = body.toString();
            request.setAttribute("_cachedRequestBody", result);
            return result;
        } catch (IOException e) {
            log.error("读取请求体失败", e);
            return null;
        }
    }

    /**
     * 生成签名头
     *
     * @param payload   请求体
     * @param secretKey 密钥
     * @return 包含签名和时间戳的签名信息
     */
    public SignatureInfo generateSignature(String payload, String secretKey) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = verifier.generateSignature(payload, timestamp, secretKey);
        return new SignatureInfo(signature, timestamp);
    }

    /**
     * 使用默认密钥生成签名
     */
    public SignatureInfo generateSignature(String payload) {
        return generateSignature(payload, null);
    }

    /**
     * 签名信息
     */
    public record SignatureInfo(String signature, String timestamp) {
    }
}
