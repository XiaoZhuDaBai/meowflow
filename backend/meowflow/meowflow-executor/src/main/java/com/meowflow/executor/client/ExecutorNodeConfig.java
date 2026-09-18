package com.meowflow.executor.client;

import lombok.Data;

/**
 * 执行器节点配置信息
 */
@Data
public class ExecutorNodeConfig {

    private String executorId;
    private String name;
    private String host;
    private int port;
    private int maxConcurrentTasks;
    private java.util.Map<String, String> capabilities;
    private int timeoutMs;
    private int connectionPoolSize;

    public static ExecutorNodeConfig defaultConfig(String host, int port) {
        ExecutorNodeConfig config = new ExecutorNodeConfig();
        config.setHost(host);
        config.setPort(port);
        config.setMaxConcurrentTasks(10);
        config.setTimeoutMs(30000);
        config.setConnectionPoolSize(20);
        return config;
    }

    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }
}
