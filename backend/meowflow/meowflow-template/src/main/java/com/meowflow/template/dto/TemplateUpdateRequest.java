package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "更新模板请求")
public class TemplateUpdateRequest {

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "工作流定义 JSON")
    private String definition;

    @Schema(description = "工作流JSON")
    private String workflowJson;

    @Schema(description = "工作流图形")
    private String workflowGraph;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "封面图标")
    private String coverIcon;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "预览图片列表")
    private String previewImages;

    @Schema(description = "标签名")
    private Set<String> tagNames;

    @Schema(description = "标签 ID")
    private Set<Long> tagIds;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "是否公开")
    private String isPublic;

    @Schema(description = "是否推荐")
    private String isFeatured;

    @Schema(description = "备注")
    private String remark;
}
