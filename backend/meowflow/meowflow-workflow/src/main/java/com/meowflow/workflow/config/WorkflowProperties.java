package com.meowflow.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作流模块配置属性。
 * <p>
 * 绑定前缀: workflow.*
 * <p>
 * 可以在 application.yml 中配置：
 * <pre>
 * workflow:
 *   engine:
 *     max-concurrent-executions: 20
 *     node-timeout-seconds: 300
 *     retry-max-attempts: 3
 *   version:
 *     auto-increment: true
 *     max-per-workflow: 100
 *   trigger:
 *     cron-max-instances: 10
 *     redis-key-prefix: "wf:schedule:"
 *   gateway:
 *     base-url: "http://localhost:8080"
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    private Engine engine = new Engine();
    private Version version = new Version();
    private Trigger trigger = new Trigger();
    private Gateway gateway = new Gateway();
    private File file = new File();

    @Data
    public static class Engine {
        /** 最大并发执行数 */
        private int maxConcurrentExecutions = 20;
        /** 节点默认超时（秒） */
        private int nodeTimeoutSeconds = 300;
        /** 节点最大重试次数 */
        private int retryMaxAttempts = 3;
        /** 执行线程池核心线程数 */
        private int executorPoolCoreSize = 10;
        /** 执行线程池最大线程数 */
        private int executorPoolMaxSize = 20;
    }

    @Data
    public static class Version {
        /** 是否自动递增版本号 */
        private boolean autoIncrement = true;
        /** 每个工作流最大版本数 */
        private int maxPerWorkflow = 100;
    }

    @Data
    public static class Trigger {
        /** Cron 任务最大同时实例数 */
        private int cronMaxInstances = 10;
        /** Redis 中调度 key 的前缀 */
        private String redisKeyPrefix = "wf:schedule:";
    }

    @Data
    public static class Gateway {
        /** 网关对外暴露的 base URL（用于构造 webhook / callback 等外部回调链接） */
        private String baseUrl = "http://localhost:8080";
    }

    @Data
    public static class File {
        /** 本地文件暂存目录（MinIO 未接入时的最小可用实现） */
        private String storageDir = "./workflow-files";
        /** 存储类型：local / minio */
        private String storageType = "local";
        /** MinIO endpoint，如 http://localhost:9000 */
        private String minioEndpoint = "http://localhost:9000";
        private String minioAccessKey = "minioadmin";
        private String minioSecretKey = "minioadmin";
        private String minioBucket = "meowflow-files";
        /** 单文件最大字节数 */
        private long maxSize = 20L * 1024 * 1024;
    }
}
