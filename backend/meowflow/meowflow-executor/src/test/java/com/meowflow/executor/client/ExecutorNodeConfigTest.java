package com.meowflow.executor.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutorNodeConfig 单元测试
 */
class ExecutorNodeConfigTest {

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Given
        ExecutorNodeConfig config = new ExecutorNodeConfig();

        // When
        config.setExecutorId("executor-123");
        config.setHost("localhost");
        config.setPort(8080);
        config.setTimeoutMs(5000);
        config.setConnectionPoolSize(20);

        // Then
        assertThat(config.getExecutorId()).isEqualTo("executor-123");
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.getTimeoutMs()).isEqualTo(5000);
        assertThat(config.getConnectionPoolSize()).isEqualTo(20);
    }

    @Test
    void defaultValues_shouldBeNull() {
        // Given
        ExecutorNodeConfig config = new ExecutorNodeConfig();

        // Then
        assertThat(config.getExecutorId()).isNull();
        assertThat(config.getHost()).isNull();
        assertThat(config.getPort()).isEqualTo(0);
    }

    @Test
    void getUrl_shouldGenerateCorrectUrl() {
        // Given
        ExecutorNodeConfig config = new ExecutorNodeConfig();
        config.setHost("192.168.1.100");
        config.setPort(9090);

        // When
        String url = config.getUrl();

        // Then
        assertThat(url).isEqualTo("http://192.168.1.100:9090");
    }

    @Test
    void defaultConfig_shouldSetCorrectDefaults() {
        // When
        ExecutorNodeConfig config = ExecutorNodeConfig.defaultConfig("localhost", 8080);

        // Then
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getMaxConcurrentTasks()).isEqualTo(10);
        assertThat(config.getTimeoutMs()).isEqualTo(30000);
        assertThat(config.getConnectionPoolSize()).isEqualTo(20);
    }
}
