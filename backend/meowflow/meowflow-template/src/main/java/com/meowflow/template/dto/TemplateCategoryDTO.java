package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模板分类信息响应")
public class TemplateCategoryDTO {

    @Schema(description = "分类ID")
    private String id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父分类ID")
    private String parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "模板数量")
    private Integer templateCount;
}
