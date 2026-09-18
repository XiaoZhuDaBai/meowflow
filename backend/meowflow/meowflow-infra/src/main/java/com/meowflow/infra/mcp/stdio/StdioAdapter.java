package com.meowflow.infra.mcp.stdio;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.mcp.MCPTool;
import com.meowflow.infra.mcp.MCPToolExecutor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Stdio 协议适配器 - 用于本地 MCP 服务
 */
@Slf4j
public class StdioAdapter implements MCPToolExecutor {

    private final MCPTool tool;
    private Process process;

    public StdioAdapter(MCPTool tool) {
        this.tool = tool;
    }

    @Override
    public Object execute(Map<String, Object> params) {
        try {
            String command = tool.getEndpoint();
            String input = buildJsonRpcRequest(tool.getName(), params);

            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.redirectErrorStream(true);

            process = pb.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                writer.write(input);
                writer.write("\n");
                writer.flush();

                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("{") || line.startsWith("[")) {
                        output.append(line);
                        break;
                    }
                }

                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "MCP 工具执行超时");
                }

                return parseJsonRpcResponse(output.toString());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stdio adapter execution error", e);
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "MCP 工具执行失败: " + e.getMessage());
        }
    }

    @Override
    public MCPTool getTool() {
        return tool;
    }

    @Override
    public boolean isHealthy() {
        try {
            if (process != null && process.isAlive()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildJsonRpcRequest(String method, Map<String, Object> params) {
        return String.format("{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":%s,\"id\":1}",
                method, com.meowflow.common.util.JsonUtils.toJson(params));
    }

    private Object parseJsonRpcResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> json = com.meowflow.common.util.JsonUtils.fromJsonToMap(response);
            if (json.containsKey("result")) {
                return json.get("result");
            }
            if (json.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) json.get("error");
                throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "MCP 工具错误: " + error.get("message"));
            }
            return json;
        } catch (Exception e) {
            return response;
        }
    }
}
