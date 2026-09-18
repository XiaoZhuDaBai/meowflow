package com.meowflow.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录表 - mf_mon_alert
 * 对应 scripts/sql/01-schema.sql
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_mon_alert")
public class AlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的告警规则ID */
    @TableField("rule_id")
    private Long alertRuleId;

    /** 告警编码 */
    @TableField(exist = false)
    private String alertCode;

    /** 告警类型 */
    @TableField(exist = false)
    private String alertType;

    /** 规则名称（列表查询时由 rule 联表填充） */
    @TableField(exist = false)
    private String ruleName;

    /** 告警标题 */
    @TableField(exist = false)
    private String title;

    /** 告警消息 */
    @TableField(exist = false)
    private String message;

    /** 严重程度: INFO / WARNING / ERROR / CRITICAL */
    @TableField(exist = false)
    private String severity;

    /** 状态: FIRING / ACKNOWLEDGED / RESOLVED / SILENCED */
    private String status;

    /** 触发值 */
    @TableField(exist = false)
    private String triggerValue;

    /** 阈值 */
    @TableField(exist = false)
    private BigDecimal thresholdValue;

    /** 目标类型 */
    @TableField(exist = false)
    private String targetType;

    /** 目标ID */
    @TableField(exist = false)
    private String targetId;

    /** 目标名称 */
    @TableField(exist = false)
    private String targetName;

    /** 用户ID */
    @TableField(exist = false)
    private Long userId;

    /** 用户名 */
    @TableField(exist = false)
    private String userName;

    /** 触发时间 */
    @TableField("triggered_at")
    private LocalDateTime firedAt;

    private String metric;

    private BigDecimal value;

    /** 确认时间 */
    @TableField(exist = false)
    private LocalDateTime acknowledgedAt;

    /** 确认人 */
    @TableField(exist = false)
    private String acknowledgedBy;

    /** 解决时间 */
    private LocalDateTime resolvedAt;

    /** 解决人 */
    @TableField("resolved_by")
    private String resolvedBy;

    /** 解决备注 */
    @TableField("resolution_note")
    private String resolutionNote;

    /** 沉默至 */
    @TableField(exist = false)
    private LocalDateTime silencedUntil;

    /** 沉默人 */
    @TableField(exist = false)
    private String silencedBy;

    /** 是否已发送通知: 0-否 1-是 */
    @TableField(exist = false)
    private Integer notificationSent;

    /** 通知时间 */
    @TableField(exist = false)
    private LocalDateTime notificationTime;

    /** 通知日志 JSON */
    @TableField(value = "notify_log", typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String notifyLog;

    /** 比较操作符（列表查询时由 rule 联表填充） */
    @TableField(exist = false)
    private String operator;

    /** 通知渠道（列表查询时由 rule 联表填充） */
    @TableField(exist = false)
    private String alertChannel;

    /** 元数据 JSON */
    @TableField(exist = false)
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private LocalDateTime updateTime;
}
