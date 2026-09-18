package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "评分统计")
public class RatingStatistics {

    @Schema(description = "平均分")
    private Double averageScore;

    @Schema(description = "总评分数")
    private Long totalCount;

    @Schema(description = "各分数段数量")
    private Map<Integer, Long> scoreDistribution;

    @Schema(description = "热门标签")
    private List<String> topTags;
}
