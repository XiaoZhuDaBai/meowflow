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
 * 消息触发器，用于钉钉、企微、飞书等外部消息入口。
 */
@Component
public class MessageTriggerExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER_MESSAGE;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object text = firstNonNull(input.get("text"), input.get("message"));
        Object sender = firstNonNull(input.get("sender"), input.get("from"));
        if (text == null && context.getInput() instanceof Map) {
            Map<?, ?> ctxInput = (Map<?, ?>) context.getInput();
            text = firstNonNull(ctxInput.get("text"), ctxInput.get("message"));
            if (sender == null) {
                sender = firstNonNull(ctxInput.get("sender"), ctxInput.get("from"));
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("triggered", true);
        output.put("triggerType", "message");
        output.put("triggerTime", java.time.LocalDateTime.now().toString());
        output.put("executionId", context.getExecutionId());
        output.put("platform", firstNonNull(input.get("platform"),
                context.getInput() instanceof Map
                        ? ((Map<?, ?>) context.getInput()).get("platform")
                        : null));
        output.put("keyword", input.get("keyword"));
        output.put("text", text);
        output.put("sender", sender);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }
}
