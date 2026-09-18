package com.meowflow.workflow.executor.tool;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 列表操作节点：filter / map / reduce / sort / first / unique / count。
 */
@Component
public class ListOperatorExecutor extends AbstractNodeExecutor {

    private static final Pattern COMPARISON =
            Pattern.compile("item\\.([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)");
    private static final Pattern FIELD_ACCESS =
            Pattern.compile("item\\.([A-Za-z_][A-Za-z0-9_]*)");

    @Override
    public NodeType getNodeType() {
        return NodeType.LIST_OPERATOR;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object listValue = input.get("list");
        if (listValue == null) {
            listValue = input.get("input");
        }
        List<Object> list = toList(listValue);
        String operation = input.get("operation") != null ? input.get("operation").toString() : "filter";
        String expression = input.get("expression") != null ? input.get("expression").toString() : "";
        String sortKey = input.get("sortKey") != null ? input.get("sortKey").toString() : null;
        String sortOrder = input.get("sortOrder") != null ? input.get("sortOrder").toString() : "asc";

        List<Object> result = switch (operation.toLowerCase()) {
            case "filter" -> filter(list, expression);
            case "map" -> map(list, expression);
            case "reduce" -> reduce(list, expression);
            case "sort" -> sort(list, sortKey, sortOrder);
            case "first" -> list.isEmpty() ? List.of() : List.of(list.get(0));
            case "last" -> list.isEmpty() ? List.of() : List.of(list.get(list.size() - 1));
            case "unique" -> unique(list);
            case "count" -> List.of(list.size());
            default -> list;
        };

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("result", result);
        output.put("count", result.size());
        output.put("firstRecord", result.isEmpty() ? null : result.get(0));
        output.put("lastRecord", result.isEmpty() ? null : result.get(result.size() - 1));
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private List<Object> toList(Object value) {
        if (value == null) return List.of();
        if (value instanceof List) return new ArrayList<>((List<?>) value);
        if (value instanceof Iterable) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Iterable<?>) value) result.add(item);
            return result;
        }
        if (value instanceof Object[]) {
            return new ArrayList<>(List.of((Object[]) value));
        }
        return List.of(value);
    }

    private List<Object> filter(List<Object> list, String expression) {
        if (expression == null || expression.isBlank()) return list;
        return list.stream().filter(item -> matches(item, expression)).toList();
    }

    private boolean matches(Object item, String expression) {
        String expr = expression.trim();
        Matcher comparison = COMPARISON.matcher(expr);
        if (comparison.matches()) {
            Object left = fieldValue(item, comparison.group(1));
            Object right = parseLiteral(comparison.group(3));
            int cmp = compare(left, right);
            return switch (comparison.group(2)) {
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                case ">=" -> cmp >= 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case "<" -> cmp < 0;
                default -> false;
            };
        }
        Matcher field = FIELD_ACCESS.matcher(expr);
        if (field.matches()) {
            return isTruthy(fieldValue(item, field.group(1)));
        }
        return isTruthy(expr.equals("item") ? item : null);
    }

    private List<Object> map(List<Object> list, String expression) {
        if (expression == null || expression.isBlank()) return list;
        return list.stream().map(item -> mapValue(item, expression.trim())).toList();
    }

    private Object mapValue(Object item, String expression) {
        if ("item".equals(expression)) return item;
        Matcher field = FIELD_ACCESS.matcher(expression);
        if (field.matches()) {
            Object value = fieldValue(item, field.group(1));
            return value != null ? value : null;
        }
        return expression;
    }

    private List<Object> reduce(List<Object> list, String expression) {
        if (list.isEmpty()) return List.of();
        Matcher field = FIELD_ACCESS.matcher(expression == null ? "" : expression.trim());
        String fieldName = field.matches() ? field.group(1) : null;
        Object first = fieldName != null ? fieldValue(list.get(0), fieldName) : list.get(0);
        if (first instanceof Number) {
            double sum = ((Number) first).doubleValue();
            for (int i = 1; i < list.size(); i++) {
                Object v = fieldName != null ? fieldValue(list.get(i), fieldName) : list.get(i);
                if (v instanceof Number) sum += ((Number) v).doubleValue();
            }
            return List.of(sum);
        }
        StringBuilder sb = new StringBuilder(String.valueOf(first));
        for (int i = 1; i < list.size(); i++) {
            Object v = fieldName != null ? fieldValue(list.get(i), fieldName) : list.get(i);
            sb.append(v);
        }
        return List.of(sb.toString());
    }

    private List<Object> sort(List<Object> list, String sortKey, String sortOrder) {
        List<Object> copy = new ArrayList<>(list);
        Comparator<Object> comparator = (a, b) -> {
            Object av = sortKey != null ? fieldValue(a, sortKey) : a;
            Object bv = sortKey != null ? fieldValue(b, sortKey) : b;
            return compare(av, bv);
        };
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        copy.sort(comparator);
        return copy;
    }

    private List<Object> unique(List<Object> list) {
        LinkedHashSet<Object> seen = new LinkedHashSet<>();
        for (Object item : list) {
            seen.add(item);
        }
        return new ArrayList<>(seen);
    }

    private Object fieldValue(Object item, String field) {
        if (item instanceof Map) {
            return ((Map<?, ?>) item).get(field);
        }
        if (item != null) {
            try {
                var method = item.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
                return method.invoke(item);
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    private Object parseLiteral(String value) {
        String s = value.trim();
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        return !value.toString().isBlank();
    }
}
