package com.meowflow.executor.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务消息体
 * 用于 RabbitMQ 队列传输的任务信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务标识
     */
    private String taskIdentifier;

    /**
     * 工作流执行ID
     */
    private String executionId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 任务输入参数
     */
    private Map<String, Object> input;

    /**
     * 执行上下文
     */
    private Map<String, String> context;

    /**
     * 当前重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 消息创建时间戳
     */
    private long timestamp;

    /**
     * 任务优先级 (1-100, 越高越优先)
     */
    private int priority;

    /**
     * 任务超时时间 (毫秒)
     */
    private long timeoutMs;

    /**
     * 目标执行器节点ID (可选，用于指定节点)
     */
    private String targetNodeId;

    /**
     * 创建新的 TaskMessage
     */
    public static TaskMessage of(Long taskId, String executionId, String nodeId) {
        return TaskMessage.builder()
                .taskId(taskId)
                .executionId(executionId)
                .nodeId(nodeId)
                .timestamp(System.currentTimeMillis())
                .retryCount(0)
                .maxRetries(3)
                .priority(50)
                .build();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.maxRetries > 0 && this.retryCount < this.maxRetries;
    }
}
