package com.meowflow.workflow.executor.knowledge;

import com.meowflow.common.exception.NodeException;
import com.meowflow.infra.search.SearchChannel;
import com.meowflow.infra.service.SearchService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索节点执行器。
 * <p>
 * 通过 {@link SearchService} 向量检索 / 关键词检索 / 混合检索三种模式。
 * 配置参数：
 * <ul>
 *   <li>knowledgeBaseId (Long): 知识库 ID（必需）</li>
 *   <li>query (String): 检索 query（支持 ${...} 变量引用）</li>
 *   <li>topK (Integer): 返回条数，默认 5</li>
 *   <li>similarityThreshold (Double): 相似度阈值，默认 0.7</li>
 *   <li>searchMode (String): hybrid / vector / keyword，默认 hybrid</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchExecutor extends AbstractNodeExecutor {

    private final SearchService searchService;

    @Override
    public NodeType getNodeType() {
        return NodeType.KNOWLEDGE_SEARCH;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Long knowledgeBaseId = parseLong(input.get("knowledgeBaseId"));
        if (knowledgeBaseId == null) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "knowledgeBaseId is required for knowledge search node");
        }

        String query = resolveToString(input.get("query"), context);
        if (query == null || query.isEmpty()) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "query is required for knowledge search node");
        }

        Integer topK = parseInt(input.get("topK"), 5);
        Double similarityThreshold = parseDouble(input.get("similarityThreshold"), 0.7);
        String searchMode = resolveToString(input.getOrDefault("searchMode", "hybrid"), context);

        log.info("知识库检索: kbId={}, query={}, topK={}, threshold={}, mode={}",
                knowledgeBaseId, query, topK, similarityThreshold, searchMode);

        List<SearchChannel.SearchResult> rawResults;
        try {
            rawResults = doSearch(query, knowledgeBaseId, topK * 2, searchMode);
        } catch (Exception e) {
            log.error("知识库检索失败: kbId={}, error={}", knowledgeBaseId, e.getMessage(), e);
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "知识库检索失败: " + e.getMessage(), e);
        }

        // 按阈值过滤
        List<Map<String, Object>> results = new ArrayList<>();
        int passed = 0;
        for (SearchChannel.SearchResult r : rawResults) {
            if (r.score() >= similarityThreshold) {
                Map<String, Object> item = new HashMap<>();
                item.put("chunkId", r.chunkId());
                item.put("documentId", r.documentId());
                item.put("content", r.content());
                item.put("score", r.score());
                item.put("metadata", r.metadata());
                results.add(item);
                passed++;
                if (passed >= topK) break;
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("query", query);
        output.put("knowledgeBaseId", knowledgeBaseId);
        output.put("results", results);
        output.put("totalResults", results.size());
        output.put("topK", topK);
        output.put("searchMode", searchMode);
        output.put("similarityThreshold", similarityThreshold);

        log.info("知识库检索成功: kbId={}, returned={}/{}", knowledgeBaseId, results.size(), rawResults.size());

        return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
    }

    private List<SearchChannel.SearchResult> doSearch(String query, Long kbId, int topK, String mode) {
        return switch (mode.toLowerCase()) {
            case "vector" -> searchService.vectorSearch(query, kbId, topK);
            case "keyword" -> searchService.keywordSearch(query, kbId, topK);
            default -> searchService.search(query, kbId, topK);
        };
    }

    private String resolveToString(Object value, ExecutionContext context) {
        if (value == null) return null;
        if (value instanceof String s) {
            Object resolved = context.resolveExpression(s);
            return resolved != null ? resolved.toString() : null;
        }
        return String.valueOf(value);
    }

    private Long parseLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (Exception e) { return null; }
    }

    private Integer parseInt(Object v, Integer def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); }
        catch (Exception e) { return def; }
    }

    private Double parseDouble(Object v, Double def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); }
        catch (Exception e) { return def; }
    }
}
