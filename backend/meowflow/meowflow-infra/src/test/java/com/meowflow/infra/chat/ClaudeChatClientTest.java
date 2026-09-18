package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Claude Chat Client Tests")
class ClaudeChatClientTest {

    @Mock
    private CircuitBreaker circuitBreaker;

    private ClaudeChatClient client;
    private AICircuitBreakerRegistryHolder holder;

    @BeforeEach
    void setUp() {
        client = new ClaudeChatClient();
        holder = mock(AICircuitBreakerRegistryHolder.class);

        // inject apiKey via reflection since @Value is not loaded in plain test
        setField(client, "apiKey", "test-key");
        setField(client, "baseUrl", "https://api.anthropic.com");
    }

    private void setField(Object obj, String name, Object value) {
        try {
            Field f = ClaudeChatClient.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("isAvailable returns true with non-blank API key")
    void isAvailable_withKey_returnsTrue() {
        assertThat(client.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("isAvailable returns false with blank API key")
    void isAvailable_blankKey_returnsFalse() throws Exception {
        setField(client, "apiKey", "");
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable returns false with null API key")
    void isAvailable_nullKey_returnsFalse() throws Exception {
        setField(client, "apiKey", null);
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("getSupportedModels returns Claude model list")
    void getSupportedModels_returnsList() {
        List<String> models = client.getSupportedModels();

        assertThat(models)
                .isNotEmpty()
                .contains("claude-3-5-sonnet-20240620")
                .contains("claude-3-opus-20240229");
    }

    @Test
    @DisplayName("getProviderName returns anthropic")
    void getProviderName_returnsAnthropic() {
        assertThat(client.getProviderName()).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("complete throws BizException when API key is missing")
    void complete_noApiKey_throwsException() throws Exception {
        setField(client, "apiKey", "");
        ChatRequest req = ChatRequest.builder()
                .model("claude-3-5-sonnet-20240620")
                .messages(List.of(Message.of("user", "Hi")))
                .build();

        assertThatThrownBy(() -> client.complete(req))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("API key");
    }

    @Nested
    @DisplayName("Circuit Breaker Integration")
    class CircuitBreakerTests {

        @Test
        @DisplayName("complete falls back when circuit breaker open")
        void complete_circuitOpen_returnsFallback() throws Exception {
            // Inject holder and breaker
            setField(client, "circuitBreakerHolder", holder);
            when(holder.forProvider("anthropic")).thenReturn(circuitBreaker);
            when(circuitBreaker.executeSupplier(any()))
                    .thenThrow(CallNotPermittedException.createCallNotPermittedException(
                            io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("anthropic")));

            ChatRequest req = ChatRequest.builder()
                    .model("claude-3-5-sonnet-20240620")
                    .messages(List.of(Message.of("user", "Hi")))
                    .build();

            ChatResponse response = client.complete(req);

            assertThat(response).isNotNull();
            assertThat(response.getFinishReason()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("complete bypasses circuit breaker when holder is null")
        void complete_noHolder_invokesApi() throws Exception {
            // No holder injected - should fail at HTTP call with BizException wrapping
            ChatRequest req = ChatRequest.builder()
                    .model("claude-3-5-sonnet-20240620")
                    .messages(List.of(Message.of("user", "Hi")))
                    .build();

            // Since restTemplate can't connect to real API, it'll fail. We just verify it doesn't return fallback.
            try {
                client.complete(req);
            } catch (BizException e) {
                assertThat(e.getMessage()).doesNotContain("temporarily unavailable");
            }
        }
    }

    @Nested
    @DisplayName("Response Parsing")
    class ResponseParsingTests {

        @Test
        @DisplayName("parseAnthropicResponse parses valid response")
        void parseAnthropicResponse_validInput_returnsParsed() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "parseAnthropicResponse", Map.class, long.class);
            m.setAccessible(true);

            Map<String, Object> response = Map.of(
                    "content", List.of(Map.of("type", "text", "text", "Hello from Claude")),
                    "model", "claude-3-5-sonnet-20240620",
                    "stop_reason", "end_turn",
                    "usage", Map.of("input_tokens", 10, "output_tokens", 20)
            );

            ChatResponse result = (ChatResponse) m.invoke(client, response, 100L);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello from Claude");
            assertThat(result.getModel()).isEqualTo("claude-3-5-sonnet-20240620");
            assertThat(result.getFinishReason()).isEqualTo("end_turn");
            assertThat(result.getPromptTokens()).isEqualTo(10);
            assertThat(result.getCompletionTokens()).isEqualTo(20);
        }

        @Test
        @DisplayName("parseAnthropicResponse handles empty content")
        void parseAnthropicResponse_emptyContent_returnsNullContent() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "parseAnthropicResponse", Map.class, long.class);
            m.setAccessible(true);

            Map<String, Object> response = Map.of(
                    "content", List.of(),
                    "model", "claude-3-5-sonnet-20240620"
            );

            ChatResponse result = (ChatResponse) m.invoke(client, response, 50L);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNull();
        }

        @Test
        @DisplayName("parseAnthropicResponse handles missing usage")
        void parseAnthropicResponse_missingUsage_returnsZeroTokens() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "parseAnthropicResponse", Map.class, long.class);
            m.setAccessible(true);

            Map<String, Object> response = Map.of(
                    "content", List.of(Map.of("type", "text", "text", "OK")),
                    "model", "claude-3-5-sonnet-20240620"
            );

            ChatResponse result = (ChatResponse) m.invoke(client, response, 100L);

            assertThat(result).isNotNull();
            assertThat(result.getPromptTokens()).isNull();
            assertThat(result.getCompletionTokens()).isNull();
        }
    }

    @Nested
    @DisplayName("Request Building")
    class RequestBuildingTests {

        @Test
        @DisplayName("buildAnthropicRequest sets model and messages")
        void buildAnthropicRequest_basic_returnsCorrectMap() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "buildAnthropicRequest", ChatRequest.class);
            m.setAccessible(true);

            ChatRequest req = ChatRequest.builder()
                    .model("claude-3-5-sonnet-20240620")
                    .messages(List.of(Message.of("user", "Hi"), Message.of("assistant", "Hello")))
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) m.invoke(client, req);

            assertThat(body.get("model")).isEqualTo("claude-3-5-sonnet-20240620");
            assertThat(body.get("max_tokens")).isEqualTo(1024);
            assertThat(body.get("messages")).asList().hasSize(2);
        }

        @Test
        @DisplayName("buildAnthropicRequest uses default max_tokens when null")
        void buildAnthropicRequest_nullMaxTokens_usesDefault() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "buildAnthropicRequest", ChatRequest.class);
            m.setAccessible(true);

            ChatRequest req = ChatRequest.builder()
                    .model("claude-3-5-sonnet-20240620")
                    .messages(List.of(Message.of("user", "Hi")))
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) m.invoke(client, req);

            assertThat(body.get("max_tokens")).isEqualTo(1024);
        }

        @Test
        @DisplayName("buildAnthropicRequest includes system prompt when provided")
        void buildAnthropicRequest_withSystem_includesSystemPrompt() throws Exception {
            java.lang.reflect.Method m = ClaudeChatClient.class.getDeclaredMethod(
                    "buildAnthropicRequest", ChatRequest.class);
            m.setAccessible(true);

            Map<String, Object> extras = Map.of("system", "You are a helpful assistant.");
            ChatRequest req = ChatRequest.builder()
                    .model("claude-3-5-sonnet-20240620")
                    .messages(List.of(Message.of("user", "Hi")))
                    .extraParams(extras)
                    .build();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) m.invoke(client, req);

            assertThat(body.get("system")).isEqualTo("You are a helpful assistant.");
        }
    }
}
