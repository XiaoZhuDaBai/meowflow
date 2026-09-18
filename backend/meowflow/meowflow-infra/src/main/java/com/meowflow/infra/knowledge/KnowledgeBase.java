package com.meowflow.infra.knowledge;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识库实体
 */
@Data
public class KnowledgeBase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String vectorStoreType;
    private Map<String, Object> config;
    private Integer documentCount;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    public static final String VECTOR_STORE_MILVUS = "milvus";
    public static final String VECTOR_STORE_PGVECTOR = "pgvector";
    public static final String VECTOR_STORE_REDIS = "redis";
    public static final String VECTOR_STORE_ELASTICSEARCH = "elasticsearch";
}
