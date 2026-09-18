package com.meowflow.workflow.executor.human;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.service.HumanTaskService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class HumanInputExecutorTest {

    @Test
    void waitsUntilHumanTaskIsCompleted() throws Exception {
        HumanTaskService service = new HumanTaskService();
        HumanInputExecutor executor = new HumanInputExecutor(service);
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Human")
                .type(NodeType.HUMAN_INPUT)
                .data(Map.of("timeout", 3))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .variables(new java.util.HashMap<>())
                .build();

        Future<NodeResult> future = Executors.newSingleThreadExecutor().submit(() ->
                executor.execute(context, node));
        Thread.sleep(150);
        assertThat(service.complete(1L, "n1", Map.of("name", "Jane"))).isTrue();

        NodeResult result = future.get();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("name", "Jane");
    }
}
