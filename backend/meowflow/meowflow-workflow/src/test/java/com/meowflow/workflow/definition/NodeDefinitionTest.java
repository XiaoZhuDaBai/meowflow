package com.meowflow.workflow.definition;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NodeDefinition 单元测试
 */
class NodeDefinitionTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        NodeDefinition.Position position = NodeDefinition.Position.builder()
                .x(100.0)
                .y(200.0)
                .build();

        // When
        NodeDefinition node = NodeDefinition.builder()
                .id("node-1")
                .type(NodeType.LLM)
                .name("Test Node")
                .position(position)
                .data(data)
                .inputs(new String[]{"input1"})
                .outputs(new String[]{"output1"})
                .build();

        // Then
        assertThat(node.getId()).isEqualTo("node-1");
        assertThat(node.getType()).isEqualTo(NodeType.LLM);
        assertThat(node.getName()).isEqualTo("Test Node");
        assertThat(node.getPosition().getX()).isEqualTo(100.0);
        assertThat(node.getPosition().getY()).isEqualTo(200.0);
        assertThat(node.getData()).containsEntry("key", "value");
        assertThat(node.getInputs()).containsExactly("input1");
        assertThat(node.getOutputs()).containsExactly("output1");
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        NodeDefinition node = new NodeDefinition();

        // Then
        assertThat(node.getId()).isNull();
        assertThat(node.getType()).isNull();
        assertThat(node.getName()).isNull();
        assertThat(node.getPosition()).isNull();
        assertThat(node.getData()).isNull();
    }

    @Test
    void setters_shouldUpdateFields() {
        // Given
        NodeDefinition node = new NodeDefinition();

        // When
        node.setId("node-1");
        node.setType(NodeType.HTTP);
        node.setName("HTTP Node");

        // Then
        assertThat(node.getId()).isEqualTo("node-1");
        assertThat(node.getType()).isEqualTo(NodeType.HTTP);
        assertThat(node.getName()).isEqualTo("HTTP Node");
    }

    @Test
    void isTrigger_whenTypeIsTrigger_returnsTrue() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("trigger-1")
                .type(NodeType.TRIGGER_MANUAL)
                .name("Manual Trigger")
                .build();

        // Then
        assertThat(node.isTrigger()).isTrue();
    }

    @Test
    void isTrigger_whenTypeIsAction_returnsFalse() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("action-1")
                .type(NodeType.LLM)
                .name("LLM Node")
                .build();

        // Then
        assertThat(node.isTrigger()).isFalse();
    }

    @Test
    void isTrigger_whenTypeIsNull_returnsFalse() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("node-1")
                .type(null)
                .build();

        // Then
        assertThat(node.isTrigger()).isFalse();
    }

    @Test
    void isEnd_whenTypeIsEnd_returnsTrue() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("end-1")
                .type(NodeType.END)
                .name("End")
                .build();

        // Then
        assertThat(node.isEnd()).isTrue();
    }

    @Test
    void isEnd_whenTypeIsNotEnd_returnsFalse() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("action-1")
                .type(NodeType.HTTP)
                .name("HTTP Node")
                .build();

        // Then
        assertThat(node.isEnd()).isFalse();
    }

    @Test
    void isEnd_whenTypeIsNull_returnsFalse() {
        // Given
        NodeDefinition node = NodeDefinition.builder()
                .id("node-1")
                .type(null)
                .build();

        // Then
        assertThat(node.isEnd()).isFalse();
    }

    @Test
    void position_shouldStoreCoordinates() {
        // Given
        NodeDefinition.Position position = NodeDefinition.Position.builder()
                .x(50.5)
                .y(75.3)
                .build();

        // Then
        assertThat(position.getX()).isEqualTo(50.5);
        assertThat(position.getY()).isEqualTo(75.3);
    }

    @Test
    void position_setters_shouldWork() {
        // Given
        NodeDefinition.Position position = new NodeDefinition.Position();

        // When
        position.setX(123.45);
        position.setY(678.90);

        // Then
        assertThat(position.getX()).isEqualTo(123.45);
        assertThat(position.getY()).isEqualTo(678.90);
    }

    @Test
    void equalsAndHashCode_byAllArgsConstructor() {
        // Given
        NodeDefinition node1 = NodeDefinition.builder()
                .id("node-1")
                .type(NodeType.LLM)
                .name("Node")
                .build();

        NodeDefinition node2 = NodeDefinition.builder()
                .id("node-1")
                .type(NodeType.LLM)
                .name("Node")
                .build();

        // Then
        assertThat(node1).isEqualTo(node2);
        assertThat(node1.hashCode()).isEqualTo(node2.hashCode());
    }
}
