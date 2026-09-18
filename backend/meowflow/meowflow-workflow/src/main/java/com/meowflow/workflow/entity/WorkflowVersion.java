package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "mf_wf_workflow_version", autoResultMap = true)
public class WorkflowVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    private String version;

    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String definition;

    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> inputSchema;

    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbMapTypeHandler.class)
    private Map<String, Object> outputSchema;

    private String dslText;

    private String changelog;

    private String publishStatus;

    private LocalDateTime publishedAt;

    private Long publishedBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
