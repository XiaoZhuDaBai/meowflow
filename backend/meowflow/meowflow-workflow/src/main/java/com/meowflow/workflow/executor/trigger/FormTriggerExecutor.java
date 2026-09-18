package com.meowflow.workflow.executor.trigger;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 表单触发器。执行入口已经由外部表单提交触发，这里负责把提交内容暴露给下游节点。
 */
@Component
public class FormTriggerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER_FORM;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Map<String, Object> formData = new HashMap<>();
        Object configured = input.get("formData");
        if (configured instanceof Map) {
            formData.putAll((Map<String, Object>) configured);
        }
        if (context.getInput() instanceof Map) {
            formData.putAll((Map<String, Object>) context.getInput());
        }

        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("triggerType", "form");
        output.put("triggerTime", java.time.LocalDateTime.now().toString());
        output.put("executionId", context.getExecutionId());
        output.put("formId", input.get("formId"));
        output.put("formData", formData);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
