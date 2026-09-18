package com.meowflow.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档分块实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("document_chunk")
public class ChunkEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块索引（从0开始）
     */
    private Integer chunkIndex;

    /**
     * Token 数量
     */
    private Integer tokenCount;

    /**
     * 向量 Key（用于向量库关联）
     */
    private String vectorKey;

    /**
     * 元数据 JSON
     */
    private String metadata;

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

