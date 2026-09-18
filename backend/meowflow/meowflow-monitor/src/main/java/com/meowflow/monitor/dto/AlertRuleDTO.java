package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建告警规则请求")
public class AlertRuleDTO {

    @Schema(description = "规则名称")
    private String name;

    @Schema(description = "规则描述")
    private String description;

    @NotBlank(message = "指标名称不能为空")
    @Schema(description = "指标名称")
    private String metricName;

    @NotBlank(message = "比较操作符不能为空")
    @Schema(description = "比较操作符: gt, lt, gte, lte, eq")
    private String operator;

    @NotNull(message = "阈值不能为空")
    @Schema(description = "阈值")
    private Double threshold;

    @Schema(description = "持续时间(秒)")
    private Integer duration;

    @Schema(description = "告警渠道: dingtalk, email, sms")
    private String alertChannel;

    @Schema(description = "Webhook URL")
    private String webhook;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
