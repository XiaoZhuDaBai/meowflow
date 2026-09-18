package com.meowflow.infra.embedding;

import java.util.List;

/**
 * Embedding 客户端接口
 */
public interface EmbeddingClient {

    /**
     * 生成单个文本的向量
     *
     * @param text 文本内容
     * @return 向量结果
     */
    EmbeddingResult embed(String text);

    /**
     * 批量生成文本向量
     *
     * @param texts 文本列表
     * @return 向量结果列表
     */
    List<EmbeddingResult> embedBatch(List<String> texts);

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 获取向量维度
     */
    int getDimension();

    /**
     * Embedding 结果
     */
    record EmbeddingResult(
            String text,
            float[] vector,
            int tokenCount,
            long latencyMs
    ) {
    }
}
