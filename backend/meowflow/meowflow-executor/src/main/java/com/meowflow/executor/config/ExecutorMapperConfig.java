package com.meowflow.executor.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Executor Mapper 扫描配置，避免 WebMvcTest 切片加载 Mapper。
 */
@Configuration
@MapperScan("com.meowflow.executor.mapper")
public class ExecutorMapperConfig {
}
