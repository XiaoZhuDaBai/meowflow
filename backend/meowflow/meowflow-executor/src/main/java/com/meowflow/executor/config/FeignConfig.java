package com.meowflow.executor.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置
 * 启用 Feign 客户端扫描
 */
@Configuration
@EnableFeignClients(basePackages = "com.meowflow.common.client")
public class FeignConfig {
}
