package com.meowflow.workflow.executor.ai;

import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AISummarizeExecutor extends AbstractAIExecutor {

    public AISummarizeExecutor(ChatModelGateway gateway) {
        super(gateway);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.SUMMARIZE;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String text = resolveString(input.getOrDefault("text", input.get("input")), context);
        Integer maxWords = parseInt(input.get("maxWords"), 200);
        String language = resolveString(input.getOrDefault("language", "zh-CN"), context);
        String tone = resolveString(input.getOrDefault("tone", "concise"), context);

        String user = "Summarize the following text in " + language
                + " within " + maxWords + " words, tone: " + tone + ".\nText:\n" + text;
        ChatResponse response = complete(context, input, user,
                "You write concise, faithful summaries.");

        Map<String, Object> output = new HashMap<>();
        output.put("summary", response.getContent());
        output.put("text", response.getContent());
        output.put("maxWords", maxWords);
        output.put("model", response.getModel());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }
}
