package com.meowflow.infra.integration;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 集成配置
 */
@Data
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String type;
    private String name;
    private String webhookUrl;
    private String secret;
    private Map<String, String> headers;
    private Map<String, Object> config;
    private boolean enabled;
    private Integer retryTimes;
    private Integer timeoutSeconds;

    public static final String TYPE_DINGTALK = "dingtalk";
    public static final String TYPE_FEISHU = "feishu";
    public static final String TYPE_WXWORK = "wxwork";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_SMS = "sms";

    public static IntegrationConfig dingtalk(String webhookUrl, String secret) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(TYPE_DINGTALK);
        config.setWebhookUrl(webhookUrl);
        config.setSecret(secret);
        config.setEnabled(true);
        config.setRetryTimes(3);
        config.setTimeoutSeconds(30);
        return config;
    }

    public static IntegrationConfig feishu(String webhookUrl) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(TYPE_FEISHU);
        config.setWebhookUrl(webhookUrl);
        config.setEnabled(true);
        config.setRetryTimes(3);
        config.setTimeoutSeconds(30);
        return config;
    }

    public static IntegrationConfig wxwork(String webhookUrl) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(TYPE_WXWORK);
        config.setWebhookUrl(webhookUrl);
        config.setEnabled(true);
        config.setRetryTimes(3);
        config.setTimeoutSeconds(30);
        return config;
    }

    public static IntegrationConfig email(String host, int port, String username, String password) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(TYPE_EMAIL);
        config.setConfig(Map.of(
                "host", host,
                "port", port,
                "username", username,
                "password", password
        ));
        config.setEnabled(true);
        config.setRetryTimes(2);
        config.setTimeoutSeconds(60);
        return config;
    }

    public static IntegrationConfig sms(String accessKeyId, String accessKeySecret, String signName) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(TYPE_SMS);
        config.setSecret(accessKeySecret);
        config.setConfig(Map.of(
                "accessKeyId", accessKeyId,
                "signName", signName
        ));
        config.setEnabled(true);
        config.setRetryTimes(3);
        config.setTimeoutSeconds(30);
        return config;
    }
}
