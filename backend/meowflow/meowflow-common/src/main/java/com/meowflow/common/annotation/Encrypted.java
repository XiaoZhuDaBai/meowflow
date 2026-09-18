package com.meowflow.common.annotation;

import java.lang.annotation.*;

/**
 * 字段加密注解
 * <p>
 * 用于标记需要加密存储的字段，配合 MyBatis-Plus TypeHandler 使用。
 * <p>
 * 使用示例：
 * <pre>
 * public class IntegrationConfig {
 *     &#64;Encrypted
 *     private String webhookSecret;
 *
 *     &#64;Encrypted
 *     private String apiKey;
 * }
 * </pre>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {

    /**
     * 加密类型，默认 AES
     */
    String type() default "AES";
}
