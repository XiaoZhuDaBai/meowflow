package com.meowflow.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日聚合指标表 - mf_mon_metric_daily
 * 对应 scripts/sql/01-schema.sql
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_mon_metric_daily")
public class MetricDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 统计日期 */
    private LocalDate metricDate;

    /** 工作流总数 */
    private Long wfTotal;

    /** 活跃工作流数 */
    private Long wfActive;

    /** 执行总次数 */
    private Long execTotal;

    /** 成功次数 */
    private Long execSuccess;

    /** 失败次数 */
    private Long execFailed;

    /** 平均耗时(ms) */
    private Integer execAvgMs;

    /** Token 总数 */
    private Long tokenTotal;

    /** 费用总额 */
    private BigDecimal costAmount;

    /** 用户总数 */
    private Long userTotal;

    /** 活跃用户数 */
    private Long userActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
