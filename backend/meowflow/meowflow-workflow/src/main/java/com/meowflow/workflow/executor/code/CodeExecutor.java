package com.meowflow.workflow.executor.code;

import com.meowflow.common.exception.NodeException;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 代码执行节点。当前通过外部 Node / Python 进程执行，进程有超时限制。
 */
@Component
public class CodeExecutor extends AbstractNodeExecutor {

    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public NodeType getNodeType() {
        return NodeType.CODE;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String source = input.get("source") != null ? input.get("source").toString() : "";
        String language = input.get("language") != null
                ? input.get("language").toString().toLowerCase()
                : "javascript";

        Map<String, Object> scriptInput = new LinkedHashMap<>();
        scriptInput.put("source", source);
        scriptInput.put("input", context.getInput() != null ? context.getInput() : Map.of());
        scriptInput.put("vars", context.getVariables() != null ? context.getVariables() : Map.of());
        Map<String, Object> contextInfo = new LinkedHashMap<>();
        contextInfo.put("executionId", context.getExecutionId());
        contextInfo.put("workflowId", context.getWorkflowId());
        scriptInput.put("context", contextInfo);

        Object result;
        if ("python".equals(language) || "python3".equals(language)) {
            result = runPython(scriptInput);
        } else {
            result = runJavaScript(scriptInput);
        }

        Map<String, Object> output = new HashMap<>();
        if (result instanceof Map) {
            output.putAll((Map<String, Object>) result);
        } else {
            output.put("result", result);
        }
        output.put("language", language);
        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private Object runJavaScript(Map<String, Object> scriptInput) {
        String wrapper = """
                const fs = require('fs');
                const __in = JSON.parse(fs.readFileSync(0, 'utf8'));
                const input = __in.input || {};
                const vars = __in.vars || {};
                const context = __in.context || {};
                const result = (function () {
                %s
                })();
                process.stdout.write(JSON.stringify(result ?? null));
                """.formatted(scriptInput.get("source").toString());
        return runProcess("node", "-e", wrapper, scriptInput);
    }

    private Object runPython(Map<String, Object> scriptInput) {
        String wrapper = """
                import sys, json
                __in = json.load(sys.stdin)
                source = __in.get('source', '')
                input = __in.get('input', {})
                vars = __in.get('vars', {})
                context = __in.get('context', {})
                g = {'input': input, 'vars': vars, 'context': context}
                exec(source, g)
                result = g.get('result')
                if result is None and 'main' in g:
                    result = g['main'](input)
                sys.stdout.write(json.dumps(result, ensure_ascii=False))
                """;
        return runProcess("python", "-c", wrapper, scriptInput);
    }

    private Object runProcess(String executable, String arg, String wrapper, Map<String, Object> scriptInput) {
        try {
            Process process = new ProcessBuilder(executable, arg, wrapper)
                    .redirectErrorStream(false)
                    .start();

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(JsonUtils.toJson(scriptInput));
                writer.flush();
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new NodeException(null, "code", "代码执行超时");
            }

            String stdout = read(process.getInputStream());
            String stderr = read(process.getErrorStream());
            if (process.exitValue() != 0) {
                throw new NodeException(null, "code", "代码执行失败: " + stderr);
            }

            String trimmed = stdout.trim();
            if (trimmed.isEmpty() || "null".equals(trimmed)) {
                return Map.of();
            }
            return JsonUtils.fromJson(trimmed, Object.class);
        } catch (NodeException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeException(null, "code", "代码执行异常: " + e.getMessage(), e);
        }
    }

    private String read(java.io.InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
