package com.meowflow.workflow.executor.ai;

import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIClassifyExecutorTest {

    @Test
    void classify_returnsCategoryFromModel() {
        ChatModelGateway gateway = mock(ChatModelGateway.class);
        when(gateway.complete(any(ChatRequest.class), any()))
                .thenReturn(ChatResponse.success("技术", "gpt", "stop", 1, 1, 10L));
        AIClassifyExecutor executor = new AIClassifyExecutor(gateway);
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Classify")
                .type(NodeType.CLASSIFY)
                .data(Map.of("categories", "技术,销售", "text", "帮我装个系统"))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("category", "技术");
    }
}
