package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "模板审核请求")
public class TemplateReviewRequest {

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "审核动作: approve, reject")
    private String action;

    @Schema(description = "审核意见")
    private String comment;
}
