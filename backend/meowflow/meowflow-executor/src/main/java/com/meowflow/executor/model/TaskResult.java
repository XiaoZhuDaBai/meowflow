package com.meowflow.executor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务结果
 */
@Data
public class TaskResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private boolean success;
    private TaskStatus status;
    private Object data;
    private String errorMessage;
    private Long costMs;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String executorNodeId;
    private Map<String, Object> metadata;

    public static TaskResult success(String taskId, Object data, long costMs) {
        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setSuccess(true);
        result.setStatus(TaskStatus.SUCCESS);
        result.setData(data);
        result.setCostMs(costMs);
        result.setEndTime(LocalDateTime.now());
        return result;
    }

    public static TaskResult failure(String taskId, String errorMessage, long costMs) {
        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setSuccess(false);
        result.setStatus(TaskStatus.FAILED);
        result.setErrorMessage(errorMessage);
        result.setCostMs(costMs);
        result.setEndTime(LocalDateTime.now());
        return result;
    }

    public static TaskResult timeout(String taskId, long costMs) {
        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setSuccess(false);
        result.setStatus(TaskStatus.TIMEOUT);
        result.setErrorMessage("Task execution timeout");
        result.setCostMs(costMs);
        result.setEndTime(LocalDateTime.now());
        return result;
    }

    public static TaskResult cancelled(String taskId) {
        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setSuccess(false);
        result.setStatus(TaskStatus.CANCELLED);
        result.setEndTime(LocalDateTime.now());
        return result;
    }

    public TaskResult withExecutor(String executorNodeId) {
        this.executorNodeId = executorNodeId;
        return this;
    }

    public TaskResult withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
}
