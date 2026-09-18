package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模板评价请求")
public class TemplateRatingRequest {

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "评分 1-5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String comment;
}
