package com.meowflow.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "com.meowflow.user",
    "com.meowflow.common"
})
@MapperScan(basePackages = "com.meowflow.user.repository")
public class MeowFlowUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowUserApplication.class, args);
    }
}
