package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "模板评分DTO")
public class TemplateRatingDTO {

    @Schema(description = "评分ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "评分 1-5")
    private Integer score;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "有帮助次数")
    private Integer helpfulCount;

    @Schema(description = "是否匿名")
    private Boolean isAnonymous;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
