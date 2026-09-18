package com.meowflow.workflow.engine;

import com.meowflow.common.context.CancellationToken;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.definition.WorkflowDefinition;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionContext {

    private static final Pattern TEMPLATE_PATTERN =
            Pattern.compile("\\{\\{([^{}]+)\\}\\}|\\$\\{([^{}]+)\\}");

    private Long executionId;
    private Long workflowId;
    private String version;
    private WorkflowDefinition definition;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private Map<String, Object> variables;

    @Builder.Default
    private Map<String, NodeResult> nodeResults = new ConcurrentHashMap<>();

    @Builder.Default
    private Map<String, Object> globalVariables = new HashMap<>();

    @Builder.Default
    private Map<String, Object> environmentVariables = new HashMap<>();

    private volatile String status;

    private Long startTime;

    private CancellationToken cancellationToken;

    /**
     * LOOP 节点的迭代游标：nodeId -> LoopCursor。
     * 用于子图多次迭代时的状态恢复。
     */
    @Builder.Default
    private Map<String, LoopCursor> loopCursors = new HashMap<>();

    public void setVariable(String key, Object value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        if (globalVariables == null) {
            globalVariables = new HashMap<>();
        }
        variables.put(key, value);
        globalVariables.put(key, value);
    }

    public Object getVariable(String key) {
        if (variables != null && variables.containsKey(key)) {
            return variables.get(key);
        }
        return globalVariables.get(key);
    }

    public void setNodeResult(String nodeId, NodeResult result) {
        if (nodeResults == null) {
            nodeResults = new ConcurrentHashMap<>();
        }
        nodeResults.put(nodeId, result);
        if (result != null && result.getOutput() != null) {
            for (Map.Entry<String, Object> entry : result.getOutput().entrySet()) {
                setVariable(nodeId + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    public NodeResult getNodeResult(String nodeId) {
        if (nodeResults == null) {
            return null;
        }
        return nodeResults.get(nodeId);
    }

    public Object resolveExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }

        String trimmed = expression.trim();
        if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
            Object resolved = resolvePath(trimmed.substring(2, trimmed.length() - 2));
            return resolved != null ? resolved : expression;
        }
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            Object resolved = resolvePath(trimmed.substring(2, trimmed.length() - 1));
            return resolved != null ? resolved : expression;
        }

        return interpolate(expression);
    }

    public Object resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String normalized = path.trim();

        if (normalized.startsWith("env.")) {
            String rest = normalized.substring(4);
            int dot = rest.indexOf('.');
            if (dot > 0) {
                return resolveNested(getEnvironmentVariable(rest.substring(0, dot)), rest.substring(dot + 1));
            }
            return getEnvironmentVariable(rest);
        }

        if (normalized.startsWith("var.")) {
            String rest = normalized.substring(4);
            int dot = rest.indexOf('.');
            if (dot > 0) {
                return resolveNested(getVariable(rest.substring(0, dot)), rest.substring(dot + 1));
            }
            return getVariable(rest);
        }

        if (normalized.startsWith("input.")) {
            String rest = normalized.substring(6);
            int dot = rest.indexOf('.');
            if (dot > 0) {
                return resolveNested(input, rest);
            }
            return input != null ? input.get(rest) : null;
        }

        if (normalized.startsWith("output.")) {
            String rest = normalized.substring(7);
            int dot = rest.indexOf('.');
            if (dot > 0) {
                return resolveNested(output, rest);
            }
            return output != null ? output.get(rest) : null;
        }

        int dot = normalized.indexOf('.');
        if (dot > 0) {
            String nodeId = normalized.substring(0, dot);
            String rest = normalized.substring(dot + 1);
            NodeResult result = nodeResults.get(nodeId);
            if (result != null) {
                if (result.getOutput() != null) {
                    Object value = resolveNested(result.getOutput(), rest);
                    if (value != null) {
                        return value;
                    }
                }
                // 触发器输出没有显式字段时，回退读取执行入参，兼容 {{trigger.query}} 等写法。
                if (result.getNodeType() != null && result.getNodeType().isTrigger()) {
                    Object fromInput = resolveNested(input, rest);
                    if (fromInput != null) {
                        return fromInput;
                    }
                }
            }
            Object stored = getVariable(normalized);
            if (stored != null) {
                return stored;
            }
            int separator = normalized.lastIndexOf('.');
            while (separator > 0) {
                Object root = getVariable(normalized.substring(0, separator));
                if (root != null) {
                    return resolveNested(root, normalized.substring(separator + 1));
                }
                separator = normalized.lastIndexOf('.', separator - 1);
            }
            return null;
        }

        return getVariable(normalized);
    }

    public Object getEnvironmentVariable(String key) {
        ensureEnvironmentVariables();
        return environmentVariables != null ? environmentVariables.get(key) : null;
    }

    public void setEnvironmentVariable(String key, Object value) {
        ensureEnvironmentVariables();
        environmentVariables.put(key, value);
    }

    private void ensureEnvironmentVariables() {
        if (environmentVariables == null) {
            environmentVariables = new HashMap<>();
        }
        if (definition == null || definition.getConfig() == null
                || !environmentVariables.isEmpty()) {
            return;
        }
        Object raw = definition.getConfig().get("environmentVariables");
        if (raw instanceof Map) {
            environmentVariables.putAll((Map<String, Object>) raw);
        }
    }

    public Map<String, Object> resolveMap(Map<String, Object> sourceMap) {
        if (sourceMap == null) return null;
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                value = resolveExpression((String) value);
            } else if (value instanceof Map) {
                value = resolveMap((Map<String, Object>) value);
            } else if (value instanceof List) {
                value = resolveList((List<?>) value);
            }
            resolved.put(entry.getKey(), value);
        }
        return resolved;
    }

    public void mergeOutput(Map<String, Object> values) {
        if (values == null) return;
        if (output == null) {
            output = new HashMap<>();
        }
        output.putAll(values);
    }

    private String interpolate(String expression) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(expression);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String path = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Object resolved = resolvePath(path);
            if (resolved == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(toDisplayValue(resolved)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<Object> resolveList(List<?> sourceList) {
        if (sourceList == null) return null;
        List<Object> resolved = new ArrayList<>();
        for (Object item : sourceList) {
            if (item instanceof String) {
                resolved.add(resolveExpression((String) item));
            } else if (item instanceof Map) {
                resolved.add(resolveMap((Map<String, Object>) item));
            } else if (item instanceof List) {
                resolved.add(resolveList((List<?>) item));
            } else {
                resolved.add(item);
            }
        }
        return resolved;
    }

    private Object resolveNested(Object root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return root;
        }
        Object current = root;
        String normalized = path.replace('[', '.').replace("]", "");
        List<String> parts = Arrays.stream(normalized.split("\\."))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .toList();
        for (String part : parts) {
            if (current == null) return null;
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List) {
                try {
                    current = ((List<?>) current).get(Integer.parseInt(part));
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return null;
                }
            } else if (current instanceof Object[]) {
                try {
                    current = ((Object[]) current)[Integer.parseInt(part)];
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private String toDisplayValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map || value instanceof List || value instanceof Object[]) {
            return JsonUtils.toJson(value);
        }
        return String.valueOf(value);
    }

    public void markRunning() {
        this.status = "running";
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }
    }

    public void markSuccess() {
        this.status = "success";
    }

    public void markFailed(String errorMessage) {
        this.status = "failed";
        if (output == null) {
            output = new HashMap<>();
        }
        output.put("_error", errorMessage);
    }

    /** 标记为「已取消」：区别于失败，避免把用户主动取消误报成执行失败。 */
    public void markCancelled(String reason) {
        this.status = "cancelled";
        if (output == null) {
            output = new HashMap<>();
        }
        output.put("_cancelReason", reason);
    }

    public long getElapsedMs() {
        if (startTime == null) return 0;
        return System.currentTimeMillis() - startTime;
    }

    public Set<String> getExecutedNodeIds() {
        return nodeResults.keySet();
    }

    public boolean isCancelled() {
        return cancellationToken != null && cancellationToken.isCancelled();
    }

    public Map<String, LoopCursor> getLoopCursors() {
        return loopCursors;
    }

    public void setLoopCursors(Map<String, LoopCursor> loopCursors) {
        this.loopCursors = loopCursors;
    }

    public void checkCancellation() throws WorkflowCancelledException {
        if (isCancelled()) {
            throw new WorkflowCancelledException(executionId, cancellationToken.getCancelReason());
        }
    }

    public java.time.Duration getNodeTimeout(com.meowflow.workflow.definition.NodeDefinition node) {
        if (node.getData() == null) return java.time.Duration.ofMinutes(5);
        // 编辑器把节点配置存在 data.config 下（见前端 toDefinition 与
        // AbstractNodeExecutor.execute），同时兼容 data.timeout 的历史写法。
        Object timeoutObj = node.getData().get("timeout");
        Object nested = node.getData().get("config");
        if (nested instanceof java.util.Map<?, ?> config && config.get("timeout") != null) {
            timeoutObj = config.get("timeout");
        }
        if (timeoutObj == null) return java.time.Duration.ofMinutes(5);
        if (timeoutObj instanceof Number) {
            return java.time.Duration.ofSeconds(((Number) timeoutObj).longValue());
        }
        if (timeoutObj instanceof String) {
            String text = ((String) timeoutObj).trim();
            // 纯数字字符串按「秒」解释（编辑器里最常见），否则按 ISO-8601 解析
            try {
                if (text.matches("\\d+")) {
                    return java.time.Duration.ofSeconds(Long.parseLong(text));
                }
                return java.time.Duration.parse(text);
            } catch (Exception e) {
                return java.time.Duration.ofMinutes(5);
            }
        }
        return java.time.Duration.ofMinutes(5);
    }
}

