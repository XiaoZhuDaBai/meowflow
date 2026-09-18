package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "创建工作流请求")
public class WorkflowCreateRequest {

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分组 ID")
    private Long groupId;

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 128, message = "工作流名称长度不能超过 128")
    @Schema(description = "工作流名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 128, message = "工作流编码长度不能超过 128")
    @Schema(description = "工作流编码（唯一）")
    private String code;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "初始版本定义")
    private WorkflowDefinitionRequest definition;
}
