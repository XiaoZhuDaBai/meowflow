package com.meowflow.common.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 执行日志响应 DTO
 * 供 Feign 调用返回时使用
 */
@Data
@Schema(description = "执行日志响应")
public class ExecutionLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "追踪ID")
    private String traceId;

    @Schema(description = "执行ID")
    private Long executionId;

    @Schema(description = "工作流ID")
    private String workflowId;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "版本")
    private Integer version;

    @Schema(description = "节点ID")
    private String nodeId;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "耗时 (毫秒)")
    private Long durationMs;

    @Schema(description = "状态: RUNNING/SUCCESS/FAILED/CANCELLED/TIMEOUT")
    private String status;

    @Schema(description = "输入数据")
    private String inputData;

    @Schema(description = "输出数据")
    private String outputData;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "执行节点")
    private String executorNode;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
