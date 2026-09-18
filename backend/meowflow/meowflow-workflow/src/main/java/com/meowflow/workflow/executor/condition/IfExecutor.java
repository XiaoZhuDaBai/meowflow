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
 * IF 节点执行器。
 *
 * <p>对标传统编程中的 if-else：对配置的条件表达式求值，输出
 * {@code selectedBranch = "true" / "false"}，由 EdgeRouter 选择 true/false 分支。</p>
 *
 * <p>表达式求值统一走 {@link ConditionExpressionEvaluator}，因此
 * {@code {{var.score}} >= 60}、{@code eq}、{@code contains}、
 * {@code isNull} 等写法都与 CONDITION / SWITCH 节点保持一致。</p>
 */
@Slf4j
@Component
public class IfExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.IF;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        // 兼容前端各版本参数命名：
        //   flow.if-else 的配置面板用 expression / condition / value
        //   旧模板可能用 condition
        Object conditionValue = firstNonBlank(input, "condition", "expression", "value");
        String condition = conditionValue != null ? String.valueOf(conditionValue) : "false";

        boolean result = ConditionExpressionEvaluator.evaluate(context, condition);

        Map<String, Object> output = new HashMap<>();
        output.put("condition", condition);
        output.put("result", result);
        output.put("selectedBranch", result ? "true" : "false");

        log.debug("IF evaluated: condition={}, result={}", condition, result);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private Object firstNonBlank(Map<String, Object> input, String... keys) {
        for (String key : keys) {
            Object value = input.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }
}
