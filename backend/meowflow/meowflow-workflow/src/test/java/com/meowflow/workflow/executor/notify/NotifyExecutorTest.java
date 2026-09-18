package com.meowflow.workflow.executor.notify;

import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.service.IntegrationService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotifyExecutorTest {

    @Test
    void sendsThroughIntegrationService() {
        IntegrationService integrationService = mock(IntegrationService.class);
        when(integrationService.send(anyString(), anyString()))
                .thenReturn(IntegrationSender.SendResult.success("msg-1", 1L));
        NotifyExecutor executor = new NotifyExecutor(integrationService);
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Notify")
                .type(NodeType.NOTIFY)
                .data(Map.of("platform", "dingtalk", "text", "hello"))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("sent", true);
    }
}
