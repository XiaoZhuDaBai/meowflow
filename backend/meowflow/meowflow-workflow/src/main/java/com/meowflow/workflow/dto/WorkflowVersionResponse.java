package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工作流版本响应")
public class WorkflowVersionResponse {

    @Schema(description = "版本 ID")
    private Long id;

    @Schema(description = "工作流 ID")
    private Long workflowId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "发布状态")
    private String publishStatus;

    @Schema(description = "变更日志")
    private String changelog;

    @Schema(description = "发布时间")
    private String publishedAt;

    @Schema(description = "发布人 ID")
    private Long publishedBy;

    @Schema(description = "创建时间")
    private String createTime;
}
