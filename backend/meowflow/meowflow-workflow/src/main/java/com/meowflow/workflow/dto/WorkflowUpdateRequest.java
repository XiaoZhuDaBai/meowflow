package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "更新工作流请求")
public class WorkflowUpdateRequest {

    @Size(max = 128, message = "工作流名称长度不能超过 128")
    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分组 ID")
    private Long groupId;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "是否公开")
    private Boolean isPublic;
}
