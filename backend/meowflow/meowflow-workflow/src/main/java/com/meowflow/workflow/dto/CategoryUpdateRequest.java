package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新分类请求")
public class CategoryUpdateRequest {

    @Size(max = 64, message = "分类名称长度不能超过 64")
    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private String status;
}
