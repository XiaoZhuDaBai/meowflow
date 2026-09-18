package com.meowflow.workflow.executor.ai;

import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatResponse;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AIClassifyExecutor extends AbstractAIExecutor {

    public AIClassifyExecutor(ChatModelGateway gateway) {
        super(gateway);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CLASSIFY;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String text = resolveString(input.getOrDefault("input", input.get("text")), context);
        String categories = formatCategories(input.get("categories"));
        String instruction = resolveString(input.get("instruction"), context);

        String system = "You are a strict text classifier. Return only the category name, nothing else.";
        String user = "Categories: " + categories + "\nText:\n" + text;
        if (instruction != null && !instruction.isBlank()) {
            user = instruction + "\n" + user;
        }

        ChatResponse response = complete(context, input, user, system);
        Map<String, Object> output = new HashMap<>();
        output.put("category", cleanAnswer(response.getContent()));
        output.put("confidence", 1.0);
        output.put("text", text);
        output.put("model", response.getModel());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }

    private String formatCategories(Object value) {
        if (value instanceof List) {
            return String.join(", ", ((List<?>) value).stream().map(String::valueOf).toList());
        }
        return value != null ? value.toString() : "";
    }
}
