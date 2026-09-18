package com.meowflow.plugin.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight SDK for third-party MeowFlow plugins.
 *
 * <p>The server endpoints live under {@code /api/plugin}, so a typical gateway
 * base URL is {@code http://localhost:8080/workflow/api/plugin}.</p>
 */
public class PluginClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<>() {
            };

    private final String baseUrl;
    private final String pluginId;
    private final String secret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PluginClient(String baseUrl, String pluginId, String secret) {
        this(baseUrl, pluginId, secret,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                new ObjectMapper());
    }

    PluginClient(String baseUrl, String pluginId, String secret,
                 HttpClient httpClient, ObjectMapper objectMapper) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId 不能为空");
        }
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.pluginId = pluginId;
        this.secret = secret;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> register(Map<String, Object> manifest) {
        Map<String, Object> body = new HashMap<>();
        if (manifest != null) {
            body.putAll(manifest);
        }
        body.put("pluginId", pluginId);
        Map<String, Object> response = execute("POST", "register", body, null, null);
        return objectValue(response.get("data"));
    }

    public List<Map<String, Object>> list() {
        Map<String, Object> response = execute("GET", "", null, null, null);
        Object data = response.get("data");
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    public Map<String, Object> get() {
        Map<String, Object> response = execute("GET", pluginId, null, null, null);
        return objectValue(response.get("data"));
    }

    public Map<String, Object> updateStatus(String status) {
        Map<String, Object> response = execute("PUT", pluginId + "/status",
                Map.of("status", status), null, null);
        return objectValue(response.get("data"));
    }

    public void delete() {
        execute("DELETE", pluginId, null, null, null);
    }

    public Map<String, Object> trigger(Map<String, Object> event) {
        return trigger(event, newEventId());
    }

    public Map<String, Object> trigger(Map<String, Object> event, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId 不能为空");
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String body = writeJson(event == null ? Map.of() : event);
        String signature = sign(body, timestamp);
        Map<String, Object> response = execute("POST", "trigger/" + pluginId, null,
                Map.of(
                        "X-Plugin-Signature", signature,
                        "X-Plugin-Event-Id", eventId,
                        "X-Plugin-Timestamp", timestamp),
                body);
        return objectValue(response.get("data"));
    }

    public String newEventId() {
        return UUID.randomUUID().toString();
    }

    public String sign(String payload, String timestamp) {
        if (payload == null || timestamp == null) {
            throw new IllegalArgumentException("payload 与 timestamp 不能为空");
        }
        return computeHmacSha256(payload + "." + timestamp, secret);
    }

    public static String computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new PluginClientException("HMAC-SHA256 签名失败", e);
        }
    }

    private Map<String, Object> execute(String method, String resource,
                                        Object jsonBody, Map<String, String> headers,
                                        String rawBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url(resource)))
                    .timeout(Duration.ofSeconds(30));
            if (jsonBody != null) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(writeJson(jsonBody)));
            } else if (rawBody != null) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(rawBody));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            if (headers != null) {
                headers.forEach(builder::header);
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            Map<String, Object> result;
            try {
                result = objectMapper.readValue(body, MAP_TYPE);
            } catch (Exception e) {
                throw new PluginClientException(
                        "插件服务返回不可解析响应，HTTP " + response.statusCode() + ": " + body, e);
            }
            Object code = result.get("code");
            int codeValue = code instanceof Number number ? number.intValue()
                    : Integer.parseInt(String.valueOf(code));
            if (codeValue != 200) {
                throw new PluginClientException(String.valueOf(result.getOrDefault("message", "插件服务调用失败")));
            }
            return result;
        } catch (PluginClientException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginClientException("插件服务调用失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectValue(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new PluginClientException("插件服务 data 字段不是对象: " + value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new PluginClientException("JSON 序列化失败", e);
        }
    }

    private String url(String resource) {
        if (resource == null || resource.isBlank()) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        for (String segment : resource.split("/")) {
            builder.append('/')
                    .append(URLEncoder.encode(segment, StandardCharsets.UTF_8)
                            .replace("+", "%20"));
        }
        return builder.toString();
    }

    private static String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
