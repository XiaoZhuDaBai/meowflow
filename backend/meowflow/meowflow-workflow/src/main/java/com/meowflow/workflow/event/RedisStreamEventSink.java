package com.meowflow.workflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.redis.RedisService;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.entity.ExecutionSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis Stream 实现的事件下沉器。
 *
 * <p>使用两个 Redis Key 存储执行数据：
 * <ul>
 *   <li>事件流：{@code meowflow:run:events:{executionId}}（Stream 类型）</li>
 *   <li>快照记录：{@code meowflow:run:snapshot:{executionId}}（String/JSON 类型）</li>
 *   <li>状态记录：{@code meowflow:run:status:{executionId}}（String 类型）</li>
 * </ul>
 *
 * <p>TTL：默认 7 天（{@code workflow.event.ttl-days} 配置）。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "workflow.event.sink", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisStreamEventSink implements RunEventSink {

    private static final String EVENTS_KEY_PREFIX = "meowflow:run:events:";
    private static final String SNAPSHOT_KEY_PREFIX = "meowflow:run:snapshot:";
    private static final String STATUS_KEY_PREFIX = "meowflow:run:status:";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Value("${workflow.event.ttl-days:7}")
    private int ttlDays = 7;

    @Override
    public void append(RunEvent event) {
        String key = EVENTS_KEY_PREFIX + event.getExecutionId();
        try {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("event_id", event.getEventId());
            fields.put("event", event.getEvent());
            fields.put("execution_id", String.valueOf(event.getExecutionId()));
            fields.put("workflow_id", String.valueOf(event.getWorkflowId()));
            fields.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : "");
            if (event.getNodeId() != null) {
                fields.put("node_id", event.getNodeId());
            }
            if (event.getNodeType() != null) {
                fields.put("node_type", event.getNodeType());
            }
            if (event.getNodeName() != null) {
                fields.put("node_name", event.getNodeName());
            }
            if (event.getData() != null) {
                fields.put("data", objectMapper.writeValueAsString(event.getData()));
            }

            String streamId = redisService.streamAdd(key, fields);
            // 回填 streamId 到事件对象，供 SSE 使用
            event.setStreamId(streamId);
            log.trace("Appended event {} to stream {} with id {}", event.getEvent(), key, streamId);

            // 设置 / 刷新 TTL
            redisService.expire(key, ttlDays, TimeUnit.DAYS);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event data: {}", event, e);
        }
    }

    @Override
    public void updateExecutionStatus(Long executionId, String status, Map<String, Object> output) {
        String key = STATUS_KEY_PREFIX + executionId;
        try {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("execution_id", executionId);
            record.put("status", status);
            record.put("output", output);
            record.put("updated_at", LocalDateTime.now().toString());
            redisService.set(key, objectMapper.writeValueAsString(record), ttlDays, TimeUnit.DAYS);
            log.trace("Updated execution status: executionId={}, status={}", executionId, status);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize execution status: executionId={}", executionId, e);
        }
    }

    @Override
    public int createSnapshot(ExecutionContext context, String waitingReason, String lastEventId) {
        String key = SNAPSHOT_KEY_PREFIX + context.getExecutionId();
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("execution_id", context.getExecutionId());
            snapshot.put("workflow_id", context.getWorkflowId());
            snapshot.put("version", context.getVersion());
            snapshot.put("status", context.getStatus());

            // 序列化已完成节点
            List<Map<String, Object>> completed = new ArrayList<>();
            for (var entry : context.getNodeResults().entrySet()) {
                var result = entry.getValue();
                Map<String, Object> nodeState = new LinkedHashMap<>();
                nodeState.put("node_id", entry.getKey());
                nodeState.put("status", result.getStatus() != null ? result.getStatus().name() : "UNKNOWN");
                nodeState.put("output", result.getOutput());
                nodeState.put("error", result.getErrorMessage());
                nodeState.put("cost_ms", result.getCostMs());
                nodeState.put("finished_at", result.getFinishedAt() != null ? result.getFinishedAt().toString() : null);
                completed.add(nodeState);
            }
            snapshot.put("completed_nodes", objectMapper.writeValueAsString(completed));

            // 序列化变量
            snapshot.put("variables", objectMapper.writeValueAsString(context.getVariables()));

            // 游标和等待原因
            snapshot.put("waiting_reason", waitingReason != null ? waitingReason : "");
            snapshot.put("last_event_id", lastEventId != null ? lastEventId : "");

            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            redisService.set(key, snapshotJson, ttlDays, TimeUnit.DAYS);
            log.trace("Created snapshot for executionId={}, waitingReason={}",
                    context.getExecutionId(), waitingReason);

            return 1; // snapshotVersion = 1 for simplicity; full impl would use INCR
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot: executionId={}", context.getExecutionId(), e);
            return 0;
        }
    }

    @Override
    public Optional<ExecutionSnapshot> getLatestSnapshot(Long executionId) {
        String key = SNAPSHOT_KEY_PREFIX + executionId;
        String json = redisService.getStr(key);
        if (json == null || json.isEmpty()) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            ExecutionSnapshot snapshot = new ExecutionSnapshot();
            snapshot.setExecutionId(executionId);
            snapshot.setWorkflowId(map.get("workflow_id") != null
                    ? Long.parseLong(map.get("workflow_id").toString()) : null);
            snapshot.setVersion((String) map.get("version"));
            snapshot.setStatus((String) map.get("status"));
            snapshot.setCompletedNodes((String) map.get("completed_nodes"));
            snapshot.setVariables((String) map.get("variables"));
            snapshot.setWaitingReason((String) map.get("waiting_reason"));
            snapshot.setLastEventId((String) map.get("last_event_id"));

            return Optional.of(snapshot);
        } catch (Exception e) {
            log.error("Failed to deserialize snapshot: executionId={}", executionId, e);
            return Optional.empty();
        }
    }

    @Override
    public EventPage fetchEvents(Long executionId, String cursor) {
        String key = EVENTS_KEY_PREFIX + executionId;

        // cursor "0-0" means start from beginning, "-" also means start from beginning
        String startId;
        if ("0-0".equals(cursor) || cursor == null || cursor.isEmpty()) {
            startId = "-";
        } else {
            // Use cursor as exclusive start: we need to read AFTER this ID
            // In Redis XRANGE, we use "(" prefix to exclude the start ID
            startId = "(" + cursor;
        }
        String endId = "+";

        List<Map<String, String>> records = redisService.streamRange(key, startId, endId);

        List<RunEvent> events = new ArrayList<>();
        String nextCursor = cursor;

        for (Map<String, String> fields : records) {
            String streamId = fields.get("_id");
            if (streamId != null) {
                nextCursor = streamId;
            }

            RunEvent event = RunEvent.builder()
                    .eventId(fields.get("event_id"))
                    .streamId(streamId)
                    .event(fields.get("event"))
                    .executionId(parseLong(fields.get("execution_id")))
                    .workflowId(parseLong(fields.get("workflow_id")))
                    .nodeId(fields.get("node_id"))
                    .nodeType(fields.get("node_type"))
                    .nodeName(fields.get("node_name"))
                    .build();

            if (fields.get("timestamp") != null && !fields.get("timestamp").isEmpty()) {
                try {
                    event.setTimestamp(LocalDateTime.parse(fields.get("timestamp")));
                } catch (Exception ignored) {
                }
            }

            String dataJson = fields.get("data");
            if (dataJson != null && !dataJson.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(dataJson, Map.class);
                    event.setData(data);
                } catch (JsonProcessingException ignored) {
                }
            }

            events.add(event);
        }

        // nextCursor: use the last record's _id for the next read position
        return new EventPage(events, nextCursor);
    }

    @Override
    public void flush() {
        // RedisTemplate writes are not buffered by default (no pipeline needed for single-threaded context)
        // Sub-classes using pipelines should override and call pipeline.execute()
    }

    private Long parseLong(String val) {
        if (val == null || val.isEmpty()) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
