package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "执行结果响应")
public class ExecutionResponse {

    @Schema(description = "执行 ID")
    private Long executionId;

    @Schema(description = "工作流 ID")
    private Long workflowId;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "输入")
    private Map<String, Object> input;

    @Schema(description = "输出")
    private Map<String, Object> output;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "Token 消耗")
    private Integer costToken;

    @Schema(description = "费用")
    private Double costAmount;

    @Schema(description = "开始时间")
    private String startedAt;

    @Schema(description = "结束时间")
    private String finishedAt;

    @Schema(description = "节点执行列表")
    private List<NodeExecutionDto> nodes;
}
