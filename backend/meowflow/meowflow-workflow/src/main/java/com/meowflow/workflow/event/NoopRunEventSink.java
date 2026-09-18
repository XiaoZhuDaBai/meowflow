package com.meowflow.workflow.event;

import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.entity.ExecutionSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 无操作事件下沉器（默认实现）。
 *
 * <p>当 Redis 不可用或 {@code workflow.event.sink=noop} 时启用。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "workflow.event.sink", havingValue = "noop", matchIfMissing = false)
public class NoopRunEventSink implements RunEventSink {

    @Override
    public void append(RunEvent event) {
        log.trace("[NoopRunEventSink] event: {}", event.getEvent());
    }

    @Override
    public void updateExecutionStatus(Long executionId, String status, Map<String, Object> output) {
        log.trace("[NoopRunEventSink] executionId={}, status={}", executionId, status);
    }

    @Override
    public int createSnapshot(ExecutionContext context, String waitingReason, String lastEventId) {
        log.trace("[NoopRunEventSink] createSnapshot for executionId={}", context.getExecutionId());
        return 0;
    }

    @Override
    public Optional<ExecutionSnapshot> getLatestSnapshot(Long executionId) {
        return Optional.empty();
    }

    @Override
    public EventPage fetchEvents(Long executionId, String cursor) {
        return new EventPage(Collections.emptyList(), cursor);
    }
}
