package com.meowflow.workflow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流执行事件。
 *
 * <p>事件流经 {@link RunEventSink} 写入 Redis Stream，前端通过 SSE 消费。
 * 所有事件按 {@link #eventId} 全局有序。</p>
 *
 * <p>枚举成员命名遵循 Dify 的事件命名约定，保证双方 SSE 格式兼容。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunEvent {

    /** 全局唯一事件 ID，格式：时间戳-递增序号，如 "1718000000-001" */
    private String eventId;

    /** Redis Stream ID（真实的 Stream 消息 ID，如 "1718000000-0"） */
    private String streamId;

    /** 事件类型，对应枚举成员名 */
    private String event;

    /** 所属执行 ID */
    private Long executionId;

    /** 所属工作流 ID */
    private Long workflowId;

    /** 事件发生时的时间戳 */
    private LocalDateTime timestamp;

    /** 关联节点 ID（节点事件专用） */
    private String nodeId;

    /** 节点类型（节点事件专用） */
    private String nodeType;

    /** 节点名称（节点事件专用） */
    private String nodeName;

    /** 事件附加数据（JSON 结构，按事件类型不同） */
    private Map<String, Object> data;

    // ── Execution events ────────────────────────────────────────────────────────

    public static RunEvent executionStarted(Long executionId, Long workflowId) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("execution_started")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static RunEvent executionSucceeded(Long executionId, Long workflowId, Map<String, Object> output) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("execution_succeeded")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("output", output != null ? output : Map.of()))
                .build();
    }

    public static RunEvent executionFailed(Long executionId, Long workflowId, String error) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("execution_failed")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("error", error != null ? error : ""))
                .build();
    }

    public static RunEvent executionCancelled(Long executionId, Long workflowId, String reason) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("execution_cancelled")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("reason", reason != null ? reason : ""))
                .build();
    }

    // ── Node events ────────────────────────────────────────────────────────────

    public static RunEvent nodeStarted(Long executionId, Long workflowId,
                                       String nodeId, String nodeType, String nodeName) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_started")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .build();
    }

    public static RunEvent nodeStreamDelta(Long executionId, Long workflowId,
                                           String nodeId, String delta) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_stream_delta")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .data(Map.of("delta", delta != null ? delta : ""))
                .build();
    }

    public static RunEvent nodeFinished(Long executionId, Long workflowId,
                                        String nodeId, String nodeType, String nodeName,
                                        String status, Map<String, Object> output,
                                        Long costMs, Integer costToken) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_finished")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .data(Map.of(
                        "status", status != null ? status : "",
                        "output", output != null ? output : Map.of(),
                        "cost_ms", costMs != null ? costMs : 0L,
                        "cost_token", costToken != null ? costToken : 0
                ))
                .build();
    }

    public static RunEvent nodeSkipped(Long executionId, Long workflowId,
                                       String nodeId, String nodeType, String nodeName,
                                       String reason) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_skipped")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .data(Map.of("reason", reason != null ? reason : "unreachable"))
                .build();
    }

    public static RunEvent nodeRetrying(Long executionId, Long workflowId,
                                        String nodeId, int attempt, int maxAttempts) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_retrying")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .data(Map.of("attempt", attempt, "max_attempts", maxAttempts))
                .build();
    }

    public static RunEvent nodeTimedOut(Long executionId, Long workflowId,
                                        String nodeId, String nodeType, String nodeName,
                                        Long timeoutSeconds) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("node_timed_out")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .nodeId(nodeId)
                .nodeType(nodeType)
                .nodeName(nodeName)
                .data(Map.of("timeout_seconds", timeoutSeconds != null ? timeoutSeconds : 0L))
                .build();
    }

    public static RunEvent snapshotCreated(Long executionId, Long workflowId, int version) {
        return RunEvent.builder()
                .eventId(nextId())
                .event("snapshot_created")
                .executionId(executionId)
                .workflowId(workflowId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("snapshot_version", version))
                .build();
    }

    // ── ID generation ─────────────────────────────────────────────────────────

    private static long counter = 0;

    private static synchronized String nextId() {
        return System.currentTimeMillis() + "-" + String.format("%03d", (int) (counter++ % 1000));
    }
}
