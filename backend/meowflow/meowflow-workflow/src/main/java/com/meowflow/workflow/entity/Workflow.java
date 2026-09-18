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
