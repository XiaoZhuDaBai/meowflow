package com.meowflow.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ 消息发送服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送消息（异步确认）
     */
    public void send(String exchange, String routingKey, Object message) {
        send(exchange, routingKey, message, null);
    }

    /**
     * 发送消息（带关联ID）
     */
    public void send(String exchange, String routingKey, Object message, String correlationId) {
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;
        CorrelationData correlationData = new CorrelationData(finalCorrelationId);
        correlationData.getFuture().whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("消息发送失败: exchange={}, routingKey={}, correlationId={}, error={}",
                        exchange, routingKey, finalCorrelationId, ex.getMessage());
            } else {
                log.debug("消息发送成功: exchange={}, routingKey={}, correlationId={}, ack={}",
                        exchange, routingKey, finalCorrelationId, result.isAck());
            }
        });

        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }

    /**
     * 发送延迟消息（需要 delayed message exchange 插件）
     *
     * @param delayMillis 延迟时间（毫秒）
     */
    public void sendDelay(String exchange, String routingKey, Object message, long delayMillis) {
        String correlationId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setHeader("x-delay", delayMillis);
            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            msg.getMessageProperties().setCorrelationId(correlationId);
            return msg;
        });

        log.debug("延迟消息已发送: exchange={}, routingKey={}, delay={}ms, correlationId={}",
                exchange, routingKey, delayMillis, correlationId);
    }

    /**
     * 发送同步消息（等待确认）
     */
    public boolean sendSync(String exchange, String routingKey, Object message, long timeoutMs) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (confirm != null && confirm.isAck()) {
                log.debug("同步消息发送成功: exchange={}, routingKey={}, correlationId={}",
                        exchange, routingKey, correlationId);
                return true;
            } else {
                log.error("同步消息发送失败: exchange={}, routingKey={}, correlationId={}, reason={}",
                        exchange, routingKey, correlationId, confirm != null ? confirm.getReason() : "unknown");
                return false;
            }
        } catch (Exception e) {
            log.error("同步消息发送异常: exchange={}, routingKey={}, correlationId={}, error={}",
                    exchange, routingKey, correlationId, e.getMessage());
            return false;
        }
    }

    /**
     * 发送工作流节点执行消息
     */
    public void sendWorkflowNode(Long workflowId, Long nodeId, String action) {
        MqMessage mqMessage = MqMessage.builder()
                .workflowId(workflowId)
                .nodeId(nodeId)
                .action(action)
                .build();
        send(RabbitConfig.WORKFLOW_EXCHANGE, RabbitConfig.WORKFLOW_NODE_ROUTING_KEY, mqMessage);
    }

    /**
     * 发送工作流完成消息
     */
    public void sendWorkflowFinish(Long workflowId) {
        MqMessage mqMessage = MqMessage.builder()
                .workflowId(workflowId)
                .action("finish")
                .build();
        send(RabbitConfig.WORKFLOW_EXCHANGE, RabbitConfig.WORKFLOW_FINISH_ROUTING_KEY, mqMessage);
    }

    /**
     * 发送通知消息
     */
    public void sendNotify(Long userId, String title, String content, String type) {
        NotifyMessage notifyMessage = NotifyMessage.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .build();
        send(RabbitConfig.NOTIFY_EXCHANGE, RabbitConfig.NOTIFY_ROUTING_KEY, notifyMessage);
    }

    /**
     * 发送延迟任务
     */
    public void sendDelayTask(Long workflowId, Long nodeId, long delayMillis) {
        MqMessage mqMessage = MqMessage.builder()
                .workflowId(workflowId)
                .nodeId(nodeId)
                .action("delay")
                .build();
        sendDelay(RabbitConfig.DELAY_EXCHANGE, RabbitConfig.DELAY_TASK_ROUTING_KEY, mqMessage, delayMillis);
    }
}
