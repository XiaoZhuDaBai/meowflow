package com.meowflow.workflow.executor.end;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AnswerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.ANSWER;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object text = input.get("text");
        if (text == null) text = input.get("answer");
        if (text == null) text = "";
        Object resolved = text instanceof String
                ? context.resolveExpression((String) text)
                : text;

        Map<String, Object> output = new HashMap<>();
        output.put("answer", resolved);
        output.put("text", resolved);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
