package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "模板搜索请求")
public class TemplateSearchRequest {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "标签ID列表")
    private List<String> tagIds;

    @Schema(description = "排序字段: useCount, rating, createTime")
    private String sortBy;

    @Schema(description = "排序方向: asc, desc")
    private String sortOrder;

    @Schema(description = "审核状态")
    private String reviewStatus;

    @Schema(description = "是否公开")
    private String isPublic;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;
}
