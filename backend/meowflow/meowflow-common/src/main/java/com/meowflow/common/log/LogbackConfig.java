package com.meowflow.common.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.composite.CompositeJsonFormatter;
import net.logstash.logback.encoder.LogstashEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Logback 配置
 * <p>
 * 配置 JSON 格式输出和 PostgreSQL 异步追加器
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "meowflow.log.async-enabled", havingValue = "true", matchIfMissing = false)
public class LogbackConfig {

    /**
     * 创建 JSON 格式的追加器
     */
    @Bean
    public PostgresAppender postgresAppender(LogPersistenceService persistenceService) {
        PostgresAppender appender = new PostgresAppender();
        appender.setPersistenceService(persistenceService);
        appender.setQueueSize(10000);
        appender.setBatchSize(100);
        appender.setFlushIntervalMs(5000);
        appender.setFallbackFile("./logs/meowflow-fallback.log");
        appender.setContext(getLoggerContext());
        appender.setName("postgresAppender");

        appender.start();
        return appender;
    }

    /**
     * JSON 布局
     */
    @Bean
    public PatternLayout jsonLayout() {
        PatternLayout layout = new PatternLayout();
        layout.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n");
        layout.setContext(getLoggerContext());
        layout.start();
        return layout;
    }

    /**
     * Logstash JSON 编码器 (可选，用于结构化日志)
     */
    @Bean
    public LogstashEncoder logstashEncoder() {
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setCustomFields("{\"application\":\"meowflow\"}");
        return encoder;
    }

    private LoggerContext getLoggerContext() {
        return (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
    }
}
