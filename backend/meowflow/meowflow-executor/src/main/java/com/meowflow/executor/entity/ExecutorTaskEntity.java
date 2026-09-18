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

    private Map<String, Object> input;

    private Map<String, Object> output;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long costMs;

    private LocalDateTime createTime;
}


