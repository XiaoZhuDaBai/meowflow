package com.meowflow.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板标签（mf_tpl_tag，DDL V7 新建）。
 * <p>字段说明：
 * <ul>
 *   <li>id            BIGSERIAL</li>
 *   <li>name          VARCHAR(64) UNIQUE</li>
 *   <li>color         VARCHAR(16)</li>
 *   <li>sort          INT DEFAULT 0</li>
 *   <li>usage_count   BIGINT DEFAULT 0  —— 维护通过 mf_tpl_template_tag 计算</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_tpl_tag")
public class TemplateTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String color;

    private Integer sort;

    private Long usageCount;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;
}



