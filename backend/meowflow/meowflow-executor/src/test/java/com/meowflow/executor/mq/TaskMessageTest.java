package com.meowflow.executor.mq;

import com.meowflow.executor.model.TaskType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskMessage} + 重试机制 的纯单测。
 */
@DisplayName("TaskMessage 重试机制")
class TaskMessageTest {

    @Test
    @DisplayName("canRetry - 新建消息可以重试")
    void canRetry_newMessage() {
        TaskMessage tm = baseMessage();
        assertThat(tm.canRetry()).isTrue();
    }

    @Test
    @DisplayName("canRetry - 达到 maxRetries 后返回 false")
    void canRetry_exceeded() {
        TaskMessage tm = baseMessage();
        tm.setMaxRetries(3);
        tm.setRetryCount(3);
        assertThat(tm.canRetry()).isFalse();
    }

    @Test
    @DisplayName("incrementRetry - 增加 retryCount")
    void incrementRetry_incrementsCount() {
        TaskMessage tm = baseMessage();
        tm.setRetryCount(0);
        tm.incrementRetry();
        assertThat(tm.getRetryCount()).isEqualTo(1);
        tm.incrementRetry();
        assertThat(tm.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TaskMessage.of - 快速创建消息")
    void of_createsCorrectMessage() {
        TaskMessage tm = TaskMessage.of(1L, "exec-001", "node-A");

        assertThat(tm.getTaskId()).isEqualTo(1L);
        assertThat(tm.getExecutionId()).isEqualTo("exec-001");
        assertThat(tm.getNodeId()).isEqualTo("node-A");
        assertThat(tm.getRetryCount()).isEqualTo(0);
        assertThat(tm.getMaxRetries()).isEqualTo(3);
        assertThat(tm.getPriority()).isEqualTo(50);
        assertThat(tm.getTimestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("canRetry - maxRetries 为 0 时不可重试")
    void canRetry_zeroMaxRetries() {
        TaskMessage tm = baseMessage();
        tm.setMaxRetries(0);
        assertThat(tm.canRetry()).isFalse();
    }

    private TaskMessage baseMessage() {
        TaskMessage tm = new TaskMessage();
        tm.setTaskId(1L);
        tm.setExecutionId("exec-001");
        tm.setNodeId("node-A");
        tm.setNodeType("HTTP_REQUEST");
        tm.setInput(Map.of("url", "https://api.example.com"));
        tm.setMaxRetries(3);
        tm.setTimestamp(System.currentTimeMillis());
        return tm;
    }
}
