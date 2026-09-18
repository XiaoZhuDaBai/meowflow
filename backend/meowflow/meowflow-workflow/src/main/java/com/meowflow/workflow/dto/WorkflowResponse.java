package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工作流响应")
public class WorkflowResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分组 ID")
    private Long groupId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "当前版本")
    private String currentVersion;

    @Schema(description = "所有者 ID")
    private Long ownerId;

    @Schema(description = "组织 ID")
    private Long orgId;

    @Schema(description = "是否公开")
    private Boolean isPublic;

    @Schema(description = "标签")
    private List<String> tags;

    @Schema(description = "总运行次数")
    private Long statTotalRun;

    @Schema(description = "最后运行时间")
    private LocalDateTime statLastRunAt;

    @Schema(description = "成功次数")
    private Long statSuccessCount;

    @Schema(description = "失败次数")
    private Long statFailCount;

    @Schema(description = "平均耗时（毫秒）")
    private Double statAvgDurationMs;

    @Schema(description = "累计费用")
    private Double statTotalCost;

    @Schema(description = "今日运行次数")
    private Long statTodayRunCount;

    @Schema(description = "今日费用")
    private Double statTodayCost;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "当前版本定义（含节点和边）")
    private WorkflowDefinitionRequest definition;
}
