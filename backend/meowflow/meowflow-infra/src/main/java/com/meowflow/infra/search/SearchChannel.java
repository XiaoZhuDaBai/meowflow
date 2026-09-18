package com.meowflow.infra.search;

import java.util.List;

/**
 * 检索通道接口
 */
public interface SearchChannel {

    /**
     * 搜索相关文档
     *
     * @param query         查询文本
     * @param knowledgeBaseId 知识库ID
     * @param topK          返回数量
     * @return 检索结果
     */
    List<SearchResult> search(String query, Long knowledgeBaseId, int topK);

    /**
     * 获取通道名称
     */
    String getChannelName();

    /**
     * 获取通道权重
     */
    default double getWeight() {
        return 1.0;
    }

    /**
     * 检索结果
     */
    record SearchResult(
            Long chunkId,
            Long documentId,
            String content,
            double score,
            String metadata
    ) {
    }
}
