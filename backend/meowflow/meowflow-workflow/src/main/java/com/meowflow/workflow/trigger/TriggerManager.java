package com.meowflow.workflow.trigger;

import com.meowflow.common.redis.RedisService;
import com.meowflow.workflow.config.WorkflowProperties;
import com.meowflow.workflow.service.ExecutionService;
import com.meowflow.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务管理器。
 * <p>
 * 支持两种调度模式：
 * <ul>
 *   <li>CRON 表达式：按 cron 规则重复触发</li>
 *   <li>ONCE：指定时间一次性触发</li>
 * </ul>
 * <p>
 * 调度元数据持久化到 Redis，进程重启后可恢复。
 * 内部委托 {@link TaskScheduler} 实际执行任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriggerManager {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final long LEASE_DURATION_MS = 30_000L; // 30s lease

    private final TaskScheduler taskScheduler;
    private final RedisService redisService;
    private final ApplicationContext applicationContext;
    private final WorkflowProperties workflowProperties;

    /** 当前实例唯一标识（用于分布式租约竞争） */
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    /** key = "wf_{workflowId}_{nodeId}", value = ScheduledFuture */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("TriggerManager 初始化，开始恢复 Redis 中的调度任务...");
        recoverFromRedis();
    }

    @PreDestroy
    public void destroy() {
        log.info("TriggerManager 关闭，取消所有调度任务...");
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        scheduledTasks.clear();
    }

    // ==================== 公共 API ====================

    /**
     * 注册/更新一个 CRON 调度任务。
     */
    public void scheduleCron(Long workflowId, String nodeId, String cronExpression) {
        String key = buildKey(workflowId, nodeId);
        cancelByKey(key);

        ScheduleMetadata meta = ScheduleMetadata.builder()
                .workflowId(workflowId)
                .nodeId(nodeId)
                .type("cron")
                .cronExpression(cronExpression)
                .status("active")
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();

        persistToRedis(key, meta);

        Runnable task = buildWorkflowExecutionTask(workflowId, nodeId);
        ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cronExpression, ZONE));
        scheduledTasks.put(key, future);

        log.info("注册 CRON 调度: workflowId={}, nodeId={}, cron={}", workflowId, nodeId, cronExpression);
    }

    /**
     * 注册一个一次性调度任务。
     */
    public void scheduleOnce(Long workflowId, String nodeId, LocalDateTime triggerTime) {
        if (triggerTime == null) {
            throw new IllegalArgumentException("triggerTime 不能为空");
        }
        ZonedDateTime zdt = triggerTime.atZone(ZONE);
        if (zdt.isBefore(ZonedDateTime.now(ZONE))) {
            throw new IllegalArgumentException("triggerTime 不能在过去: " + triggerTime);
        }

        String key = buildKey(workflowId, nodeId);
        cancelByKey(key);

        ScheduleMetadata meta = ScheduleMetadata.builder()
                .workflowId(workflowId)
                .nodeId(nodeId)
                .type("once")
                .triggerTime(triggerTime)
                .status("active")
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .build();

        persistToRedis(key, meta);

        Runnable task = buildWorkflowExecutionTask(workflowId, nodeId);
        ScheduledFuture<?> future = taskScheduler.schedule(task, zdt.toInstant());
        scheduledTasks.put(key, future);

        log.info("注册一次性调度: workflowId={}, nodeId={}, triggerTime={}", workflowId, nodeId, triggerTime);
    }

    /**
     * 取消指定节点的调度。
     */
    public void cancelScheduledTask(Long workflowId, String nodeId) {
        cancelByKey(buildKey(workflowId, nodeId));
    }

    private void cancelByKey(String key) {
        ScheduledFuture<?> future = scheduledTasks.remove(key);
        if (future != null) {
            future.cancel(false);
            log.debug("取消调度: key={}", key);
        }
        redisService.delete(redisKey(key));
    }

    /**
     * 取消工作流的所有调度。
     */
    public void cancelAllTasks(Long workflowId) {
        scheduledTasks.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith("wf_" + workflowId + "_")) {
                entry.getValue().cancel(false);
                redisService.delete(redisKey(entry.getKey()));
                return true;
            }
            return false;
        });
        log.info("取消工作流所有调度: workflowId={}", workflowId);
    }

    /**
     * 查询调度状态。
     */
    public boolean isScheduled(Long workflowId, String nodeId) {
        return scheduledTasks.containsKey(buildKey(workflowId, nodeId));
    }

    // ==================== 私有方法 ====================

    private String buildKey(Long workflowId, String nodeId) {
        return "wf_" + workflowId + "_" + nodeId;
    }

    private String redisKey(String key) {
        return workflowProperties.getTrigger().getRedisKeyPrefix() + key;
    }

    private void persistToRedis(String key, ScheduleMetadata meta) {
        redisService.set(redisKey(key), meta);
    }

    private void recoverFromRedis() {
        try {
            String prefix = workflowProperties.getTrigger().getRedisKeyPrefix();
            Object raw = redisService.get(prefix + "wf_");
            if (raw == null) {
                log.info("Redis 中无恢复的调度任务");
                return;
            }

            if (raw instanceof java.util.List<?> list) {
                for (Object item : list) {
                    if (item instanceof String k) {
                        recoverOne(prefix + k);
                    }
                }
            } else if (raw instanceof Map) {
                for (Object subKey : ((Map<?, ?>) raw).keySet()) {
                    recoverOne(prefix + subKey.toString());
                }
            } else if (raw instanceof String k) {
                recoverOne(k);
            }
        } catch (Exception e) {
            log.warn("从 Redis 恢复调度任务时出错（不影响启动）: {}", e.getMessage());
        }
    }

    private void recoverOne(String redisKey) {
        try {
            String prefix = workflowProperties.getTrigger().getRedisKeyPrefix();
            String key = redisKey.startsWith(prefix)
                    ? redisKey.substring(prefix.length())
                    : redisKey;

            Object raw = redisService.get(redisKey);
            if (raw == null) return;

            ScheduleMetadata meta = toMetadata(raw);
            if (meta == null || !"active".equals(meta.getStatus())) return;

            if ("cron".equals(meta.getType())) {
                try {
                    Runnable task = buildWorkflowExecutionTask(meta.getWorkflowId(), meta.getNodeId());
                    ScheduledFuture<?> future = taskScheduler.schedule(task,
                            new CronTrigger(meta.getCronExpression(), ZONE));
                    scheduledTasks.put(key, future);
                    log.info("恢复 CRON 调度: workflowId={}, nodeId={}, cron={}",
                            meta.getWorkflowId(), meta.getNodeId(), meta.getCronExpression());
                } catch (Exception e) {
                    log.warn("恢复 CRON 调度失败（跳过）: {}: {}", redisKey, e.getMessage());
                }
            } else if ("once".equals(meta.getType()) && meta.getTriggerTime() != null) {
                ZonedDateTime zdt = meta.getTriggerTime().atZone(ZONE);
                if (zdt.isAfter(ZonedDateTime.now(ZONE))) {
                    Runnable task = buildWorkflowExecutionTask(meta.getWorkflowId(), meta.getNodeId());
                    ScheduledFuture<?> future = taskScheduler.schedule(task, zdt.toInstant());
                    scheduledTasks.put(key, future);
                    log.info("恢复一次性调度: workflowId={}, nodeId={}, triggerTime={}",
                            meta.getWorkflowId(), meta.getNodeId(), meta.getTriggerTime());
                } else {
                    log.debug("一次性调度已过期，删除: {}", redisKey);
                    redisService.delete(redisKey);
                }
            }
        } catch (Exception e) {
            log.warn("恢复单个调度失败（跳过）: {}: {}", redisKey, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ScheduleMetadata toMetadata(Object raw) {
        if (raw instanceof ScheduleMetadata m) return m;
        if (raw instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) raw;
            return ScheduleMetadata.builder()
                    .workflowId(toLong(map.get("workflowId")))
                    .nodeId((String) map.get("nodeId"))
                    .type((String) map.get("type"))
                    .cronExpression((String) map.get("cronExpression"))
                    .triggerTime(toLocalDateTime(map.get("triggerTime")))
                    .status((String) map.get("status"))
                    .createTime(toLong(map.get("createTime")))
                    .updateTime(toLong(map.get("updateTime")))
                    .lastFireTime(toLong(map.get("lastFireTime")))
                    .lastExecutionId(toLong(map.get("lastExecutionId")))
                    .lockOwner((String) map.get("lockOwner"))
                    .lockExpiresAt(toLong(map.get("lockExpiresAt")))
                    .build();
        }
        return null;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime ldt) return ldt;
        if (v instanceof String s) {
            if (s.contains("T")) return LocalDateTime.parse(s);
            return LocalDateTime.parse(s.replace(" ", "T"));
        }
        return null;
    }

    private Runnable buildWorkflowExecutionTask(Long workflowId, String nodeId) {
        return () -> {
            String taskKey = buildKey(workflowId, nodeId);
            try {
                // 1. 尝试获取分布式租约（防止集群重复触发）
                String lockKey = redisKey(taskKey) + ":lease";
                String prev = redisService.getStr(lockKey);
                long now = System.currentTimeMillis();
                long expiresAt = now + LEASE_DURATION_MS;

                if (prev != null) {
                    // 检查旧租约是否已过期
                    try {
                        long prevExpires = Long.parseLong(prev.split(":")[0]);
                        if (prevExpires > now) {
                            log.debug("Task {} skip: lease held by {} until {}",
                                    taskKey, prev, prevExpires);
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                }

                // 尝试原子性获取租约（setIfAbsent 语义）
                redisService.set(lockKey, instanceId + ":" + expiresAt, 35, TimeUnit.SECONDS);

                // 双重检查
                String currentHolder = redisService.getStr(lockKey);
                if (currentHolder == null || !currentHolder.startsWith(instanceId)) {
                    log.debug("Task {} lost lease competition", taskKey);
                    return;
                }

                log.info("调度触发执行（获得租约）: workflowId={}, nodeId={}, instance={}",
                        workflowId, nodeId, instanceId);
                ExecutionService executionService = applicationContext.getBean(ExecutionService.class);
                WorkflowService workflowService = applicationContext.getBean(WorkflowService.class);

                Optional<?> wfOpt = getWorkflow(workflowService, workflowId);
                if (wfOpt.isEmpty()) {
                    log.warn("工作流不存在，跳过调度: workflowId={}", workflowId);
                    return;
                }

                var wf = (com.meowflow.workflow.dto.WorkflowResponse) wfOpt.get();
                if (!"running".equals(wf.getStatus())) {
                    log.warn("工作流未发布，跳过调度: workflowId={}, status={}", workflowId, wf.getStatus());
                    return;
                }

                var request = new com.meowflow.workflow.dto.ExecutionRequest();
                request.setWorkflowId(workflowId);
                request.setTriggerType("cron");
                request.setAsync(true);

                var execResp = executionService.execute(request, 0L);
                Long execId = execResp.getExecutionId();

                // 更新元数据中的触发记录
                updateLastFire(workflowId, nodeId, execId);

                log.info("调度执行触发成功: workflowId={}, executionId={}", workflowId, execId);

            } catch (Exception e) {
                log.error("调度执行失败: workflowId={}, nodeId={}", workflowId, nodeId, e);
            }
        };
    }

    private Optional<?> getWorkflow(WorkflowService svc, Long id) {
        try {
            return Optional.ofNullable(svc.getById(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 更新调度元数据中的触发记录。
     */
    private void updateLastFire(Long workflowId, String nodeId, Long executionId) {
        try {
            String key = buildKey(workflowId, nodeId);
            Object raw = redisService.get(redisKey(key));
            ScheduleMetadata meta = toMetadata(raw);
            if (meta != null) {
                meta.setLastFireTime(System.currentTimeMillis());
                meta.setLastExecutionId(executionId);
                meta.setUpdateTime(System.currentTimeMillis());
                persistToRedis(key, meta);
            }
        } catch (Exception e) {
            log.warn("更新调度元数据失败: workflowId={}, nodeId={}: {}", workflowId, nodeId, e.getMessage());
        }
    }
}
