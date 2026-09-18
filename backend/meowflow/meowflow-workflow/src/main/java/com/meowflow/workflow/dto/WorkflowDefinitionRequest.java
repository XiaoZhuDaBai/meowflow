package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "工作流版本定义请求")
public class WorkflowDefinitionRequest {

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "节点列表")
    private List<NodeDefinitionDto> nodes;

    @Schema(description = "连线列表")
    private List<EdgeDto> edges;

    @Schema(description = "全局变量")
    private Map<String, Object> variables;

    @Schema(description = "输入 Schema")
    private Map<String, Object> inputSchema;

    @Schema(description = "输出 Schema")
    private Map<String, Object> outputSchema;

    @Schema(description = "变更日志")
    private String changelog;

    @Data
    @Schema(description = "节点定义 DTO")
    public static class NodeDefinitionDto {
        private String id;
        private String type;
        private String name;
        private PositionDto position;
        private Map<String, Object> data;
    }

    @Data
    @Schema(description = "位置 DTO")
    public static class PositionDto {
        private double x;
        private double y;
    }

    @Data
    @Schema(description = "连线定义 DTO")
    public static class EdgeDto {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private String type;
        private EdgeDataDto data;
    }

    @Data
    @Schema(description = "连线数据 DTO")
    public static class EdgeDataDto {
        private String label;
        private String type;
        private Map<String, Object> config;
    }
}
