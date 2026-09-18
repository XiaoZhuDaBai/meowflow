package com.meowflow.workflow.executor.flow;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WaitExecutor 单元测试
 */
class WaitExecutorTest {

    private final WaitExecutor executor = new WaitExecutor();

    private NodeDefinition createWaitNode(Map<String, Object> data) {
        return NodeDefinition.builder()
                .id("wait-1")
                .type(NodeType.WAIT)
                .name("Wait Node")
                .data(data)
                .build();
    }

    @Test
    void getNodeType_shouldReturnWait() {
        assertThat(executor.getNodeType()).isEqualTo(NodeType.WAIT);
    }

    @Test
    void execute_withDefaultDuration_waits1Second() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        NodeDefinition node = createWaitNode(null);

        // When
        long startTime = System.currentTimeMillis();
        NodeResult result = executor.execute(ctx, node);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("waited", true);
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(1000L);
        assertThat(elapsed).isGreaterThanOrEqualTo(900L); // Allow 100ms tolerance
    }

    @Test
    void execute_withDurationMsKey() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("durationMs", 200);
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(200L);
    }

    @Test
    void execute_withWaitMsKey() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("waitMs", 250);
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(250L);
    }

    @Test
    void execute_withDurationKey() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("duration", 150);
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(150L);
    }

    @Test
    void execute_withNumericValue() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = new HashMap<>();
        data.put("durationMs", 100); // Integer
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(100L);
    }

    @Test
    void execute_capsAt5Minutes() throws Exception {
        // Given - request 10 minutes (way over limit), but don't really sleep in unit test.
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("durationMs", 600_000L); // 10 minutes
        NodeDefinition node = createWaitNode(data);
        AtomicLong sleptMs = new AtomicLong(-1);
        WaitExecutor noWaitExecutor = new WaitExecutor() {
            @Override
            protected void sleep(long waitMs) {
                sleptMs.set(waitMs);
            }
        };

        // When
        NodeResult result = noWaitExecutor.execute(ctx, node);

        // Then - should be capped to 5 minutes (300000ms)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(300_000L);
        assertThat(sleptMs.get()).isEqualTo(300_000L);
    }

    @Test
    void execute_handlesZeroWaitTime() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("durationMs", 0);
        NodeDefinition node = createWaitNode(data);

        // When
        long startTime = System.currentTimeMillis();
        NodeResult result = executor.execute(ctx, node);
        long elapsed = System.currentTimeMillis() - startTime;

        // Then - should complete almost immediately
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(0L);
        assertThat(elapsed).isLessThan(50); // Should be very fast
    }

    @Test
    void execute_handlesNegativeWaitTime() throws Exception {
        // Given - negative should be treated as 0
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = Map.of("durationMs", -100);
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(0L); // clamped to 0
    }

    @Test
    void execute_withInvalidStringValue_fallsBackToDefault() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = new HashMap<>();
        data.put("durationMs", "not a number");
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then - falls back to default 1000ms
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(1000L);
    }

    @Test
    void execute_prioritizesDurationMsOverWaitMs() throws Exception {
        // Given - both keys set, durationMs takes precedence
        ExecutionContext ctx = new ExecutionContext();
        Map<String, Object> data = new HashMap<>();
        data.put("durationMs", 100);
        data.put("waitMs", 500);
        NodeDefinition node = createWaitNode(data);

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then - durationMs wins
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("waitedMs")).isEqualTo(100L);
    }

    @Test
    void execute_result_containsWaitedFlag() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        NodeDefinition node = createWaitNode(Map.of("durationMs", 50));

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.getOutput()).containsEntry("waited", true);
    }

    @Test
    void execute_setsCostMs() throws Exception {
        // Given
        ExecutionContext ctx = new ExecutionContext();
        NodeDefinition node = createWaitNode(Map.of("durationMs", 100));

        // When
        NodeResult result = executor.execute(ctx, node);

        // Then
        assertThat(result.getCostMs()).isNotNull().isGreaterThanOrEqualTo(100L);
    }

    @Test
    void concurrentExecution_handlesIndependently() throws Exception {
        // Given
        NodeDefinition node = createWaitNode(Map.of("durationMs", 100));

        // When - run in parallel
        java.util.concurrent.CompletableFuture<NodeResult> f1 = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> executor.execute(new ExecutionContext(), node));
        java.util.concurrent.CompletableFuture<NodeResult> f2 = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> executor.execute(new ExecutionContext(), node));

        NodeResult r1 = f1.get(5, TimeUnit.SECONDS);
        NodeResult r2 = f2.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(r1.isSuccess()).isTrue();
        assertThat(r2.isSuccess()).isTrue();
    }
}

