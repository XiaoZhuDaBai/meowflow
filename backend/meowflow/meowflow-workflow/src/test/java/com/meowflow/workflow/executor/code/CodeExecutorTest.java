package com.meowflow.workflow.executor.code;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodeExecutorTest {

    @Test
    void executesJavaScriptWithNode() {
        CodeExecutor executor = new CodeExecutor();
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Code")
                .type(NodeType.CODE)
                .data(Map.of(
                        "language", "javascript",
                        "source", "return { sum: input.a + input.b };"
                ))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .input(Map.of("a", 1, "b", 2))
                .variables(new java.util.HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(((Number) result.getOutput().get("sum")).intValue()).isEqualTo(3);
    }
}
