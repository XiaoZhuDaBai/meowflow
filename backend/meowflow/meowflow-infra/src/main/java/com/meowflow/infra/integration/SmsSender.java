package com.meowflow.infra.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SMS 发送器
 */
@Slf4j
public class SmsSender implements IntegrationSender {

    private final IntegrationConfig config;
    private final RestTemplate restTemplate;

    public SmsSender(IntegrationConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public SendResult send(String message) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> cfg = config.getConfig();
            String accessKeyId = (String) cfg.get("accessKeyId");
            String signName = (String) cfg.get("signName");

            Map<String, Object> body = Map.of(
                    "PhoneNumbers", cfg.get("phone"),
                    "SignName", signName,
                    "TemplateCode", cfg.get("templateCode"),
                    "TemplateParam", Map.of("code", message)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Acs-Cloud-Resource", "AcsCloudResource");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = "https://dysmsapi.aliyuncs.com/?AccessKeyId=" + accessKeyId;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && "OK".equals(response.getBody().get("Code"))) {
                return SendResult.success(
                        (String) response.getBody().get("RequestId"),
                        System.currentTimeMillis() - start);
            }
            return SendResult.failure("SMS 发送失败: " + response.getBody(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("SMS send error", e);
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
        return IntegrationConfig.TYPE_SMS;
    }
}
