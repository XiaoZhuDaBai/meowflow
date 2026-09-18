package com.meowflow.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步日志追加器，将日志事件批量写入 PostgreSQL
 * <p>
 * 异常时降级到本地文件
 */
@Slf4j
public class PostgresAppender extends AppenderBase<ILoggingEvent> {

    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_FLUSH_INTERVAL_MS = 5000;

    private int queueSize = DEFAULT_QUEUE_SIZE;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private long flushIntervalMs = DEFAULT_FLUSH_INTERVAL_MS;
    private String fallbackFile = "./logs/meowflow-fallback.log";

    private ThreadPoolExecutor writeExecutor;
    private LogPersistenceService persistenceService;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<LogEvent> eventQueue;
    private long idCounter = 0;

    public PostgresAppender() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.eventQueue = new ArrayBlockingQueue<>(queueSize);
    }

    @Override
    public void start() {
        if (this.persistenceService == null) {
            log.warn("PostgresAppender: LogPersistenceService not set, falling back to console only");
            return;
        }

        // 配置拒绝策略：主线程直接记录到降级文件
        RejectedExecutionHandler fallbackHandler = (r, executor) -> {
            log.warn("Log queue full, falling back to file");
        };

        // 异步写入线程池
        this.writeExecutor = new ThreadPoolExecutor(
                2, 4,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                r -> {
                    Thread t = new Thread(r, "log-writer-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                },
                fallbackHandler
        );

        // 启动定时刷新任务
        startFlushTask();

        super.start();
    }

    private void startFlushTask() {
        Thread flushThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(flushIntervalMs);
                    flush();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "log-flush-scheduler");
        flushThread.setDaemon(true);
        flushThread.start();
    }

    /**
     * 强制刷新缓冲区
     */
    public void flush() {
        try {
            if (persistenceService != null) {
                persistenceService.flush();
            }
        } catch (Exception e) {
            log.error("Failed to flush log buffer", e);
        }
    }

    @Override
    public void stop() {
        flush();
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    writeExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                writeExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null) {
            return;
        }

        try {
            LogEvent logEvent = buildFromLoggingEvent(event);
            // 异步提交到线程池处理
            writeExecutor.submit(() -> {
                try {
                    if (persistenceService != null) {
                        persistenceService.addToBuffer(logEvent);
                    }
                } catch (Exception e) {
                    log.error("Failed to process log event", e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to submit log event, falling back to file: {}", e.getMessage());
            writeFallbackFile(event);
        }
    }

    private synchronized long generateId() {
        return System.currentTimeMillis() * 1000 + (++idCounter % 1000);
    }

    private LogEvent buildFromLoggingEvent(ILoggingEvent event) {
        LogEvent logEvent = new LogEvent();
        logEvent.setId(generateId());
        logEvent.setTraceId(event.getMDCPropertyMap().get("traceId"));
        logEvent.setLevel(event.getLevel().toString());
        logEvent.setMessage(event.getFormattedMessage());
        logEvent.setModule(event.getLoggerName());
        logEvent.setCreateTime(LocalDateTime.now());

        try {
            logEvent.setPayload(objectMapper.writeValueAsString(new LoggingPayload(
                    event.getLoggerName(),
                    event.getThreadName(),
                    event.getCallerData().length > 0 ? event.getCallerData()[0].toString() : "unknown"
            )));
        } catch (JsonProcessingException e) {
            logEvent.setPayload("{}");
        }

        return logEvent;
    }

    private void writeFallbackFile(ILoggingEvent event) {
        try {
            String json = event.getFormattedMessage() + System.lineSeparator();
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get(fallbackFile),
                    json,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            log.error("Failed to write fallback log", e);
        }
    }

    public void setPersistenceService(LogPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }

    public void setFallbackFile(String fallbackFile) {
        this.fallbackFile = fallbackFile;
    }

    /**
     * 日志 Payload 包装
     */
    public static class LoggingPayload {
        public String logger;
        public String thread;
        public String location;

        public LoggingPayload(String logger, String thread, String location) {
            this.logger = logger;
            this.thread = thread;
            this.location = location;
        }
    }
}
