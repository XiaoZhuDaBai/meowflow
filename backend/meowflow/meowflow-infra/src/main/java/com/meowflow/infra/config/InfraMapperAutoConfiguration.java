package com.meowflow.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Infra 模块 MyBatis Mapper 自动配置类。
 *
 * 统一管理 infra 模块内所有 Mapper 的扫描路径，使用方只需 {@code @Import(InfraMapperAutoConfiguration.class)} 即可，
 * 无需在各自的 @MapperScan 中硬编码 infra 的包路径。
 *
 * @see com.meowflow.infra.agent
 * @see com.meowflow.infra.service
 * @see com.meowflow.infra.persistence.mapper
 */
@Configuration
@MapperScan({
    "com.meowflow.infra.agent",
    "com.meowflow.infra.service",
    "com.meowflow.infra.persistence.mapper"
})
public class InfraMapperAutoConfiguration {
}
