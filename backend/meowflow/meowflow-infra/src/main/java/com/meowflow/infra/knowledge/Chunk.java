package com.meowflow.infra.knowledge;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档分块实体
 */
@Data
public class Chunk implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long documentId;
    private String content;
    private Integer chunkIndex;
    private Float[] vector;
    private String vectorKey;
    private Integer tokenCount;
    private String metadata;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
