package com.meowflow.infra.mcp.sse;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.mcp.MCPTool;
import com.meowflow.infra.mcp.MCPToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SSE 协议适配器 - 用于远程 MCP 服务
 */
@Slf4j
public class SseAdapter implements MCPToolExecutor {

    private final MCPTool tool;
    private final RestTemplate restTemplate;

    public SseAdapter(MCPTool tool) {
        this.tool = tool;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Object execute(Map<String, Object> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (tool.getHeaders() != null) {
                tool.getHeaders().forEach(headers::set);
            }

            Map<String, Object> requestBody = Map.of(
                    "method", tool.getName(),
                    "params", params
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    tool.getEndpoint(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseSseResponse(response.getBody());
            }
            throw new BizException(ResultCode.SERVICE_UNAVAILABLE, "MCP SSE 服务调用失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("SSE adapter execution error", e);
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
            HttpHeaders headers = new HttpHeaders();
            if (tool.getHeaders() != null) {
                tool.getHeaders().forEach(headers::set);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    tool.getEndpoint(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("SSE health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Object parseSseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        StringBuilder data = new StringBuilder();
        for (String line : response.split("\n")) {
            if (line.startsWith("data: ")) {
                data.append(line.substring(6));
            }
        }

        if (!data.isEmpty()) {
            try {
                return com.meowflow.common.util.JsonUtils.fromJsonToMap(data.toString());
            } catch (Exception e) {
                return data.toString();
            }
        }
        return response;
    }
}
