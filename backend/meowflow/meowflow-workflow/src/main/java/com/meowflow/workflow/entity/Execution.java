package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "mf_wf_execution", autoResultMap = true)
public class Execution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private String version;

    private String triggerType;

    private Long triggerUserId;

    private String status;

    @TableField(value = "\"input\"", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> input;

    @TableField(value = "\"output\"", typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> output;

    private String errorMessage;

    private Long costMs;

    private Integer costToken;

    private Double costAmount;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}


