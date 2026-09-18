package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "执行日志")
public class ExecutionLogDto {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "执行 ID")
    private Long executionId;

    @Schema(description = "节点 ID")
    private String nodeId;

    @Schema(description = "日志级别: DEBUG/INFO/WARN/ERROR")
    private String level;

    @Schema(description = "日志消息")
    private String message;

    @Schema(description = "附加数据")
    private Map<String, Object> payload;

    @Schema(description = "创建时间")
    private String createdAt;
}
