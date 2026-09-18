package com.meowflow.executor.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutorConfig 单元测试
 */
class ExecutorConfigTest {

    @Test
    void openAPI_shouldReturnOpenAPI() {
        // Given
        ExecutorConfig config = new ExecutorConfig();

        // When
        var openAPI = config.openAPI();

        // Then
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("MeowFlow Executor API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }
}
