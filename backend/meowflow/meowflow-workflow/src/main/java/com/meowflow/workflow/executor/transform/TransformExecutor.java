package com.meowflow.workflow.executor.transform;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板转换节点：对模板做变量插值（支持 {{...}} / ${...}）。
 */
@Component
public class TransformExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRANSFORM;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object templateObj = input.get("template");
        if (templateObj == null) {
            templateObj = input.get("source");
        }
        String template = templateObj != null ? templateObj.toString() : "";
        Object resolved = context.resolveExpression(template);

        Map<String, Object> output = new HashMap<>();
        output.put("text", resolved);
        output.put("result", resolved);
        output.put("template", template);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
