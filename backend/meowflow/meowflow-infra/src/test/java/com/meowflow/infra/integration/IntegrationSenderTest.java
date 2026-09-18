package com.meowflow.infra.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IntegrationSender 单元测试
 */
@DisplayName("IntegrationSender Tests")
class IntegrationSenderTest {

    @Nested
    @DisplayName("SendResult Tests")
    class SendResultTests {

        @Test
        @DisplayName("Should create successful result")
        void shouldCreateSuccessfulResult() {
            IntegrationSender.SendResult result = IntegrationSender.SendResult.success("msg-123", 150L);

            assertThat(result.success()).isTrue();
            assertThat(result.messageId()).isEqualTo("msg-123");
            assertThat(result.errorMessage()).isNull();
            assertThat(result.costMs()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            IntegrationSender.SendResult result = IntegrationSender.SendResult.failure("网络错误", 200L);

            assertThat(result.success()).isFalse();
            assertThat(result.messageId()).isNull();
            assertThat(result.errorMessage()).isEqualTo("网络错误");
            assertThat(result.costMs()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("SendCallback Tests")
    class SendCallbackTests {

        @Test
        @DisplayName("Should handle successful callback")
        void shouldHandleSuccessfulCallback() {
            AtomicBoolean successCalled = new AtomicBoolean(false);
            AtomicReference<String> messageId = new AtomicReference<>();

            IntegrationSender.SendCallback callback = new IntegrationSender.SendCallback() {
                @Override
                public void onSuccess(String msgId) {
                    successCalled.set(true);
                    messageId.set(msgId);
                }

                @Override
                public void onFailure(String error) {
                }
            };

            callback.onSuccess("test-msg-id");

            assertThat(successCalled.get()).isTrue();
            assertThat(messageId.get()).isEqualTo("test-msg-id");
        }

        @Test
        @DisplayName("Should handle failure callback")
        void shouldHandleFailureCallback() {
            AtomicBoolean failureCalled = new AtomicBoolean(false);
            AtomicReference<String> errorMsg = new AtomicReference<>();

            IntegrationSender.SendCallback callback = new IntegrationSender.SendCallback() {
                @Override
                public void onSuccess(String msgId) {
                }

                @Override
                public void onFailure(String error) {
                    failureCalled.set(true);
                    errorMsg.set(error);
                }
            };

            callback.onFailure("连接超时");

            assertThat(failureCalled.get()).isTrue();
            assertThat(errorMsg.get()).isEqualTo("连接超时");
        }
    }

    @Nested
    @DisplayName("DingtalkSender Tests")
    class DingtalkSenderTests {

        private IntegrationConfig config;

        @BeforeEach
        void setUp() {
            config = new IntegrationConfig();
            config.setType(IntegrationConfig.TYPE_DINGTALK);
            config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
            config.setSecret("");
            config.setEnabled(true);
        }

        @Test
        @DisplayName("Should create DingtalkSender with correct type")
        void shouldCreateDingtalkSender() {
            DingtalkSender sender = new DingtalkSender(config);

            assertThat(sender.getType()).isEqualTo(IntegrationConfig.TYPE_DINGTALK);
        }
    }

    @Nested
    @DisplayName("FeishuSender Tests")
    class FeishuSenderTests {

        private IntegrationConfig config;

        @BeforeEach
        void setUp() {
            config = new IntegrationConfig();
            config.setType(IntegrationConfig.TYPE_FEISHU);
            config.setWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/test-hook");
            config.setEnabled(true);
        }

        @Test
        @DisplayName("Should create FeishuSender with correct type")
        void shouldCreateFeishuSender() {
            FeishuSender sender = new FeishuSender(config);

            assertThat(sender.getType()).isEqualTo(IntegrationConfig.TYPE_FEISHU);
        }
    }

    @Nested
    @DisplayName("WxworkSender Tests")
    class WxworkSenderTests {

        private IntegrationConfig config;

        @BeforeEach
        void setUp() {
            config = new IntegrationConfig();
            config.setType(IntegrationConfig.TYPE_WXWORK);
            config.setWebhookUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test-key");
            config.setEnabled(true);
        }

        @Test
        @DisplayName("Should create WxworkSender with correct type")
        void shouldCreateWxworkSender() {
            WxworkSender sender = new WxworkSender(config);

            assertThat(sender.getType()).isEqualTo(IntegrationConfig.TYPE_WXWORK);
        }
    }

    @Nested
    @DisplayName("IntegrationConfig Tests")
    class IntegrationConfigTests {

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            IntegrationConfig config = new IntegrationConfig();
            config.setType("dingtalk");
            config.setName("测试配置");
            config.setWebhookUrl("https://example.com/webhook");
            config.setSecret("secret123");
            config.setEnabled(true);

            assertThat(config.getType()).isEqualTo("dingtalk");
            assertThat(config.getName()).isEqualTo("测试配置");
            assertThat(config.getWebhookUrl()).isEqualTo("https://example.com/webhook");
            assertThat(config.getSecret()).isEqualTo("secret123");
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have correct type constants")
        void shouldHaveCorrectTypeConstants() {
            assertThat(IntegrationConfig.TYPE_DINGTALK).isEqualTo("dingtalk");
            assertThat(IntegrationConfig.TYPE_FEISHU).isEqualTo("feishu");
            assertThat(IntegrationConfig.TYPE_WXWORK).isEqualTo("wxwork");
            assertThat(IntegrationConfig.TYPE_EMAIL).isEqualTo("email");
            assertThat(IntegrationConfig.TYPE_SMS).isEqualTo("sms");
        }
    }
}
