package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FORK 节点执行器（并行分支起点）。
 * <p>
 * 标记后续下游节点可并行执行，输出下游分支数量。
 * 实际并行执行由 WorkflowEngine 配合 {@link com.meowflow.workflow.executor.condition.JoinExecutor} 实现。
 */
@Slf4j
@Component
public class ForkExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.FORK;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        // 前端的 flow.parallel 参数面板是「分支数量」（number，默认 2），
        // 而部分模板/旧 DSL 会传分支名列表。过去这里直接强转 List<String>，
        // 传数字时会抛 ClassCastException，节点直接失败、整条并行链路瘫痪。
        // 这里两种形态都接受。
        Object raw = input.get("branches");
        List<String> branches = new ArrayList<>();
        int branchCount = 0;

        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    branches.add(String.valueOf(item));
                }
            }
            branchCount = branches.size();
        } else if (raw instanceof Number number) {
            branchCount = Math.max(0, number.intValue());
        } else if (raw instanceof String text) {
            String trimmed = text.trim();
            if (!trimmed.isEmpty()) {
                try {
                    branchCount = Math.max(0, Integer.parseInt(trimmed));
                } catch (NumberFormatException e) {
                    // 逗号分隔的分支名
                    for (String part : trimmed.split(",")) {
                        if (!part.isBlank()) {
                            branches.add(part.trim());
                        }
                    }
                    branchCount = branches.size();
                }
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("branches", branches);
        output.put("branchCount", branchCount);
        output.put("parallel", true);

        log.info("FORK executed: {} branches from node {}", branchCount, node.getId());

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }
}
