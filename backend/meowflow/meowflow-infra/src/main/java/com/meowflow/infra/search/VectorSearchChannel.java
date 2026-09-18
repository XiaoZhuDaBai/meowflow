package com.meowflow.infra.search;

import com.meowflow.infra.embedding.EmbeddingClient;
import com.meowflow.infra.vector.PGVectorStore;
import com.meowflow.infra.vector.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量检索通道
 */
@Slf4j
@Component
public class VectorSearchChannel implements SearchChannel {

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    /**
     * 仅供测试使用的默认构造。生产环境由 Spring 通过 {@link #VectorSearchChannel(EmbeddingClient, VectorStore)} 注入。
     */
    protected VectorSearchChannel() {
        this(null, null);
    }

    @Autowired
    public VectorSearchChannel(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    @Override
    public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
        try {
            long start = System.currentTimeMillis();
            EmbeddingClient.EmbeddingResult embedding = embeddingClient.embed(query);
            log.debug("Vector embedding took {}ms", System.currentTimeMillis() - start);

            VectorStore.VectorFilter filter = new VectorStore.VectorFilter(
                    knowledgeBaseId.toString(),
                    null,
                    null
            );

            List<VectorStore.VectorSearchResult> results = vectorStore.search(
                    embedding.vector(),
                    topK,
                    filter
            );

            return results.stream()
                    .map(r -> new SearchResult(
                            extractChunkId(r.key()),
                            extractDocumentId(r.key()),
                            extractContent(r.metadata()),
                            r.score(),
                            r.metadata()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Vector search error", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getChannelName() {
        return "vector";
    }

    @Override
    public double getWeight() {
        return 0.7;
    }

    private Long extractChunkId(String key) {
        if (key == null || key.isEmpty()) return 0L;
        try {
            String[] parts = key.split(":");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long extractDocumentId(String key) {
        if (key == null || key.isEmpty()) return 0L;
        try {
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                return Long.parseLong(parts[parts.length - 2]);
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String extractContent(String metadata) {
        if (metadata == null || metadata.isEmpty()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = mapper.readValue(metadata, java.util.Map.class);
            return (String) map.getOrDefault("content", "");
        } catch (Exception e) {
            return metadata;
        }
    }
}
