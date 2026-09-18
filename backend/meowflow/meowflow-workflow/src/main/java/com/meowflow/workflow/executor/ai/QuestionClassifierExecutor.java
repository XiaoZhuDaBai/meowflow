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
public class QuestionClassifierExecutor extends AbstractAIExecutor {

    public QuestionClassifierExecutor(ChatModelGateway gateway) {
        super(gateway);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.QUESTION_CLASSIFIER;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String query = resolveString(input.getOrDefault("query", input.get("input")), context);
        Object categoriesValue = input.get("categories");
        String categories = categoriesValue instanceof List
                ? String.join(", ", ((List<?>) categoriesValue).stream().map(String::valueOf).toList())
                : categoriesValue != null ? categoriesValue.toString() : "";
        String instruction = resolveString(input.get("instruction"), context);

        String user = "Classify the question into one of: " + categories + "\nQuestion:\n" + query;
        if (instruction != null && !instruction.isBlank()) {
            user = instruction + "\n" + user;
        }
        ChatResponse response = complete(context, input, user,
                "Return only the category name, nothing else.");

        Map<String, Object> output = new HashMap<>();
        output.put("category", cleanAnswer(response.getContent()));
        output.put("query", query);
        output.put("model", response.getModel());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output)
                .withCostToken(response.getTotalTokens());
    }
}
