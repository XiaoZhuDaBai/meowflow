package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建分类请求")
public class CategoryCreateRequest {

    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码长度不能超过 64")
    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过 64")
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;
}
