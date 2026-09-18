package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.NodeType;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeResult {

    private String nodeId;
    private NodeType nodeType;
    private String nodeName;

    private NodeStatus status;

    private Map<String, Object> input;
    private Map<String, Object> output;

    private String errorMessage;
    private String stackTrace;

    private Integer retryCount;

    private Long costMs;
    private Integer costToken;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public enum NodeStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        SKIPPED,
        FAILED,
        CANCELLED,
        TIMED_OUT
    }

    public boolean isSuccess() {
        return status == NodeStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == NodeStatus.FAILED;
    }

    public boolean isFinished() {
        return status == NodeStatus.SUCCESS
                || status == NodeStatus.SKIPPED
                || status == NodeStatus.FAILED
                || status == NodeStatus.CANCELLED
                || status == NodeStatus.TIMED_OUT;
    }

    public long getElapsedMs() {
        if (startedAt == null || finishedAt == null) return 0;
        return java.time.Duration.between(startedAt, finishedAt).toMillis();
    }

    public static NodeResult pending(String nodeId, NodeType nodeType, String nodeName) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.PENDING)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static NodeResult running(String nodeId, NodeType nodeType, String nodeName) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static NodeResult success(String nodeId, NodeType nodeType, String nodeName, Map<String, Object> output) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.SUCCESS)
                .output(output)
                .finishedAt(LocalDateTime.now())
                .build();
    }

    public static NodeResult failed(String nodeId, NodeType nodeType, String nodeName, String errorMessage, Throwable ex) {
        String trace = null;
        if (ex != null) {
            java.io.StringWriter writer = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(writer));
            trace = writer.toString();
        }
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.FAILED)
                .errorMessage(errorMessage)
                .stackTrace(trace)
                .finishedAt(LocalDateTime.now())
                .build();
    }

    public static NodeResult skipped(String nodeId, NodeType nodeType, String nodeName) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.SKIPPED)
                .finishedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 节点执行超时。
     *
     * @param nodeId   节点 ID
     * @param nodeType 节点类型
     * @param nodeName 节点名称
     * @param timeout  超时时长（秒）
     * @return 超时结果
     */
    public static NodeResult timedOut(String nodeId, NodeType nodeType, String nodeName, Duration timeout) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.TIMED_OUT)
                .errorMessage("Node execution timed out after " + timeout.getSeconds() + " seconds")
                .costMs(timeout.toMillis())
                .finishedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 节点被取消。
     *
     * @param nodeId   节点 ID
     * @param nodeType 节点类型
     * @param nodeName 节点名称
     * @param reason   取消原因（可选）
     * @return 取消结果
     */
    public static NodeResult cancelled(String nodeId, NodeType nodeType, String nodeName, String reason) {
        return NodeResult.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .status(NodeStatus.CANCELLED)
                .errorMessage(reason != null ? reason : "Cancelled")
                .finishedAt(LocalDateTime.now())
                .build();
    }

    public NodeResult withCostMs(Long costMs) {
        this.costMs = costMs;
        return this;
    }

    public NodeResult withStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public NodeResult withFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public NodeResult withCostToken(Integer costToken) {
        this.costToken = costToken;
        return this;
    }
}

