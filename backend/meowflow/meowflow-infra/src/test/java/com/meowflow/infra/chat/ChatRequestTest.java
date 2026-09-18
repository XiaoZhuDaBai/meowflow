package com.meowflow.infra.chat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatRequestTest {

    @Test
    void of_shouldCreateBasicRequest() {
        List<Message> messages = List.of(
            Message.system("You are helpful"),
            Message.user("Hello")
        );
        ChatRequest request = ChatRequest.of("gpt-4", messages);

        assertNotNull(request);
        assertEquals("gpt-4", request.getModel());
        assertEquals(2, request.getMessages().size());
        assertNull(request.getTemperature());
        assertNull(request.getMaxTokens());
    }

    @Test
    void of_withParameters_shouldCreateRequestWithParameters() {
        List<Message> messages = List.of(Message.user("Hello"));
        ChatRequest request = ChatRequest.of("gpt-4", messages, 0.7, 1000);

        assertNotNull(request);
        assertEquals("gpt-4", request.getModel());
        assertEquals(0.7, request.getTemperature());
        assertEquals(1000, request.getMaxTokens());
    }

    @Test
    void builder_shouldCreateRequest() {
        ChatRequest request = ChatRequest.builder()
            .model("claude-3")
            .messages(List.of(Message.user("Hi")))
            .temperature(0.5)
            .maxTokens(500)
            .extraParams(Map.of("top_p", 0.9))
            .build();

        assertNotNull(request);
        assertEquals("claude-3", request.getModel());
        assertEquals(0.5, request.getTemperature());
        assertEquals(500, request.getMaxTokens());
        assertEquals(0.9, request.getExtraParams().get("top_p"));
    }

    @Test
    void request_shouldSupportExtraParams() {
        ChatRequest request = ChatRequest.builder()
            .model("test-model")
            .messages(List.of())
            .extraParams(Map.of(
                "custom_param", "value",
                "number_param", 42
            ))
            .build();

        assertNotNull(request.getExtraParams());
        assertEquals("value", request.getExtraParams().get("custom_param"));
        assertEquals(42, request.getExtraParams().get("number_param"));
    }

    @Test
    void request_shouldHandleNullMessages() {
        ChatRequest request = ChatRequest.builder()
            .model("test-model")
            .messages(null)
            .build();

        assertNull(request.getMessages());
    }

    @Test
    void request_shouldHandleNullTemperature() {
        ChatRequest request = ChatRequest.builder()
            .model("test-model")
            .temperature(null)
            .build();

        assertNull(request.getTemperature());
    }
}
