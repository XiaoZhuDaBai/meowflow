package com.meowflow.infra.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void of_shouldCreateMessageWithRoleAndContent() {
        Message message = Message.of("user", "Hello");

        assertNotNull(message);
        assertEquals("user", message.getRole());
        assertEquals("Hello", message.getContent());
    }

    @Test
    void system_shouldCreateSystemMessage() {
        Message message = Message.system("You are a helpful assistant");

        assertNotNull(message);
        assertEquals("system", message.getRole());
        assertEquals("You are a helpful assistant", message.getContent());
    }

    @Test
    void user_shouldCreateUserMessage() {
        Message message = Message.user("What is AI?");

        assertNotNull(message);
        assertEquals("user", message.getRole());
        assertEquals("What is AI?", message.getContent());
    }

    @Test
    void assistant_shouldCreateAssistantMessage() {
        Message message = Message.assistant("AI stands for Artificial Intelligence");

        assertNotNull(message);
        assertEquals("assistant", message.getRole());
        assertEquals("AI stands for Artificial Intelligence", message.getContent());
    }

    @Test
    void builder_shouldCreateMessage() {
        Message message = Message.builder()
            .role("user")
            .content("Test message")
            .build();

        assertNotNull(message);
        assertEquals("user", message.getRole());
        assertEquals("Test message", message.getContent());
    }

    @Test
    void messageBuilder_shouldWork() {
        Message message = Message.messageBuilder()
            .role("user")
            .content("Builder test")
            .build();

        assertNotNull(message);
        assertEquals("user", message.getRole());
        assertEquals("Builder test", message.getContent());
    }

    @Test
    void message_shouldBeImmutableInRole() {
        Message message = new Message("user", "content");
        assertEquals("user", message.getRole());
    }

    @Test
    void messageList_shouldWorkWithMultipleRoles() {
        List<Message> messages = List.of(
            Message.system("You are a helpful assistant"),
            Message.user("Hello"),
            Message.assistant("Hello! How can I help you?")
        );

        assertEquals(3, messages.size());
        assertEquals("system", messages.get(0).getRole());
        assertEquals("user", messages.get(1).getRole());
        assertEquals("assistant", messages.get(2).getRole());
    }
}
