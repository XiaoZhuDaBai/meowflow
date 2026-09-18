package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "执行日志响应")
public class ExecutionLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "执行ID")
    private Long executionId;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "日志消息")
    private String message;

    @Schema(description = "额外数据")
    private String payload;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "输出数据")
    private String outputData;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
