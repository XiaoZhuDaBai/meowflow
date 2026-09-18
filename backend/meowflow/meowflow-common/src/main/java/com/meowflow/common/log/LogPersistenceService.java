package com.meowflow.common.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.util.IdGeneratorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 日志持久化服务
 * <p>
 * 支持单条保存和批量保存，自动设置 id (Snowflake)、traceId、createTime
 */
@Slf4j
@Service
public class LogPersistenceService {

    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 5000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final BlockingQueue<LogEvent> buffer = new LinkedBlockingQueue<>(50000);
    private volatile boolean running = true;

    private static final String INSERT_SQL = """
            INSERT INTO mf_log_event
            (id, trace_id, module, level, type, message, payload,
             user_id, user_name, ip, duration, status, error_message, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_SQL_POSTGRES = """
            INSERT INTO mf_log_event
            (id, trace_id, module, level, type, message, payload,
             user_id, user_name, ip, duration, status, error_message, create_time)
            VALUES (nextval('mf_log_event_id_seq'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public LogPersistenceService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();

        // 启动批量写入调度器
        startFlushScheduler();
    }

    private void startFlushScheduler() {
        Thread scheduler = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(FLUSH_INTERVAL_MS);
                    flush();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "log-flush-scheduler");
        scheduler.setDaemon(true);
        scheduler.start();
    }

    /**
     * 保存单条日志事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(LogEvent event) {
        if (event == null) {
            return;
        }
        fillDefaults(event);
        executeInsert(List.of(event));
    }

    /**
     * 批量保存日志事件
     */
    public void saveBatch(List<LogEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (LogEvent event : events) {
            fillDefaults(event);
        }
        executeInsert(events);
    }

    /**
     * 添加到缓冲区，等待批量写入
     */
    public void addToBuffer(LogEvent event) {
        if (event == null) {
            return;
        }
        fillDefaults(event);
        buffer.offer(event);
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    /**
     * 强制刷新缓冲区
     */
    public void flush() {
        List<LogEvent> batch = new ArrayList<>(BATCH_SIZE);
        buffer.drainTo(batch, BATCH_SIZE);
        if (!batch.isEmpty()) {
            try {
                executeInsert(batch);
            } catch (Exception e) {
                log.error("Failed to flush {} log events", batch.size(), e);
                // 降级：写回缓冲区，下次重试
                batch.forEach(buffer::offer);
            }
        }
    }

    private void fillDefaults(LogEvent event) {
        if (event.getId() == null) {
            event.setId(generateId());
        }
        if (event.getTraceId() == null) {
            event.setTraceId(TraceContextHolder.getTraceId());
        }
        if (event.getCreateTime() == null) {
            event.setCreateTime(LocalDateTime.now());
        }
        // 自动填充用户信息
        try {
            if (event.getUserId() == null) {
                Long userId = UserContextHolder.getUserId();
                if (userId != null) {
                    event.setUserId(userId);
                }
            }
            if (event.getUserName() == null) {
                event.setUserName(UserContextHolder.getUsername());
            }
        } catch (Exception e) {
            // 上下文可能为空，忽略
        }
    }

    private static long idCounter = 0;

    private static synchronized long generateId() {
        return System.currentTimeMillis() * 1000 + (++idCounter % 1000);
    }

    private void executeInsert(List<LogEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_SQL, events, events.size(),
            (PreparedStatement ps, LogEvent event) -> {
                ps.setLong(1, event.getId());
                ps.setString(2, event.getTraceId());
                ps.setString(3, event.getModule());
                ps.setString(4, event.getLevel());
                ps.setString(5, event.getType());
                ps.setString(6, event.getMessage());
                ps.setString(7, event.getPayload());
                ps.setObject(8, event.getUserId());
                ps.setString(9, event.getUserName());
                ps.setString(10, event.getIp());
                ps.setObject(11, event.getDuration());
                ps.setObject(12, event.getStatus());
                ps.setString(13, event.getErrorMessage());
                ps.setTimestamp(14, Timestamp.valueOf(event.getCreateTime()));
            });

        log.debug("Persisted {} log events", events.size());
    }

    public void shutdown() {
        running = false;
        flush();
    }
}
