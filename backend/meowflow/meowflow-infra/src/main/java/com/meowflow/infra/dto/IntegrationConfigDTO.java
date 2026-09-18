package com.meowflow.infra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 集成配置 DTO
 *
 * <p>用于第三方集成的配置参数(Webhook/密钥/自定义配置)传输。</p>
 */
@Data
public class IntegrationConfigDTO {

    private Long id;

    /** 集成类型: dingtalk / feishu / wxwork / email / sms */
    @NotBlank(message = "集成类型不能为空")
    private String type;

    /** 配置名称 */
    @NotBlank(message = "配置名称不能为空")
    private String name;

    /** Webhook URL */
    private String webhookUrl;

    /** 密钥 (加密存储) */
    private String secret;

    private String accessKeyId;
    private String accessKeySecret;

    /** 自定义配置 JSON */
    private String customConfig;

    private Boolean enabled;
    private Integer retryTimes;
    private Integer timeoutSeconds;

    /** 测试结果 */
    private transient Boolean lastTestResult;
    private transient String lastTestMessage;
}
