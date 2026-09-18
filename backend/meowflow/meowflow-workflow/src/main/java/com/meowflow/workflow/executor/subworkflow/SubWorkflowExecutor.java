package com.meowflow.workflow.executor.subworkflow;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import com.meowflow.workflow.service.ExecutionService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 子工作流节点：在当前工作流内同步执行另一个已发布工作流。
 */
@Component
public class SubWorkflowExecutor extends AbstractNodeExecutor {

    private final ApplicationContext applicationContext;

    public SubWorkflowExecutor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.SUB_WORKFLOW;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Long targetWorkflowId = parseLong(
                input.get("workflowId") != null ? input.get("workflowId") : input.get("targetWorkflowId"));
        if (targetWorkflowId == null) {
            throw new NodeException(node.getId(), node.getType().getCode(), "子工作流 ID 必填");
        }
        if (targetWorkflowId.equals(context.getWorkflowId())) {
            throw new NodeException(node.getId(), node.getType().getCode(), "子工作流不能递归调用自身");
        }

        Map<String, Object> subInput = new HashMap<>();
        Object configuredInput = input.get("input");
        if (configuredInput instanceof Map) {
            subInput.putAll((Map<String, Object>) configuredInput);
        }

        ExecutionRequest request = new ExecutionRequest();
        request.setWorkflowId(targetWorkflowId);
        request.setTriggerType("subworkflow");
        request.setInput(subInput);
        request.setAsync(false);

        ExecutionService executionService = applicationContext.getBean(ExecutionService.class);
        ExecutionResponse response = executionService.execute(request, 0L);

        Map<String, Object> output = new HashMap<>();
        output.put("workflowId", targetWorkflowId);
        output.put("executionId", response.getExecutionId());
        output.put("status", response.getStatus());
        output.put("output", response.getOutput());
        output.put("error", response.getErrorMessage());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
