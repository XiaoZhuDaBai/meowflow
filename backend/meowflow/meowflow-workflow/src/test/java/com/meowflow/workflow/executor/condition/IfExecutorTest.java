package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IfExecutor Tests")
class IfExecutorTest {

    private IfExecutor executor;
    private ExecutionContext context;
    private NodeDefinition node;

    @BeforeEach
    void setUp() {
        executor = new IfExecutor();
        context = ExecutionContext.builder().build();
        node = NodeDefinition.builder()
                .id("if-1")
                .type(NodeType.IF)
                .name("Test IF")
                .data(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("getNodeType returns IF")
    void getNodeType_returnsIF() {
        assertThat(executor.getNodeType()).isEqualTo(NodeType.IF);
    }

    @Test
    @DisplayName("supports returns true for IF type")
    void supports_returnsTrueForIF() {
        assertThat(executor.supports(NodeType.IF)).isTrue();
    }

    @Test
    @DisplayName("supports returns false for non-IF type")
    void supports_returnsFalseForOther() {
        assertThat(executor.supports(NodeType.LLM)).isFalse();
    }

    @Test
    @DisplayName("executes true branch when condition is true string")
    void execute_trueCondition_selectsTrueBranch() {
        Map<String, Object> input = Map.of("condition", "true");

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput())
                .containsEntry("result", true)
                .containsEntry("selectedBranch", "true");
    }

    @Test
    @DisplayName("executes false branch when condition is false string")
    void execute_falseCondition_selectsFalseBranch() {
        Map<String, Object> input = Map.of("condition", "false");

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput())
                .containsEntry("result", false)
                .containsEntry("selectedBranch", "false");
    }

    @Test
    @DisplayName("treats '1' as true")
    void execute_numericOne_returnsTrue() {
        Map<String, Object> input = Map.of("condition", "1");

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.getOutput()).containsEntry("result", true);
    }

    @Test
    @DisplayName("treats '0' as false")
    void execute_numericZero_returnsFalse() {
        Map<String, Object> input = Map.of("condition", "0");

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.getOutput()).containsEntry("result", false);
    }

    @Test
    @DisplayName("handles Boolean true directly")
    void execute_booleanTrue_returnsTrue() {
        Map<String, Object> input = Map.of("condition", Boolean.TRUE);

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.getOutput()).containsEntry("result", true);
    }

    @Test
    @DisplayName("handles numeric non-zero as true")
    void execute_numericPositive_returnsTrue() {
        Map<String, Object> input = Map.of("condition", 42);

        NodeResult result = executor.execute(context, withData(node, input));

        assertThat(result.getOutput()).containsEntry("result", true);
    }

    @Test
    @DisplayName("default condition is false")
    void execute_noCondition_returnsFalse() {
        NodeResult result = executor.execute(context, withData(node, Map.of()));

        assertThat(result.getOutput()).containsEntry("result", false);
    }

    private NodeDefinition withData(NodeDefinition node, Map<String, Object> data) {
        node.setData(data);
        return node;
    }
}
