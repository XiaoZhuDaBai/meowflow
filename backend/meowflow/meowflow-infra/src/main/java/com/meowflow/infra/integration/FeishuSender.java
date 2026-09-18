package com.meowflow.infra.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 飞书发送器
 */
@Slf4j
public class FeishuSender implements IntegrationSender {

    private final IntegrationConfig config;
    private final RestTemplate restTemplate;

    public FeishuSender(IntegrationConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public SendResult send(String message) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "msg_type", "text",
                    "content", Map.of("text", message)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    config.getWebhookUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && "success".equals(response.getBody().get("code"))) {
                return SendResult.success(
                        String.valueOf(response.getBody().get("msg_id")),
                        System.currentTimeMillis() - start);
            }
            return SendResult.failure("飞书发送失败: " + response.getBody(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Feishu send error", e);
            return SendResult.failure(e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public void sendAsync(String message, SendCallback callback) {
        try {
            new Thread(() -> {
                SendResult result = send(message);
                if (result.success()) {
                    callback.onSuccess(result.messageId());
                } else {
                    callback.onFailure(result.errorMessage());
                }
            }).start();
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    @Override
    public String getType() {
        return IntegrationConfig.TYPE_FEISHU;
    }
}
