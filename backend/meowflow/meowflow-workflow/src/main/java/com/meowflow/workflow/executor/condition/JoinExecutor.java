package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JOIN 节点执行器（并行分支汇合点）。
 * <p>
 * 等待来自上游 FORK 的所有分支完成。支持的合并策略：
 * <ul>
 *     <li>ALL - 等待所有分支</li>
 *     <li>ANY - 任一分支完成即可</li>
 *     <li>N_OF_M - 完成指定数量即可</li>
 * </ul>
 * <p>
 * 由于工作流执行引擎目前为单线程顺序执行，该节点主要用于标记合并语义和汇总结果。
 */
@Slf4j
@Component
public class JoinExecutor extends AbstractNodeExecutor {

    public static final String STRATEGY_ALL = "ALL";
    public static final String STRATEGY_ANY = "ANY";
    public static final String STRATEGY_N_OF_M = "N_OF_M";

    @Override
    public NodeType getNodeType() {
        return NodeType.JOIN;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String strategy = (String) input.getOrDefault("strategy", STRATEGY_ALL);
        Integer requiredCount = parseInteger(input.get("requiredCount"), 1);

        Map<String, Object> output = new HashMap<>();
        output.put("strategy", strategy);
        output.put("requiredCount", requiredCount);
        output.put("joined", true);

        log.info("JOIN executed: strategy={}, required={}", strategy, requiredCount);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private Integer parseInteger(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
