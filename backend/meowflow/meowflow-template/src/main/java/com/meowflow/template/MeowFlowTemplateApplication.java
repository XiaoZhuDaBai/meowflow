package com.meowflow.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 模板服务启动类
 *
 * <p>组件扫描范围限定为 template 和 common，避免拉入其他模块的 Bean。
 * 公共 Bean（IdGenerator、CryptoConfig 等）通过 common 的自动配置生效。
 */
@SpringBootApplication(scanBasePackages = {"com.meowflow.template", "com.meowflow.common"})
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class MeowFlowTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowTemplateApplication.class, args);
    }
}

