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
public class PluginTriggerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER_PLUGIN;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("triggerType", "plugin");
        output.put("triggerTime", java.time.LocalDateTime.now().toString());
        output.put("pluginId", input.get("pluginId"));
        output.put("payload", context.getInput());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
