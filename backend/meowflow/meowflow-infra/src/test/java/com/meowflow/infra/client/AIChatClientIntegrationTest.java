package com.meowflow.infra.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.service.ChatClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAI / Claude 客户端测试 —— 通过 WireMock 拦截 HTTP 请求并返回 mock 响应。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@DisplayName("AIChatClient 集成 — WireMock 模拟 OpenAI / Claude")
class AIChatClientIntegrationTest extends BaseIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("meowflow.infra.chat.openai.base-url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("meowflow.infra.chat.anthropic.base-url", () -> "http://localhost:" + wireMock.getPort());
        registry.add("meowflow.infra.chat.openai.api-key", () -> "test-key");
        registry.add("meowflow.infra.chat.anthropic.api-key", () -> "test-key");
    }

    @Autowired
    private ChatClientFactory chatClientFactory;

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void cleanup() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("OpenAI chat — 返回 choices[0].message.content")
    void openAi_returnsContent() {
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "cmpl-1",
                                  "object": "chat.completion",
                                  "created": 1700000000,
                                  "model": "gpt-4",
                                  "choices": [
                                    {
                                      "index": 0,
                                      "message": {"role":"assistant","content":"hello world"},
                                      "finish_reason": "stop"
                                    }
                                  ],
                                  "usage": {"prompt_tokens":5,"completion_tokens":2,"total_tokens":7}
                                }
                                """)));

        ChatClient.Message msg = new ChatClient.Message("user", "hi");
        ChatClient.ChatResponse resp = chatClientFactory.chat(
                "gpt-4o-mini", List.of(msg), ChatClient.ChatOptions.defaults());

        assertThat(resp).isNotNull();
        assertThat(resp.content()).isEqualTo("hello world");
    }

    @Test
    @DisplayName("Claude chat — 返回 content[0].text")
    void claude_returnsContent() {
        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "msg_1",
                                  "type": "message",
                                  "role": "assistant",
                                  "content": [{"type": "text", "text": "claude says hi"}],
                                  "model": "claude-3",
                                  "usage": {"input_tokens":3,"output_tokens":3}
                                }
                                """)));

        ChatClient.Message msg = new ChatClient.Message("user", "hi");
        ChatClient.ChatResponse resp = chatClientFactory.chat(
                "claude-3-5-sonnet-20240620", List.of(msg), ChatClient.ChatOptions.defaults());

        assertThat(resp.content()).isEqualTo("claude says hi");
    }
}

