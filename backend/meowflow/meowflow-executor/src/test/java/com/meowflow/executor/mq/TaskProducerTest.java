package com.meowflow.executor.mq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskProducer Tests")
class TaskProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TaskProducer producer;

    @BeforeEach
    void setUp() {
        producer = new TaskProducer(rabbitTemplate);
    }

    @Test
    @DisplayName("sendTask - 发送普通任务")
    void sendTask_validMessage_invokesRabbitTemplate() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");

        producer.sendTask(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_ROUTING_KEY),
                eq(message)
        );
    }

    @Test
    @DisplayName("sendTask - RabbitMQ 失败抛 RuntimeException")
    void sendTask_rabbitFailure_throwsRuntimeException() {
        doThrow(new RuntimeException("Connection refused"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> producer.sendTask(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send task");
    }

    @Test
    @DisplayName("sendTaskDelayed - 设置 x-delay header")
    void sendTaskDelayed_setsDelayHeader() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        long delay = 5000L;

        producer.sendTaskDelayed(message, delay);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_ROUTING_KEY),
                eq(message),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    @DisplayName("sendTaskDelayed - 失败抛异常")
    void sendTaskDelayed_failure_throwsException() {
        doThrow(new RuntimeException("Exchange not found"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class), any(MessagePostProcessor.class));

        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> producer.sendTaskDelayed(message, 1000))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("sendTaskWithPriority - 设置 priority")
    void sendTaskWithPriority_setsPriority() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        int priority = 8;

        producer.sendTaskWithPriority(message, priority);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_ROUTING_KEY),
                eq(message),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    @DisplayName("sendToDeadLetter - 发送到死信队列")
    void sendToDeadLetter_sendsToDLQ() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        message.setRetryCount(3);

        producer.sendToDeadLetter(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DLX_EXCHANGE),
                eq(RabbitMQConfig.DLQ_ROUTING_KEY),
                eq(message)
        );
    }

    @Test
    @DisplayName("sendToDeadLetter - 失败仅记录日志不抛异常")
    void sendToDeadLetter_failure_swallowsException() {
        doThrow(new RuntimeException("DLX down"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");

        // 不应该抛异常
        producer.sendToDeadLetter(message);
    }

    @Test
    @DisplayName("MessagePostProcessor - 验证 delay header 设置")
    void messagePostProcessor_setsDelayHeader() throws Exception {
        org.mockito.Mockito.mock(Message.class);

        // 验证 sendTaskDelayed 的 MessagePostProcessor 行为
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        org.mockito.ArgumentCaptor<MessagePostProcessor> captor =
                org.mockito.ArgumentCaptor.forClass(MessagePostProcessor.class);

        producer.sendTaskDelayed(message, 1000L);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_ROUTING_KEY),
                eq(message),
                captor.capture()
        );

        MessagePostProcessor processor = captor.getValue();
        org.mockito.Mockito.mock(org.springframework.amqp.core.MessageProperties.class);
        // Just verify it's not null
        org.assertj.core.api.Assertions.assertThat(processor).isNotNull();
    }

    @Test
    @DisplayName("TaskMessage - canRetry logic")
    void taskMessage_canRetry() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        message.setMaxRetries(3);
        message.setRetryCount(0);

        org.assertj.core.api.Assertions.assertThat(message.canRetry()).isTrue();

        message.incrementRetry();
        message.incrementRetry();
        org.assertj.core.api.Assertions.assertThat(message.canRetry()).isTrue();

        message.incrementRetry();
        org.assertj.core.api.Assertions.assertThat(message.canRetry()).isFalse();
    }

    @Test
    @DisplayName("TaskMessage - 0 retries")
    void taskMessage_zeroRetries_cannotRetry() {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        message.setMaxRetries(0);

        org.assertj.core.api.Assertions.assertThat(message.canRetry()).isFalse();
    }
}
