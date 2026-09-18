package com.meowflow.workflow.executor.condition;

import com.meowflow.workflow.engine.ExecutionContext;

import java.util.Collection;
import java.util.Map;

/**
 * 条件表达式求值工具。
 *
 * <p>此前 {@code ConditionExecutor}、{@code SwitchExecutor}、{@code IfExecutor}
 * 各自实现了一套求值，能力却不一致：CONDITION / SWITCH 支持 {@code >=} 之类的
 * 比较运算，而 IF 只认裸 {@code true}/{@code 1}，于是像
 * {@code {{var.score}} >= 60} 这种最常见的写法会被判成 false ——
 * 节点执行「成功」，但永远走 false 分支，属于静默错误。</p>
 *
 * <p>这里统一实现，保证三种条件节点语义一致。</p>
 */
public final class ConditionExpressionEvaluator {

    private static final String[] OPERATORS = {
            "==", "!=", ">=", "<=", ">", "<",
            "contains", "startsWith", "endsWith"
    };

    private ConditionExpressionEvaluator() {
    }

    /**
     * 求值一个条件表达式。
     *
     * @param context    执行上下文（用于解析 {{...}} 引用）
     * @param expression 表达式，可为 null/空
     * @return 布尔结果；表达式为空时返回 false
     */
    public static boolean evaluate(ExecutionContext context, String expression) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        String expr = expression.trim();

        // 无右侧值的一元操作符
        if (expr.equals("isNull") || expr.startsWith("isNull ")) {
            return context.resolveExpression(expr.replace("isNull", "").trim()) == null;
        }
        if (expr.equals("isNotNull") || expr.startsWith("isNotNull ")) {
            return context.resolveExpression(expr.replace("isNotNull", "").trim()) != null;
        }
        if (expr.equals("isEmpty") || expr.startsWith("isEmpty ")) {
            return isEmpty(context.resolveExpression(expr.replace("isEmpty", "").trim()));
        }

        // 二元操作符
        for (String op : OPERATORS) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                String leftExpr = expr.substring(0, idx).trim();
                String rightExpr = expr.substring(idx + op.length()).trim();
                Object left = context.resolveExpression(leftExpr);
                Object right = context.resolveExpression(rightExpr);
                return compare(left, op, right);
            }
        }

        // 没有操作符：按真值判断
        return isTruthy(context.resolveExpression(expr));
    }

    /** 按给定操作符比较两个值。 */
    public static boolean compare(Object left, String operator, Object right) {
        String op = operator == null ? "truthy" : operator.trim();
        return switch (op) {
            case "eq", "==" -> eq(left, right);
            case "ne", "!=" -> !eq(left, right);
            case "gt", ">" -> left != null && right != null && compareValues(left, right) > 0;
            case "gte", ">=" -> left != null && right != null && compareValues(left, right) >= 0;
            case "lt", "<" -> left != null && right != null && compareValues(left, right) < 0;
            case "lte", "<=" -> left != null && right != null && compareValues(left, right) <= 0;
            case "contains" -> left != null
                    && left.toString().contains(right != null ? right.toString() : "");
            case "startsWith" -> left != null
                    && left.toString().startsWith(right != null ? right.toString() : "");
            case "endsWith" -> left != null
                    && left.toString().endsWith(right != null ? right.toString() : "");
            default -> isTruthy(left);
        };
    }

    /** 相等判断：数值按数值比较（85 == "85" 成立），否则按字符串。 */
    public static boolean eq(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        return compareValues(left, right) == 0;
    }

    /** 数值优先比较；任一侧不是数值时退化为字符串比较。 */
    public static int compareValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    /** 真值判断：true / 1 / 非零数字 / 非空字符串；含比较符的字符串会再求值一次。 */
    public static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.doubleValue() != 0;
        String s = value.toString().trim();
        if (s.equalsIgnoreCase("true") || s.equals("1")) return true;
        if (s.equalsIgnoreCase("false") || s.equals("0") || s.isEmpty()) return false;
        try {
            return Double.parseDouble(s) != 0;
        } catch (NumberFormatException ignored) {
            // 兜底：解析结果本身可能还是一段比较表达式（如 {{expr}} 解析出 "85 >= 60"）。
            // 这里不带 context（字面量已解析完），按纯文本比较一次。
            for (String op : OPERATORS) {
                int idx = s.indexOf(op);
                if (idx > 0) {
                    String left = s.substring(0, idx).trim();
                    String right = s.substring(idx + op.length()).trim();
                    return compare(literal(left), op, literal(right));
                }
            }
            return false;
        }
    }

    /** 把字面量转成合适的类型（数字 / 布尔 / 字符串）。 */
    private static Object literal(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
        if (s.equalsIgnoreCase("null")) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) {
            return s;
        }
    }

    public static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isEmpty();
        if (value instanceof Collection<?> c) return c.isEmpty();
        if (value instanceof Map<?, ?> m) return m.isEmpty();
        if (value instanceof Object[] a) return a.length == 0;
        return false;
    }
}
