package com.meowflow.workflow.executor.condition;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.engine.EdgeRouter;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeExecutionRecorder;
import com.meowflow.workflow.engine.CancellationRegistry;
import com.meowflow.workflow.executor.NodeRegistry;
import com.meowflow.workflow.executor.transform.TransformExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LoopSubgraphDriverTest {

    @Test
    void executeSubgraph_runsSubgraphNodesForEachItem() {
        NodeDefinition loopNode = NodeDefinition.builder()
                .id("loop")
                .type(NodeType.LOOP)
                .name("Loop")
                .data(Map.of("subgraphNodes", List.of("n1")))
                .build();
        NodeDefinition subNode = NodeDefinition.builder()
                .id("n1")
                .type(NodeType.TRANSFORM)
                .name("Transform")
                .data(Map.of("template", "{{loop.currentItem.value}}"))
                .build();
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(loopNode, subNode))
                .edges(List.of())
                .build();

        NodeRegistry registry = new NodeRegistry(List.of(new TransformExecutor()));
        registry.init();
        LoopSubgraphDriver driver = new LoopSubgraphDriver(
                new EdgeRouter(),
                mock(NodeExecutionRecorder.class),
                new CancellationRegistry(),
                Executors.newSingleThreadExecutor(),
                registry);

        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .version("v1")
                .definition(definition)
                .variables(new java.util.HashMap<>())
                .build();
        Map<String, Object> input = Map.of("items", List.of("a", "b"));

        LoopSubgraphDriver.LoopExecutionResult result = driver.executeSubgraph(
                context, loopNode, input, new CancellationToken(1L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getIterations()).isEqualTo(2);
        assertThat(result.getIterationResults()).hasSize(2);
        assertThat(((Map<String, Object>) result.getIterationResults().get(0).get("output")))
                .containsEntry("text", "a");
        assertThat(((Map<String, Object>) result.getIterationResults().get(1).get("output")))
                .containsEntry("text", "b");
    }

    @Test
    void executeSubgraph_runsEmbeddedSubgraphFromConfig() {
        NodeDefinition loopNode = NodeDefinition.builder()
                .id("loop")
                .type(NodeType.LOOP)
                .name("Loop")
                .data(Map.of("config", Map.of("subgraph", Map.of(
                        "nodes", List.of(Map.of(
                                "id", "embedded-1",
                                "type", "flow.template-transform",
                                "name", "Transform",
                                "data", Map.of("config", Map.of("template", "{{item.value}}"))
                        )),
                        "edges", List.of()
                ))))
                .build();
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(loopNode))
                .edges(List.of())
                .build();

        NodeRegistry registry = new NodeRegistry(List.of(new TransformExecutor()));
        registry.init();
        LoopSubgraphDriver driver = new LoopSubgraphDriver(
                new EdgeRouter(),
                mock(NodeExecutionRecorder.class),
                new CancellationRegistry(),
                Executors.newSingleThreadExecutor(),
                registry);
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .version("v1")
                .definition(definition)
                .variables(new java.util.HashMap<>())
                .build();

        LoopSubgraphDriver.LoopExecutionResult result = driver.executeSubgraph(
                context, loopNode, Map.of("items", List.of("x")), new CancellationToken(1L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((Map<String, Object>) result.getIterationResults().get(0).get("output")))
                .containsEntry("text", "x");
    }
}
