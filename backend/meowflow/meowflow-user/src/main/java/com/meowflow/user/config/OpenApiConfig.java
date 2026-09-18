package com.meowflow.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeowFlow User API")
                        .description("喵流平台用户服务 API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MeowFlow Team")
                                .email("support@meowflow.com")
                                .url("https://meowflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("开发环境"),
                        new Server().url("https://api.meowflow.com").description("生产环境")
                ));
    }
}
