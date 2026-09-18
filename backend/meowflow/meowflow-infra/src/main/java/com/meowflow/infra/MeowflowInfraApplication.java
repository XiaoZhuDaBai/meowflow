package com.meowflow.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Infra 模块调试启动类
 *
 * <p><strong>注意</strong>：此启动类仅用于开发/调试，方便在 IDE 中直接看到 infra 模块所有 Bean。
 *
 * <p>生产部署时，infra 模块作为 <strong>库模块</strong>使用，由业务模块（workflow、template 等）
 * 通过 Maven 依赖引入，其 Bean 由 Spring Boot 自动装配机制加载：
 * <ul>
 *   <li>依赖：业务模块的 pom.xml 添加 meowflow-infra 依赖</li>
 *   <li>自动装配：infra 模块的 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *       会自动注册所有配置类</li>
 * </ul>
 *
 * <p>如果业务模块需要禁用某些 infra Bean，可以通过 application.yml 配置或覆盖 Bean。
 */
@SpringBootApplication
public class MeowflowInfraApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "dev");
        System.setProperty("spring.flyway.enabled", "false");
        SpringApplication.run(MeowflowInfraApplication.class, args);
    }
}