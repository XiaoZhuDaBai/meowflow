package com.meowflow.executor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务上下文
 */
@Data
public class TaskContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionId;
    private String workflowId;
    private String nodeId;
    private String taskId;
    private Map<String, Object> variables;
    private Map<String, Object> sharedData;
    private LocalDateTime startTime;
    private String createdBy;
    private String callbackUrl;
    private Map<String, String> headers;

    public TaskContext() {
        this.variables = new HashMap<>();
        this.sharedData = new HashMap<>();
        this.headers = new HashMap<>();
        this.startTime = LocalDateTime.now();
    }

    public static TaskContext of(String executionId, String workflowId) {
        TaskContext ctx = new TaskContext();
        ctx.setExecutionId(executionId);
        ctx.setWorkflowId(workflowId);
        return ctx;
    }

    public void setVariable(String key, Object value) {
        this.variables.put(key, value);
    }

    public Object getVariable(String key) {
        return this.variables.get(key);
    }

    public void setShared(String key, Object value) {
        this.sharedData.put(key, value);
    }

    public Object getShared(String key) {
        return this.sharedData.get(key);
    }

    public Map<String, Object> getAllVariables() {
        Map<String, Object> all = new HashMap<>();
        all.putAll(this.variables);
        all.putAll(this.sharedData);
        return all;
    }

    public long getElapsedMs() {
        if (startTime == null) return 0;
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }
}
