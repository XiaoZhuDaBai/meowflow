package com.meowflow.common.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行日志创建请求 DTO
 * 供 Feign 调用时使用
 */
@Data
@Schema(description = "执行日志创建请求")
public class ExecutionLogCreateRequest {

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "执行ID")
    private String executionId;

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "工作流版本")
    private Integer version;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型: START/END/LLM/MCP/WEBHOOK/BRANCH/MERGE")
    private String nodeType;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "触发类型: MANUAL/SCHEDULE/WEBHOOK/API")
    private String triggerType;

    @Schema(description = "输入数据 (JSON)")
    private String inputData;

    @Schema(description = "执行节点")
    private String executorNode;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行状态")
    private String status;
}
