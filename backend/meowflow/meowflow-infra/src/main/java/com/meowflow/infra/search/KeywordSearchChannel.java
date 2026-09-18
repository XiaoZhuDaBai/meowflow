package com.meowflow.infra.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 关键词检索通道
 */
@Slf4j
@Component
public class KeywordSearchChannel implements SearchChannel {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+");

    @Override
    public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
        try {
            List<String> keywords = extractKeywords(query);
            return doKeywordSearch(keywords, knowledgeBaseId, topK);
        } catch (Exception e) {
            log.error("Keyword search error", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getChannelName() {
        return "keyword";
    }

    @Override
    public double getWeight() {
        return 0.3;
    }

    private List<String> extractKeywords(String query) {
        return WORD_PATTERN.matcher(query.toLowerCase())
                .results()
                .map(mr -> mr.group())
                .collect(Collectors.toList());
    }

    protected List<SearchResult> doKeywordSearch(List<String> keywords, Long knowledgeBaseId, int topK) {
        return new ArrayList<>();
    }

    protected double calculateKeywordScore(String content, List<String> keywords) {
        if (keywords.isEmpty()) return 0;

        String lowerContent = content.toLowerCase();
        int matchCount = 0;
        for (String keyword : keywords) {
            if (lowerContent.contains(keyword)) {
                matchCount++;
            }
        }

        return (double) matchCount / keywords.size();
    }
}
