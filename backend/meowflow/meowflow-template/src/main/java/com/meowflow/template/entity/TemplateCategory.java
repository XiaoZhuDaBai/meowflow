package com.meowflow.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 模板分类（实际映射到 mf_wf_category；分类是模板与工作流共享的）。
 * <p>与公共 DDL 对齐：
 * <ul>
 *   <li>id           BIGSERIAL</li>
 *   <li>parent_id    BIGINT  DEFAULT 0</li>
 *   <li>code         VARCHAR(64) NOT NULL UNIQUE</li>
 *   <li>name         VARCHAR(64)</li>
 *   <li>level        INT     DEFAULT 1        (V7 新增)</li>
 *   <li>sort         INT     DEFAULT 0</li>
 *   <li>icon         VARCHAR(255)</li>
 *   <li>description  VARCHAR(255)            (V7 新增)</li>
 *   <li>status       VARCHAR(16) DEFAULT 'active'</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_wf_category")
public class TemplateCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String code;

    private String name;

    private Integer level;

    private Integer sort;

    private String icon;

    private String description;

    private String status;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    @TableField(exist = false)
    private Set<TemplateCategory> children = new HashSet<>();

    @TableField(exist = false)
    private Integer templateCount;
}


