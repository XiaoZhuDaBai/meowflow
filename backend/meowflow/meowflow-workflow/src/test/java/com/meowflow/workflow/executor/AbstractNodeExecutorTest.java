package com.meowflow.workflow.executor;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractNodeExecutorTest {

    @Test
    void execute_mergesFrontendConfigIntoInput() {
        AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
        AbstractNodeExecutor executor = new AbstractNodeExecutor() {
            @Override
            protected NodeResult doExecute(ExecutionContext context, NodeDefinition node,
                                           Map<String, Object> input) {
                captured.set(input);
                return NodeResult.success(node.getId(), node.getType(), node.getName(), new HashMap<>());
            }
        };

        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("LLM")
                .type(NodeType.LLM)
                .data(Map.of(
                        "model", "gpt-4o",
                        "config", Map.of("prompt", "hello")
                ))
                .build();

        executor.execute(ExecutionContext.builder().build(), node);

        assertThat(captured.get())
                .containsEntry("model", "gpt-4o")
                .containsEntry("prompt", "hello");
    }
}
