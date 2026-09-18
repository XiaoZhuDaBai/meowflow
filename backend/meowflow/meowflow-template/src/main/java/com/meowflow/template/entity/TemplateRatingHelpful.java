package com.meowflow.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板评分"有帮助"记录，对应 mf_tpl_rating_helpful。
 * <p>ratingId 使用 Long 与 mf_tpl_rating.id 对齐。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_tpl_rating_helpful")
public class TemplateRatingHelpful implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ratingId;

    private Long userId;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;
}

