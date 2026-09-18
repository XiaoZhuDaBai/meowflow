package com.meowflow.common.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    // ==================== 交换机定义 ====================

    /**
     * 工作流事件交换机
     */
    public static final String WORKFLOW_EXCHANGE = "workflow.event.exchange";

    /**
     * 任务通知交换机
     */
    public static final String NOTIFY_EXCHANGE = "notify.exchange";

    /**
     * 延迟消息交换机（用于延时任务）
     */
    public static final String DELAY_EXCHANGE = "delay.exchange";

    /**
     * 消息回调交换机
     */
    public static final String CALLBACK_EXCHANGE = "callback.exchange";

    // ==================== 队列定义 ====================

    /**
     * 工作流节点执行队列
     */
    public static final String WORKFLOW_NODE_QUEUE = "workflow.node.queue";

    /**
     * 工作流完成队列
     */
    public static final String WORKFLOW_FINISH_QUEUE = "workflow.finish.queue";

    /**
     * 任务通知队列
     */
    public static final String NOTIFY_QUEUE = "notify.queue";

    /**
     * 延时任务队列
     */
    public static final String DELAY_TASK_QUEUE = "delay.task.queue";

    /**
     * 消息回调队列
     */
    public static final String CALLBACK_QUEUE = "callback.queue";

    /**
     * 死信队列
     */
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    // ==================== 路由键定义 ====================

    public static final String WORKFLOW_NODE_ROUTING_KEY = "workflow.node.execute";
    public static final String WORKFLOW_FINISH_ROUTING_KEY = "workflow.finish";
    public static final String NOTIFY_ROUTING_KEY = "notify.message";
    public static final String DELAY_TASK_ROUTING_KEY = "delay.task";
    public static final String CALLBACK_ROUTING_KEY = "callback.message";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        return template;
    }

    // ==================== 交换机配置 ====================

    /**
     * 工作流事件交换机（Topic 类型）
     */
    @Bean
    public TopicExchange workflowExchange() {
        return ExchangeBuilder.topicExchange(WORKFLOW_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 通知交换机（Direct 类型）
     */
    @Bean
    public DirectExchange notifyExchange() {
        return ExchangeBuilder.directExchange(NOTIFY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 延迟交换机（x-delayed-message 类型）
     * 需要 RabbitMQ 3.8+ 并安装 delayed message exchange 插件
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    /**
     * 消息回调交换机（Direct 类型）
     */
    @Bean
    public DirectExchange callbackExchange() {
        return ExchangeBuilder.directExchange(CALLBACK_EXCHANGE)
                .durable(true)
                .build();
    }

    // ==================== 队列配置 ====================

    /**
     * 工作流节点执行队列
     */
    @Bean
    public Queue workflowNodeQueue() {
        return QueueBuilder.durable(WORKFLOW_NODE_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 工作流完成队列
     */
    @Bean
    public Queue workflowFinishQueue() {
        return QueueBuilder.durable(WORKFLOW_FINISH_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 延时任务队列
     */
    @Bean
    public Queue delayTaskQueue() {
        return QueueBuilder.durable(DELAY_TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 消息回调队列
     */
    @Bean
    public Queue callbackQueue() {
        return QueueBuilder.durable(CALLBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // ==================== 绑定配置 ====================

    @Bean
    public Binding workflowNodeBinding() {
        return BindingBuilder.bind(workflowNodeQueue())
                .to(workflowExchange())
                .with(WORKFLOW_NODE_ROUTING_KEY);
    }

    @Bean
    public Binding workflowFinishBinding() {
        return BindingBuilder.bind(workflowFinishQueue())
                .to(workflowExchange())
                .with(WORKFLOW_FINISH_ROUTING_KEY);
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue())
                .to(notifyExchange())
                .with(NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding delayTaskBinding() {
        return BindingBuilder.bind(delayTaskQueue())
                .to(delayExchange())
                .with(DELAY_TASK_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public Binding callbackBinding() {
        return BindingBuilder.bind(callbackQueue())
                .to(callbackExchange())
                .with(CALLBACK_ROUTING_KEY);
    }
}
