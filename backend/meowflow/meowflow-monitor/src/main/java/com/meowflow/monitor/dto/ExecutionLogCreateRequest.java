package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建执行日志请求")
public class ExecutionLogCreateRequest {

    @Schema(description = "执行ID")
    private String executionId;

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "输入数据")
    private String inputData;
}
