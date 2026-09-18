package com.meowflow.infra.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索后处理器 - 对检索结果进行排序、去重、重排序等处理
 */
@Slf4j
@Component
public class SearchPostProcessor {

    /**
     * 对结果进行去重
     */
    public List<SearchChannel.SearchResult> deduplicate(List<SearchChannel.SearchResult> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        r -> r.chunkId() + "-" + r.documentId(),
                        r -> r,
                        (a, b) -> a.score() >= b.score() ? a : b
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(SearchChannel.SearchResult::score).reversed())
                .toList();
    }

    /**
     * 过滤低分结果
     */
    public List<SearchChannel.SearchResult> filterByScore(List<SearchChannel.SearchResult> results, double minScore) {
        return results.stream()
                .filter(r -> r.score() >= minScore)
                .toList();
    }

    /**
     * RRF 重排序 (Reciprocal Rank Fusion)
     */
    public List<SearchChannel.SearchResult> rrfReRank(List<List<SearchChannel.SearchResult>> resultLists, int k) {
        java.util.Map<String, Double> scores = new java.util.HashMap<>();
        java.util.Map<String, SearchChannel.SearchResult> resultMap = new java.util.HashMap<>();

        for (List<SearchChannel.SearchResult> results : resultLists) {
            for (int i = 0; i < results.size(); i++) {
                SearchChannel.SearchResult r = results.get(i);
                String key = r.chunkId() + "-" + r.documentId();
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(key, rrfScore, Double::sum);
                resultMap.putIfAbsent(key, r);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    SearchChannel.SearchResult original = resultMap.get(e.getKey());
                    return new SearchChannel.SearchResult(
                            original.chunkId(),
                            original.documentId(),
                            original.content(),
                            e.getValue(),
                            original.metadata()
                    );
                })
                .toList();
    }

    /**
     * 按文档分组
     */
    public java.util.Map<Long, List<SearchChannel.SearchResult>> groupByDocument(List<SearchChannel.SearchResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(SearchChannel.SearchResult::documentId));
    }

    /**
     * 每个文档只保留 topN 个 chunk
     */
    public List<SearchChannel.SearchResult> limitPerDocument(List<SearchChannel.SearchResult> results, int topN) {
        return results.stream()
                .collect(Collectors.groupingBy(SearchChannel.SearchResult::documentId))
                .values()
                .stream()
                .flatMap(group -> group.stream()
                        .sorted(Comparator.comparingDouble(SearchChannel.SearchResult::score).reversed())
                        .limit(topN))
                .sorted(Comparator.comparingDouble(SearchChannel.SearchResult::score).reversed())
                .toList();
    }

    /**
     * 构建上下文片段 - 将相关片段合并
     */
    public String buildContext(List<SearchChannel.SearchResult> results, int maxLength) {
        StringBuilder context = new StringBuilder();
        for (SearchChannel.SearchResult r : results) {
            if (context.length() + r.content().length() + 50 > maxLength) {
                break;
            }
            context.append("\n---\n").append(r.content());
        }
        return context.length() > 0 ? context.substring(5) : context.toString();
    }
}
