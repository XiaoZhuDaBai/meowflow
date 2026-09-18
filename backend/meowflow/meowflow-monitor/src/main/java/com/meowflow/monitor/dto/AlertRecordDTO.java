package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "告警记录响应")
public class AlertRecordDTO {

    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "指标类型")
    private String metricType;

    @Schema(description = "指标名称")
    private String metricName;

    @Schema(description = "当前值")
    private Double currentValue;

    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "比较操作符")
    private String operator;

    @Schema(description = "严重程度")
    private String severity;

    @Schema(description = "状态: FIRING, RESOLVED, SILENCED")
    private String status;

    @Schema(description = "通知渠道")
    private String alertChannel;

    @Schema(description = "告警消息")
    private String alertMessage;

    @Schema(description = "通知状态")
    private String notifyStatus;

    @Schema(description = "通知次数")
    private Integer notifyCount;

    @Schema(description = "最近通知时间")
    private LocalDateTime notifyTime;

    @Schema(description = "触发时间")
    private LocalDateTime firedAt;

    @Schema(description = "解决时间")
    private LocalDateTime resolveTime;

    @Schema(description = "解决说明")
    private String resolveComment;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}