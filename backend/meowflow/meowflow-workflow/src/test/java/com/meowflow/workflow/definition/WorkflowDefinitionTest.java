package com.meowflow.workflow.definition;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkflowDefinition 单元测试
 */
class WorkflowDefinitionTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "value1");

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        // When
        WorkflowDefinition def = WorkflowDefinition.builder()
                .workflowId("wf-1")
                .version("v1.0")
                .variables(variables)
                .inputSchema(schema)
                .outputSchema(schema)
                .config(Map.of("key", "value"))
                .build();

        // Then
        assertThat(def.getWorkflowId()).isEqualTo("wf-1");
        assertThat(def.getVersion()).isEqualTo("v1.0");
        assertThat(def.getVariables()).containsEntry("var1", "value1");
        assertThat(def.getInputSchema()).containsEntry("type", "object");
        assertThat(def.getConfig()).containsEntry("key", "value");
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        WorkflowDefinition def = new WorkflowDefinition();

        // Then
        assertThat(def.getWorkflowId()).isNull();
        assertThat(def.getVersion()).isNull();
        assertThat(def.getNodes()).isNull();
        assertThat(def.getEdges()).isNull();
    }

    @Test
    void findNode_whenNodeExists_returnsNode() {
        // Given
        NodeDefinition trigger = node("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition action = node("action", NodeType.LLM);
        NodeDefinition endNode = node("end", NodeType.END);

        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(trigger, action, endNode))
                .build();

        // When
        NodeDefinition found = def.findNode("action");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("action");
        assertThat(found.getType()).isEqualTo(NodeType.LLM);
    }

    @Test
    void findNode_whenNodeNotExists_returnsNull() {
        // Given
        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(node("node-1", NodeType.LLM)))
                .build();

        // When
        NodeDefinition found = def.findNode("nonexistent");

        // Then
        assertThat(found).isNull();
    }

    @Test
    void findNode_whenNodesIsNull_returnsNull() {
        // Given
        WorkflowDefinition def = new WorkflowDefinition();

        // When
        NodeDefinition found = def.findNode("any");

        // Then
        assertThat(found).isNull();
    }

    @Test
    void getTriggerNodes_shouldReturnOnlyTriggers() {
        // Given
        NodeDefinition trigger1 = node("trigger1", NodeType.TRIGGER_MANUAL);
        NodeDefinition trigger2 = node("trigger2", NodeType.TRIGGER_WEBHOOK);
        NodeDefinition action = node("action", NodeType.LLM);
        NodeDefinition endNode = node("end", NodeType.END);

        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(trigger1, trigger2, action, endNode))
                .build();

        // When
        List<NodeDefinition> triggers = def.getTriggerNodes();

        // Then
        assertThat(triggers).hasSize(2);
        assertThat(triggers).extracting(NodeDefinition::getId)
                .containsExactlyInAnyOrder("trigger1", "trigger2");
    }

    @Test
    void getTriggerNodes_whenNoTriggers_returnsEmpty() {
        // Given
        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(node("action", NodeType.LLM)))
                .build();

        // When
        List<NodeDefinition> triggers = def.getTriggerNodes();

        // Then
        assertThat(triggers).isEmpty();
    }

    @Test
    void getTriggerNodes_whenNodesIsNull_returnsEmpty() {
        // Given
        WorkflowDefinition def = new WorkflowDefinition();

        // When
        List<NodeDefinition> triggers = def.getTriggerNodes();

        // Then
        assertThat(triggers).isEmpty();
    }

    @Test
    void getEndNodes_shouldReturnOnlyEnds() {
        // Given
        NodeDefinition trigger = node("trigger", NodeType.TRIGGER_MANUAL);
        NodeDefinition action = node("action", NodeType.LLM);
        NodeDefinition end1 = node("end", NodeType.END);
        NodeDefinition end2 = node("aggregator", NodeType.AGGREGATOR);

        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(trigger, action, end1, end2))
                .build();

        // When
        List<NodeDefinition> ends = def.getEndNodes();

        // Then
        assertThat(ends).hasSize(2);
        assertThat(ends).extracting(NodeDefinition::getId)
                .containsExactlyInAnyOrder("end", "aggregator");
    }

    @Test
    void getEndNodes_whenNoEnds_returnsEmpty() {
        // Given
        WorkflowDefinition def = WorkflowDefinition.builder()
                .nodes(List.of(node("action", NodeType.LLM)))
                .build();

        // When
        List<NodeDefinition> ends = def.getEndNodes();

        // Then
        assertThat(ends).isEmpty();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        WorkflowDefinition def = new WorkflowDefinition();
        List<NodeDefinition> nodes = List.of(node("n1", NodeType.LLM));
        List<Edge> edges = List.of();

        // When
        def.setWorkflowId("wf-2");
        def.setVersion("v2");
        def.setNodes(nodes);
        def.setEdges(edges);
        def.setVariables(Map.of("k", "v"));

        // Then
        assertThat(def.getWorkflowId()).isEqualTo("wf-2");
        assertThat(def.getVersion()).isEqualTo("v2");
        assertThat(def.getNodes()).isSameAs(nodes);
        assertThat(def.getEdges()).isSameAs(edges);
        assertThat(def.getVariables()).containsEntry("k", "v");
    }

    @Test
    void equalsAndHashCode_shouldUseAllFields() {
        // Given
        WorkflowDefinition def1 = WorkflowDefinition.builder()
                .workflowId("wf-1")
                .version("v1")
                .build();

        WorkflowDefinition def2 = WorkflowDefinition.builder()
                .workflowId("wf-1")
                .version("v1")
                .build();

        // Then
        assertThat(def1).isEqualTo(def2);
        assertThat(def1.hashCode()).isEqualTo(def2.hashCode());
    }

    private NodeDefinition node(String id, NodeType type) {
        return NodeDefinition.builder().id(id).type(type).name(id).build();
    }
}
