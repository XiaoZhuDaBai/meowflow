package com.meowflow.workflow.definition;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.HashMap;
import java.util.Map;

/**
 * 边的类型枚举
 */
public enum EdgeType {
    
    /**
     * 普通边（默认）
     */
    DEFAULT("default", "普通边"),
    
    /**
     * 前端通用条件边（带表达式或按标签路由）
     */
    CONDITION("condition", "条件"),

    /**
     * 条件真分支
     */
    CONDITION_TRUE("condition-true", "条件为真"),
    
    /**
     * 条件假分支
     */
    CONDITION_FALSE("condition-false", "条件为假"),
    
    /**
     * 循环边
     */
    LOOP("loop", "循环"),
    
    /**
     * 并行分支
     */
    PARALLEL("parallel", "并行分支"),
    
    /**
     * 错误边
     */
    ERROR("error", "错误");

    private final String code;
    private final String description;

    private static final Map<String, EdgeType> ALIASES = buildAliases();

    EdgeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static EdgeType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }
        return ALIASES.getOrDefault(code.trim().toLowerCase(), DEFAULT);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EdgeType fromJson(String code) {
        return fromCode(code);
    }

    private static Map<String, EdgeType> buildAliases() {
        Map<String, EdgeType> aliases = new HashMap<>();
        for (EdgeType type : values()) {
            aliases.put(type.code, type);
            aliases.put(type.name().toLowerCase(), type);
        }
        aliases.put("condition-true", CONDITION_TRUE);
        aliases.put("condition-false", CONDITION_FALSE);
        return aliases;
    }
}
