package com.meowflow.executor.mq;

import com.meowflow.executor.model.TaskStatus;
import com.meowflow.executor.service.TaskDispatchService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 任务消息消费者
 * 从 RabbitMQ 队列接收并处理任务消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumer {

    private final TaskDispatchService dispatchService;

    /**
     * 消费主任务队列消息
     * 使用手动 ACK 模式确保消息可靠处理
     */
    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void consumeTask(TaskMessage taskMessage,
                           Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received task from queue: taskId={}, executionId={}, retryCount={}",
                taskMessage.getTaskId(), taskMessage.getExecutionId(), taskMessage.getRetryCount());

        try {
            // 执行任务
            dispatchService.executeTask(taskMessage);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.debug("Task executed and acknowledged: {}", taskMessage.getTaskId());

        } catch (Exception e) {
            log.error("Failed to execute task: {}", taskMessage.getTaskId(), e);
            handleTaskFailure(taskMessage, channel, deliveryTag, e);
        }
    }

    /**
     * 消费死信队列消息
     * 记录失败日志并更新任务状态
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void consumeDeadLetter(TaskMessage taskMessage,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.error("Received dead letter: taskId={}, executionId={}, retryCount={}",
                taskMessage.getTaskId(), taskMessage.getExecutionId(), taskMessage.getRetryCount());

        try {
            // 处理死信任务 - 记录失败、更新状态等
            dispatchService.handleDeadLetterTask(taskMessage);

            // 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to handle dead letter: {}", taskMessage.getTaskId(), e);
            try {
                // 拒绝消息不重新入队
                channel.basicReject(deliveryTag, false);
            } catch (Exception ex) {
                log.error("Failed to reject message: {}", taskMessage.getTaskId(), ex);
            }
        }
    }

    /**
     * 处理任务执行失败
     */
    private void handleTaskFailure(TaskMessage taskMessage, Channel channel,
                                   long deliveryTag, Exception e) {
        try {
            if (taskMessage.canRetry()) {
                // 增加重试次数并拒绝消息（触发重试逻辑）
                taskMessage.incrementRetry();
                log.warn("Task will be retried: taskId={}, nextRetry={}",
                        taskMessage.getTaskId(), taskMessage.getRetryCount());

                // 拒绝消息，重新入队（由 RabbitMQ 重新投递）
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过最大重试次数，发送到死信队列
                log.error("Task exceeded max retries, sending to DLQ: taskId={}",
                        taskMessage.getTaskId());
                channel.basicNack(deliveryTag, false, false);
            }
        } catch (Exception ex) {
            log.error("Failed to handle task failure: {}", taskMessage.getTaskId(), ex);
            try {
                channel.basicReject(deliveryTag, false);
            } catch (Exception ignored) {
            }
        }
    }
}
