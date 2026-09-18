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
public class AIExtractExecutor extends AbstractAIExecutor {

    public AIExtractExecutor(ChatModelGateway gateway) {
        super(gateway);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.EXTRACT;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String text = resolveString(input.getOrDefault("input", input.get("text")), context);
        String schema = resolveString(input.getOrDefault("schema", "{}"), context);
        String instruction = resolveString(input.get("instruction"), context);

        String system = "You extract structured data. Return a JSON object only, no markdown fences.";
        String user = "Schema:\n" + schema + "\nText:\n" + text;
        if (instruction != null && !instruction.isBlank()) {
            user = instruction + "\n" + user;
        }

        ChatResponse response = complete(context, input, user, system);
        Map<String, Object> extracted = extractJsonObject(response.getContent());
        Map<String, Object> output = new HashMap<>();
        output.put("entities", extracted);
        output.put("extracted", extracted);
        output.put("text", text);
        output.put("model", response.getModel());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }
}
