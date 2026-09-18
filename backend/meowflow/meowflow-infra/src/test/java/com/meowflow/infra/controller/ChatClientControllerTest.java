package com.meowflow.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.chat.ChatModelGateway;
import com.meowflow.infra.chat.ChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatClientController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ChatClientController HTTP 层")
class ChatClientControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ChatModelGateway chatModelGateway;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /chat")
    void chat_invokesGateway() throws Exception {
        when(chatModelGateway.complete(any()))
                .thenReturn(ChatResponse.success("OK", "gpt-test", "stop", 1, 1, 1L));

        mockMvc.perform(post("/api/infra/chat/chat")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("OK"));
        verify(chatModelGateway).complete(any());
    }

    @Test
    @DisplayName("POST /stream returns SSE stream")
    void stream_returnsSse() throws Exception {
        when(chatModelGateway.stream(any())).thenReturn(Flux.empty());

        mockMvc.perform(post("/api/infra/chat/stream")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /chat/providers")
    void providers_returnsSupportedProviders() throws Exception {
        mockMvc.perform(get("/api/infra/chat/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    private ChatClientController.ChatRequest buildRequest() {
        ChatClientController.ChatRequest req = new ChatClientController.ChatRequest();
        req.setModel("gpt-4");
        ChatClientController.MessageDTO m = new ChatClientController.MessageDTO();
        m.setRole("user");
        m.setContent("hello");
        req.setMessages(List.of(m));
        return req;
    }
}
