package com.meowflow.infra.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 结束原因
     */
    private String finishReason;

    /**
     * 提示词token数
     */
    private Integer promptTokens;

    /**
     * 完成token数
     */
    private Integer completionTokens;

    /**
     * 延迟时间（毫秒）
     */
    private Long latencyMs;

    @Builder.Default
    private List<Map<String, Object>> toolCalls = java.util.List.of();

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String content, String model, String finishReason,
                                        Integer promptTokens, Integer completionTokens, Long latencyMs) {
        return ChatResponse.builder()
                .content(content)
                .model(model)
                .finishReason(finishReason)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .latencyMs(latencyMs)
                .build();
    }

    /**
     * 获取总token数
     */
    public Integer getTotalTokens() {
        if (promptTokens == null) promptTokens = 0;
        if (completionTokens == null) completionTokens = 0;
        return promptTokens + completionTokens;
    }
}
