package com.meowflow.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("document")
public class DocumentEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /**
     * 文档内容（文本内容或文件路径）
     */
    private String content;

    /**
     * 内容类型：text, pdf, docx, markdown, url
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件路径（MinIO路径）
     */
    private String filePath;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 状态：pending, processing, completed, failed
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

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

