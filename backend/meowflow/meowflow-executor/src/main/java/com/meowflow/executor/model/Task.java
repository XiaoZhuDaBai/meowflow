package com.meowflow.executor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务
 */
@Data
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskId;
    private String name;
    private TaskType type;
    private TaskStatus status;
    private String workflowId;
    private String workflowName;
    private Integer workflowVersion;
    private String triggerType;
    private Long userId;
    private String executionId;
    private String nodeId;
    private String executorNodeId;
    private String payload;
    private Map<String, Object> params;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetries;
    private Long timeoutMs;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long costMs;
    private String errorMessage;
    private String result;
    private String createdBy;
    private String assignedTo;

    public static Task of(String taskId, String name, TaskType type) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setName(name);
        task.setType(type);
        task.setStatus(TaskStatus.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setRetryCount(0);
        task.setPriority(50);
        return task;
    }

    public void markRunning() {
        this.status = TaskStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    public void markSuccess(String result) {
        this.status = TaskStatus.SUCCESS;
        this.result = result;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.costMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    public void markFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.costMs = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    public void markCancelled() {
        this.status = TaskStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    public void markTimeout() {
        this.status = TaskStatus.TIMEOUT;
        this.errorMessage = "Task execution timeout";
        this.endTime = LocalDateTime.now();
    }

    public boolean canRetry() {
        return this.maxRetries != null && this.retryCount < this.maxRetries;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.status = TaskStatus.RETRYING;
    }
}
