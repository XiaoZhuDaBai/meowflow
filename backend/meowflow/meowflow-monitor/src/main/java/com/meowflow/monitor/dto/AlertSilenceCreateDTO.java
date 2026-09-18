package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建告警沉默请求")
public class AlertSilenceCreateDTO {

    @Schema(description = "规则ID")
    private String ruleId;

    @Schema(description = "告警渠道")
    private String alertChannel;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "沉默原因")
    private String reason;

    @Schema(description = "备注")
    private String remark;
}

