package com.meowflow.executor.service;

import com.meowflow.executor.entity.ExecutorNodeEntity;
import com.meowflow.executor.mapper.ExecutorNodeMapper;
import com.meowflow.executor.model.*;
import com.meowflow.executor.registry.ExecutorNode;
import com.meowflow.executor.registry.HealthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行节点注册服务 - 内存 + DB 双写
 * ExecutorNodeEntity 用于持久化状态统计（心跳时更新）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutorNodeRegistry {

    private final Map<String, ExecutorNode> nodes = new ConcurrentHashMap<>();
    private final HealthChecker healthChecker;
    private final ExecutorNodeMapper executorNodeMapper;

    public ExecutorNode register(String name, String host, int port, int maxConcurrentTasks) {
        String nodeId = generateNodeId(host, port);
        ExecutorNode node = ExecutorNode.of(nodeId, name, host, port);
        node.setMaxConcurrentTasks(maxConcurrentTasks);

        // Persist to DB
        try {
            ExecutorNodeEntity entity = toEntity(node);
            executorNodeMapper.insert(entity);
        } catch (Exception e) {
            log.warn("Failed to persist executor node to DB: {}", e.getMessage());
        }

        nodes.put(nodeId, node);
        healthChecker.registerNode(node);
        log.info("Registered executor node: {} at {}", nodeId, node.getUrl());
        return node;
    }

    public void unregister(String nodeId) {
        ExecutorNode removed = nodes.remove(nodeId);
        if (removed != null) {
            try {
                executorNodeMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExecutorNodeEntity>()
                                .eq(ExecutorNodeEntity::getNodeId, nodeId)
                                .set(ExecutorNodeEntity::getStatus, "offline"));
            } catch (Exception e) {
                log.warn("Failed to update executor node status in DB: {}", e.getMessage());
            }
            healthChecker.unregisterNode(nodeId);
            log.info("Unregistered executor node: {}", nodeId);
        }
    }

    public ExecutorNode heartbeat(String nodeId) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.heartbeat();
            healthChecker.heartbeat(node);

            // Persist heartbeat stats to DB (get current task counts from AtomicInteger)
            try {
                int activeTasks = node.getCurrentTasks() != null ? node.getCurrentTasks().get() : 0;
                executorNodeMapper.updateHeartbeat(
                        nodeId,
                        node.getStatus(),
                        LocalDateTime.now(),
                        activeTasks,
                        0L, // completedTasks - not tracked in in-memory node
                        0L, // failedTasks - not tracked in in-memory node
                        0L, // memoryUsed - not tracked in in-memory node
                        LocalDateTime.now()
                );
            } catch (Exception e) {
                log.warn("Failed to update heartbeat in DB: {}", e.getMessage());
            }
        }
        return node;
    }

    public ExecutorNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<ExecutorNode> getAllNodes() {
        return nodes.values().stream().toList();
    }

    public List<ExecutorNode> getHealthyNodes() {
        return healthChecker.getHealthyNodes();
    }

    public List<ExecutorNode> getNodesByCapability(String capability) {
        return nodes.values().stream()
                .filter(n -> n.hasCapability(capability))
                .toList();
    }

    public ExecutorNode updateNodeStatus(String nodeId, String status) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.setStatus(status);
            try {
                executorNodeMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ExecutorNodeEntity>()
                                .eq(ExecutorNodeEntity::getNodeId, nodeId)
                                .set(ExecutorNodeEntity::getStatus, status));
            } catch (Exception e) {
                log.warn("Failed to update node status in DB: {}", e.getMessage());
            }
        }
        return node;
    }

    public void updateNodeCapabilities(String nodeId, List<String> capabilities) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.getCapabilities().clear();
            capabilities.forEach(node::addCapability);
        }
    }

    public void updateNodeWeight(String nodeId, int weight) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.setWeight(weight);
        }
    }

    public void updateNodeMaxConcurrentTasks(String nodeId, int maxTasks) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.setMaxConcurrentTasks(maxTasks);
        }
    }

    public ExecutorNode taskStarted(String nodeId) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.taskStarted();
        }
        return node;
    }

    public ExecutorNode taskCompleted(String nodeId) {
        ExecutorNode node = nodes.get(nodeId);
        if (node != null) {
            node.taskCompleted();
        }
        return node;
    }

    public int getTotalNodes() {
        return nodes.size();
    }

    public int getHealthyNodesCount() {
        return getHealthyNodes().size();
    }

    private String generateNodeId(String host, int port) {
        return String.format("executor-%s-%d-%d", host.replace(".", "-"), port, System.currentTimeMillis() % 10000);
    }

    private ExecutorNodeEntity toEntity(ExecutorNode node) {
        ExecutorNodeEntity entity = new ExecutorNodeEntity();
        entity.setNodeId(node.getNodeId());
        entity.setName(node.getName());
        entity.setHost(node.getHost());
        entity.setPort(node.getPort());
        entity.setStatus(node.getStatus());
        entity.setLastHeartbeat(LocalDateTime.now());
        entity.setCpuCount(0);
        entity.setMemoryTotal(0L);
        entity.setMemoryUsed(0L);
        entity.setActiveTasks(0);
        entity.setCompletedTasks(0L);
        entity.setFailedTasks(0L);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }
}
