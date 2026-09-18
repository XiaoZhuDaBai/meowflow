package com.meowflow.workflow.executor.end;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SetVariableExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.SET_VARIABLE;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> variables = (Map<String, Object>) input.get("variables");

        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
                output.put(entry.getKey(), entry.getValue());
            }
        }

        Object assignmentsObj = input.get("assignments");
        boolean overwrite = !Boolean.FALSE.equals(input.get("overwrite"));
        if (assignmentsObj instanceof Iterable) {
            for (Object raw : (Iterable<?>) assignmentsObj) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> assignment = (Map<?, ?>) raw;
                Object name = assignment.get("name");
                if (name == null || name.toString().isBlank()) continue;
                Object value = assignment.get("value");
                Object resolved = value instanceof String
                        ? context.resolveExpression((String) value)
                        : value;
                String varName = name.toString();
                if (overwrite || context.getVariable(varName) == null) {
                    context.setVariable(varName, resolved);
                    output.put(varName, resolved);
                }
            }
        }

        List<String> clearVars = (List<String>) input.get("clearVariables");
        if (clearVars != null) {
            for (String varName : clearVars) {
                context.getVariables().remove(varName);
                context.getGlobalVariables().remove(varName);
                output.put("cleared_" + varName, true);
            }
        }

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
