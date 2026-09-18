package com.meowflow.infra.knowledge;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档实体
 */
@Data
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String contentType;
    private Long knowledgeBaseId;
    private Long fileSize;
    private String filePath;
    private String metadata;
    private Integer chunkCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";
}
