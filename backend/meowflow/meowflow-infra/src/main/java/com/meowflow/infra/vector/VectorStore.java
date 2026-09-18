package com.meowflow.infra.vector;

import java.util.List;

/**
 * 向量存储接口
 */
public interface VectorStore {

    /**
     * 插入向量
     */
    void insert(VectorEntry entry);

    /**
     * 批量插入向量
     */
    void insertBatch(List<VectorEntry> entries);

    /**
     * 搜索最近邻向量
     *
     * @param vector 查询向量
     * @param topK 返回数量
     * @param filter 过滤条件（如知识库ID）
     * @return 搜索结果
     */
    List<VectorSearchResult> search(float[] vector, int topK, VectorFilter filter);

    /**
     * 删除向量
     */
    void delete(String collectionName, String vectorKey);

    /**
     * 删除集合
     */
    void deleteCollection(String collectionName);

    /**
     * 检查集合是否存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 创建集合
     */
    void createCollection(String collectionName, int dimension);

    /**
     * 获取集合信息
     */
    CollectionInfo getCollectionInfo(String collectionName);

    /**
     * 向量条目
     */
    record VectorEntry(
            String collectionName,
            String key,
            float[] vector,
            String metadata
    ) {}

    /**
     * 向量搜索结果
     */
    record VectorSearchResult(
            String key,
            float[] vector,
            float score,
            String metadata
    ) {}

    /**
     * 向量过滤条件
     */
    record VectorFilter(
            String knowledgeBaseId,
            String documentId,
            List<String> tags
    ) {}

    /**
     * 集合信息
     */
    record CollectionInfo(
            String name,
            int dimension,
            long count,
            long createdAt
    ) {}
}
