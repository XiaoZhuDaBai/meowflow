package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "执行工作流请求")
public class ExecutionRequest {

    @NotNull(message = "工作流 ID 不能为空")
    @Schema(description = "工作流 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long workflowId;

    @Schema(description = "版本号（不传则使用当前版本）")
    private String version;

    @Schema(description = "触发类型")
    private String triggerType = "manual";

    @Schema(description = "输入参数")
    private Map<String, Object> input;

    /** 是否以调试模式启动执行。 */
    private Boolean debug;

    /** 启动时预置的断点节点 ID。 */
    private List<String> breakpointNodeIds;

    @Schema(description = "是否异步执行")
    private Boolean async = false;
}

