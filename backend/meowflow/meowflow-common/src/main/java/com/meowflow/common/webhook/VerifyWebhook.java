package com.meowflow.common.webhook;

import java.lang.annotation.*;

/**
 * Webhook 签名验证注解
 * <p>
 * 标记需要验证 Webhook 签名的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VerifyWebhook {

    /**
     * 密钥（为空则使用配置的默认密钥）
     */
    String secretKey() default "";

    /**
     * 签名请求头名称
     */
    String signatureHeader() default "X-Webhook-Signature";

    /**
     * 时间戳请求头名称
     */
    String timestampHeader() default "X-Webhook-Timestamp";

    /**
     * 是否启用验证
     */
    boolean enabled() default true;
}
