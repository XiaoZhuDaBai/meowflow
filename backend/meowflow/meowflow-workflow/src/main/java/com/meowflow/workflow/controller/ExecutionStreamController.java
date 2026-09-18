package com.meowflow.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.workflow.event.RunEvent;
import com.meowflow.workflow.event.RunEventSink;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SSE 实时事件流控制器。
 *
 * <p>前端通过 {@code GET /api/execution/{id}/events?after=cursor} 订阅执行事件。
 * 后端通过 Redis Stream 实现事件广播，支持多客户端订阅与游标重放。</p>
 *
 * <p>与 dify-agent 的 SSE 端点格式兼容。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/execution")
@Tag(name = "执行事件流", description = "SSE 实时推送执行事件")
@RequiredArgsConstructor
public class ExecutionStreamController {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 min
    private static final int FETCH_BATCH_SIZE = 100;

    /** 同时可服务的 SSE 订阅上限。每个订阅占用一个线程（200ms 轮询 Redis）。 */
    private static final int MAX_SSE_SUBSCRIBERS = 100;

    private final RunEventSink eventSink;
    private final ObjectMapper objectMapper;

    /**
     * SSE 推送线程池。
     *
     * <p>必须是有界池：每个订阅者会独占一个线程并在整个连接生命周期内轮询 Redis
     * （最长 30 分钟），用无界 cached pool 的话订阅数一多就会无限创建线程。</p>
     */
    private final ExecutorService sseExecutor = new ThreadPoolExecutor(
            4, MAX_SSE_SUBSCRIBERS, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(MAX_SSE_SUBSCRIBERS),
            r -> {
                Thread t = new Thread(r, "workflow-sse-" + SSE_THREAD_SEQ.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy());

    private static final java.util.concurrent.atomic.AtomicInteger SSE_THREAD_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(0);

    @jakarta.annotation.PreDestroy
    public void shutdownSseExecutor() {
        sseExecutor.shutdownNow();
    }

    @GetMapping(value = "/{executionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 执行事件流",
               description = "订阅指定执行的实时事件流，支持 after=cursor 游标重放")
    public SseEmitter streamEvents(
            @PathVariable Long executionId,
            @RequestParam(defaultValue = "0-0") String after) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        try {
            sseExecutor.submit(() -> pumpEvents(emitter, executionId, after));
        } catch (RejectedExecutionException ree) {
            log.warn("SSE 订阅数已达上限 {}，拒绝 executionId={} 的订阅",
                    MAX_SSE_SUBSCRIBERS, executionId);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 忽略
            }
            return emitter;
        }

        emitter.onCompletion(() -> log.debug("SSE completed: executionId={}", executionId));
        emitter.onTimeout(() -> log.debug("SSE timed out: executionId={}", executionId));
        emitter.onError(e -> log.warn("SSE error: executionId={}: {}", executionId, e.getMessage()));

        return emitter;
    }

    /** 事件推送主体：先回放历史事件，再轮询增量。 */
    private void pumpEvents(SseEmitter emitter, Long executionId, String after) {
        try {
            // 1. 先回放自 cursor 以来的历史事件，并用 Redis Stream cursor 推进游标。
            String lastId = after;
            RunEventSink.EventPage replayPage = eventSink.fetchEvents(executionId, lastId);
            for (RunEvent event : replayPage.events()) {
                if (!sendEvent(emitter, event) || isTerminalEvent(event)) {
                    return;
                }
            }
            lastId = replayPage.nextCursor();

            // 2. 进入阻塞轮询：新事件通过 Redis Stream 推送。
            while (!Thread.currentThread().isInterrupted()) {
                RunEventSink.EventPage page = eventSink.fetchEvents(executionId, lastId);
                for (RunEvent event : page.events()) {
                    if (!sendEvent(emitter, event) || isTerminalEvent(event)) {
                        return;
                    }
                }
                lastId = page.nextCursor();
                try {
                    Thread.sleep(200); // 200ms 轮询间隔
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("SSE stream error for executionId={}: {}", executionId, e.getMessage());
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean sendEvent(SseEmitter emitter, RunEvent event) {
        try {
            String json = serializeEvent(event);
            emitter.send(SseEmitter.event()
                    .id(event.getStreamId() != null ? event.getStreamId() : event.getEventId())
                    .name(event.getEvent())
                    .data(json));
            return true;
        } catch (Exception e) {
            log.warn("Failed to send SSE event {}: {}", event.getEventId(), e.getMessage());
            return false;
        }
    }

    private boolean isTerminalEvent(RunEvent event) {
        return "execution_succeeded".equals(event.getEvent())
                || "execution_failed".equals(event.getEvent())
                || "execution_cancelled".equals(event.getEvent());
    }

    private String serializeEvent(RunEvent event) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event.getEvent());
            payload.put("id", event.getStreamId() != null ? event.getStreamId() : event.getEventId());
            
            Map<String, Object> data = new HashMap<>();
            data.put("execution_id", event.getExecutionId());
            data.put("workflow_id", event.getWorkflowId());
            if (event.getNodeId() != null) {
                data.put("node_id", event.getNodeId());
            }
            if (event.getNodeType() != null) {
                data.put("node_type", event.getNodeType());
            }
            if (event.getNodeName() != null) {
                data.put("node_name", event.getNodeName());
            }
            if (event.getTimestamp() != null) {
                data.put("timestamp", event.getTimestamp().toString());
            }
            if (event.getData() != null) {
                data.put("data", event.getData());
            }
            
            payload.put("data", data);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize event {}: {}", event.getEventId(), e.getMessage());
            return "{}";
        }
    }
}


