package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "告警沉默响应")
public class AlertSilenceDTO {

    @Schema(description = "沉默ID")
    private String id;

    @Schema(description = "规则ID")
    private String ruleId;

    @Schema(description = "告警渠道")
    private String alertChannel;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "沉默原因")
    private String reason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
