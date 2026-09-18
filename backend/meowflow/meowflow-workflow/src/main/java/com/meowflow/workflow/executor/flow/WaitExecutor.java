package com.meowflow.workflow.executor.flow;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WaitExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.WAIT;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        long waitMs = parseLong(input.get("durationMs"),
                parseLong(input.get("waitMs"), parseLong(input.get("duration"), 1000L)));
        waitMs = Math.max(0, Math.min(300_000L, waitMs));

        try {
            sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> output = new HashMap<>();
        output.put("waitedMs", waitMs);
        output.put("waited", true);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    /** 可覆写以便测试验证等待时长，而不必真实阻塞。 */
    protected void sleep(long waitMs) throws InterruptedException {
        Thread.sleep(waitMs);
    }

    private Long parseLong(Object value, Long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}



