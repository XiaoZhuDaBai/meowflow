package com.meowflow.workflow.executor;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
public abstract class AbstractNodeExecutor implements NodeExecutor {

    @Override
    public NodeResult execute(ExecutionContext context, NodeDefinition node) {
        long startTime = System.currentTimeMillis();
        log.info("Executing node: id={}, type={}, name={}", node.getId(), node.getType(), node.getName());

        NodeResult running = NodeResult.running(node.getId(), node.getType(), node.getName());
        running.setStartedAt(LocalDateTime.now());

        try {
            Map<String, Object> rawInput = new HashMap<>();
            if (node.getData() != null) {
                rawInput.putAll(node.getData());
                // 前端编辑器把参数放在 data.config 下；模板/旧 DSL 则直接放在 data 顶层。
                // 统一合并到执行器可见的输入，避免两套定义跑不通。
                Object nestedConfig = node.getData().get("config");
                if (nestedConfig instanceof Map) {
                    rawInput.putAll((Map<String, Object>) nestedConfig);
                }
            }
            Map<String, Object> resolvedInput = context.resolveMap(rawInput);

            NodeResult result = doExecute(context, node, resolvedInput);
            result.setCostMs(System.currentTimeMillis() - startTime);
            result.setStartedAt(running.getStartedAt());
            result.setFinishedAt(LocalDateTime.now());

            if (result.isSuccess() && result.getOutput() != null) {
                Map<String, Object> output = result.getOutput();
                if (!output.containsKey("_nodeId")) {
                    output.put("_nodeId", node.getId());
                }
                if (!output.containsKey("_nodeName")) {
                    output.put("_nodeName", node.getName());
                }
            }

            log.info("Node execution completed: id={}, status={}, costMs={}",
                    node.getId(), result.getStatus(), result.getCostMs());
            return result;

        } catch (NodeException e) {
            log.error("Node execution failed with NodeException: id={}", node.getId(), e);
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    e.getMessage(), e)
                    .withCostMs(System.currentTimeMillis() - startTime)
                    .withStartedAt(running.getStartedAt())
                    .withFinishedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Node execution failed unexpectedly: id={}", node.getId(), e);
            return NodeResult.failed(node.getId(), node.getType(), node.getName(),
                    e.getMessage(), e)
                    .withCostMs(System.currentTimeMillis() - startTime)
                    .withStartedAt(running.getStartedAt())
                    .withFinishedAt(LocalDateTime.now());
        }
    }

    protected abstract NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input);

    protected Map<String, Object> getInputParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    protected Object getInputValue(Map<String, Object> input, String key) {
        return input.get(key);
    }
}
