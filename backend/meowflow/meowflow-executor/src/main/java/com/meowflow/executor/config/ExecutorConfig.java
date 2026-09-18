package com.meowflow.executor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeowFlow Executor API")
                        .version("1.0.0")
                        .description("执行调度服务 - 任务分发与执行器管理"));
    }
}
