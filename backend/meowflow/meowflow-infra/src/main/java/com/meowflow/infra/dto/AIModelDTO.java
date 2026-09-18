package com.meowflow.infra.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型配置 DTO (传输层对象)
 *
 * <p>字段与 {@link com.meowflow.infra.entity.AIModelEntity} 大致对应,
 * 用于 Controller 入参/出参,内部数据可通过 MapStruct 转换或手工拷贝。</p>
 */
@Data
public class AIModelDTO {

    private Long id;

    @NotBlank(message = "模型名称不能为空")
    private String name;

    /** 厂商: openai/anthropic/azure/baidu/ali 等 */
    @NotBlank(message = "厂商不能为空")
    private String provider;

    /** 模型标识: gpt-4o, claude-3-opus */
    @NotBlank(message = "模型标识不能为空")
    private String modelKey;

    /** API Base URL,可为空 */
    private String baseUrl;

    /** API Key (入库时加密,查询时返回脱敏) */
    private String apiKey;

    /** 脱敏 API Key,只读字段 */
    private String apiKeyMasked;

    /** API Secret (某些提供商使用) */
    private String apiSecret;

    private Integer maxContextTokens;
    private Integer maxOutputTokens;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;

    /** 模型能力标签,逗号分隔 */
    private String capabilities;

    private String version;
    private Boolean enabled;
    private Boolean isDefault;
    private Integer sortOrder;
    private String remark;
}
