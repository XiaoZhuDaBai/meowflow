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
 * SWITCH 节点执行器。
 * <p>
 * 多路选择：根据 expression 的值匹配 case 列表，匹配成功的 case 名称输出到 selectedBranch。
 * 如果没有匹配则使用 default 分支（如果提供）。
 */
@Slf4j
@Component
public class SwitchExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.SWITCH;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object expressionObj = input.get("expression");
        String expression = expressionObj != null ? String.valueOf(expressionObj) : "";
        Object resolved = expression.isBlank() ? null : context.resolveExpression(expression);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = (List<Map<String, Object>>) input.get("cases");
        String defaultBranch = (String) input.getOrDefault("defaultBranch", "default");

        String selectedBranch = defaultBranch;
        List<Map<String, Object>> matchedCases = new ArrayList<>();

        if (cases != null) {
            for (Map<String, Object> caseDef : cases) {
                boolean matched = false;
                Object caseExpression = caseDef.get("expression");
                if (caseExpression != null && !caseExpression.toString().isBlank()) {
                    matched = evaluateBooleanExpression(context, caseExpression.toString());
                } else {
                    Object caseValue = caseDef.get("value");
                    Object caseResolved = caseValue != null ? context.resolveExpression(String.valueOf(caseValue)) : null;
                    matched = equalsIgnoreCase(resolved, caseResolved);
                }

                if (matched) {
                    String branchName = String.valueOf(caseDef.getOrDefault(
                            "name", caseDef.getOrDefault("id", "case_" + matchedCases.size())));
                    matchedCases.add(caseDef);
                    if (selectedBranch.equals(defaultBranch)) {
                        selectedBranch = branchName;
                    }
                }
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("expression", expression);
        output.put("resolvedValue", resolved);
        output.put("selectedBranch", selectedBranch);
        output.put("matchedCount", matchedCases.size());

        // 若命中的分支名就是 true/false（很多前端模板这样命名端口），
        // 额外补一个布尔结果，让 condition-true / condition-false 边也能命中。
        Boolean asBoolean = toBooleanBranch(selectedBranch);
        if (asBoolean != null) {
            output.put("result", asBoolean);
        }

        log.debug("SWITCH evaluated: expression={}, resolved={}, branch={}",
                expression, resolved, selectedBranch);

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    /** 分支名是否为布尔语义（true/false/是/否…）。 */
    private Boolean toBooleanBranch(String branch) {
        if (branch == null) return null;
        String s = branch.trim();
        if (s.equalsIgnoreCase("true") || s.equals("1") || "是".equals(s)) return Boolean.TRUE;
        if (s.equalsIgnoreCase("false") || s.equals("0") || "否".equals(s)) return Boolean.FALSE;
        return null;
    }

    private boolean evaluateBooleanExpression(ExecutionContext context, String expression) {
        Object resolved = context.resolveExpression(expression);
        if (resolved instanceof Boolean) {
            return (Boolean) resolved;
        }
        if (resolved instanceof Number) {
            return ((Number) resolved).doubleValue() != 0;
        }

        String value = String.valueOf(resolved).trim();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.isEmpty()) {
            return false;
        }

        String[] operators = {"==", "!=", ">=", "<=", ">", "<",
                "contains", "startsWith", "endsWith"};
        for (String operator : operators) {
            int index = value.indexOf(operator);
            if (index <= 0) {
                continue;
            }
            String leftExpr = value.substring(0, index).trim();
            String rightExpr = value.substring(index + operator.length()).trim();
            Object left = context.resolveExpression(leftExpr);
            Object right = context.resolveExpression(rightExpr);
            return compare(left, operator, right);
        }
        return false;
    }

    private boolean compare(Object left, String operator, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double a = ((Number) left).doubleValue();
            double b = ((Number) right).doubleValue();
            return switch (operator) {
                case "==" -> a == b;
                case "!=" -> a != b;
                case ">=" -> a >= b;
                case "<=" -> a <= b;
                case ">" -> a > b;
                case "<" -> a < b;
                case "contains" -> String.valueOf(left).contains(String.valueOf(right));
                case "startsWith" -> String.valueOf(left).startsWith(String.valueOf(right));
                case "endsWith" -> String.valueOf(left).endsWith(String.valueOf(right));
                default -> false;
            };
        }
        return switch (operator) {
            case "==" -> String.valueOf(left).equals(String.valueOf(right));
            case "!=" -> !String.valueOf(left).equals(String.valueOf(right));
            case ">=" -> String.valueOf(left).compareTo(String.valueOf(right)) >= 0;
            case "<=" -> String.valueOf(left).compareTo(String.valueOf(right)) <= 0;
            case ">" -> String.valueOf(left).compareTo(String.valueOf(right)) > 0;
            case "<" -> String.valueOf(left).compareTo(String.valueOf(right)) < 0;
            case "contains" -> String.valueOf(left).contains(String.valueOf(right));
            case "startsWith" -> String.valueOf(left).startsWith(String.valueOf(right));
            case "endsWith" -> String.valueOf(left).endsWith(String.valueOf(right));
            default -> false;
        };
    }

    private boolean equalsIgnoreCase(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return String.valueOf(a).trim().equalsIgnoreCase(String.valueOf(b).trim());
    }
}
