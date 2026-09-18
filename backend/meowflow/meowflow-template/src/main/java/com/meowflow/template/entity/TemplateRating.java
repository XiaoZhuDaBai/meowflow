package com.meowflow.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板评分，与 V7 迁移创建的 mf_tpl_rating 表对应。
 * <p>id 与 template_id 均使用 BIGINT；userId 仍使用 String 以兼容 Snowflake 生成的 ID。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_tpl_rating")
public class TemplateRating implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    /** 兼容旧雪花 ID；DB 列允许从字符串回填 */
    private Long userId;

    private Integer score;

    private String content;

    private String tags;

    private Integer helpfulCount;

    private Boolean isAnonymous;

    private String status;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;
}

