package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 条件分支节点执行器。
 * <p>
 * 支持丰富的比较操作符：
 * <ul>
 *   <li>关系: eq, ne, gt, gte, lt, lte</li>
 *   <li>字符串: contains, startsWith, endsWith</li>
 *   <li>空值: isNull, isNotNull, isEmpty</li>
 *   <li>布尔: true / false / 1 / 0</li>
 * </ul>
 * <p>
 * 条件表达式支持两种格式：
 * <ul>
 *   <li>简写: "eq", "gt", "true" 等（从 input 中取 "field" 和 "value"）</li>
 *   <li>完整表达式: "field == value", "field > 100" 等</li>
 * </ul>
 */
@Slf4j
@Component
public class ConditionExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.CONDITION;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        // 兼容三种配置格式：
        // 1. condition="field == value"
        // 2. 前端 expression="field == value"
        // 3. 前端 builder={ left, op, right }
        // 4. 简写配置: field + operator + value
        String conditionExpr = resolveToString(input.get("condition"), context);
        if (isBlank(conditionExpr)) {
            conditionExpr = resolveToString(input.get("expression"), context);
        }
        if (isBlank(conditionExpr)) {
            conditionExpr = resolveBuilderExpression(context, input.get("builder"));
        }

        boolean result;
        if (conditionExpr != null && !conditionExpr.isEmpty()) {
            // 完整表达式解析
            result = evaluateExpression(context, conditionExpr);
        } else {
            // 简写格式
            String field = resolveToString(input.get("field"), context);
            String operator = resolveToString(input.get("operator"), context);
            Object value = input.get("value");

            if (field == null && value == null) {
                // 无参数，默认 true
                result = true;
            } else {
                Object fieldValue = field != null ? context.resolveExpression(field) : null;
                Object compareValue = value != null ? context.resolveExpression(String.valueOf(value)) : null;
                result = evaluate(fieldValue, operator, compareValue);
            }
        }

        String selectedBranch = result ? "true" : "false";

        Map<String, Object> output = new HashMap<>();
        output.put("result", result);
        output.put("selectedBranch", selectedBranch);
        if (conditionExpr != null) {
            output.put("condition", conditionExpr);
        }

        log.debug("Condition evaluated: result={}", result);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private String resolveBuilderExpression(ExecutionContext context, Object builderValue) {
        if (!(builderValue instanceof Map)) {
            return null;
        }
        Map<?, ?> builder = (Map<?, ?>) builderValue;
        Object left = builder.get("left");
        Object op = builder.get("op") != null ? builder.get("op") : builder.get("operator");
        Object right = builder.get("right");
        if (left == null && right == null) {
            return null;
        }
        String operator = op != null ? op.toString() : "==";
        String leftExpr = left != null ? left.toString() : "";
        String rightExpr = right != null ? right.toString() : "";
        return leftExpr + " " + operator + " " + rightExpr;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 解析完整表达式，支持：field op value 格式。
     */
    private boolean evaluateExpression(ExecutionContext context, String expression) {
        if (expression == null || expression.isBlank()) {
            return true;
        }

        String expr = expression.trim();

        // 先处理 isNull / isNotNull / isEmpty（无右侧值）
        if (expr.startsWith("isNull ") || expr.equals("isNull")) {
            String field = expr.replace("isNull ", "").trim();
            return evaluateIsNull(context.resolveExpression(field));
        }
        if (expr.startsWith("isNotNull ") || expr.equals("isNotNull")) {
            String field = expr.replace("isNotNull ", "").trim();
            return !evaluateIsNull(context.resolveExpression(field));
        }
        if (expr.startsWith("isEmpty ") || expr.equals("isEmpty")) {
            String field = expr.replace("isEmpty ", "").trim();
            return evaluateIsEmpty(context.resolveExpression(field));
        }

        // 二元操作符
        String[] operators = {"==", "!=", ">=", "<=", ">", "<",
                "contains", "startsWith", "endsWith"};
        for (String op : operators) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                String left = expr.substring(0, idx).trim();
                String right = expr.substring(idx + op.length()).trim();
                Object leftVal = context.resolveExpression(left);
                Object rightVal = context.resolveExpression(right);
                return evaluate(leftVal, op, rightVal);
            }
        }

        // 没有操作符，按布尔值处理
        return evaluate(expr, null, null);
    }

    /**
     * 通用比较方法。
     */
    private boolean evaluate(Object left, String operator, Object right) {
        String op = operator == null ? "truthy" : operator;
        boolean result;
        switch (op) {
            case "eq", "==" -> {
                result = evaluateEq(left, right);
            }
            case "ne", "!=" -> {
                result = evaluateNe(left, right);
            }
            case "gt", ">" -> {
                result = evaluateGt(left, right);
            }
            case "gte", ">=" -> {
                result = evaluateGte(left, right);
            }
            case "lt", "<" -> {
                result = evaluateLt(left, right);
            }
            case "lte", "<=" -> {
                result = evaluateLte(left, right);
            }
            case "contains" -> {
                result = left != null && left.toString().contains(right != null ? right.toString() : "");
            }
            case "startsWith" -> {
                result = left != null && left.toString().startsWith(right != null ? right.toString() : "");
            }
            case "endsWith" -> {
                result = left != null && left.toString().endsWith(right != null ? right.toString() : "");
            }
            default -> {
                result = "truthy".equals(op) && isTruthy(left);
            }
        }
        return result;
    }

    private boolean evaluateEq(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return compareNumeric(left, right) == 0;
    }

    private boolean evaluateNe(Object left, Object right) {
        if (left == null && right == null) return false;
        if (left == null || right == null) return true;
        return compareNumeric(left, right) != 0;
    }

    private boolean evaluateGt(Object left, Object right) {
        if (left == null || right == null) return false;
        return compareNumeric(left, right) > 0;
    }

    private boolean evaluateGte(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return compareNumeric(left, right) >= 0;
    }

    private boolean evaluateLt(Object left, Object right) {
        if (left == null || right == null) return false;
        return compareNumeric(left, right) < 0;
    }

    private boolean evaluateLte(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return compareNumeric(left, right) <= 0;
    }

    private boolean evaluateIsNull(Object value) {
        return value == null;
    }

    private boolean evaluateIsEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isEmpty();
        if (value instanceof Collection<?> c) return c.isEmpty();
        if (value instanceof Map<?, ?> m) return m.isEmpty();
        if (value instanceof Object[] a) return a.length == 0;
        return false;
    }

    private int compareNumeric(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        String s = value.toString().trim();
        return s.equalsIgnoreCase("true") || s.equals("1");
    }

    private String resolveToString(Object value, ExecutionContext context) {
        if (value == null) return null;
        if (value instanceof String s) {
            Object resolved = context.resolveExpression(s);
            return resolved != null ? resolved.toString() : null;
        }
        return String.valueOf(value);
    }
}
