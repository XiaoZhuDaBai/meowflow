package com.meowflow.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则表 - mf_mon_alert_rule
 * 对应 scripts/sql/01-schema.sql
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_mon_alert_rule")
public class AlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 规则编码 */
    @TableField(exist = false)
    private String code;

    /** 规则描述 */
    private String description;

    /** 告警类型 */
    @TableField(exist = false)
    private String alertType;

    /** 指标名称 */
    @TableField("metric")
    private String metricName;

    /** 条件类型: gt / lt / eq / gte / lte */
    @TableField("condition")
    private String conditionType;

    /** 阈值 */
    @TableField("threshold")
    private BigDecimal thresholdValue;

    /** 时间窗口(秒) */
    @TableField("duration_s")
    private Integer timeWindowSeconds;

    /** 评估间隔(秒) */
    @TableField(exist = false)
    private Integer evaluationInterval;

    /** 严重程度: INFO / WARNING / ERROR / CRITICAL */
    @TableField("severity")

    private String severity;

    /** 是否启用: 0-禁用 1-启用 */
    private Boolean enabled;

    /** 通知渠道 */
    @TableField(value = "channels", typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String notificationChannels;

    /** Webhook URL */
    private String webhook;

    /** 接收人列表 */
    @TableField(exist = false)
    private String recipients;

    /** 冷却时间(秒) */
    @TableField(exist = false)
    private Integer cooldownSeconds;

    /** 是否自动解决: 0-否 1-是 */
    @TableField(exist = false)
    private Integer autoResolve;

    /** 解决条件 */
    @TableField(exist = false)
    private String resolveCondition;

    /** 所有者ID */
    private Long ownerId;

    /** 创建人 */
    @TableField(exist = false)
    private Long createBy;

    /** 更新人 */
    @TableField(exist = false)
    private Long updateBy;

    /** 是否删除: 0-否 1-是 */
    @TableField(exist = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
