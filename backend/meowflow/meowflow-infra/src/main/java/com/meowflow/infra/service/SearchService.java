package com.meowflow.infra.service;

import com.meowflow.infra.search.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检索服务
 */
@Slf4j
@Service
public class SearchService {

    private final VectorSearchChannel vectorChannel;
    private final KeywordSearchChannel keywordChannel;
    private final HybridSearchChannel hybridChannel;
    private final SearchPostProcessor postProcessor;

    public SearchService(
            VectorSearchChannel vectorChannel,
            KeywordSearchChannel keywordChannel,
            HybridSearchChannel hybridChannel,
            SearchPostProcessor postProcessor) {
        this.vectorChannel = vectorChannel;
        this.keywordChannel = keywordChannel;
        this.hybridChannel = hybridChannel;
        this.postProcessor = postProcessor;
    }

    public List<SearchChannel.SearchResult> search(String query, Long knowledgeBaseId, int topK) {
        return hybridChannel.search(query, knowledgeBaseId, topK);
    }

    public List<SearchChannel.SearchResult> vectorSearch(String query, Long knowledgeBaseId, int topK) {
        try {
            return vectorChannel.search(query, knowledgeBaseId, topK);
        } catch (Exception e) {
            log.error("Vector search failed", e);
            return new java.util.ArrayList<>();
        }
    }

    public List<SearchChannel.SearchResult> keywordSearch(String query, Long knowledgeBaseId, int topK) {
        return keywordChannel.search(query, knowledgeBaseId, topK);
    }

    public List<SearchChannel.SearchResult> searchWithRerank(String query, Long knowledgeBaseId, int topK) {
        List<SearchChannel.SearchResult> vectorResults = vectorChannel.search(query, knowledgeBaseId, topK * 2);
        List<SearchChannel.SearchResult> keywordResults = keywordChannel.search(query, knowledgeBaseId, topK * 2);
        return postProcessor.rrfReRank(List.of(vectorResults, keywordResults), 60);
    }

    public String buildSearchContext(String query, Long knowledgeBaseId, int topK, int maxLength) {
        List<SearchChannel.SearchResult> results = search(query, knowledgeBaseId, topK);
        return postProcessor.buildContext(results, maxLength);
    }

    public List<SearchChannel.SearchResult> deduplicateResults(List<SearchChannel.SearchResult> results) {
        return postProcessor.deduplicate(results);
    }

    public List<SearchChannel.SearchResult> filterByScore(List<SearchChannel.SearchResult> results, double minScore) {
        return postProcessor.filterByScore(results, minScore);
    }
}
