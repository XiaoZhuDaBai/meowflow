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
@DisplayName("JoinExecutor Tests")
class JoinExecutorTest {

    private JoinExecutor executor;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        executor = new JoinExecutor();
        context = ExecutionContext.builder().build();
    }

    @Test
    @DisplayName("getNodeType returns JOIN")
    void getNodeType_returnsJOIN() {
        assertThat(executor.getNodeType()).isEqualTo(NodeType.JOIN);
    }

    @Test
    @DisplayName("supports returns true for JOIN type")
    void supports_returnsTrueForJOIN() {
        assertThat(executor.supports(NodeType.JOIN)).isTrue();
    }

    @Test
    @DisplayName("supports returns false for non-JOIN type")
    void supports_returnsFalseForOther() {
        assertThat(executor.supports(NodeType.LLM)).isFalse();
    }

    @Test
    @DisplayName("executes with ALL strategy by default")
    void execute_defaultStrategy_usesAll() {
        NodeDefinition node = NodeDefinition.builder()
                .id("join-1").type(NodeType.JOIN).name("Test JOIN")
                .data(new HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput())
                .containsEntry("strategy", "ALL")
                .containsEntry("joined", true)
                .containsEntry("requiredCount", 1);
    }

    @Test
    @DisplayName("executes with ANY strategy")
    void execute_anyStrategy_usesAny() {
        NodeDefinition node = NodeDefinition.builder()
                .id("join-1").type(NodeType.JOIN).name("Test JOIN")
                .data(Map.of("strategy", "ANY", "requiredCount", 1))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.getOutput()).containsEntry("strategy", "ANY");
    }

    @Test
    @DisplayName("executes with N_OF_M strategy")
    void execute_nOfMStrategy_usesNOfM() {
        NodeDefinition node = NodeDefinition.builder()
                .id("join-1").type(NodeType.JOIN).name("Test JOIN")
                .data(Map.of("strategy", "N_OF_M", "requiredCount", 2))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.getOutput())
                .containsEntry("strategy", "N_OF_M")
                .containsEntry("requiredCount", 2);
    }

    @Test
    @DisplayName("strategy constants are correctly defined")
    void strategyConstants_correctValues() {
        assertThat(JoinExecutor.STRATEGY_ALL).isEqualTo("ALL");
        assertThat(JoinExecutor.STRATEGY_ANY).isEqualTo("ANY");
        assertThat(JoinExecutor.STRATEGY_N_OF_M).isEqualTo("N_OF_M");
    }
}