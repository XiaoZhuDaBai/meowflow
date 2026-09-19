package com.meowflow.executor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@TableName("mf_exe_task")
public class ExecutorTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private Long executionId;

    private String nodeId;

    private String status;

    private Integer priority;

    /**
     * 对应 mf_exe_task.input（jsonb）。必须显式声明 TypeHandler，
     * 否则 jsonb 列无法绑定 Java Map，写入时报 "is of type jsonb but expression is of type character varying"。
     */
    @TableField(value = "input", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> input;

    /** 对应 mf_exe_task.output（jsonb），同 input。 */
    @TableField(value = "output", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> output;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long costMs;

    private LocalDateTime createTime;
}


