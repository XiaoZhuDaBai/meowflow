package com.meowflow.workflow.engine;

import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.executor.NodeRegistry;
import com.meowflow.workflow.executor.NodeExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowEngineTest {

    @Mock
    private NodeRegistry nodeRegistry;

    @Mock
    private EdgeRouter edgeRouter;

    @Mock
    private NodeExecutionRecorder nodeExecutionRecorder;

    private WorkflowEngine workflowEngine;
    private Executor executor;
    private CancellationRegistry cancellationRegistry;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        cancellationRegistry = new CancellationRegistry();
        meterRegistry = new SimpleMeterRegistry();
        when(edgeRouter.selectDownstreamEdges(
                any(NodeDefinition.class), anyList(), any(NodeResult.class),
                any(WorkflowDefinition.class), any(ExecutionContext.class)))
                .thenAnswer(invocation -> ((List<Edge>) invocation.getArgument(1)).stream()
                        .map(Edge::getTarget)
                        .toList());
        workflowEngine = new WorkflowEngine(nodeRegistry, edgeRouter, nodeExecutionRecorder,
                cancellationRegistry, executor, meterRegistry);
    }

    @Test
    void execute_shouldReturnSuccessResult() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition llm = createNode("llm", NodeType.LLM);
        NodeDefinition end = createNode("end", NodeType.END);

        List<Edge> edges = List.of(
            createEdge("trigger", "llm"),
            createEdge("llm", "end")
        );

        WorkflowDefinition definition = createDefinition(List.of(trigger, llm, end), edges);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any())).thenAnswer(invocation -> {
            NodeDefinition node = invocation.getArgument(1);
            Map<String, Object> output = "llm".equals(node.getId())
                    ? Map.of("result", "done")
                    : Map.of("status", "ok");
            return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
        });

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(mockExecutor);
        when(nodeRegistry.getExecutor(NodeType.LLM)).thenReturn(mockExecutor);
        when(nodeRegistry.getExecutor(NodeType.END)).thenReturn(mockExecutor);

        ExecutionContext context = ExecutionContext.builder()
            .executionId(1L)
            .workflowId(1L)
            .version("v1")
            .definition(definition)
            .input(Map.of("key", "value"))
            .variables(new HashMap<>())
            .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertNotNull(result.getOutput());
    }

    @Test
    void execute_shouldThrowExceptionWhenNoTriggerNodes() {
        NodeDefinition node1 = createNode("n1", NodeType.LLM);
        List<Edge> edges = List.of();

        WorkflowDefinition definition = createDefinition(List.of(node1), edges);

        ExecutionContext context = ExecutionContext.builder()
            .executionId(1L)
            .workflowId(1L)
            .version("v1")
            .definition(definition)
            .input(Map.of())
            .variables(new HashMap<>())
            .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertEquals("failed", result.getStatus());
        assertTrue(result.getErrorMessage().contains("触发节点"));
    }

    @Test
    void execute_shouldHandleNodeExecutionFailure() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition llm = createNode("llm", NodeType.LLM);

        List<Edge> edges = List.of(createEdge("trigger", "llm"));

        WorkflowDefinition definition = createDefinition(List.of(trigger, llm), edges);

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any())).thenAnswer(invocation -> {
            NodeDefinition node = invocation.getArgument(1);
            if ("llm".equals(node.getId())) {
                return NodeResult.failed(node.getId(), node.getType(), node.getName(), "LLM调用失败", new RuntimeException());
            }
            return NodeResult.success(node.getId(), node.getType(), node.getName(), null);
        });

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(mockExecutor);
        when(nodeRegistry.getExecutor(NodeType.LLM)).thenReturn(mockExecutor);

        ExecutionContext context = ExecutionContext.builder()
            .executionId(1L)
            .workflowId(1L)
            .version("v1")
            .definition(definition)
            .input(Map.of())
            .variables(new HashMap<>())
            .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertNotNull(result);
        assertEquals("failed", result.getStatus());
    }

    @Test
    void execute_shouldRetryNodeWhenConfigured() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition llm = createNode("llm", NodeType.LLM);
        llm.setData(Map.of("config", Map.of("retries", 1, "retryOnFail", true)));
        NodeDefinition end = createNode("end", NodeType.END);
        WorkflowDefinition definition = createDefinition(
                List.of(trigger, llm, end),
                List.of(createEdge("trigger", "llm"), createEdge("llm", "end")));

        AtomicInteger calls = new AtomicInteger();
        NodeExecutor triggerExecutor = mockExecutor(NodeResult.success("trigger", NodeType.TRIGGER_MANUAL, "trigger", Map.of()));
        NodeExecutor llmExecutor = mock(NodeExecutor.class);
        when(llmExecutor.execute(any(), any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                return NodeResult.failed("llm", NodeType.LLM, "llm", "boom", null);
            }
            return NodeResult.success("llm", NodeType.LLM, "llm", Map.of("ok", true));
        });
        NodeExecutor endExecutor = mockExecutor(NodeResult.success("end", NodeType.END, "end", Map.of("finished", true)));

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(triggerExecutor);
        when(nodeRegistry.getExecutor(NodeType.LLM)).thenReturn(llmExecutor);
        when(nodeRegistry.getExecutor(NodeType.END)).thenReturn(endExecutor);

        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .version("v1")
                .definition(definition)
                .input(Map.of())
                .variables(new HashMap<>())
                .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(2, calls.get());
    }

    @Test
    void execute_shouldUseFallbackValueAfterFailure() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition llm = createNode("llm", NodeType.LLM);
        llm.setData(Map.of("config", Map.of("fallbackValue", "fallback")));
        NodeDefinition end = createNode("end", NodeType.END);
        WorkflowDefinition definition = createDefinition(
                List.of(trigger, llm, end),
                List.of(createEdge("trigger", "llm"), createEdge("llm", "end")));

        NodeExecutor triggerExecutor = mockExecutor(NodeResult.success("trigger", NodeType.TRIGGER_MANUAL, "trigger", Map.of()));
        NodeExecutor llmExecutor = mock(NodeExecutor.class);
        when(llmExecutor.execute(any(), any())).thenReturn(
                NodeResult.failed("llm", NodeType.LLM, "llm", "boom", null));
        NodeExecutor endExecutor = mockExecutor(NodeResult.success("end", NodeType.END, "end", Map.of("finished", true)));

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(triggerExecutor);
        when(nodeRegistry.getExecutor(NodeType.LLM)).thenReturn(llmExecutor);
        when(nodeRegistry.getExecutor(NodeType.END)).thenReturn(endExecutor);

        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .version("v1")
                .definition(definition)
                .input(Map.of())
                .variables(new HashMap<>())
                .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    void execute_doesNotRunUnselectedConditionBranch() throws Exception {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition condition = createNode("condition", NodeType.CONDITION);
        NodeDefinition yes = createNode("yes", NodeType.END);
        NodeDefinition no = createNode("no", NodeType.NOTIFY);
        NodeDefinition end = createNode("end", NodeType.END);

        Edge yesEdge = createEdge("condition", "yes");
        yesEdge.setType(com.meowflow.workflow.definition.EdgeType.CONDITION);
        yesEdge.setData(com.meowflow.workflow.definition.Edge.EdgeData.builder().label("通过").build());
        Edge noEdge = createEdge("condition", "no");
        noEdge.setType(com.meowflow.workflow.definition.EdgeType.CONDITION);
        noEdge.setData(com.meowflow.workflow.definition.Edge.EdgeData.builder().label("失败").build());

        WorkflowDefinition definition = createDefinition(
                List.of(trigger, condition, yes, no, end),
                List.of(
                        createEdge("trigger", "condition"),
                        yesEdge,
                        noEdge,
                        createEdge("yes", "end"),
                        createEdge("no", "end")
                ));

        NodeExecutor triggerExecutor = mockExecutor(NodeResult.success("trigger", NodeType.TRIGGER_MANUAL, "trigger", Map.of()));
        NodeExecutor conditionExecutor = mockExecutor(NodeResult.success("condition", NodeType.CONDITION, "condition",
                Map.of("selectedBranch", "true")));
        NodeExecutor yesExecutor = mockExecutor(NodeResult.success("yes", NodeType.END, "yes", Map.of("finished", true)));
        java.util.concurrent.atomic.AtomicBoolean noExecuted = new java.util.concurrent.atomic.AtomicBoolean(false);
        NodeExecutor noExecutor = mock(NodeExecutor.class);
        when(noExecutor.execute(any(), any())).thenAnswer(invocation -> {
            noExecuted.set(true);
            return NodeResult.success("no", NodeType.END, "no", Map.of());
        });
        NodeExecutor endExecutor = mockExecutor(NodeResult.success("end", NodeType.END, "end", Map.of("finished", true)));

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(triggerExecutor);
        when(nodeRegistry.getExecutor(NodeType.CONDITION)).thenReturn(conditionExecutor);
        when(nodeRegistry.getExecutor(NodeType.END)).thenReturn(yesExecutor);
        when(nodeRegistry.getExecutor(NodeType.NOTIFY)).thenReturn(noExecutor);

        // 用真实 EdgeRouter 验证条件分支只推进选中边。
        WorkflowEngine engine = new WorkflowEngine(nodeRegistry, new EdgeRouter(),
                nodeExecutionRecorder, cancellationRegistry, executor, meterRegistry);
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .version("v1")
                .definition(definition)
                .input(Map.of())
                .variables(new HashMap<>())
                .build();

        ExecutionResult result = engine.execute(context);

        assertTrue(result.isSuccess());
        assertFalse(noExecuted.get());
        assertThat(context.getNodeResult("no").getStatus()).isEqualTo(NodeResult.NodeStatus.SKIPPED);
    }

    @Test
    void execute_shouldReturnFailedResultForUnsupportedNodeType() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition unknown = createNode("unknown", NodeType.HTTP);

        List<Edge> edges = List.of(createEdge("trigger", "unknown"));

        WorkflowDefinition definition = createDefinition(List.of(trigger, unknown), edges);

        NodeExecutor triggerExecutor = mock(NodeExecutor.class);
        when(triggerExecutor.execute(any(), any())).thenAnswer(inv -> {
            NodeDefinition node = inv.getArgument(1);
            return NodeResult.success(node.getId(), node.getType(), node.getName(), null);
        });
        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(triggerExecutor);
        when(nodeRegistry.getExecutor(NodeType.HTTP)).thenReturn(null);

        ExecutionContext context = ExecutionContext.builder()
            .executionId(1L)
            .workflowId(1L)
            .version("v1")
            .definition(definition)
            .input(Map.of())
            .variables(new HashMap<>())
            .build();

        ExecutionResult result = workflowEngine.execute(context);

        assertEquals("failed", result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void validateDefinition_shouldReturnFalseForNullNodes() {
        WorkflowDefinition definition = WorkflowDefinition.builder().build();
        definition.setNodes(null);

        assertFalse(workflowEngine.validateDefinition(definition));
    }

    @Test
    void validateDefinition_shouldReturnFalseForEmptyNodes() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setNodes(List.of());

        assertFalse(workflowEngine.validateDefinition(definition));
    }

    @Test
    void validateDefinition_shouldReturnFalseForNoTriggerNodes() {
        NodeDefinition node1 = createNode("n1", NodeType.LLM);
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setNodes(List.of(node1));

        assertFalse(workflowEngine.validateDefinition(definition));
    }

    @Test
    void validateDefinition_shouldReturnTrueForValidDefinition() {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition end = createNode("end", NodeType.END);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setNodes(List.of(trigger, end));
        definition.setEdges(List.of(createEdge("trigger", "end")));

        assertTrue(workflowEngine.validateDefinition(definition));
    }

    @Test
    void executeAsync_shouldReturnCompletableFuture() throws Exception {
        NodeDefinition trigger = createNode("trigger", NodeType.TRIGGER_MANUAL);

        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setNodes(List.of(trigger));
        definition.setEdges(List.of());

        NodeExecutor mockExecutor = mock(NodeExecutor.class);
        when(mockExecutor.execute(any(), any())).thenReturn(
            NodeResult.success("trigger", NodeType.TRIGGER_MANUAL, "trigger", null));

        when(nodeRegistry.getExecutor(NodeType.TRIGGER_MANUAL)).thenReturn(mockExecutor);

        ExecutionContext context = ExecutionContext.builder()
            .executionId(1L)
            .workflowId(1L)
            .version("v1")
            .definition(definition)
            .input(Map.of())
            .variables(new HashMap<>())
            .build();

        ExecutionResult result = workflowEngine.executeAsync(context).get();

        assertNotNull(result);
        assertEquals("success", result.getStatus());
    }

    private NodeExecutor mockExecutor(NodeResult result) {
        NodeExecutor executor = mock(NodeExecutor.class);
        when(executor.execute(any(), any())).thenReturn(result);
        return executor;
    }

    private NodeDefinition createNode(String id, NodeType type) {
        return NodeDefinition.builder()
            .id(id)
            .name(id)
            .type(type)
            .build();
    }

    private Edge createEdge(String source, String target) {
        Edge edge = new Edge();
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }

    private WorkflowDefinition createDefinition(List<NodeDefinition> nodes, List<Edge> edges) {
        return WorkflowDefinition.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
}
