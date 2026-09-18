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
public class AggregatorExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.AGGREGATOR;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();

        Object inputData = input.get("input");
        if (inputData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = (Map<String, Object>) inputData;
            output.put("aggregated", inputMap);
            output.put("count", inputMap.size());
        } else if (inputData instanceof Iterable) {
            int count = 0;
            for (Object item : (Iterable<?>) inputData) {
                count++;
            }
            output.put("aggregated", inputData);
            output.put("count", count);
        } else {
            output.put("aggregated", inputData);
            output.put("count", 1);
        }

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
