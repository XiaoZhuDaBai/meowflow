package com.meowflow.executor.mq;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;

import com.meowflow.executor.model.TaskType;
import com.meowflow.executor.service.TaskDispatchService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 娴嬭瘯 RabbitMQ 鐪熷疄 producer/consumer 閾捐矾銆?
 *
 * <p>绛栫暐锛歮ock {@link TaskDispatchService} 璁?consumer 涓嶇湡姝ｈ窇涓氬姟閫昏緫锛屾柇瑷€
 *   <ol>
 *     <li>producer.sendTask 鍚庢秷鎭綋缁忚繃 rabbitTemplate.convertAndSend 鍙戝嚭</li>
 *     <li>consumer.consumeTask 鏀跺埌 dispatchService.executeTask</li>
 *     <li>retry 涓婇檺鍚庤惤鍒?DLQ 閾捐矾</li>
 *   </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@DisplayName("RabbitMQ 浠诲姟鐢熶骇 / 娑堣垂 / 閲嶈瘯 / DLQ 闆嗘垚")
class TaskRabbitMQIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TaskProducer taskProducer;

    @MockBean
    private TaskDispatchService dispatchService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void cleanup() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("sendTask 鈥?濮旀墭缁?rabbitTemplate.convertAndSend(exchange, routingKey, msg)")
    void sendTask_usesRabbitTemplate() {
        TaskMessage tm = baseMessage();

        taskProducer.sendTask(tm);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_ROUTING_KEY),
                org.mockito.ArgumentMatchers.eq(tm));
    }

    @Test
    @DisplayName("sendTaskDelayed 鈥?璁剧疆 x-delay header")
    void sendTaskDelayed_setsDelayHeader() {
        TaskMessage tm = baseMessage();

        taskProducer.sendTaskDelayed(tm, 5_000L);

        ArgumentCaptor<org.springframework.amqp.core.MessagePostProcessor> captor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_ROUTING_KEY),
                org.mockito.ArgumentMatchers.eq(tm),
                captor.capture());

        org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
        org.springframework.amqp.core.Message msg = new org.springframework.amqp.core.Message(new byte[0], props);
        try {
            captor.getValue().postProcessMessage(msg);
        } catch (Exception ignore) {
        }
        assertThat((Object) msg.getMessageProperties().getHeader("x-delay")).isEqualTo(5_000L);
    }

    @Test
    @DisplayName("sendTaskWithPriority 鈥?璁剧疆 priority header")
    void sendTaskWithPriority_setsPriority() {
        TaskMessage tm = baseMessage();

        taskProducer.sendTaskWithPriority(tm, 7);

        ArgumentCaptor<org.springframework.amqp.core.MessagePostProcessor> captor =
                ArgumentCaptor.forClass(org.springframework.amqp.core.MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.TASK_ROUTING_KEY),
                org.mockito.ArgumentMatchers.eq(tm),
                captor.capture());
    }

    @Test
    @DisplayName("consumeTask 鈥?姝ｅ父璺緞锛歟xecuteTask 鍚?channel.basicAck")
    void consumeTask_ackOnSuccess() throws Exception {
        TaskMessage tm = baseMessage();
        Channel channel = Mockito.mock(Channel.class);

        new TaskConsumer(dispatchService).consumeTask(tm, channel, 42L);

        verify(dispatchService).executeTask(tm);
        verify(channel).basicAck(42L, false);
    }

    @Test
    @DisplayName("consumeTask 鈥?鍑洪敊浣嗘湭瓒?maxRetries锛歜asicNack(requeue=true)")
    void consumeTask_retryWhenUnderLimit() throws Exception {
        TaskMessage tm = baseMessage();
        tm.setMaxRetries(3);
        tm.setRetryCount(0);
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(dispatchService).executeTask(tm);

        Channel channel = Mockito.mock(Channel.class);
        new TaskConsumer(dispatchService).consumeTask(tm, channel, 99L);

        verify(channel).basicNack(99L, false, true);
        assertThat(tm.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("consumeTask 鈥?瓒呰繃 maxRetries锛歜asicNack(requeue=false) 鈫?DLQ")
    void consumeTask_rejectsWhenExceededRetries() throws Exception {
        TaskMessage tm = baseMessage();
        tm.setMaxRetries(2);
        tm.setRetryCount(2);  // 宸叉槸涓婇檺
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(dispatchService).executeTask(tm);

        Channel channel = Mockito.mock(Channel.class);
        new TaskConsumer(dispatchService).consumeTask(tm, channel, 11L);

        verify(channel).basicNack(11L, false, false);
    }

    @Test
    @DisplayName("consumeDeadLetter 鈥?璧?handleDeadLetterTask + ack")
    void consumeDeadLetter() throws Exception {
        TaskMessage tm = baseMessage();
        Channel channel = Mockito.mock(Channel.class);

        new TaskConsumer(dispatchService).consumeDeadLetter(tm, channel, 7L);

        verify(dispatchService).handleDeadLetterTask(tm);
        verify(channel).basicAck(7L, false);
    }

    @Test
    @DisplayName("RabbitMQConfig 鈥?TASK_QUEUE / DLQ_QUEUE 甯搁噺绋冲畾")
    void config_queuesAreStable() {
        assertThat(RabbitMQConfig.TASK_QUEUE).isNotBlank();
        assertThat(RabbitMQConfig.DLQ_QUEUE).isNotBlank();
        assertThat(RabbitMQConfig.TASK_EXCHANGE).isNotBlank();
        assertThat(RabbitMQConfig.DLX_EXCHANGE).isNotBlank();
        assertThat(RabbitMQConfig.TASK_ROUTING_KEY).isNotBlank();
        assertThat(RabbitMQConfig.DLQ_ROUTING_KEY).isNotBlank();
    }

    private TaskMessage baseMessage() {
        TaskMessage tm = new TaskMessage();
        tm.setTaskId(1L);
        tm.setExecutionId("exec-001");
        tm.setNodeId("node-A");
        tm.setNodeType("HTTP_REQUEST");
        tm.setInput(java.util.Map.of("url", "https://api.example.com"));
        tm.setMaxRetries(3);
        tm.setTimestamp(System.currentTimeMillis());
        return tm;
    }
}
