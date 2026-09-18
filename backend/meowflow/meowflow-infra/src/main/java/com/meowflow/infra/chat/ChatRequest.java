package com.meowflow.infra.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数，控制随机性
     */
    private Double temperature;

    /**
     * 最大生成token数
     */
    private Integer maxTokens;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;

    /**
     * 创建默认请求
     */
    public static ChatRequest of(String model, List<Message> messages) {
        return ChatRequest.builder()
                .model(model)
                .messages(messages)
                .build();
    }

    /**
     * 创建带参数的请求
     */
    public static ChatRequest of(String model, List<Message> messages, Double temperature, Integer maxTokens) {
        return ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}
