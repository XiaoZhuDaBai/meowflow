package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "指标查询请求")
public class MetricsQuery {

    @Schema(description = "指标类型")
    private String metricType;

    @Schema(description = "指标名称")
    private String metricName;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 100;
}
