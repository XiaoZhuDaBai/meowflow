package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "评分请求")
public class RatingRequest {

    @Schema(description = "评分 1-5")
    @Min(1)
    @Max(5)
    private Integer score;

    @Schema(description = "评论内容")
    @Size(max = 1000)
    private String content;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "是否匿名")
    private Boolean isAnonymous;
}
