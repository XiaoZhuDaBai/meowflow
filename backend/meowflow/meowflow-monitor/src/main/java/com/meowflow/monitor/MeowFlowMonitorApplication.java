package com.meowflow.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.meowflow.monitor", "com.meowflow.common"}, exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class MeowFlowMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowMonitorApplication.class, args);
    }
}

