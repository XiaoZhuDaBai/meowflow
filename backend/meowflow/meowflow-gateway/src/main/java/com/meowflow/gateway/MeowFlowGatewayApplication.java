package com.meowflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MeowFlow 统一网关启动类
 *
 * <p>基于 Spring Cloud Gateway + Sentinel，提供：</p>
 * <ul>
 *   <li>路由转发与负载均衡（Nacos Discovery + LoadBalancer）</li>
 *   <li>统一认证鉴权（Sa-Token）</li>
 *   <li>网关层限流熔断（Sentinel Gateway Flow Rule）</li>
 *   <li>请求日志与 Trace ID 透传</li>
 * </ul>
 */
@EnableDiscoveryClient
@SpringBootApplication
public class MeowFlowGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowGatewayApplication.class, args);
    }
}