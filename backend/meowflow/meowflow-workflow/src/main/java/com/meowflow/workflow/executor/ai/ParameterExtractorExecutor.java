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
public class ParameterExtractorExecutor extends AbstractAIExecutor {

    public ParameterExtractorExecutor(ChatModelGateway gateway) {
        super(gateway);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PARAMETER_EXTRACTOR;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String text = resolveString(input.getOrDefault("text", input.get("input")), context);
        String schema = resolveString(input.getOrDefault("schema", "{}"), context);
        String instruction = resolveString(input.get("instruction"), context);

        String user = "Extract parameters from text according to schema. Return JSON only.\nSchema:\n"
                + schema + "\nText:\n" + text;
        if (instruction != null && !instruction.isBlank()) {
            user = instruction + "\n" + user;
        }
        ChatResponse response = complete(context, input, user,
                "You are a parameter extraction engine. Return a JSON object only.");

        Map<String, Object> output = new HashMap<>();
        output.put("params", extractJsonObject(response.getContent()));
        output.put("text", text);
        output.put("model", response.getModel());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }
}
