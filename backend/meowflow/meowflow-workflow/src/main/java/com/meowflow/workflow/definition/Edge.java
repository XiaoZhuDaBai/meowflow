package com.meowflow.workflow.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Edge {

    private String id;

    private String source;

    private String target;

    private String sourceHandle;

    private String targetHandle;

    private EdgeType type;

    private EdgeData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeData {
        private String label;
        private ConditionConfig condition;
        /** 前端连线配置（条件表达式、循环条件、错误状态等） */
        private Map<String, Object> config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionConfig {
        private String expression;
        private String operator;
        private Object value;
    }
}
