package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "执行日志查询请求")
public class ExecutionLogQuery {

    @Schema(description = "执行ID")
    private String executionId;

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 20;
}
