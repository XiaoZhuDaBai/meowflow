package com.meowflow.infra.integration;

/**
 * 集成发送器接口
 */
public interface IntegrationSender {

    /**
     * 发送消息
     *
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult send(String message);

    /**
     * 异步发送消息
     *
     * @param message 消息内容
     * @param callback 回调
     */
    void sendAsync(String message, SendCallback callback);

    /**
     * 获取发送器类型
     */
    String getType();

    /**
     * 发送结果
     */
    record SendResult(
            boolean success,
            String messageId,
            String errorMessage,
            long costMs
    ) {
        public static SendResult success(String messageId, long costMs) {
            return new SendResult(true, messageId, null, costMs);
        }

        public static SendResult failure(String errorMessage, long costMs) {
            return new SendResult(false, null, errorMessage, costMs);
        }
    }

    /**
     * 发送回调
     */
    interface SendCallback {
        void onSuccess(String messageId);
        void onFailure(String errorMessage);
    }
}
