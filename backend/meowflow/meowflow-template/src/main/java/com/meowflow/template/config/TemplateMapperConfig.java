package com.meowflow.template.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Template Mapper 扫描配置，避免 WebMvcTest 切片加载 Mapper。
 */
@Configuration
@MapperScan("com.meowflow.template.repository")
public class TemplateMapperConfig {
}
