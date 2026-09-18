package com.meowflow.infra.integration;

import com.meowflow.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 钉钉发送器
 */
@Slf4j
public class DingtalkSender implements IntegrationSender {

    private final IntegrationConfig config;
    private final RestTemplate restTemplate;

    public DingtalkSender(IntegrationConfig config) {
        this.config = config;
        this.restTemplate = createRestTemplate();
    }

    /**
     * 创建 RestTemplate。子类可重写以注入 mock 实例。
     */
    protected RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    @Override
    public SendResult send(String message) {
        long start = System.currentTimeMillis();
        try {
            String webhookUrl = config.getWebhookUrl();

            if (StringUtils.isNotEmpty(config.getSecret())) {
                webhookUrl = signUrl(webhookUrl, config.getSecret());
            }

            Map<String, Object> body = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", message)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && 0 == (Integer) response.getBody().get("errcode")) {
                String messageId = (String) response.getBody().get("messageId");
                return SendResult.success(messageId, System.currentTimeMillis() - start);
            }
            return SendResult.failure("钉钉发送失败: " + response.getBody(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Dingtalk send error", e);
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
        return IntegrationConfig.TYPE_DINGTALK;
    }

    private String signUrl(String webhookUrl, String secret) {
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

            return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("Sign webhook URL error", e);
            return webhookUrl;
        }
    }
}
