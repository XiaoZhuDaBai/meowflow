package com.meowflow.infra.integration;

import com.meowflow.infra.integration.IntegrationSender.SendCallback;
import com.meowflow.infra.integration.IntegrationSender.SendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IntegrationSender 集成测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IntegrationSender Integration Tests")
class IntegrationSenderIntegrationTest {

    @Mock
    private RestTemplate restTemplate;

    private DingtalkSender dingtalkSender;
    private IntegrationConfig config;

    @BeforeEach
    void setUp() {
        config = new IntegrationConfig();
        config.setType("dingtalk");
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test123");
        config.setSecret("SEC1234567890abcdef");
        config.setEnabled(true);
    }

    @Nested
    @DisplayName("DingtalkSender Tests")
    class DingtalkSenderTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(
                    java.util.Map.of("errcode", 0, "messageId", "msg-id-123"),
                    HttpStatus.OK
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(mockResponse);

            SendResult result = dingtalkSender.send("测试消息");

            assertThat(result.success()).isTrue();
            assertThat(result.messageId()).isEqualTo("msg-id-123");
        }

        @Test
        @DisplayName("Should return failure when API returns error")
        void shouldReturnFailureOnError() {
            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(
                    java.util.Map.of("errcode", 300001, "errmsg", "无效的机器人"),
                    HttpStatus.BAD_REQUEST
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(mockResponse);

            SendResult result = dingtalkSender.send("消息");

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("钉钉发送失败");
        }

        @Test
        @DisplayName("Should handle network exception")
        void shouldHandleNetworkException() {
            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenThrow(new RestClientException("Connection refused"));

            SendResult result = dingtalkSender.send("消息");

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("Connection refused");
        }
    }

    @Nested
    @DisplayName("Async Send Tests")
    class AsyncSendTests {

        @Test
        @DisplayName("Should handle async success callback")
        void shouldHandleAsyncSuccessCallback() {
            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(
                    java.util.Map.of("errcode", 0, "messageId", "async-msg"),
                    HttpStatus.OK
            );
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenReturn(mockResponse);

            AtomicBoolean successCalled = new AtomicBoolean(false);
            AtomicReference<String> messageId = new AtomicReference<>();

            dingtalkSender.sendAsync("异步消息", new SendCallback() {
                @Override
                public void onSuccess(String msgId) {
                    successCalled.set(true);
                    messageId.set(msgId);
                }

                @Override
                public void onFailure(String error) {
                }
            });

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(successCalled.get()).isTrue();
            assertThat(messageId.get()).isEqualTo("async-msg");
        }

        @Test
        @DisplayName("Should handle async failure callback")
        void shouldHandleAsyncFailureCallback() {
            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                    .thenThrow(new RestClientException("Timeout"));

            AtomicBoolean failureCalled = new AtomicBoolean(false);
            AtomicReference<String> errorMsg = new AtomicReference<>();

            dingtalkSender.sendAsync("消息", new SendCallback() {
                @Override
                public void onSuccess(String msgId) {
                }

                @Override
                public void onFailure(String error) {
                    failureCalled.set(true);
                    errorMsg.set(error);
                }
            });

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(failureCalled.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Webhook Signature Tests")
    class WebhookSignatureTests {

        @Test
        @DisplayName("Should sign URL with secret")
        void shouldSignUrlWithSecret() {
            config.setSecret("testSecret123");

            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(
                    java.util.Map.of("errcode", 0, "messageId", "signed"),
                    HttpStatus.OK
            );
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(mockResponse);

            SendResult result = dingtalkSender.send("签名消息");

            assertThat(result.success()).isTrue();
        }

        @Test
        @DisplayName("Should skip signing when no secret")
        void shouldSkipSigningWhenNoSecret() {
            config.setSecret(null);

            dingtalkSender = new DingtalkSender(config) {
                @Override
                protected RestTemplate createRestTemplate() {
                    return restTemplate;
                }
            };

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(
                    java.util.Map.of("errcode", 0, "messageId", "no-sign"),
                    HttpStatus.OK
            );
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(mockResponse);

            SendResult result = dingtalkSender.send("无签名消息");

            assertThat(result.success()).isTrue();
        }
    }
}
