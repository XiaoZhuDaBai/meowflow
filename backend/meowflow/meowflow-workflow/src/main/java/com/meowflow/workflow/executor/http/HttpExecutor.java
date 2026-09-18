package com.meowflow.workflow.executor.http;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpExecutor extends AbstractNodeExecutor {

    private final RestTemplate restTemplate;

    @Override
    public NodeType getNodeType() {
        return NodeType.HTTP;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        String url = (String) context.resolveExpression((String) input.get("url"));
        String method = (String) input.getOrDefault("method", "GET");
        Map<String, Object> headers = getInputParam(input, "headers");
        Object body = input.get("body");

        if (url == null || url.isEmpty()) {
            throw new NodeException(node.getId(), node.getType().getCode(), "HTTP URL is required");
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                httpHeaders.set(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, httpHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method.toUpperCase()),
                    entity,
                    String.class
            );

            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.getStatusCode().value());
            output.put("body", response.getBody());
            output.put("headers", response.getHeaders());

            return NodeResult.success(node.getId(), node.getType(), node.getName(), output);

        } catch (Exception e) {
            log.error("HTTP request failed: url={}", url, e);
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "HTTP request failed: " + e.getMessage(), e);
        }
    }
}
