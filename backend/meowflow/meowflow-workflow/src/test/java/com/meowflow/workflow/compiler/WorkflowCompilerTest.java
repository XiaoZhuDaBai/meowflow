package com.meowflow.workflow.compiler;

import com.meowflow.common.exception.WorkflowException;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.EdgeType;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.executor.NodeExecutor;
import com.meowflow.workflow.executor.NodeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WorkflowCompilerTest {

    @Test
    void compile_rejectsNodesWithoutRegisteredExecutor() {
        NodeRegistry registry = registryWithManualAndEnd();
        WorkflowCompiler compiler = new WorkflowCompiler(registry);
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(
                        node("trigger", NodeType.TRIGGER_MANUAL),
                        node("code", NodeType.CODE),
                        node("end", NodeType.END)
                ))
                .edges(List.of(
                        edge("trigger", "code"),
                        edge("code", "end")
                ))
                .build();

        assertThatThrownBy(() -> compiler.compile(definition, 1L))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("未注册执行器");
    }

    @Test
    void compile_buildsPlanForSupportedNodes() {
        NodeRegistry registry = registryWithManualAndEnd();
        WorkflowCompiler compiler = new WorkflowCompiler(registry);
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .nodes(List.of(
                        node("trigger", NodeType.TRIGGER_MANUAL),
                        node("end", NodeType.END)
                ))
                .edges(List.of(edge("trigger", "end")))
                .build();

        WorkflowCompiler.ExecutionPlan plan = compiler.compile(definition, 1L);

        assertThat(plan).isNotNull();
        assertThat(plan.getTriggerNodeIds()).containsExactly("trigger");
    }

    @Test
    void jsonDeserialization_acceptsFrontendNodeAndEdgeTypeAliases() {
        String json = """
                {
                  "nodes": [
                    {"id": "t", "type": "trigger.webhook", "name": "Webhook"},
                    {"id": "e", "type": "end.aggregator", "name": "End"}
                  ],
                  "edges": [
                    {"id": "e1", "source": "t", "target": "e", "type": "condition"}
                  ]
                }
                """;

        WorkflowDefinition definition = JsonUtils.fromJson(json, WorkflowDefinition.class);

        assertThat(definition.getNodes().get(0).getType()).isEqualTo(NodeType.TRIGGER_WEBHOOK);
        assertThat(definition.getNodes().get(1).getType()).isEqualTo(NodeType.AGGREGATOR);
        assertThat(definition.getEdges().get(0).getType()).isEqualTo(EdgeType.CONDITION);
    }

    private NodeRegistry registryWithManualAndEnd() {
        NodeExecutor executor = mock(NodeExecutor.class);
        NodeRegistry registry = new NodeRegistry(List.of());
        registry.register(NodeType.TRIGGER_MANUAL, executor);
        registry.register(NodeType.END, executor);
        return registry;
    }

    private NodeDefinition node(String id, NodeType type) {
        return NodeDefinition.builder().id(id).type(type).name(id).build();
    }

    private Edge edge(String source, String target) {
        return Edge.builder().source(source).target(target).build();
    }
}
