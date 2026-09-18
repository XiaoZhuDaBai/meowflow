package com.meowflow.workflow.trigger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度任务元数据，持久化到 Redis。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long workflowId;
    private String nodeId;
    /** cron 表达式，或 "once" */
    private String type;
    private String cronExpression;
    /** 仅对 once 类型有效 */
    private LocalDateTime triggerTime;
    /** 调度状态：active / paused / cancelled */
    private String status;
    private Long createTime;
    private Long updateTime;

    /** 上次触发时间（毫秒时间戳） */
    private Long lastFireTime;

    /** 上次触发对应的执行 ID */
    private Long lastExecutionId;

    /** 当前持有分布式租约的节点标识（podId / instanceId） */
    private String lockOwner;

    /** 租约过期时间（毫秒时间戳）；过期后其他节点可抢注 */
    private Long lockExpiresAt;
}
