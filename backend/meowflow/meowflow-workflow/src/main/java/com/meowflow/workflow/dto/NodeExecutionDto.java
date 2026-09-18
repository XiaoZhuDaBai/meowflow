package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点执行结果")
public class NodeExecutionDto {

    @Schema(description = "节点执行 ID")
    private Long id;

    @Schema(description = "节点 ID")
    private String nodeId;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "输入")
    private Map<String, Object> input;

    @Schema(description = "输出")
    private Map<String, Object> output;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "消耗 token")
    private Integer costToken;

    @Schema(description = "开始时间")
    private String startedAt;

    @Schema(description = "结束时间")
    private String finishedAt;

    @Schema(description = "创建时间")
    private String createTime;
}
