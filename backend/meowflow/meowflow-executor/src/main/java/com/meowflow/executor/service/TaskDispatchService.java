package com.meowflow.executor.service;

import com.meowflow.common.util.IdGenerator;
import com.meowflow.executor.loadbalancer.*;
import com.meowflow.executor.model.Task;
import com.meowflow.executor.model.TaskQueue;
import com.meowflow.executor.model.TaskResult;
import com.meowflow.executor.model.TaskStatus;
import com.meowflow.executor.model.TaskType;
import com.meowflow.executor.mq.RabbitMQConfig;
import com.meowflow.executor.mq.TaskMessage;
import com.meowflow.executor.mq.TaskProducer;
import com.meowflow.executor.registry.ExecutorNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务分发服务
 * 支持本地队列和 RabbitMQ 分布式队列两种模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDispatchService {

    private final ExecutorNodeRegistry nodeRegistry;
    private final Map<String, LoadBalancer> loadBalancers = new ConcurrentHashMap<>();

    @Autowired
    private TaskProducer taskProducer;

    @Autowired(required = false)
    private RabbitMQConfig rabbitMQConfig;

    private volatile boolean distributedMode = false;

    public Task dispatch(Task task) {
        return dispatch(task, "least-connections");
    }

    /**
     * 分布式任务分发 - 通过 RabbitMQ 发送到队列
     * 适用于多实例部署场景
     */
    public void dispatchToQueue(Task task) {
        log.info("Dispatching task to RabbitMQ queue: taskId={}", task.getTaskId());

        // 1. 负载均衡选择节点 (用于记录，不影响分发)
        ExecutorNode selectedNode = selectNodeForTask(task);

        // 2. 构建消息
        TaskMessage message = TaskMessage.builder()
                .taskId(task.getId())
                .taskIdentifier(task.getTaskId())
                .executionId(task.getExecutionId())
                .nodeId(task.getNodeId())
                .nodeType(task.getType() != null ? task.getType().name() : null)
                .input(task.getParams())
                .context(buildContextMap(task))
                .retryCount(task.getRetryCount() != null ? task.getRetryCount() : 0)
                .maxRetries(task.getMaxRetries() != null ? task.getMaxRetries() : 3)
                .priority(task.getPriority() != null ? task.getPriority() : 50)
                .timeoutMs(task.getTimeoutMs() != null ? task.getTimeoutMs() : 300000L)
                .targetNodeId(selectedNode != null ? selectedNode.getNodeId() : null)
                .timestamp(System.currentTimeMillis())
                .build();

        // 3. 发送到 RabbitMQ
        if (taskProducer != null) {
            if (task.getPriority() != null && task.getPriority() > 70) {
                // 高优先级任务
                taskProducer.sendTaskWithPriority(message, Math.min(task.getPriority() / 10, 9));
            } else {
                taskProducer.sendTask(message);
            }
        }

        // 4. 更新任务状态
        task.setStatus(TaskStatus.PENDING);
        if (selectedNode != null) {
            task.setExecutorNodeId(selectedNode.getNodeId());
        }

        log.info("Task dispatched to queue: taskId={}, nodeId={}",
                task.getTaskId(), selectedNode != null ? selectedNode.getNodeId() : "any");
    }

    /**
     * 分布式任务分发 - 延迟重试
     */
    public void dispatchToQueueDelayed(TaskMessage taskMessage, long delayMs) {
        if (taskProducer != null) {
            taskProducer.sendTaskDelayed(taskMessage, delayMs);
        }
    }

    /**
     * 执行从队列接收的任务消息
     */
    public void executeTask(TaskMessage taskMessage) {
        log.info("Executing task from queue: taskId={}, executionId={}",
                taskMessage.getTaskId(), taskMessage.getExecutionId());

        // 根据 nodeId 或 targetNodeId 定位任务
        String targetNodeId = taskMessage.getTargetNodeId();
        ExecutorNode targetNode = null;

        if (targetNodeId != null) {
            targetNode = nodeRegistry.getNode(targetNodeId);
        }

        if (targetNode == null) {
            // 使用负载均衡选择节点
            targetNode = selectNodeForTaskById(taskMessage.getTaskId());
        }

        if (targetNode != null) {
            log.info("Executing task on node: taskId={}, nodeId={}",
                    taskMessage.getTaskId(), targetNode.getNodeId());
            nodeRegistry.taskStarted(targetNode.getNodeId());
        }
    }

    /**
     * 处理死信任务
     */
    public void handleDeadLetterTask(TaskMessage taskMessage) {
        log.error("Handling dead letter task: taskId={}, executionId={}, retryCount={}",
                taskMessage.getTaskId(), taskMessage.getExecutionId(), taskMessage.getRetryCount());

        // 记录失败日志
        // 更新任务状态为 FAILED
        // 发送告警通知
    }

    /**
     * 本地分发模式
     */
    public Task dispatch(Task task, String strategy) {
        List<ExecutorNode> candidates = filterCandidates(task);

        if (candidates.isEmpty()) {
            log.warn("No available executor nodes for task: {}", task.getTaskId());
            return null;
        }

        LoadBalancer balancer = loadBalancers.getOrDefault(strategy,
                loadBalancers.get("least-connections"));

        ExecutorNode selected = balancer.select(candidates);

        if (selected == null) {
            log.warn("Load balancer returned null for task: {}", task.getTaskId());
            return null;
        }

        task.setExecutorNodeId(selected.getNodeId());
        task.setStatus(TaskStatus.ASSIGNED);
        nodeRegistry.taskStarted(selected.getNodeId());

        log.info("Dispatched task {} to node {} using {} strategy",
                task.getTaskId(), selected.getNodeId(), strategy);

        return task;
    }

    public Task dispatchByCapability(Task task, String capability) {
        List<ExecutorNode> candidates = nodeRegistry.getNodesByCapability(capability)
                .stream()
                .filter(ExecutorNode::canAcceptTask)
                .toList();

        if (candidates.isEmpty()) {
            log.warn("No nodes with capability {} for task: {}", capability, task.getTaskId());
            return dispatch(task);
        }

        ExecutorNode selected = candidates.stream()
                .min((a, b) -> Integer.compare(a.getCurrentTasks().get(), b.getCurrentTasks().get()))
                .orElse(null);

        if (selected != null) {
            task.setExecutorNodeId(selected.getNodeId());
            task.setStatus(TaskStatus.ASSIGNED);
            nodeRegistry.taskStarted(selected.getNodeId());
            log.info("Dispatched task {} to capable node {}", task.getTaskId(), selected.getNodeId());
        }

        return task;
    }

    public Task dispatchWithFallback(Task task) {
        String[] strategies = {"least-connections", "weighted", "round-robin", "random"};

        for (String strategy : strategies) {
            List<ExecutorNode> candidates = filterCandidates(task);
            if (candidates.isEmpty()) continue;

            LoadBalancer balancer = loadBalancers.get(strategy);
            ExecutorNode selected = balancer.select(candidates);

            if (selected != null) {
                task.setExecutorNodeId(selected.getNodeId());
                task.setStatus(TaskStatus.ASSIGNED);
                nodeRegistry.taskStarted(selected.getNodeId());
                log.info("Dispatched task {} to node {} using fallback strategy {}",
                        task.getTaskId(), selected.getNodeId(), strategy);
                return task;
            }
        }

        log.warn("All fallback strategies failed for task: {}", task.getTaskId());
        return null;
    }

    public void notifyTaskCompleted(String nodeId) {
        nodeRegistry.taskCompleted(nodeId);
    }

    public void notifyTaskFailed(String nodeId) {
        nodeRegistry.taskCompleted(nodeId);
    }

    private List<ExecutorNode> filterCandidates(Task task) {
        return nodeRegistry.getHealthyNodes().stream()
                .filter(ExecutorNode::canAcceptTask)
                .toList();
    }

    /**
     * 根据任务选择节点
     */
    private ExecutorNode selectNodeForTask(Task task) {
        List<ExecutorNode> candidates = filterCandidates(task);
        if (candidates.isEmpty()) {
            return null;
        }
        LoadBalancer balancer = loadBalancers.getOrDefault("least-connections",
                loadBalancers.get("random"));
        return balancer.select(candidates);
    }

    /**
     * 根据任务ID选择节点
     */
    private ExecutorNode selectNodeForTaskById(Long taskId) {
        List<ExecutorNode> candidates = nodeRegistry.getHealthyNodes().stream()
                .filter(ExecutorNode::canAcceptTask)
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        LoadBalancer balancer = loadBalancers.get("round-robin");
        if (balancer == null) {
            balancer = loadBalancers.values().iterator().next();
        }
        return balancer.select(candidates);
    }

    /**
     * 构建上下文Map
     */
    private Map<String, String> buildContextMap(Task task) {
        Map<String, String> context = new HashMap<>();
        context.put("workflowId", task.getWorkflowId());
        context.put("executorNodeId", task.getExecutorNodeId());
        context.put("createdBy", task.getCreatedBy());
        return context;
    }

    /**
     * 启用/禁用分布式模式
     */
    public void setDistributedMode(boolean enabled) {
        this.distributedMode = enabled;
        log.info("Distributed mode set to: {}", enabled);
    }

    /**
     * 检查是否启用分布式模式
     */
    public boolean isDistributedMode() {
        return distributedMode;
    }

    public LoadBalancer getLoadBalancer(String name) {
        return loadBalancers.get(name);
    }

    public Map<String, LoadBalancer> getAllLoadBalancers() {
        return loadBalancers;
    }
}
