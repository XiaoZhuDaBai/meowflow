package com.meowflow.workflow.definition;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge 单元测试
 */
class EdgeTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Edge.ConditionConfig condition = Edge.ConditionConfig.builder()
                .expression("x > 0")
                .operator(">")
                .value(0)
                .build();

        Map<String, Object> config = new HashMap<>();
        config.put("key", "value");

        Edge.EdgeData data = Edge.EdgeData.builder()
                .label("True Branch")
                .condition(condition)
                .config(config)
                .build();

        // When
        Edge edge = Edge.builder()
                .id("e1")
                .source("node-1")
                .target("node-2")
                .sourceHandle("true")
                .targetHandle("input")
                .type(EdgeType.CONDITION_TRUE)
                .data(data)
                .build();

        // Then
        assertThat(edge.getId()).isEqualTo("e1");
        assertThat(edge.getSource()).isEqualTo("node-1");
        assertThat(edge.getTarget()).isEqualTo("node-2");
        assertThat(edge.getSourceHandle()).isEqualTo("true");
        assertThat(edge.getTargetHandle()).isEqualTo("input");
        assertThat(edge.getType()).isEqualTo(EdgeType.CONDITION_TRUE);
        assertThat(edge.getData().getLabel()).isEqualTo("True Branch");
        assertThat(edge.getData().getCondition().getExpression()).isEqualTo("x > 0");
        assertThat(edge.getData().getConfig()).containsEntry("key", "value");
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        Edge edge = new Edge();

        // Then
        assertThat(edge.getId()).isNull();
        assertThat(edge.getSource()).isNull();
        assertThat(edge.getTarget()).isNull();
        assertThat(edge.getType()).isNull();
    }

    @Test
    void setters_shouldUpdateFields() {
        // Given
        Edge edge = new Edge();

        // When
        edge.setId("e2");
        edge.setSource("A");
        edge.setTarget("B");
        edge.setType(EdgeType.DEFAULT);

        // Then
        assertThat(edge.getId()).isEqualTo("e2");
        assertThat(edge.getSource()).isEqualTo("A");
        assertThat(edge.getTarget()).isEqualTo("B");
        assertThat(edge.getType()).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void conditionConfig_shouldStoreAllConditionFields() {
        // Given
        Edge.ConditionConfig condition = Edge.ConditionConfig.builder()
                .expression("count > 5")
                .operator(">")
                .value(5)
                .build();

        // Then
        assertThat(condition.getExpression()).isEqualTo("count > 5");
        assertThat(condition.getOperator()).isEqualTo(">");
        assertThat(condition.getValue()).isEqualTo(5);
    }

    @Test
    void conditionConfig_setters_shouldWork() {
        // Given
        Edge.ConditionConfig condition = new Edge.ConditionConfig();

        // When
        condition.setExpression("flag == true");
        condition.setOperator("==");
        condition.setValue(true);

        // Then
        assertThat(condition.getExpression()).isEqualTo("flag == true");
        assertThat(condition.getOperator()).isEqualTo("==");
        assertThat(condition.getValue()).isEqualTo(true);
    }

    @Test
    void edgeData_shouldStoreLabelConditionAndConfig() {
        // Given
        Edge.EdgeData data = new Edge.EdgeData();
        Map<String, Object> config = new HashMap<>();
        config.put("priority", 1);

        // When
        data.setLabel("My Edge");
        data.setConfig(config);

        // Then
        assertThat(data.getLabel()).isEqualTo("My Edge");
        assertThat(data.getConfig()).containsEntry("priority", 1);
    }

    @Test
    void equalsAndHashCode_shouldUseAllFields() {
        // Given
        Edge edge1 = Edge.builder()
                .id("e1")
                .source("A")
                .target("B")
                .type(EdgeType.DEFAULT)
                .build();

        Edge edge2 = Edge.builder()
                .id("e1")
                .source("A")
                .target("B")
                .type(EdgeType.DEFAULT)
                .build();

        // Then
        assertThat(edge1).isEqualTo(edge2);
        assertThat(edge1.hashCode()).isEqualTo(edge2.hashCode());
    }
}
