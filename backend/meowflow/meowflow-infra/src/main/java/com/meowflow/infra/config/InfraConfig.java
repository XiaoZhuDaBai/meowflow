package com.meowflow.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infra 模块 OpenAPI 配置
 *
 * <p>使用 {@link ConditionalOnMissingBean} 确保不与业务模块的 OpenAPI Bean 冲突，
 * 业务模块（如 template、user、executor）通常自带 OpenAPI 配置。
 */
@Configuration
public class InfraConfig {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeowFlow AI Infrastructure API")
                        .version("1.0.0")
                        .description("AI 基础设施模块 - LLM、Embedding、知识库、MCP 工具、集成通知等"));
    }
}