package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建分类请求")
public class CategoryCreateRequest {

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
