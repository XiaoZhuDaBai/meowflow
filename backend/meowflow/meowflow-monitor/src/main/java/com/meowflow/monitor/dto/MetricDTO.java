package com.meowflow.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "指标响应")
public class MetricDTO {

    @Schema(description = "指标ID")
    private String id;

    @Schema(description = "日期")
    private LocalDate metricDate;

    @Schema(description = "指标类型")
    private String metricType;

    @Schema(description = "指标名称")
    private String metricName;

    @Schema(description = "指标键")
    private String metricKey;

    @Schema(description = "值")
    private Double value;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "总数")
    private Long count;

    @Schema(description = "总和")
    private Double sum;

    @Schema(description = "平均值")
    private Double avg;

    @Schema(description = "最小值")
    private Double min;

    @Schema(description = "最大值")
    private Double max;

    @Schema(description = "P50")
    private Double p50;

    @Schema(description = "P90")
    private Double p90;

    @Schema(description = "P99")
    private Double p99;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}


