package com.meowflow.infra.chat;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Chat Client Tests")
class OpenAIChatClientTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private OpenAIChatClient clientWithApiKey;
    private OpenAIChatClient clientWithoutApiKey;

    @BeforeEach
    void setUp() throws Exception {
        clientWithApiKey = new OpenAIChatClient("test-api-key", "https://api.openai.com");

        clientWithoutApiKey = new OpenAIChatClient("", "https://api.openai.com");
    }

    @Test
    @DisplayName("Should return true when API key is configured")
    void testIsAvailable_True() {
        assertTrue(clientWithApiKey.isAvailable());
    }

    @Test
    @DisplayName("Should return false when API key is null")
    void testIsAvailable_False_NullKey() throws Exception {
        OpenAIChatClient client = new OpenAIChatClient(null, "https://api.openai.com");
        assertFalse(client.isAvailable());
    }

    @Test
    @DisplayName("Should return false when API key is empty")
    void testIsAvailable_False_EmptyKey() {
        assertFalse(clientWithoutApiKey.isAvailable());
    }

    @Test
    @DisplayName("Should return false when API key is whitespace only")
    void testIsAvailable_False_WhitespaceKey() {
        OpenAIChatClient client = new OpenAIChatClient("   ", "https://api.openai.com");
        assertFalse(client.isAvailable());
    }

    @Test
    @DisplayName("Should return correct supported models list")
    void testGetSupportedModels() {
        List<String> models = clientWithApiKey.getSupportedModels();

        assertNotNull(models);
        assertEquals(4, models.size());
        assertTrue(models.contains("gpt-4o"));
        assertTrue(models.contains("gpt-4o-mini"));
        assertTrue(models.contains("gpt-4-turbo"));
        assertTrue(models.contains("gpt-3.5-turbo"));
    }

    @Test
    @DisplayName("Should return 'openai' as provider name")
    void testGetProviderName() {
        assertEquals("openai", clientWithApiKey.getProviderName());
    }

    @Test
    @DisplayName("Should throw BizException when API key is not configured")
    void testComplete_ApiKeyNotConfigured() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> clientWithoutApiKey.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("OpenAI API key is not configured"));
    }

    @Test
    @DisplayName("Should throw BizException when API key is null")
    void testComplete_ApiKeyNull() throws Exception {
        OpenAIChatClient client = new OpenAIChatClient(null, "https://api.openai.com");

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> client.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("Should complete successfully with valid response")
    void testComplete_Success() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Hello! How can I help you?"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o",
                "usage", Map.of(
                        "prompt_tokens", 10,
                        "completion_tokens", 20
                )
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate originalRestTemplate = (RestTemplate) restTemplateField.get(clientWithApiKey);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        assertEquals("Hello! How can I help you?", response.getContent());
        assertEquals("gpt-4o", response.getModel());
        assertEquals("stop", response.getFinishReason());
        assertEquals(10, response.getPromptTokens());
        assertEquals(20, response.getCompletionTokens());
        assertEquals(30, response.getTotalTokens());

        verify(mockRestTemplate, times(1)).postForEntity(
                eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should handle API error response")
    void testComplete_ApiError() throws Exception {
        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Connection timeout"));

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> clientWithApiKey.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("OpenAI API error"));
    }

    @Test
    @DisplayName("Should handle non-2xx response status")
    void testComplete_NonSuccessStatus() throws Exception {
        Map<String, Object> responseBody = Map.of("error", "Invalid request");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> clientWithApiKey.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertEquals("OpenAI API call failed", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle empty response body")
    void testComplete_EmptyResponseBody() throws Exception {
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> clientWithApiKey.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("Should handle empty choices in response")
    void testComplete_EmptyChoices() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(),
                "model", "gpt-4o"
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> clientWithApiKey.complete(request));

        assertEquals(ResultCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("Empty response"));
    }

    @Test
    @DisplayName("Should complete with temperature parameter")
    void testComplete_WithTemperature() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Response with temperature"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o",
                "usage", Map.of(
                        "prompt_tokens", 5,
                        "completion_tokens", 10
                )
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .temperature(0.7)
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        assertEquals("Response with temperature", response.getContent());
        verify(mockRestTemplate).postForEntity(
                anyString(),
                argThat(arg -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.http.HttpEntity<Map<String, Object>> entity =
                            (org.springframework.http.HttpEntity<Map<String, Object>>) arg;
                    Map<String, Object> body = entity.getBody();
                    return body != null && body.containsKey("temperature") && 0.7 == (Double) body.get("temperature");
                }),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should complete with maxTokens parameter")
    void testComplete_WithMaxTokens() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Limited response"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o",
                "usage", Map.of(
                        "prompt_tokens", 5,
                        "completion_tokens", 50
                )
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .maxTokens(100)
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        assertEquals("Limited response", response.getContent());
        verify(mockRestTemplate).postForEntity(
                anyString(),
                argThat(arg -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.http.HttpEntity<Map<String, Object>> entity =
                            (org.springframework.http.HttpEntity<Map<String, Object>>) arg;
                    Map<String, Object> body = entity.getBody();
                    return body != null && body.containsKey("max_tokens") && 100 == (Integer) body.get("max_tokens");
                }),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should handle messages with system role")
    void testComplete_WithSystemMessage() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Response to system prompt"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o",
                "usage", Map.of(
                        "prompt_tokens", 15,
                        "completion_tokens", 8
                )
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(Arrays.asList(
                        Message.system("You are a helpful assistant"),
                        Message.user("Hello")
                ))
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        verify(mockRestTemplate).postForEntity(
                anyString(),
                argThat(arg -> {
                    @SuppressWarnings("unchecked")
                    org.springframework.http.HttpEntity<Map<String, Object>> entity =
                            (org.springframework.http.HttpEntity<Map<String, Object>>) arg;
                    Map<String, Object> body = entity.getBody();
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
                    return messages != null &&
                            messages.size() == 2 &&
                            "system".equals(messages.get(0).get("role")) &&
                            "You are a helpful assistant".equals(messages.get(0).get("content"));
                }),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should handle baseUrl with trailing slash")
    void testBaseUrl_TrailingSlash() throws Exception {
        OpenAIChatClient client = new OpenAIChatClient("test-key", "https://api.openai.com/");

        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Response"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o",
                "usage", Map.of("prompt_tokens", 1, "completion_tokens", 1)
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                eq("https://api.openai.com/v1/chat/completions"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(client, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        ChatResponse response = client.complete(request);

        assertNotNull(response);
        assertEquals("Response", response.getContent());
    }

    @Test
    @DisplayName("Should handle null usage in response")
    void testComplete_NullUsage() throws Exception {
        Map<String, Object> responseBody = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "Response without usage"),
                        "finish_reason", "stop"
                )),
                "model", "gpt-4o"
        );

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        assertEquals(0, response.getPromptTokens());
        assertEquals(0, response.getCompletionTokens());
        assertEquals(0, response.getTotalTokens());
    }

        @Test
        @DisplayName("Should handle null message in choice")
        void testComplete_NullMessageInChoice() throws Exception {
            java.util.Map<String, Object> choice = new java.util.HashMap<>();
            choice.put("message", null);
            choice.put("finish_reason", "stop");
            java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
            responseBody.put("choices", List.of(choice));
            responseBody.put("model", "gpt-4o");
            responseBody.put("usage", Map.of("prompt_tokens", 1, "completion_tokens", 1));

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Field restTemplateField = OpenAIChatClient.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(mockResponse);

        restTemplateField.set(clientWithApiKey, mockRestTemplate);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.user("Hello")))
                .build();

        ChatResponse response = clientWithApiKey.complete(request);

        assertNotNull(response);
        assertEquals("", response.getContent());
    }
}
