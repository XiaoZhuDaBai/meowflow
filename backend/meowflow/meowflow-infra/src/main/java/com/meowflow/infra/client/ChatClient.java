package com.meowflow.infra.client;

import java.util.List;
import java.util.Map;

/**
 * AI 对话客户端接口（已废弃，请迁移到 meowflow.infra.chat）。
 *
 * @deprecated Use {@link com.meowflow.infra.chat.ChatClient} and
 *             {@link com.meowflow.infra.chat.ChatModelGateway}
 */
@Deprecated
public interface ChatClient {

    /**
     * 发送对话请求
     *
     * @param messages 消息列表
     * @param options  请求选项
     * @return AI 响应
     */
    ChatResponse chat(List<Message> messages, ChatOptions options);

    /**
     * 流式对话
     *
     * @param messages  消息列表
     * @param options   请求选项
     * @param callback  流式回调
     */
    void streamChat(List<Message> messages, ChatOptions options, StreamCallback callback);

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 是否可用
     */
    boolean isAvailable();

    /**
     * 消息对象
     */
    record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }
    }

    /**
     * AI 响应
     */
    record ChatResponse(
            String content,
            String model,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs,
            String finishReason
    ) {
    }

    /**
     * 请求选项
     */
    record ChatOptions(
            Double temperature,
            Integer maxTokens,
            Double topP,
            List<String> stop,
            Map<String, Object> extraParams
    ) {
        public static ChatOptions defaults() {
            return new ChatOptions(0.7, 2048, null, null, null);
        }

        public static ChatOptions of(Double temperature, Integer maxTokens) {
            return new ChatOptions(temperature, maxTokens, null, null, null);
        }
    }

    /**
     * 流式回调
     */
    interface StreamCallback {
        void onChunk(String content, int index);

        void onComplete(ChatResponse response);

        void onError(Throwable error);
    }
}
