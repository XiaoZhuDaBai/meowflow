package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.NodeType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NodeResult 单元测试
 */
class NodeResultCompleteTest {

    @Test
    void success_shouldCreateSuccessResult() {
        // Given
        Map<String, Object> output = Map.of("result", "value");

        // When
        NodeResult result = NodeResult.success("node-1", NodeType.LLM, "LLM Node", output);

        // Then
        assertThat(result.getNodeId()).isEqualTo("node-1");
        assertThat(result.getNodeType()).isEqualTo(NodeType.LLM);
        assertThat(result.getNodeName()).isEqualTo("LLM Node");
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.SUCCESS);
        assertThat(result.getOutput()).isEqualTo(output);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailed()).isFalse();
        assertThat(result.isFinished()).isTrue();
    }

    @Test
    void failed_shouldStoreStackTrace() {
        // Given
        RuntimeException ex = new RuntimeException("test error");

        // When
        NodeResult result = NodeResult.failed("node-1", NodeType.LLM, "Test", "Error msg", ex);

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Error msg");
        assertThat(result.getStackTrace()).isNotNull();
        assertThat(result.getStackTrace()).contains("RuntimeException");
    }

    @Test
    void failed_withNullException_hasNullStackTrace() {
        // When
        NodeResult result = NodeResult.failed("node-1", NodeType.LLM, "Test", "Error msg", null);

        // Then
        assertThat(result.getStackTrace()).isNull();
    }

    @Test
    void timedOut_shouldIncludeTimeoutInMessage() {
        // When
        NodeResult result = NodeResult.timedOut("node-1", NodeType.LLM, "Test", Duration.ofSeconds(30));

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.TIMED_OUT);
        assertThat(result.getErrorMessage()).contains("30 seconds");
        assertThat(result.getCostMs()).isEqualTo(30000L);
        assertThat(result.isFinished()).isTrue();
    }

    @Test
    void cancelled_shouldUseDefaultReason() {
        // When
        NodeResult result = NodeResult.cancelled("node-1", NodeType.LLM, "Test", null);

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.CANCELLED);
        assertThat(result.getErrorMessage()).isEqualTo("Cancelled");
    }

    @Test
    void cancelled_shouldUseCustomReason() {
        // When
        NodeResult result = NodeResult.cancelled("node-1", NodeType.LLM, "Test", "User cancelled");

        // Then
        assertThat(result.getErrorMessage()).isEqualTo("User cancelled");
    }

    @Test
    void isFinished_forEachTerminalState_returnsTrue() {
        // Success
        NodeResult success = NodeResult.success("n1", NodeType.LLM, "n1", null);
        assertThat(success.isFinished()).isTrue();

        // Skipped
        NodeResult skipped = NodeResult.skipped("n1", NodeType.LLM, "n1");
        assertThat(skipped.isFinished()).isTrue();

        // Failed
        NodeResult failed = NodeResult.failed("n1", NodeType.LLM, "n1", "error", null);
        assertThat(failed.isFinished()).isTrue();

        // Cancelled
        NodeResult cancelled = NodeResult.cancelled("n1", NodeType.LLM, "n1", "reason");
        assertThat(cancelled.isFinished()).isTrue();
    }

    @Test
    void isFinished_forNonTerminalStates_returnsFalse() {
        // Pending
        NodeResult pending = NodeResult.pending("n1", NodeType.LLM, "n1");
        assertThat(pending.isFinished()).isFalse();

        // Running
        NodeResult running = NodeResult.running("n1", NodeType.LLM, "n1");
        assertThat(running.isFinished()).isFalse();

        // Timed out is considered finished (terminal)
        NodeResult timedOut = NodeResult.timedOut("n1", NodeType.LLM, "n1", Duration.ofSeconds(30));
        assertThat(timedOut.isFinished()).isTrue();
    }

    @Test
    void getElapsedMs_whenBothTimesSet_returnsDifference() {
        // Given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(5);

        NodeResult result = NodeResult.builder()
                .startedAt(start)
                .finishedAt(end)
                .build();

        // Then
        assertThat(result.getElapsedMs()).isEqualTo(5000L);
    }

    @Test
    void getElapsedMs_whenTimesNull_returnsZero() {
        // Given
        NodeResult result = new NodeResult();

        // Then
        assertThat(result.getElapsedMs()).isEqualTo(0);
    }

    @Test
    void getElapsedMs_whenOnlyStartTime_returnsZero() {
        // Given
        NodeResult result = NodeResult.builder()
                .startedAt(LocalDateTime.now())
                .finishedAt(null)
                .build();

        // Then
        assertThat(result.getElapsedMs()).isEqualTo(0);
    }

    @Test
    void withCostMs_shouldSetAndReturnSelf() {
        // Given
        NodeResult result = new NodeResult();

        // When
        NodeResult returned = result.withCostMs(1000L);

        // Then
        assertThat(returned).isSameAs(result);
        assertThat(result.getCostMs()).isEqualTo(1000L);
    }

    @Test
    void withStartedAt_shouldSetAndReturnSelf() {
        // Given
        NodeResult result = new NodeResult();
        LocalDateTime now = LocalDateTime.now();

        // When
        NodeResult returned = result.withStartedAt(now);

        // Then
        assertThat(returned).isSameAs(result);
        assertThat(result.getStartedAt()).isEqualTo(now);
    }

    @Test
    void withFinishedAt_shouldSetAndReturnSelf() {
        // Given
        NodeResult result = new NodeResult();
        LocalDateTime now = LocalDateTime.now();

        // When
        NodeResult returned = result.withFinishedAt(now);

        // Then
        assertThat(returned).isSameAs(result);
        assertThat(result.getFinishedAt()).isEqualTo(now);
    }

    @Test
    void withCostToken_shouldSetAndReturnSelf() {
        // Given
        NodeResult result = new NodeResult();

        // When
        NodeResult returned = result.withCostToken(500);

        // Then
        assertThat(returned).isSameAs(result);
        assertThat(result.getCostToken()).isEqualTo(500);
    }

    @Test
    void builder_shouldSupportChaining() {
        // When
        NodeResult result = NodeResult.builder()
                .nodeId("node-1")
                .nodeType(NodeType.HTTP)
                .nodeName("HTTP Node")
                .status(NodeResult.NodeStatus.SUCCESS)
                .costMs(100L)
                .costToken(50)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .build();

        // Then
        assertThat(result.getNodeId()).isEqualTo("node-1");
        assertThat(result.getCostMs()).isEqualTo(100L);
    }

    @Test
    void pending_shouldSetStartedAt() {
        // Given
        NodeResult result = NodeResult.pending("node-1", NodeType.LLM, "LLM Node");

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.PENDING);
        assertThat(result.getStartedAt()).isNotNull();
    }

    @Test
    void running_shouldSetStartedAt() {
        // Given
        NodeResult result = NodeResult.running("node-1", NodeType.LLM, "LLM Node");

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.RUNNING);
        assertThat(result.getStartedAt()).isNotNull();
    }

    @Test
    void success_shouldSetFinishedAt() {
        // Given
        NodeResult result = NodeResult.success("node-1", NodeType.LLM, "LLM Node", new HashMap<>());

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.SUCCESS);
        assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    void skipped_shouldSetFinishedAt() {
        // Given
        NodeResult result = NodeResult.skipped("node-1", NodeType.LLM, "LLM Node");

        // Then
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.SKIPPED);
        assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    void nodeStatus_enumValues_areAllTerminalOrNonTerminal() {
        // Verify enum has expected values
        assertThat(NodeResult.NodeStatus.values()).contains(
                NodeResult.NodeStatus.PENDING,
                NodeResult.NodeStatus.RUNNING,
                NodeResult.NodeStatus.SUCCESS,
                NodeResult.NodeStatus.SKIPPED,
                NodeResult.NodeStatus.FAILED,
                NodeResult.NodeStatus.CANCELLED,
                NodeResult.NodeStatus.TIMED_OUT
        );
    }

    @Test
    void settersAndGetters_allFields() {
        // Given
        NodeResult result = new NodeResult();
        Map<String, Object> input = Map.of("k", "v");
        Map<String, Object> output = Map.of("r", "x");

        // When
        result.setNodeId("node-1");
        result.setNodeType(NodeType.LLM);
        result.setNodeName("Test Node");
        result.setStatus(NodeResult.NodeStatus.SUCCESS);
        result.setInput(input);
        result.setOutput(output);
        result.setErrorMessage(null);
        result.setRetryCount(2);
        result.setCostMs(100L);
        result.setCostToken(50);

        // Then
        assertThat(result.getNodeId()).isEqualTo("node-1");
        assertThat(result.getNodeType()).isEqualTo(NodeType.LLM);
        assertThat(result.getNodeName()).isEqualTo("Test Node");
        assertThat(result.getStatus()).isEqualTo(NodeResult.NodeStatus.SUCCESS);
        assertThat(result.getInput()).isSameAs(input);
        assertThat(result.getOutput()).isSameAs(output);
        assertThat(result.getRetryCount()).isEqualTo(2);
        assertThat(result.getCostMs()).isEqualTo(100L);
        assertThat(result.getCostToken()).isEqualTo(50);
    }
}
