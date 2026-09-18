package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "mf_wf_node_execution", autoResultMap = true)
public class NodeExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private String nodeId;

    private String nodeType;

    private String nodeName;

    private String status;

    @TableField(value = "\"input\"", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> input;

    @TableField(value = "\"output\"", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> output;

    private String errorMessage;

    private Integer retryCount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long costMs;

    private Integer costToken;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}


