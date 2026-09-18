package com.meowflow.infra.service;

import com.meowflow.infra.integration.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集成服务
 */
@Slf4j
@Service
public class IntegrationService {

    private final Map<String, IntegrationConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, IntegrationSender> senders = new ConcurrentHashMap<>();

    /**
     * 注册 sender 实例（用于测试注入 mock）
     */
    public void registerSender(String type, IntegrationSender sender) {
        senders.put(type, sender);
    }

    public void saveConfig(IntegrationConfig config) {
        configs.put(config.getType(), config);
        senders.put(config.getType(), createSender(config));
        log.info("Saved integration config: {}", config.getType());
    }

    public IntegrationConfig getConfig(String type) {
        return configs.get(type);
    }

    public List<IntegrationConfig> listConfigs() {
        return configs.values().stream().toList();
    }

    public void deleteConfig(String type) {
        configs.remove(type);
        senders.remove(type);
        log.info("Deleted integration config: {}", type);
    }

    public IntegrationSender.SendResult send(String type, String message) {
        IntegrationSender sender = senders.get(type);
        if (sender == null) {
            IntegrationConfig config = configs.get(type);
            if (config == null) {
                return IntegrationSender.SendResult.failure("集成配置不存在: " + type, 0);
            }
            sender = createSender(config);
            senders.put(type, sender);
        }
        return sender.send(message);
    }

    public void sendAsync(String type, String message, IntegrationSender.SendCallback callback) {
        IntegrationSender sender = senders.get(type);
        if (sender == null) {
            callback.onFailure("集成配置不存在: " + type);
            return;
        }
        sender.sendAsync(message, callback);
    }

    public void sendWithRetry(String type, String message, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            IntegrationSender.SendResult result = send(type, message);
            if (result.success()) {
                log.info("Message sent successfully after {} attempts", attempts + 1);
                return;
            }
            attempts++;
            log.warn("Send failed (attempt {}): {}", attempts, result.errorMessage());
            if (attempts < maxRetries) {
                try {
                    Thread.sleep(1000L * attempts);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.error("Failed to send message after {} attempts", maxRetries);
    }

    private IntegrationSender createSender(IntegrationConfig config) {
        return switch (config.getType()) {
            case IntegrationConfig.TYPE_DINGTALK -> new DingtalkSender(config);
            case IntegrationConfig.TYPE_FEISHU -> new FeishuSender(config);
            case IntegrationConfig.TYPE_WXWORK -> new WxworkSender(config);
            case IntegrationConfig.TYPE_EMAIL -> new EmailSender(config);
            case IntegrationConfig.TYPE_SMS -> new SmsSender(config);
            default -> throw new IllegalArgumentException("不支持的集成类型: " + config.getType());
        };
    }

    public IntegrationConfig createDingtalkConfig(String webhookUrl, String secret) {
        return IntegrationConfig.dingtalk(webhookUrl, secret);
    }

    public IntegrationConfig createFeishuConfig(String webhookUrl) {
        return IntegrationConfig.feishu(webhookUrl);
    }

    public IntegrationConfig createWxworkConfig(String webhookUrl) {
        return IntegrationConfig.wxwork(webhookUrl);
    }
}
