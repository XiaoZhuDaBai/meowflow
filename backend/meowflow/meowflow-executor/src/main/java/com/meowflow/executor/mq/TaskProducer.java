package com.meowflow.executor.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 任务消息生产者
 * 负责将任务消息发送到 RabbitMQ 队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送任务到主队列
     *
     * @param taskMessage 任务消息
     */
    public void sendTask(TaskMessage taskMessage) {
        log.info("Sending task to queue: taskId={}, executionId={}, nodeId={}",
                taskMessage.getTaskId(), taskMessage.getExecutionId(), taskMessage.getNodeId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_ROUTING_KEY,
                    taskMessage
            );
            log.debug("Task sent successfully: {}", taskMessage.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task: {}", taskMessage.getTaskId(), e);
            throw new RuntimeException("Failed to send task to queue", e);
        }
    }

    /**
     * 延迟发送任务 (用于重试机制)
     * 需要 RabbitMQ delayed-message-exchange 插件支持
     *
     * @param taskMessage 任务消息
     * @param delayMs      延迟时间(毫秒)
     */
    public void sendTaskDelayed(TaskMessage taskMessage, long delayMs) {
        log.info("Sending delayed task: taskId={}, delay={}ms, retryCount={}",
                taskMessage.getTaskId(), delayMs, taskMessage.getRetryCount());

        try {
            MessagePostProcessor messagePostProcessor = message -> {
                message.getMessageProperties().setHeader("x-delay", delayMs);
                return message;
            };

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TASK_EXCHANGE,
                    RabbitMQConfig.TASK_ROUTING_KEY,
                    taskMessage,
                    messagePostProcessor
            );

            log.debug("Delayed task sent successfully: {}", taskMessage.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send delayed task: {}", taskMessage.getTaskId(), e);
            throw new RuntimeException("Failed to send delayed task to queue", e);
        }
    }

    /**
     * 发送任务到死信队列 (不常用，通常由 RabbitMQ 自动路由)
     *
     * @param taskMessage 任务消息
     */
    public void sendToDeadLetter(TaskMessage taskMessage) {
        log.warn("Sending task to dead letter queue: taskId={}, retryCount={}",
                taskMessage.getTaskId(), taskMessage.getRetryCount());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.DLQ_ROUTING_KEY,
                    taskMessage
            );
        } catch (Exception e) {
            log.error("Failed to send task to DLQ: {}", taskMessage.getTaskId(), e);
        }
    }

    /**
     * 带优先级的任务发送
     *
     * @param taskMessage 任务消息
     * @param priority    优先级 (0-9)
     */
    public void sendTaskWithPriority(TaskMessage taskMessage, int priority) {
        log.info("Sending priority task: taskId={}, priority={}",
                taskMessage.getTaskId(), priority);

        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setPriority(priority);
            return message;
        };

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.TASK_ROUTING_KEY,
                taskMessage,
                messagePostProcessor
        );
    }
}
