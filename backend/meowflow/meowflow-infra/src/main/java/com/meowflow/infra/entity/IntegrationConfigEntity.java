package com.meowflow.infra.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.meowflow.common.annotation.Encrypted;
import com.meowflow.common.mybatis.EncryptedStringTypeHandler;
import com.meowflow.infra.integration.IntegrationConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 集成配置实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_integration_config")
public class IntegrationConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 集成类型: dingtalk, feishu, wxwork, email, sms
     */
    private String type;

    /**
     * 配置名称
     */
    private String name;

    /**
     * Webhook URL
     */
    private String webhookUrl;

    /**
     * 密钥/密码 (加密存储)
     */
    @Encrypted
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String secret;

    /**
     * Access Key ID (加密存储)
     */
    @Encrypted
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String accessKeyId;

    /**
     * Access Key Secret (加密存储)
     */
    @Encrypted
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String accessKeySecret;

    /**
     * 自定义配置 (JSON格式，部分敏感字段需加密)
     */
    private String customConfig;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 超时时间(秒)
     */
    private Integer timeoutSeconds;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;

    /**
     * 转换为旧的发送层 IntegrationConfig。
     * 仅作为过渡,后续可逐步迁移到基于 Entity 的发送实现。
     */
    public IntegrationConfig toIntegrationConfig() {
        IntegrationConfig config = new IntegrationConfig();
        config.setId(this.id);
        config.setType(this.type);
        config.setName(this.name);
        config.setWebhookUrl(this.webhookUrl);
        config.setSecret(this.secret);
        config.setEnabled(this.enabled != null && this.enabled);
        config.setRetryTimes(this.retryTimes);
        config.setTimeoutSeconds(this.timeoutSeconds);
        return config;
    }
}
