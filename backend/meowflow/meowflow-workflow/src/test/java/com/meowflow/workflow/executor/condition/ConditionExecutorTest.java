package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionExecutorTest {

    private ConditionExecutor executor;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        executor = new ConditionExecutor();
        context = ExecutionContext.builder()
                .input(Map.of("score", 85))
                .variables(new HashMap<>())
                .build();
    }

    @Test
    void executeSupportsFrontendExpression() {
        NodeDefinition node = NodeDefinition.builder()
                .id("c-1")
                .type(NodeType.CONDITION)
                .name("Condition")
                .data(Map.of("expression", "{{input.score}} >= 80"))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("selectedBranch", "true");
    }

    @Test
    void executeSupportsFrontendBuilder() {
        NodeDefinition node = NodeDefinition.builder()
                .id("c-1")
                .type(NodeType.CONDITION)
                .name("Condition")
                .data(Map.of("builder", Map.of(
                        "left", "{{input.score}}",
                        "op", ">=",
                        "right", "80")))
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("selectedBranch", "true");
    }
}
