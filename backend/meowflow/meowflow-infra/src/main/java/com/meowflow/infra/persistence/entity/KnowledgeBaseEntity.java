package com.meowflow.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("knowledge_base")
public class KnowledgeBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /**
     * 向量存储类型：milvus, pgvector, redis, elasticsearch
     */
    private String vectorStoreType;

    /**
     * 向量维度
     */
    private Integer dimension;

    /**
     * 状态：active, inactive
     */
    private String status;

    /**
     * 扩展配置 JSON
     */
    private String config;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}

