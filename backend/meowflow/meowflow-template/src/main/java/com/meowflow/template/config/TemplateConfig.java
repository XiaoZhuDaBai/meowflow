package com.meowflow.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateConfig {

    @Bean
    public OpenAPI templateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeowFlow Template API")
                        .description("模板管理服务 API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MeowFlow Team")
                                .email("support@meowflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
