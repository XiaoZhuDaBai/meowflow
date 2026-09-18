package com.meowflow.infra.service;

import com.meowflow.infra.integration.DingtalkSender;
import com.meowflow.infra.integration.FeishuSender;
import com.meowflow.infra.integration.IntegrationConfig;
import com.meowflow.infra.integration.IntegrationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IntegrationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationService Tests")
class IntegrationServiceTest {

    @Mock
    private DingtalkSender dingtalkSender;

    @Mock
    private FeishuSender feishuSender;

    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService();
    }

    @Nested
    @DisplayName("Config Management Tests")
    class ConfigManagementTests {

        @Test
        @DisplayName("Should save and retrieve config")
        void shouldSaveAndRetrieveConfig() {
            IntegrationConfig config = new IntegrationConfig();
            config.setType("dingtalk");
            config.setName("钉钉配置");
            config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
            config.setEnabled(true);

            integrationService.saveConfig(config);

            IntegrationConfig retrieved = integrationService.getConfig("dingtalk");
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getName()).isEqualTo("钉钉配置");
        }

        @Test
        @DisplayName("Should list all configs")
        void shouldListAllConfigs() {
            IntegrationConfig config1 = new IntegrationConfig();
            config1.setType("dingtalk");
            config1.setName("钉钉");
            config1.setEnabled(true);

            IntegrationConfig config2 = new IntegrationConfig();
            config2.setType("feishu");
            config2.setName("飞书");
            config2.setEnabled(true);

            integrationService.saveConfig(config1);
            integrationService.saveConfig(config2);

            List<IntegrationConfig> configs = integrationService.listConfigs();

            assertThat(configs).hasSize(2);
        }

        @Test
        @DisplayName("Should delete config")
        void shouldDeleteConfig() {
            IntegrationConfig config = new IntegrationConfig();
            config.setType("dingtalk");
            config.setEnabled(true);
            integrationService.saveConfig(config);

            integrationService.deleteConfig("dingtalk");

            assertThat(integrationService.getConfig("dingtalk")).isNull();
        }

        @Test
        @DisplayName("Should update existing config")
        void shouldUpdateExistingConfig() {
            IntegrationConfig config = new IntegrationConfig();
            config.setType("dingtalk");
            config.setName("原名称");
            config.setEnabled(true);
            integrationService.saveConfig(config);

            IntegrationConfig updated = new IntegrationConfig();
            updated.setType("dingtalk");
            updated.setName("新名称");
            updated.setEnabled(false);
            integrationService.saveConfig(updated);

            IntegrationConfig retrieved = integrationService.getConfig("dingtalk");
            assertThat(retrieved.getName()).isEqualTo("新名称");
            assertThat(retrieved.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Quick Config Creation Tests")
    class QuickConfigCreationTests {

        @Test
        @DisplayName("Should create dingtalk config with secret")
        void shouldCreateDingtalkConfigWithSecret() {
            String webhookUrl = "https://oapi.dingtalk.com/robot/send?access_token=test";
            String secret = "SEC1234567890abcdef";

            IntegrationConfig config = integrationService.createDingtalkConfig(webhookUrl, secret);

            assertThat(config).isNotNull();
            assertThat(config.getType()).isEqualTo("dingtalk");
            assertThat(config.getWebhookUrl()).isEqualTo(webhookUrl);
            assertThat(config.getSecret()).isEqualTo(secret);
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should create dingtalk config without secret")
        void shouldCreateDingtalkConfigWithoutSecret() {
            String webhookUrl = "https://oapi.dingtalk.com/robot/send?access_token=test";

            IntegrationConfig config = integrationService.createDingtalkConfig(webhookUrl, null);

            assertThat(config).isNotNull();
            assertThat(config.getSecret()).isNullOrEmpty();
        }

        @Test
        @DisplayName("Should create feishu config")
        void shouldCreateFeishuConfig() {
            String webhookUrl = "https://open.feishu.cn/open-apis/bot/v2/hook/test-hook";

            IntegrationConfig config = integrationService.createFeishuConfig(webhookUrl);

            assertThat(config).isNotNull();
            assertThat(config.getType()).isEqualTo("feishu");
            assertThat(config.getWebhookUrl()).isEqualTo(webhookUrl);
        }

        @Test
        @DisplayName("Should create wxwork config")
        void shouldCreateWxworkConfig() {
            String webhookUrl = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test";

            IntegrationConfig config = integrationService.createWxworkConfig(webhookUrl);

            assertThat(config).isNotNull();
            assertThat(config.getType()).isEqualTo("wxwork");
            assertThat(config.getWebhookUrl()).isEqualTo(webhookUrl);
        }
    }

    @Nested
    @DisplayName("Send Tests")
    class SendTests {

        @Test
        @DisplayName("Should send message through registered sender")
        void shouldSendMessageThroughRegisteredSender() {
            IntegrationSender.SendResult expectedResult = IntegrationSender.SendResult.success("msg-123", 100L);
            when(dingtalkSender.send(anyString())).thenReturn(expectedResult);
            integrationService.registerSender("dingtalk", dingtalkSender);

            IntegrationSender.SendResult result = integrationService.send("dingtalk", "测试消息");

            assertThat(result.success()).isTrue();
            assertThat(result.messageId()).isEqualTo("msg-123");
        }

        @Test
        @DisplayName("Should return failure for unregistered type")
        void shouldReturnFailureForUnregisteredType() {
            IntegrationSender.SendResult result = integrationService.send("unknown-type", "消息");

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("集成配置不存在");
        }

        @Test
        @DisplayName("Should return failure for disabled config")
        void shouldReturnFailureForDisabledConfig() {
            IntegrationConfig config = new IntegrationConfig();
            config.setType("dingtalk");
            config.setEnabled(false);
            integrationService.saveConfig(config);

            IntegrationSender.SendResult result = integrationService.send("dingtalk", "消息");

            assertThat(result.success()).isFalse();
        }
    }

    @Nested
    @DisplayName("Async Send Tests")
    class AsyncSendTests {

        @Test
        @DisplayName("Should send async message with callback")
        void shouldSendAsyncMessageWithCallback() {
            IntegrationSender.SendResult expectedResult = IntegrationSender.SendResult.success("async-msg", 50L);
            doAnswer(inv -> {
                IntegrationSender.SendCallback cb = inv.getArgument(1);
                cb.onSuccess("async-msg");
                return null;
            }).when(dingtalkSender).sendAsync(anyString(), any());
            integrationService.registerSender("dingtalk", dingtalkSender);

            boolean[] callbackCalled = {false};
            integrationService.sendAsync("dingtalk", "异步消息", new IntegrationSender.SendCallback() {
                @Override
                public void onSuccess(String messageId) {
                    callbackCalled[0] = true;
                }

                @Override
                public void onFailure(String errorMessage) {
                }
            });

            assertThat(callbackCalled[0]).isTrue();
        }
    }

    @Nested
    @DisplayName("Send With Retry Tests")
    class SendWithRetryTests {

        @Test
        @DisplayName("Should retry failed send")
        void shouldRetryFailedSend() {
            when(dingtalkSender.send(anyString()))
                    .thenReturn(IntegrationSender.SendResult.failure("失败", 10L))
                    .thenReturn(IntegrationSender.SendResult.success("重试成功", 50L));
            integrationService.registerSender("dingtalk", dingtalkSender);

            integrationService.sendWithRetry("dingtalk", "重试消息", 3);

            verify(dingtalkSender, times(2)).send(anyString());
        }

        @Test
        @DisplayName("Should stop retrying after max attempts")
        void shouldStopAfterMaxAttempts() {
            when(dingtalkSender.send(anyString()))
                    .thenReturn(IntegrationSender.SendResult.failure("一直失败", 10L));
            integrationService.registerSender("dingtalk", dingtalkSender);

            integrationService.sendWithRetry("dingtalk", "消息", 3);

            verify(dingtalkSender, times(3)).send(anyString());
        }
    }
}
