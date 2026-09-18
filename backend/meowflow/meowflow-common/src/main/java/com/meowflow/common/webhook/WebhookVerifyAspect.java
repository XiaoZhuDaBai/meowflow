package com.meowflow.common.webhook;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Webhook 签名验证切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebhookVerifyAspect {

    private final WebhookSignatureVerifier verifier;

    @Around("@annotation(com.meowflow.common.webhook.VerifyWebhook)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        VerifyWebhook verifyWebhook = getAnnotation(joinPoint);

        if (!verifyWebhook.enabled()) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        String signature = request.getHeader(verifyWebhook.signatureHeader());
        String timestamp = request.getHeader(verifyWebhook.timestampHeader());

        if (signature == null || timestamp == null) {
            log.warn("Webhook 签名头缺失: signature={}, timestamp={}", signature, timestamp);
            throw new BizException(ResultCode.BAD_REQUEST, "Webhook 签名验证失败：签名或时间戳缺失");
        }

        String payload = getRequestBody(request);
        String secretKey = verifyWebhook.secretKey();

        boolean valid = verifier.verify(payload, signature, timestamp,
                secretKey.isEmpty() ? null : secretKey);

        if (!valid) {
            log.warn("Webhook 签名验证失败");
            throw new BizException(ResultCode.UNAUTHORIZED, "Webhook 签名验证失败");
        }

        log.debug("Webhook 签名验证通过");
        return joinPoint.proceed();
    }

    private VerifyWebhook getAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            java.lang.reflect.Method method = signature.getMethod();
            return method.getAnnotation(VerifyWebhook.class);
        } catch (Exception e) {
            return joinPoint.getTarget().getClass().getAnnotation(VerifyWebhook.class);
        }
    }

    private String getRequestBody(HttpServletRequest request) {
        try {
            StringBuilder body = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        } catch (IOException e) {
            log.error("读取请求体失败", e);
            return "";
        }
    }
}
