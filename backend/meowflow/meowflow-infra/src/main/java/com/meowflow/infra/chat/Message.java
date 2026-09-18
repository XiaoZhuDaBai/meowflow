package com.meowflow.infra.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 消息对象
 */
@Data
@Builder(builderMethodName = "messageBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * 角色：system, user, assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    private String name;

    private List<Map<String, Object>> toolCalls;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 创建系统消息
     */
    public static Message system(String content) {
        return new Message("system", content);
    }

    /**
     * 创建用户消息
     */
    public static Message user(String content) {
        return new Message("user", content);
    }

    /**
     * 创建助手消息
     */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }

    public static Message tool(String name, String content) {
        Message message = new Message("tool", content);
        message.setName(name);
        return message;
    }

    public static Message assistantWithToolCalls(String content, List<Map<String, Object>> toolCalls) {
        Message message = new Message("assistant", content);
        message.setToolCalls(toolCalls);
        return message;
    }

    /**
     * 从角色和内容创建消息
     */
    public static Message of(String role, String content) {
        return new Message(role, content);
    }

    /**
     * 创建 Builder
     */
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    /**
     * Builder 类
     */
    public static class MessageBuilder {
        private String role;
        private String content;

        public MessageBuilder role(String role) {
            this.role = role;
            return this;
        }

        public MessageBuilder content(String content) {
            this.content = content;
            return this;
        }

        public Message build() {
            return new Message(role, content);
        }
    }
}
