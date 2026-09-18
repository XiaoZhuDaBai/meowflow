package com.meowflow.workflow.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Workflow 模块 Mapper 扫描配置。
 *
 * <p>独立于启动类，避免 @WebMvcTest 切片加载 Mapper 时因缺少
 * SqlSessionFactory 导致上下文启动失败。</p>
 */
@Configuration
@MapperScan(value = "com.meowflow.workflow", annotationClass = Mapper.class)
public class WorkflowMapperConfig {
}
