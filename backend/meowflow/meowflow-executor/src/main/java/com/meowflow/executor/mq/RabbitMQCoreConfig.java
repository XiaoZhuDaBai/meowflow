package com.meowflow.executor.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 核心配置
 * 配置 Executor 特有的队列、交换机、绑定关系
 *
 * 注意：ConnectionFactory、RabbitTemplate 由 Spring Boot 自动配置提供，
 * JSON MessageConverter 与 Executor 专属队列在本配置中定义。
 */
@Configuration
public class RabbitMQCoreConfig {

    @Autowired
    private RabbitMQConfig mqConfig;

    @Autowired
    private ConnectionFactory connectionFactory;

    private MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 监听器容器工厂配置
     * JSON 转换器使用 Jackson2 实现，与 Spring Boot 自动配置的 RabbitTemplate 保持一致
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(mqConfig.getPrefetch());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }

    // ========== 主任务队列配置 ==========

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(RabbitMQConfig.TASK_EXCHANGE, true, false);
    }

    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(RabbitMQConfig.TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConfig.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConfig.DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue())
                .to(taskExchange())
                .with(RabbitMQConfig.TASK_ROUTING_KEY);
    }

    // ========== 死信队列配置 ==========

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConfig.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue executorDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConfig.DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(executorDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMQConfig.DLQ_ROUTING_KEY);
    }
}
