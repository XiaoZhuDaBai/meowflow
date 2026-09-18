package com.meowflow.workflow.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人工输入节点的等待/继续协调器。
 */
@Service
public class HumanTaskService {

    private final Map<String, CompletableFuture<Map<String, Object>>> tasks = new ConcurrentHashMap<>();

    public CompletableFuture<Map<String, Object>> waitForTask(Long executionId, String nodeId) {
        String key = key(executionId, nodeId);
        return tasks.computeIfAbsent(key, k -> new CompletableFuture<>());
    }

    public boolean complete(Long executionId, String nodeId, Map<String, Object> input) {
        CompletableFuture<Map<String, Object>> future = tasks.remove(key(executionId, nodeId));
        if (future == null) {
            return false;
        }
        future.complete(input != null ? input : Map.of());
        return true;
    }

    public boolean cancel(Long executionId, String nodeId) {
        CompletableFuture<Map<String, Object>> future = tasks.remove(key(executionId, nodeId));
        if (future == null) {
            return false;
        }
        future.cancel(true);
        return true;
    }

    public void cancelExecution(Long executionId) {
        String prefix = executionId + ":";
        tasks.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().cancel(true);
                return true;
            }
            return false;
        });
    }

    private String key(Long executionId, String nodeId) {
        return executionId + ":" + nodeId;
    }
}
