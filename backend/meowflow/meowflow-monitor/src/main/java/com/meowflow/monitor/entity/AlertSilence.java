package com.meowflow.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警沉默表 - mf_mon_alert_silence
 * 对应 scripts/sql/00-init.sql 或 Flyway 迁移
 * 如果数据库中没有此表，可以删除此类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_mon_alert_silence")
public class AlertSilence implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 沉默规则名称 */
    private String name;

    /** 匹配类型: RULE / ALERT / TARGET */
    private String matchType;

    /** 告警规则ID */
    private Long alertRuleId;

    /** 告警ID */
    private Long alertId;

    /** 目标匹配模式 */
    private String targetPattern;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 沉默原因 */
    private String reason;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
