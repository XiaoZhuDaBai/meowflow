package com.meowflow.executor.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 支持分布式任务分发、死信队列、延迟消息
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "meowflow.mq")
public class RabbitMQConfig {

    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";

    // Exchange 配置常量
    public static final String TASK_EXCHANGE = "meowflow.task";
    public static final String TASK_QUEUE = "meowflow.task.queue";
    public static final String TASK_ROUTING_KEY = "task.execute";

    // Dead Letter Queue 配置
    public static final String DLX_EXCHANGE = "meowflow.task.dlx";
    public static final String DLQ_QUEUE = "meowflow.task.dlq";
    public static final String DLQ_ROUTING_KEY = "dlq";

    // 延迟消息 Exchange 配置
    public static final String DELAYED_EXCHANGE = "meowflow.task.delayed";
    public static final String DELAYED_QUEUE = "meowflow.task.delayed.queue";
    public static final String DELAYED_ROUTING_KEY = "task.delayed";

    private int prefetch = 10;
    private int maxRetries = 3;
    private long initialRetryInterval = 1000L;
    private double retryMultiplier = 2.0;
}
