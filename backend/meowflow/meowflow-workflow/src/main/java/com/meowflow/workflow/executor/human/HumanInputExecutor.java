package com.meowflow.workflow.executor.human;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import com.meowflow.workflow.service.HumanTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class HumanInputExecutor extends AbstractNodeExecutor {

    private final HumanTaskService humanTaskService;

    @Override
    public NodeType getNodeType() {
        return NodeType.HUMAN_INPUT;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Duration timeout = context.getNodeTimeout(node);
        CompletableFuture<Map<String, Object>> task = humanTaskService.waitForTask(
                context.getExecutionId(), node.getId());

        Map<String, Object> response;
        try {
            response = task.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            humanTaskService.cancel(context.getExecutionId(), node.getId());
            throw new NodeException(node.getId(), node.getType().getCode(), "等待人工输入超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            humanTaskService.cancel(context.getExecutionId(), node.getId());
            throw new NodeException(node.getId(), node.getType().getCode(), "人工输入被中断", e);
        } catch (CancellationException | java.util.concurrent.ExecutionException e) {
            humanTaskService.cancel(context.getExecutionId(), node.getId());
            throw new NodeException(node.getId(), node.getType().getCode(), "人工输入被取消");
        }

        Map<String, Object> output = new HashMap<>(response);
        output.put("completed", true);
        output.put("submittedAt", java.time.LocalDateTime.now().toString());
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
