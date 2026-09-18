package com.meowflow.executor.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 执行节点健康检查器
 */
@Slf4j
@Component
public class HealthChecker {

    private final Map<String, ExecutorNode> nodes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long MAX_HEARTBEAT_INTERVAL_SECONDS = 60;

    public HealthChecker() {
        scheduler.scheduleAtFixedRate(this::checkNodeHealth, 10, 10, TimeUnit.SECONDS);
    }

    public void registerNode(ExecutorNode node) {
        nodes.put(node.getNodeId(), node);
        log.info("Registered executor node: {} at {}", node.getNodeId(), node.getUrl());
    }

    public void unregisterNode(String nodeId) {
        ExecutorNode removed = nodes.remove(nodeId);
        if (removed != null) {
            log.info("Unregistered executor node: {}", nodeId);
        }
    }

    public void heartbeat(ExecutorNode node) {
        node.heartbeat();
        nodes.put(node.getNodeId(), node);
    }

    public ExecutorNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Map<String, ExecutorNode> getAllNodes() {
        return new ConcurrentHashMap<>(nodes);
    }

    private void checkNodeHealth() {
        LocalDateTime now = LocalDateTime.now();
        nodes.forEach((nodeId, node) -> {
            long secondsSinceHeartbeat = java.time.Duration.between(node.getLastHeartbeat(), now).getSeconds();
            if (secondsSinceHeartbeat > MAX_HEARTBEAT_INTERVAL_SECONDS) {
                log.warn("Executor node {} missed heartbeat ({}s ago), marking as inactive",
                        nodeId, secondsSinceHeartbeat);
                node.setStatus(ExecutorNode.NodeStatus.INACTIVE);
            }
        });
    }

    public java.util.List<ExecutorNode> getHealthyNodes() {
        return nodes.values().stream()
                .filter(ExecutorNode::isHealthy)
                .toList();
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
