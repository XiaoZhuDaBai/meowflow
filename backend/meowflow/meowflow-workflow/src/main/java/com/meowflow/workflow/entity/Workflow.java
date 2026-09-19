package com.meowflow.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_wf_workflow")
public class Workflow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private Long groupId;

    private String name;

    private String code;

    private String description;

    private String icon;

    private String status;

    private String currentVersion;

    private Long ownerId;

    private Long orgId;

    private Boolean isPublic;

    /**
     * 标签，对应 mf_wf_workflow.tags（jsonb）。
     * 必须显式声明 TypeHandler：jsonb 列无法直接绑定 Java List，
     * 否则创建/更新工作流会报 "column tags is of type jsonb but expression is of type character varying"（前端表现为 500）。
     */
    @TableField(value = "tags", typeHandler = com.meowflow.common.mybatis.JsonbListTypeHandler.class)
    private List<String> tags;

    private Long statTotalRun;

    private LocalDateTime statLastRunAt;

    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
