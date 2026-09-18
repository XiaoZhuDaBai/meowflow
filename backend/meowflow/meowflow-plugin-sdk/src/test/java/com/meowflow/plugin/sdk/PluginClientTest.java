package com.meowflow.plugin.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private int port;
    private String requestBody;
    private String signature;
    private String eventId;
    private String timestamp;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handle);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registerReturnsManifest() {
        PluginClient client = client("register", "secret");
        Map<String, Object> manifest = client.register(Map.of("name", "demo"));

        assertEquals("register", manifest.get("pluginId"));
        assertEquals("active", manifest.get("status"));
    }

    @Test
    void triggerSendsSignedEventAndEventId() {
        PluginClient client = client("trigger", "topsecret");
        Map<String, Object> result = client.trigger(Map.of("message", "hello"), "event-1");

        assertEquals("trigger", result.get("pluginId"));
        assertNotNull(requestBody);
        assertNotNull(signature);
        assertEquals("event-1", eventId);
        assertNotNull(timestamp);
        assertEquals(PluginClient.computeHmacSha256(requestBody + "." + timestamp, "topsecret"),
                signature);
    }

    @Test
    void lifecycleEndpointsReturnExpectedData() {
        PluginClient client = client("lifecycle", "secret");

        assertEquals(Map.of("pluginId", "lifecycle", "status", "active"), client.get());
        assertEquals(Map.of("pluginId", "lifecycle", "status", "disabled"), client.updateStatus("disabled"));
        assertEquals(List.of(Map.of("pluginId", "lifecycle")), client.list());
        client.delete();
    }

    private PluginClient client(String pluginId, String secret) {
        return new PluginClient("http://localhost:" + port + "/api/plugin", pluginId, secret);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requestBody = exchange.getRequestBody() == null
                ? null
                : new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        signature = exchange.getRequestHeaders().getFirst("X-Plugin-Signature");
        eventId = exchange.getRequestHeaders().getFirst("X-Plugin-Event-Id");
        timestamp = exchange.getRequestHeaders().getFirst("X-Plugin-Timestamp");

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        Object data = switch (path) {
            case "/api/plugin/register" -> Map.of("pluginId", "register", "status", "active");
            case "/api/plugin/trigger/trigger" -> Map.of("pluginId", "trigger");
            case "/api/plugin/lifecycle" -> "GET".equals(method)
                    ? Map.of("pluginId", "lifecycle", "status", "active")
                    : null;
            case "/api/plugin/lifecycle/status" -> Map.of("pluginId", "lifecycle", "status", "disabled");
            case "/api/plugin" -> List.of(Map.of("pluginId", "lifecycle"));
            default -> null;
        };

        Map<String, Object> response = data == null
                ? Map.of("code", 200, "message", "success")
                : Map.of("code", 200, "message", "success", "data", data);
        byte[] bytes = mapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
