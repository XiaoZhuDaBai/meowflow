package com.meowflow.workflow.engine;

import com.meowflow.workflow.entity.NodeExecution;
import com.meowflow.workflow.repository.NodeExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 节点执行记录器。
 *
 * 在节点生命周期（开始 / 完成 / 跳过 / 超时）发生时即时写入 {@code mf_wf_node_execution} 表，
 * 而不是等到整个工作流结束后再批量插入。
 * 这使得：
 * <ul>
 *   <li>长时间运行的工作流可以实时查询已完成节点的输出。</li>
 *   <li>进程崩溃后可从 {@code mf_wf_node_execution} 恢复已完成节点的状态。</li>
 *   <li>SSE / 前端可以展示实时执行进度。</li>
 * </ul>
 *
 * 所有写操作均在同一 {@link NodeExecutionRepository} 事务内完成；
 * 若写失败，记录 error 日志后降级（不抛出异常影响执行链路）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeExecutionRecorder {

    private final NodeExecutionRepository nodeExecutionRepository;

    /**
     * 记录节点开始执行。
     *
     * @param executionId 工作流执行 ID
     * @param node       节点定义
     * @param input      已解析的输入参数（可选，可传 null）
     */
    public void recordStarted(Long executionId, com.meowflow.workflow.definition.NodeDefinition node,
                              java.util.Map<String, Object> input) {
        try {
            NodeExecution record = new NodeExecution();
            record.setExecutionId(executionId);
            record.setNodeId(node.getId());
            record.setNodeType(node.getType() != null ? node.getType().getCode() : null);
            record.setNodeName(node.getName());
            record.setStatus("running");
            record.setInput(input);
            record.setStartedAt(java.time.LocalDateTime.now());
            nodeExecutionRepository.insert(record);
            log.debug("NodeExecution recorded STARTED: executionId={}, nodeId={}", executionId, node.getId());
        } catch (Exception e) {
            log.error("Failed to record NodeExecution STARTED: executionId={}, nodeId={}",
                    executionId, node.getId(), e);
        }
    }

    /**
     * 记录节点完成。
     *
     * @param executionId 工作流执行 ID
     * @param node       节点定义
     * @param result     节点执行结果
     */
    public void recordFinished(Long executionId, com.meowflow.workflow.definition.NodeDefinition node,
                               NodeResult result) {
        try {
            NodeExecution record = latestRecord(executionId, node.getId());
            record.setExecutionId(executionId);
            record.setNodeId(node.getId());
            record.setNodeType(node.getType() != null ? node.getType().getCode() : null);
            record.setNodeName(node.getName());
            record.setStatus(mapStatus(result.getStatus()));
            record.setInput(result.getInput());
            record.setOutput(result.getOutput());
            record.setErrorMessage(result.getErrorMessage());
            record.setRetryCount(result.getRetryCount());
            record.setCostMs(result.getCostMs());
            record.setCostToken(result.getCostToken());
            // 引擎自造的 timedOut / cancelled 结果没有 startedAt，不要用它覆盖已有值
            if (result.getStartedAt() != null) {
                record.setStartedAt(result.getStartedAt());
            }
            record.setFinishedAt(result.getFinishedAt());
            if (record.getId() == null) {
                nodeExecutionRepository.insert(record);
            } else {
                nodeExecutionRepository.updateById(record);
            }
            log.debug("NodeExecution recorded FINISHED: executionId={}, nodeId={}, status={}",
                    executionId, node.getId(), record.getStatus());
        } catch (Exception e) {
            log.error("Failed to record NodeExecution FINISHED: executionId={}, nodeId={}",
                    executionId, node.getId(), e);
        }
    }

    /**
     * 记录节点被跳过（条件未命中、孤岛节点等）。
     *
     * @param executionId 工作流执行 ID
     * @param node       节点定义
     * @param reason     跳过原因（可选）
     */
    public void recordSkipped(Long executionId, com.meowflow.workflow.definition.NodeDefinition node,
                               String reason) {
        try {
            NodeExecution record = latestRecord(executionId, node.getId());
            record.setExecutionId(executionId);
            record.setNodeId(node.getId());
            record.setNodeType(node.getType() != null ? node.getType().getCode() : null);
            record.setNodeName(node.getName());
            record.setStatus("skipped");
            record.setErrorMessage(reason);
            if (record.getStartedAt() == null) {
                record.setStartedAt(java.time.LocalDateTime.now());
            }
            record.setFinishedAt(java.time.LocalDateTime.now());
            if (record.getId() == null) {
                nodeExecutionRepository.insert(record);
            } else {
                nodeExecutionRepository.updateById(record);
            }
            log.debug("NodeExecution recorded SKIPPED: executionId={}, nodeId={}, reason={}",
                    executionId, node.getId(), reason);
        } catch (Exception e) {
            log.error("Failed to record NodeExecution SKIPPED: executionId={}, nodeId={}",
                    executionId, node.getId(), e);
        }
    }

    /**
     * 取当前应被更新的节点执行行：优先复用该节点本轮迭代刚插入的 running 行，
     * 否则回退到最新一行；都没有则返回新实体（由调用方插入）。
     *
     * <p>用「最新的 running 行」而不是「唯一一行」，是为了兼容循环节点：
     * 每轮迭代都会插入新的 running 行，若按唯一行查询会抛
     * {@code TooManyResultsException}。</p>
     */
    private NodeExecution latestRecord(Long executionId, String nodeId) {
        java.util.List<NodeExecution> rows =
                nodeExecutionRepository.findAllByExecutionIdAndNodeId(executionId, nodeId);
        if (rows == null || rows.isEmpty()) {
            return new NodeExecution();
        }
        for (NodeExecution row : rows) {
            if ("running".equals(row.getStatus())) {
                return row;
            }
        }
        return rows.get(0);
    }

    private String mapStatus(NodeResult.NodeStatus status) {
        if (status == null) return "unknown";
        return switch (status) {
            case SUCCESS -> "success";
            case FAILED -> "failed";
            case SKIPPED -> "skipped";
            case RUNNING -> "running";
            case PENDING -> "pending";
            case CANCELLED -> "cancelled";
            case TIMED_OUT -> "timed_out";
        };
    }
}
