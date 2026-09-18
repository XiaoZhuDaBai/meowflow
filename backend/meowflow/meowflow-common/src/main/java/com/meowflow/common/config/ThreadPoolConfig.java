package com.meowflow.common.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置
 * <p>
 * 定义 8 个专用线程池用于不同场景：
 * - workflow-executor: 工作流执行
 * - llm-call: LLM 调用
 * - http-request: HTTP 请求
 * - mcp-batch: MCP 批量处理
 * - knowledge-search: 知识库搜索
 * - notify-send: 通知发送
 * - db-operation: 数据库操作
 * - schedule-task: 定时任务
 *
 * <p>所有线程池均使用 Alibaba TTL (TransmittableThreadLocal) 包装，
 * 确保 UserContext、TraceContext 等能够在异步线程中正确传递。
 *
 * @see AsyncConfig 异步任务配置
 * @see TtlConfig TTL 上下文透传配置
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Value("${meowflow.thread-pool.workflow-executor.core-size:10}")
    private int workflowCoreSize;

    @Value("${meowflow.thread-pool.llm-call.core-size:20}")
    private int llmCoreSize;

    @Value("${meowflow.thread-pool.http-request.core-size:50}")
    private int httpCoreSize;

    @Value("${meowflow.thread-pool.mcp-batch.core-size:10}")
    private int mcpCoreSize;

    @Value("${meowflow.thread-pool.knowledge-search.core-size:50}")
    private int knowledgeCoreSize;

    @Value("${meowflow.thread-pool.notify-send.core-size:20}")
    private int notifyCoreSize;

    @Value("${meowflow.thread-pool.db-operation.core-size:15}")
    private int dbCoreSize;

    @Value("${meowflow.thread-pool.schedule-task.core-size:10}")
    private int scheduleCoreSize;

    // ==================== 工作流执行线程池 ====================

    /**
     * 工作流执行线程池
     * 核心 10，最大 20，队列 1000
     */
    @Bean("workflowExecutorPool")
    public Executor workflowExecutorPool() {
        return TtlExecutors.getTtlExecutor(buildPool(workflowCoreSize, 20, 1000, "workflow-executor"));
    }

    // ==================== LLM 调用线程池 ====================

    /**
     * LLM 调用线程池
     * 核心 20，最大 50，队列 2000
     */
    @Bean("llmCallPool")
    public Executor llmCallPool() {
        return TtlExecutors.getTtlExecutor(buildPool(llmCoreSize, 50, 2000, "llm-call"));
    }

    // ==================== HTTP 请求线程池 ====================

    /**
     * HTTP 请求线程池
     * 核心 50，最大 100，队列 5000
     */
    @Bean("httpRequestPool")
    public Executor httpRequestPool() {
        return TtlExecutors.getTtlExecutor(buildPool(httpCoreSize, 100, 5000, "http-request"));
    }

    // ==================== MCP 批量处理线程池 ====================

    /**
     * MCP 批量处理线程池
     * 核心 10，最大 20，队列 100
     */
    @Bean("mcpBatchPool")
    public Executor mcpBatchPool() {
        return TtlExecutors.getTtlExecutor(buildPool(mcpCoreSize, 20, 100, "mcp-batch"));
    }

    // ==================== 知识库搜索线程池 ====================

    /**
     * 知识库搜索线程池
     * 核心 50，最大 100，队列 500
     */
    @Bean("knowledgeSearchPool")
    public Executor knowledgeSearchPool() {
        return TtlExecutors.getTtlExecutor(buildPool(knowledgeCoreSize, 100, 500, "knowledge-search"));
    }

    // ==================== 通知发送线程池 ====================

    /**
     * 通知发送线程池
     * 核心 20，最大 50，队列 200
     */
    @Bean("notifySendPool")
    public Executor notifySendPool() {
        return TtlExecutors.getTtlExecutor(buildPool(notifyCoreSize, 50, 200, "notify-send"));
    }

    // ==================== 数据库操作线程池 ====================

    /**
     * 数据库操作线程池
     * 核心 15，最大 30，队列 200
     */
    @Bean("dbOperationPool")
    public Executor dbOperationPool() {
        return TtlExecutors.getTtlExecutor(buildPool(dbCoreSize, 30, 200, "db-operation"));
    }

    // ==================== 定时任务线程池 ====================

    /**
     * 定时任务线程池
     * 核心 10，最大 20，队列 100
     */
    @Bean("scheduleTaskPool")
    public Executor scheduleTaskPool() {
        return TtlExecutors.getTtlExecutor(buildPool(scheduleCoreSize, 20, 100, "schedule-task"));
    }

    // ==================== 节点执行线程池 ====================

    /**
     * 节点执行线程池
     * 核心 20，最大 50，队列 500
     */
    @Bean("nodeExecutorPool")
    public Executor nodeExecutorPool() {
        return TtlExecutors.getTtlExecutor(buildPool(20, 50, 500, "node-executor"));
    }

    // ==================== 日志写入线程池 ====================

    /**
     * 日志写入线程池
     * 核心 2，最大 4，队列 10000
     */
    @Bean("logExecutor")
    public Executor logExecutor() {
        return TtlExecutors.getTtlExecutor(buildPool(2, 4, 10000, "log-writer"));
    }

    // ==================== 通用方法 ====================

    /**
     * 构建线程池
     */
    private ThreadPoolExecutor buildPool(int coreSize, int maxSize, int queueCapacity, String name) {
        return new ThreadPoolExecutor(
                coreSize, maxSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(name),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 命名线程工厂
     */
    public static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    /**
     * 获取线程池状态信息
     */
    @Operation(summary = "获取线程池状态")
    public Map<String, ThreadPoolStatus> getThreadPoolStatus(
            @Value("${meowflow.thread-pool.workflow-executor.core-size:10}") int wCore, @Value("${meowflow.thread-pool.workflow-executor.max-size:20}") int wMax, @Value("${meowflow.thread-pool.workflow-executor.queue-capacity:1000}") int wQueue,
            @Value("${meowflow.thread-pool.llm-call.core-size:20}") int lCore, @Value("${meowflow.thread-pool.llm-call.max-size:50}") int lMax, @Value("${meowflow.thread-pool.llm-call.queue-capacity:2000}") int lQueue) {

        return Map.of(
                "workflow", new ThreadPoolStatus(wCore, wMax, wQueue),
                "llm", new ThreadPoolStatus(lCore, lMax, lQueue)
        );
    }

    /**
     * 线程池状态
     */
    public record ThreadPoolStatus(int coreSize, int maxSize, int queueCapacity) {}
}