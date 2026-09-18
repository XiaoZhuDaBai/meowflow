package com.meowflow.monitor.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Monitor Mapper 扫描配置，避免 WebMvcTest 切片加载 Mapper。
 */
@Configuration
@MapperScan("com.meowflow.monitor.repository")
public class MonitorMapperConfig {
}
