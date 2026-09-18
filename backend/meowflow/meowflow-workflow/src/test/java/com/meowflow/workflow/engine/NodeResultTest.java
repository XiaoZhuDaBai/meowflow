package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeResultTest {

    @Test
    void success_shouldCreateSuccessfulResult() {
        Map<String, Object> input = Map.of("key", "input");
        Map<String, Object> output = Map.of("result", "success");

        NodeResult result = NodeResult.success("node-1", NodeType.LLM, "LLM Node", output);

        assertNotNull(result);
        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeResult.NodeStatus.SUCCESS, result.getStatus());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailed());
        assertNull(result.getInput());
        assertEquals(output, result.getOutput());
        assertNull(result.getErrorMessage());
    }

    @Test
    void failed_shouldCreateFailedResult() {
        NodeResult result = NodeResult.failed("node-1", NodeType.LLM, "LLM Node", "Execution error", new RuntimeException());

        assertNotNull(result);
        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeResult.NodeStatus.FAILED, result.getStatus());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailed());
        assertEquals("Execution error", result.getErrorMessage());
    }

    @Test
    void skipped_shouldCreateSkippedResult() {
        NodeResult result = NodeResult.skipped("node-1", NodeType.LLM, "LLM Node");

        assertNotNull(result);
        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeResult.NodeStatus.SKIPPED, result.getStatus());
        assertFalse(result.isSuccess());
        assertFalse(result.isFailed());
    }

    @Test
    void pending_shouldCreatePendingResult() {
        NodeResult result = NodeResult.pending("node-1", NodeType.LLM, "LLM Node");

        assertNotNull(result);
        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeResult.NodeStatus.PENDING, result.getStatus());
        assertFalse(result.isSuccess());
        assertFalse(result.isFailed());
    }

    @Test
    void running_shouldCreateRunningResult() {
        NodeResult result = NodeResult.running("node-1", NodeType.LLM, "LLM Node");

        assertNotNull(result);
        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeResult.NodeStatus.RUNNING, result.getStatus());
        assertFalse(result.isSuccess());
        assertFalse(result.isFailed());
    }

    @Test
    void builder_shouldSetAllFields() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(5);

        NodeResult result = NodeResult.builder()
            .nodeId("node-1")
            .nodeType(NodeType.LLM)
            .nodeName("Test Node")
            .status(NodeResult.NodeStatus.SUCCESS)
            .input(Map.of("key", "value"))
            .output(Map.of("result", "done"))
            .errorMessage(null)
            .retryCount(0)
            .costMs(500L)
            .costToken(100)
            .startedAt(startTime)
            .finishedAt(endTime)
            .build();

        assertEquals("node-1", result.getNodeId());
        assertEquals(NodeType.LLM, result.getNodeType());
        assertEquals("Test Node", result.getNodeName());
        assertEquals(NodeResult.NodeStatus.SUCCESS, result.getStatus());
        assertEquals(Map.of("key", "value"), result.getInput());
        assertEquals(Map.of("result", "done"), result.getOutput());
        assertEquals(0, result.getRetryCount());
        assertEquals(500L, result.getCostMs());
        assertEquals(100, result.getCostToken());
        assertEquals(startTime, result.getStartedAt());
        assertEquals(endTime, result.getFinishedAt());
    }

    @Test
    void costTracking_shouldWorkCorrectly() {
        NodeResult result = NodeResult.builder()
            .nodeId("node-1")
            .status(NodeResult.NodeStatus.SUCCESS)
            .startedAt(LocalDateTime.now())
            .build();

        result.setFinishedAt(LocalDateTime.now());
        result.setCostMs(100L);
        result.setCostToken(50);

        assertEquals(100L, result.getCostMs());
        assertEquals(50, result.getCostToken());
    }

    @Test
    void retryCount_shouldBeIncremental() {
        NodeResult result = NodeResult.builder()
            .nodeId("node-1")
            .status(NodeResult.NodeStatus.PENDING)
            .retryCount(0)
            .build();

        assertEquals(0, result.getRetryCount());

        result.setRetryCount(1);
        assertEquals(1, result.getRetryCount());

        result.setRetryCount(2);
        assertEquals(2, result.getRetryCount());
    }

    @Test
    void isFinished_shouldReturnTrueForTerminalStates() {
        NodeResult successResult = NodeResult.success("node-1", NodeType.LLM, "Node", null);
        assertTrue(successResult.isFinished());

        NodeResult failedResult = NodeResult.failed("node-2", NodeType.LLM, "Node", "error", null);
        assertTrue(failedResult.isFinished());

        NodeResult pendingResult = NodeResult.pending("node-3", NodeType.LLM, "Node");
        assertFalse(pendingResult.isFinished());
    }

    @Test
    void getElapsedMs_shouldCalculateDuration() {
        NodeResult result = NodeResult.builder()
            .nodeId("node-1")
            .status(NodeResult.NodeStatus.SUCCESS)
            .startedAt(LocalDateTime.now())
            .finishedAt(LocalDateTime.now().plusSeconds(2))
            .build();

        assertTrue(result.getElapsedMs() >= 2000);
    }
}
