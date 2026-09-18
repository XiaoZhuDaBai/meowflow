package com.meowflow.workflow.executor.end;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SetVariableExecutorTest {

    @Test
    void assignsResolvedValues() {
        SetVariableExecutor executor = new SetVariableExecutor();
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Assign")
                .type(NodeType.SET_VARIABLE)
                .data(Map.of("config", Map.of(
                        "assignments", List.of(Map.of("name", "foo", "value", "{{input.x}}")),
                        "overwrite", true
                )))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .input(Map.of("x", 42))
                .variables(new java.util.HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("foo", 42);
        assertThat(context.getVariable("foo")).isEqualTo(42);
    }
}
