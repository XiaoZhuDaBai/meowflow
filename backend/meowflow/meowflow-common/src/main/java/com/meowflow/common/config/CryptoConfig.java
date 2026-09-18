package com.meowflow.common.config;

import com.meowflow.common.mybatis.EncryptedLongTypeHandler;
import com.meowflow.common.mybatis.EncryptedStringTypeHandler;
import com.meowflow.common.security.AESEncryptor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 加密配置
 *
 * <p>支持通过环境变量或配置中心注入敏感密钥。
 *
 * <p>仅当 {@code meowflow.crypto.aes-key} 显式配置时才创建 Bean，
 * 避免无加密需求的模块（如 monitor）因缺配置导致启动失败。
 *
 * <p><strong>TypeHandler 生命周期</strong>：
 * MyBatis 的 TypeHandler 由其自身管理，不走 Spring 容器生命周期。
 * 为确保 TypeHandler 能使用 Spring Bean 的 AESEncryptor，
 * 本配置在容器启动后（{@link ApplicationRunner}）主动将 encryptor 注入到 TypeHandler 实例。
 */
@Slf4j
@Data
@ConditionalOnProperty(prefix = "meowflow.crypto", name = "aes-key")
@Configuration
@ConfigurationProperties(prefix = "meowflow.crypto")
public class CryptoConfig {

    /**
     * AES-256 密钥 (base64 编码，32 字节)
     * 生产环境应从环境变量或密钥管理服务获取
     */
    private String aesKey;

    /**
     * 密钥标识，用于密钥轮换
     */
    private String keyId;

    /**
     * 创建 AES 加密器 Bean
     */
    @Bean
    public AESEncryptor aesEncryptor() {
        AESEncryptor encryptor = new AESEncryptor();
        encryptor.setAesKey(this.aesKey);
        log.info("AESEncryptor Bean 创建完成，keyId: {}", keyId);
        return encryptor;
    }

    /**
     * 将 AESEncryptor Bean 注入到 MyBatis TypeHandler
     *
     * <p>使用 {@link Order} 确保在所有 Mapper 初始化完成后执行。
     * MyBatis TypeHandler 在 Mapper 初始化时被注册，此时需要 encryptor 已就绪。
     */
    @Bean
    @Order(100)
    public ApplicationRunner typeHandlerInitializer(AESEncryptor encryptor) {
        return args -> {
            log.info("初始化 MyBatis TypeHandler，注入 AESEncryptor");
            EncryptedStringTypeHandler.setEncryptor(encryptor);
            EncryptedLongTypeHandler.setEncryptor(encryptor);
            log.info("TypeHandler 初始化完成");
        };
    }
}
