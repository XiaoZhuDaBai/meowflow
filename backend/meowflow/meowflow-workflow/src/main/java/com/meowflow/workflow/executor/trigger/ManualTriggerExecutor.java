package com.meowflow.workflow.executor.trigger;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ManualTriggerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER_MANUAL;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("triggerType", "manual");
        output.put("triggerTime", java.time.LocalDateTime.now().toString());
        output.put("executionId", context.getExecutionId());

        if (context.getInput() != null) {
            output.put("input", context.getInput());
        }

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
