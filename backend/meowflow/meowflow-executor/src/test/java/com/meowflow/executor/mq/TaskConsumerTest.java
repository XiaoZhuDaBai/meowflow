package com.meowflow.executor.mq;

import com.meowflow.executor.service.TaskDispatchService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskConsumer Tests")
class TaskConsumerTest {

    @Mock
    private TaskDispatchService dispatchService;

    @Mock
    private Channel channel;

    private TaskConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TaskConsumer(dispatchService);
    }

    @Test
    @DisplayName("consumeTask - 成功执行 ack 消息")
    void consumeTask_successfulExec_acksMessage() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        long deliveryTag = 100L;

        consumer.consumeTask(message, channel, deliveryTag);

        verify(dispatchService).executeTask(message);
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    @DisplayName("consumeTask - 执行失败且可重试，nack 重新入队")
    void consumeTask_failureWithRetry_nacksWithRequeue() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        message.setMaxRetries(3);
        message.setRetryCount(0);
        long deliveryTag = 100L;

        doThrow(new RuntimeException("execution failed")).when(dispatchService).executeTask(message);

        consumer.consumeTask(message, channel, deliveryTag);

        verify(channel).basicNack(deliveryTag, false, true);
    }

    @Test
    @DisplayName("consumeTask - 超过最大重试次数，不重新入队")
    void consumeTask_failureMaxRetriesExceeded_nacksWithoutRequeue() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        message.setMaxRetries(2);
        message.setRetryCount(2); // 已经达到最大重试次数
        long deliveryTag = 100L;

        doThrow(new RuntimeException("execution failed")).when(dispatchService).executeTask(message);

        consumer.consumeTask(message, channel, deliveryTag);

        verify(channel).basicNack(deliveryTag, false, false);
    }

    @Test
    @DisplayName("consumeDeadLetter - 成功处理后 ack")
    void consumeDeadLetter_success_acks() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        long deliveryTag = 200L;

        consumer.consumeDeadLetter(message, channel, deliveryTag);

        verify(dispatchService).handleDeadLetterTask(message);
        verify(channel).basicAck(deliveryTag, false);
    }

    @Test
    @DisplayName("consumeDeadLetter - 处理失败 reject 不重新入队")
    void consumeDeadLetter_failure_rejects() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        long deliveryTag = 200L;

        doThrow(new RuntimeException("processing failed")).when(dispatchService).handleDeadLetterTask(message);

        consumer.consumeDeadLetter(message, channel, deliveryTag);

        verify(channel).basicReject(deliveryTag, false);
    }

    @Test
    @DisplayName("consumeTask - channel 错误被捕获")
    void consumeTask_channelError_swallows() throws IOException {
        TaskMessage message = TaskMessage.of(1L, "exec-1", "node-1");
        long deliveryTag = 100L;

        doThrow(new IOException("Channel closed")).when(channel).basicAck(anyLong(), anyBoolean());

        // 不应抛出异常
        consumer.consumeTask(message, channel, deliveryTag);
    }
}
