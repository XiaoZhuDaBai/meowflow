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
public class EndExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.END;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("finished", true);
        output.put("endTime", java.time.LocalDateTime.now().toString());
        output.put("totalNodes", context.getExecutedNodeIds().size());
        output.put("elapsedMs", context.getElapsedMs());

        if (input != null) {
            output.putAll(input);
        }

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
