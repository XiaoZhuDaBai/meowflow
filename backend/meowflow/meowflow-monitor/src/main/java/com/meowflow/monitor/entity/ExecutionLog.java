package com.meowflow.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流执行日志表 - mf_wf_execution_log
 * 对应 scripts/sql/01-schema.sql
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "mf_wf_execution_log", autoResultMap = true)
public class ExecutionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链路追踪ID */
    @TableField(exist = false)
    private String traceId;

    /** 执行记录ID */
    private Long executionId;

    /** 工作流ID */
    @TableField(exist = false)
    private String workflowId;

    /** 工作流名称 */
    @TableField(exist = false)
    private String workflowName;

    /** 版本号 */
    @TableField(exist = false)
    private Integer version;

    /** 节点ID */
    private String nodeId;

    /** 节点名称 */
    @TableField(exist = false)
    private String nodeName;

    /** 节点类型 */
    @TableField(exist = false)
    private String nodeType;

    /** 用户ID */
    @TableField(exist = false)
    private Long userId;

    /** 用户名称 */
    @TableField(exist = false)
    private String userName;

    /** 触发类型 */
    @TableField(exist = false)
    private String triggerType;

    /** 开始时间 */
    @TableField(exist = false)
    private LocalDateTime startTime;

    /** 结束时间 */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /** 执行时长(毫秒) */
    @TableField(exist = false)
    private Long durationMs;

    /** 状态 */
    @TableField(exist = false)
    private String status;

    /** 输入数据 */
    @TableField(exist = false)
    private String inputData;

    /** 输出数据 */
    @TableField(exist = false)
    private String outputData;

    /** 错误消息 */
    @TableField(exist = false)
    private String errorMessage;

    /** 错误堆栈 */
    @TableField(exist = false)
    private String errorStack;

    /** 执行节点 */
    @TableField(exist = false)
    private String executorNode;

    /** 日志级别: debug/info/warn/error */
    private String level;

    /** 日志消息 */
    private String message;

    /** 额外数据 JSON */
    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String payload;

    @TableField(exist = false)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}




