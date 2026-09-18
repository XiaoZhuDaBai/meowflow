package com.meowflow.executor.registry;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行节点
 */
@Data
public class ExecutorNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nodeId;
    private String name;
    private String host;
    private int port;
    private String status;
    private Set<String> capabilities;
    private Map<String, Object> properties;
    private int weight;
    private int maxConcurrentTasks;
    private AtomicInteger currentTasks;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime registerTime;
    private double loadFactor;
    private String version;

    public static ExecutorNode of(String nodeId, String name, String host, int port) {
        ExecutorNode node = new ExecutorNode();
        node.setNodeId(nodeId);
        node.setName(name);
        node.setHost(host);
        node.setPort(port);
        node.setStatus(NodeStatus.ACTIVE);
        node.setCapabilities(new java.util.HashSet<>());
        node.setProperties(new java.util.HashMap<>());
        node.setWeight(100);
        node.setMaxConcurrentTasks(10);
        node.setCurrentTasks(new AtomicInteger(0));
        node.setLastHeartbeat(LocalDateTime.now());
        node.setRegisterTime(LocalDateTime.now());
        node.setLoadFactor(0);
        return node;
    }

    public void heartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        updateLoadFactor();
    }

    public boolean isHealthy() {
        if (!NodeStatus.ACTIVE.equals(status)) return false;
        return java.time.Duration.between(lastHeartbeat, LocalDateTime.now()).toSeconds() < 60;
    }

    public boolean canAcceptTask() {
        return isHealthy() && currentTasks.get() < maxConcurrentTasks;
    }

    public void taskStarted() {
        currentTasks.incrementAndGet();
        updateLoadFactor();
    }

    public void taskCompleted() {
        currentTasks.decrementAndGet();
        updateLoadFactor();
    }

    public void addCapability(String capability) {
        this.capabilities.add(capability);
    }

    public boolean hasCapability(String capability) {
        return this.capabilities.contains(capability);
    }

    private void updateLoadFactor() {
        if (maxConcurrentTasks > 0) {
            this.loadFactor = (double) currentTasks.get() / maxConcurrentTasks;
        }
    }

    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }

    public static class NodeStatus {
        public static final String ACTIVE = "active";
        public static final String INACTIVE = "inactive";
        public static final String BUSY = "busy";
        public static final String DRAINING = "draining";
    }
}
