package com.meowflow.executor;

import com.meowflow.common.config.ThreadPoolConfig;
import com.meowflow.common.util.IdGeneratorFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import({IdGeneratorFactory.class, ThreadPoolConfig.class})
public class MeowFlowExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowExecutorApplication.class, args);
    }
}
