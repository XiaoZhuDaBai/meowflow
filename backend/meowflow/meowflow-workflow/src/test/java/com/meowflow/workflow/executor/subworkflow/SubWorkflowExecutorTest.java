package com.meowflow.workflow.executor.subworkflow;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubWorkflowExecutorTest {

    @Test
    void executesTargetWorkflow() {
        ExecutionService executionService = mock(ExecutionService.class);
        when(executionService.execute(any(), eq(0L))).thenReturn(
                ExecutionResponse.builder()
                        .executionId(9L)
                        .workflowId(2L)
                        .status("success")
                        .output(Map.of("ok", true))
                        .build());
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(ExecutionService.class)).thenReturn(executionService);

        SubWorkflowExecutor executor = new SubWorkflowExecutor(applicationContext);
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Sub")
                .type(NodeType.SUB_WORKFLOW)
                .data(Map.of("workflowId", 2))
                .build();
        ExecutionContext context = ExecutionContext.builder()
                .executionId(1L)
                .workflowId(1L)
                .variables(new java.util.HashMap<>())
                .build();

        NodeResult result = executor.execute(context, node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("executionId", 9L);
    }
}
