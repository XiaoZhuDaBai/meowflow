package com.meowflow.infra.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meowflow.common.annotation.Encrypted;
import com.meowflow.common.mybatis.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型配置实体
 * <p>
 * 示例：展示如何在模型配置中使用字段级加密
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_ai_model")
public class AIModelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型提供商: openai, anthropic, azure, baidu, ali, etc.
     */
    private String provider;

    /**
     * 模型标识 (如 gpt-4, claude-3-opus)
     */
    @TableField("model_type")
    private String modelKey;

    /**
     * API Base URL
     */
    @TableField("api_base")
    private String baseUrl;

    /**
     * API Key (加密存储)
     */
    @Encrypted
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    /**
     * API Secret (某些提供商使用, 加密存储)
     */
    @TableField(exist = false)
    private String apiSecret;

    /**
     * 最大上下文窗口 (token数)
     */
    @TableField(exist = false)
    private Integer maxContextTokens;

    /**
     * 最大输出 token 数
     */
    @TableField("max_tokens")
    private Integer maxOutputTokens;

    /**
     * 输入价格 (元/千token)
     */
    @TableField(exist = false)
    private BigDecimal inputPrice;

    /**
     * 输出价格 (元/千token)
     */
    @TableField(exist = false)
    private BigDecimal outputPrice;

    /**
     * 模型能力标签 (逗号分隔): chat, embedding, vision, function_call
     */
    private String capabilities;

    /**
     * 模型版本
     */
    @TableField(exist = false)
    private String version;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否为默认模型
     */
    private Boolean isDefault;

    /**
     * 排序权重
     */
    @TableField("priority")
    private Integer sortOrder;

    /**
     * 备注
     */
    @TableField(exist = false)
    private String remark;

    @JsonProperty("apiKeyMasked")
    public String getApiKeyMasked() {
        if (apiKey == null || apiKey.isBlank()) return null;
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

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
    @TableField(exist = false)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(exist = false)
    private String updateBy;

    /**
     * 逻辑删除标记
     */
    @TableField(exist = false)
    private Integer deleted;
}

