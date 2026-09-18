package com.meowflow.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 模板实体（与公共 DDL mf_tpl_template 对齐）。
 *
 * <p>字段分组:
 * <ul>
 *   <li>身份: id (BIGSERIAL)</li>
 *   <li>内容: name / description / definition / workflowJson / workflowGraph</li>
 *   <li>展示: categoryId / icon / coverImage / coverIcon / previewImages</li>
 *   <li>属性: industry / scene / tags(JSONB 透传) / tagIds(BIGINT 解析) / useCount / score / reviewCount</li>
 *   <li>状态: status / reviewStatus / reviewBy / reviewTime / reviewRemark</li>
 *   <li>归属: author / authorId / price / version(业务版本) / templateVersion(修订计数) / isPublic / isFeatured</li>
 *   <li>审计: createBy / createTime / updateBy / updateTime / remark</li>
 *   <li>非持久化: tagList / category</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "mf_tpl_template", autoResultMap = true)
public class Template implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /**
     * 工作流定义 JSON（节点+连线），与 mf_tpl_template.definition 是同一含义的不同字段名
     * （definition 字段面向工作流引擎，workflowJson 面向模板用户/编辑器）。
     */
    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String definition;

    /**
     * 供模板市场使用的轻量 workflowJson；
     * 解析逻辑应能同时处理 definition 与 workflowJson，存储侧既可以走 JSONB 也可走 TEXT。
     */
    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String workflowJson;

    /**
     * 轻量归一化 workflow graph，用于 SVG 预览。
     */
    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String workflowGraph;

    private Long categoryId;

    /** 主图标（emoji 或 URL） */
    private String icon;

    private String coverImage;

    private String coverIcon;

    private String previewImages;

    private String industry;

    private String scene;

    /** tags JSONB 字符串（与关联表 mf_tpl_template_tag 二选一） */
    @TableField(typeHandler = com.meowflow.common.mybatis.JsonbStringTypeHandler.class)
    private String tags;

    private Long useCount;

    /** 0.00 - 5.00 */
    private Double score;

    private Long reviewCount;

    private String status;

    private String reviewStatus;

    private String reviewRemark;

    private String reviewComment;

    private Long reviewBy;

    private LocalDateTime reviewTime;

    private String author;

    private Long authorId;

    private Double price;

    /** 业务版本（v1 / v2 …） */
    private String version;

    /** 修订计数（与 version 字段并存；后者随更新递增） */
    private Integer templateVersion;

    private String isPublic;

    private String isFeatured;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    /** 非持久化：标签名集合（详情/搜索响应填充） */
    @TableField(exist = false)
    private Set<String> tagIds = new HashSet<>();

    /** 非持久化：标签对象集合 */
    @TableField(exist = false)
    private Set<TemplateTag> tagList = new HashSet<>();

    /** 非持久化：所属分类对象 */
    @TableField(exist = false)
    private TemplateCategory category;
}


