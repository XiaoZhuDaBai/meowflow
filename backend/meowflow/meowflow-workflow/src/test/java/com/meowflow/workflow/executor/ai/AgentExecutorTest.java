package com.meowflow.workflow.executor.ai;

import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatRequest;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.infra.service.MCPToolService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentExecutorTest {

    @Test
    void agentCallsToolThenReturnsFinalAnswer() {
        ChatModelGateway gateway = mock(ChatModelGateway.class);
        when(gateway.complete(any(ChatRequest.class), any()))
                .thenReturn(
                        ChatResponse.success(
                                "{\"action\":\"tool\",\"name\":\"calc\",\"arguments\":{\"x\":1}}",
                                "gpt", "stop", 1, 1, 10L),
                        ChatResponse.success(
                                "{\"action\":\"final\",\"answer\":\"done\"}",
                                "gpt", "stop", 1, 1, 10L)
                );
        MCPToolService mcpToolService = mock(MCPToolService.class);
        when(mcpToolService.executeTool(eq("calc"), any())).thenReturn("ok");

        AgentExecutor executor = new AgentExecutor(gateway, mcpToolService, null);
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Agent")
                .type(NodeType.AGENT)
                .data(Map.of("query", "帮我算一下", "tools", "calc", "maxIterations", 3, "strategy", "react"))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("answer", "done");
        assertThat((List<?>) result.getOutput().get("toolOutputs")).hasSize(1);
    }
}
