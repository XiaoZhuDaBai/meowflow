package com.meowflow.infra.service;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志实体
 */
@Data
@TableName("mf_ai_invoke_log")
public class AIInvokeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long executionId;
    private String workflowId;
    private String nodeId;

    /**
     * 模型名称: gpt-4o, claude-3-5-sonnet, etc.
     */
    private String model;

    /**
     * 提供商: openai, anthropic, ali, baidu
     */
    private String provider;

    private String prompt;
    private String completion;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    /**
     * 实际费用 (USD)
     */
    private Double costAmount;

    /**
     * 耗时 (ms)
     */
    private Long costMs;

    /**
     * 状态: 1=成功 0=失败
     */
    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

