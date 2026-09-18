package com.meowflow.infra.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 混合检索通道 - 结合向量检索和关键词检索
 */
@Slf4j
@Component
public class HybridSearchChannel implements SearchChannel {

    private final SearchChannel vectorChannel;
    private final SearchChannel keywordChannel;

    public HybridSearchChannel(
            @Qualifier("vectorSearchChannel") SearchChannel vectorChannel,
            @Qualifier("keywordSearchChannel") SearchChannel keywordChannel) {
        this.vectorChannel = vectorChannel;
        this.keywordChannel = keywordChannel;
    }

    @Override
    public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
        try {
            List<SearchResult> vectorResults = vectorChannel.search(query, knowledgeBaseId, topK * 2);
            List<SearchResult> keywordResults = keywordChannel.search(query, knowledgeBaseId, topK * 2);

            return mergeResults(vectorResults, keywordResults, topK);
        } catch (Exception e) {
            log.error("Hybrid search error", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getChannelName() {
        return "hybrid";
    }

    @Override
    public double getWeight() {
        return 1.0;
    }

    private List<SearchResult> mergeResults(List<SearchResult> vectorResults, List<SearchResult> keywordResults, int topK) {
        Map<String, SearchResult> mergedMap = new java.util.HashMap<>();
        double vectorWeight = 0.7;
        double keywordWeight = 0.3;

        for (SearchResult r : vectorResults) {
            String key = r.chunkId() + "-" + r.documentId();
            mergedMap.put(key, new SearchResult(
                    r.chunkId(), r.documentId(), r.content(),
                    r.score() * vectorWeight, r.metadata()));
        }

        for (SearchResult r : keywordResults) {
            String key = r.chunkId() + "-" + r.documentId();
            if (mergedMap.containsKey(key)) {
                SearchResult existing = mergedMap.get(key);
                mergedMap.put(key, new SearchResult(
                        existing.chunkId(), existing.documentId(), existing.content(),
                        existing.score() + r.score() * keywordWeight, existing.metadata()));
            } else {
                mergedMap.put(key, new SearchResult(
                        r.chunkId(), r.documentId(), r.content(),
                        r.score() * keywordWeight, r.metadata()));
            }
        }

        return mergedMap.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .toList();
    }
}
