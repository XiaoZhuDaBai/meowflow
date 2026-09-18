package com.meowflow.executor.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 延迟消息配置
 * 使用 RabbitMQ delayed-message-exchange 插件实现延迟队列
 * 需要确保插件已安装: rabbitmq-plugins enable rabbitmq_delayed_message_exchange
 */
@Configuration
public class DelayedMessageConfig {

    /**
     * 创建延迟消息交换机
     * 交换机类型: x-delayed-message
     * 需要 RabbitMQ 启用 rabbitmq_delayed_message_exchange 插件
     */
    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(
                RabbitMQConfig.DELAYED_EXCHANGE,
                "x-delayed-message",
                true,  // durable
                false  // autoDelete
                , args
        );
    }

    /**
     * 延迟消息队列
     */
    @Bean
    public Queue delayedQueue() {
        return QueueBuilder.durable(RabbitMQConfig.DELAYED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConfig.DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * 延迟队列绑定
     */
    @Bean
    public Binding delayedBinding() {
        return BindingBuilder.bind(delayedQueue())
                .to(delayedExchange())
                .with(RabbitMQConfig.DELAYED_ROUTING_KEY)
                .noargs();
    }
}
