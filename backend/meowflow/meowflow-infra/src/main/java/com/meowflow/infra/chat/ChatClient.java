package com.meowflow.infra.chat;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话客户端接口
 */
public interface ChatClient {

    /**
     * 同步对话
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse complete(ChatRequest request);

    /**
     * 流式对话
     *
     * @param request 聊天请求
     * @return 响应内容流
     */
    Flux<String> streamComplete(ChatRequest request);

    /**
     * 获取支持的模型列表
     *
     * @return 支持的模型名称列表
     */
    List<String> getSupportedModels();

    /**
     * 获取客户端提供商名称
     *
     * @return 提供商名称
     */
    String getProviderName();

    /**
     * 检查客户端是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
