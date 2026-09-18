package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模板信息响应")
public class TemplateDTO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    /** 工作流引擎侧的 JSON（对应 mf_tpl_template.definition） */
    @Schema(description = "工作流定义 JSON")
    private String definition;

    @Schema(description = "工作流JSON")
    private String workflowJson;

    @Schema(description = "工作流图形")
    private String workflowGraph;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "主图标")
    private String icon;

    @Schema(description = "封面图")
    private String coverImage;

    /**
     * 模板封面图标（FontAwesome 类名或 emoji），在没有 coverImage 时作为 fallback 展示。
     */
    @Schema(description = "封面图标（FontAwesome 类名或 emoji）")
    private String coverIcon;

    @Schema(description = "预览图列表")
    private String previewImages;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "标签名列表")
    private Set<String> tagNames;

    @Schema(description = "标签ID列表")
    private Set<Long> tagIds;

    @Schema(description = "使用次数")
    private Long useCount;

    /** 0.00 - 5.00 */
    @Schema(description = "评分")
    private Double score;

    @Schema(description = "评价数量")
    private Integer reviewCount;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "当前用户是否点赞")
    private Boolean isLiked;

    @Schema(description = "当前用户是否收藏")
    private Boolean isFavorited;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "审核状态")
    private String reviewStatus;

    @Schema(description = "审核意见")
    private String reviewRemark;

    @Schema(description = "审核人ID")
    private Long reviewBy;

    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "业务版本")
    private String version;

    @Schema(description = "修订计数")
    private Integer templateVersion;

    @Schema(description = "创建者ID")
    private String createBy;

    @Schema(description = "创建者名称")
    private String createByName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否公开")
    private String isPublic;

    @Schema(description = "是否推荐")
    private String isFeatured;

    @Schema(description = "备注")
    private String remark;
}
