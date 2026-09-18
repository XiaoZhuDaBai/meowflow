package com.meowflow.workflow.executor.trigger;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class CronTriggerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER_CRON;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("triggerType", "cron");
        output.put("triggerTime", LocalDateTime.now().toString());

        String cronExpression = (String) input.get("cronExpression");
        ZoneId zoneId = ZoneId.systemDefault();
        long timestamp = System.currentTimeMillis();

        output.put("cronExpression", cronExpression != null ? cronExpression : "N/A");
        output.put("executionId", context.getExecutionId());
        output.put("timestamp", timestamp);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
