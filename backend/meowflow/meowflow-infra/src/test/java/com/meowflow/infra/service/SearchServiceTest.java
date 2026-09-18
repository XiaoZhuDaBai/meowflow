package com.meowflow.infra.service;

import com.meowflow.infra.search.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SearchService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Tests")
class SearchServiceTest {

    @Mock
    private VectorSearchChannel vectorChannel;

    @Mock
    private KeywordSearchChannel keywordChannel;

    @Mock
    private HybridSearchChannel hybridChannel;

    @Mock
    private SearchPostProcessor postProcessor;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(vectorChannel, keywordChannel, hybridChannel, postProcessor);
    }

    @Nested
    @DisplayName("search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should delegate search to hybrid channel")
        void shouldDelegateSearchToHybridChannel() {
            String query = "测试查询";
            Long kbId = 1L;
            int topK = 10;
            List<SearchChannel.SearchResult> expectedResults = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.95, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "内容2", 0.85, "{}")
            );
            when(hybridChannel.search(query, kbId, topK)).thenReturn(expectedResults);

            List<SearchChannel.SearchResult> results = searchService.search(query, kbId, topK);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).content()).isEqualTo("内容1");
        }
    }

    @Nested
    @DisplayName("vectorSearch Tests")
    class VectorSearchTests {

        @Test
        @DisplayName("Should delegate vector search to vector channel")
        void shouldDelegateVectorSearch() {
            String query = "语义查询";
            Long kbId = 1L;
            int topK = 5;
            List<SearchChannel.SearchResult> expectedResults = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "向量结果", 0.92, "{}")
            );
            when(vectorChannel.search(query, kbId, topK)).thenReturn(expectedResults);

            List<SearchChannel.SearchResult> results = searchService.vectorSearch(query, kbId, topK);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).content()).isEqualTo("向量结果");
        }

        @Test
        @DisplayName("Should return empty list when vector search fails")
        void shouldReturnEmptyListOnError() {
            when(vectorChannel.search(anyString(), anyLong(), anyInt()))
                    .thenThrow(new RuntimeException("Vector store error"));

            List<SearchChannel.SearchResult> results = searchService.vectorSearch("query", 1L, 5);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("keywordSearch Tests")
    class KeywordSearchTests {

        @Test
        @DisplayName("Should delegate keyword search to keyword channel")
        void shouldDelegateKeywordSearch() {
            String query = "关键词查询";
            Long kbId = 1L;
            int topK = 5;
            List<SearchChannel.SearchResult> expectedResults = Arrays.asList(
                    new SearchChannel.SearchResult(3L, 200L, "关键词匹配", 0.88, "{}")
            );
            when(keywordChannel.search(query, kbId, topK)).thenReturn(expectedResults);

            List<SearchChannel.SearchResult> results = searchService.keywordSearch(query, kbId, topK);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).content()).isEqualTo("关键词匹配");
        }
    }

    @Nested
    @DisplayName("searchWithRerank Tests")
    class SearchWithRerankTests {

        @Test
        @DisplayName("Should combine vector and keyword results with RRF")
        void shouldCombineResultsWithRRF() {
            String query = "混合查询";
            Long kbId = 1L;
            int topK = 5;

            List<SearchChannel.SearchResult> vectorResults = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "向量内容", 0.9, "{}")
            );
            List<SearchChannel.SearchResult> keywordResults = Arrays.asList(
                    new SearchChannel.SearchResult(2L, 100L, "关键词内容", 0.8, "{}")
            );
            List<SearchChannel.SearchResult> rerankedResults = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "向量内容", 0.95, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "关键词内容", 0.85, "{}")
            );

            when(vectorChannel.search(query, kbId, topK * 2)).thenReturn(vectorResults);
            when(keywordChannel.search(query, kbId, topK * 2)).thenReturn(keywordResults);
            when(postProcessor.rrfReRank(anyList(), eq(60))).thenReturn(rerankedResults);

            List<SearchChannel.SearchResult> results = searchService.searchWithRerank(query, kbId, topK);

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("buildSearchContext Tests")
    class BuildSearchContextTests {

        @Test
        @DisplayName("Should build context from search results")
        void shouldBuildContextFromResults() {
            String query = "查询";
            Long kbId = 1L;
            int topK = 3;
            int maxLength = 1000;

            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.9, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "内容2", 0.8, "{}")
            );
            String expectedContext = "内容1\n\n内容2";
            when(hybridChannel.search(query, kbId, topK)).thenReturn(results);
            when(postProcessor.buildContext(results, maxLength)).thenReturn(expectedContext);

            String context = searchService.buildSearchContext(query, kbId, topK, maxLength);

            assertThat(context).isEqualTo(expectedContext);
        }

        @Test
        @DisplayName("Should handle empty results gracefully")
        void shouldHandleEmptyResults() {
            when(hybridChannel.search(anyString(), anyLong(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(postProcessor.buildContext(anyList(), anyInt())).thenReturn("");

            String context = searchService.buildSearchContext("查询", 1L, 5, 1000);

            assertThat(context).isEmpty();
        }
    }

    @Nested
    @DisplayName("deduplicateResults Tests")
    class DeduplicateResultsTests {

        @Test
        @DisplayName("Should deduplicate results")
        void shouldDeduplicateResults() {
            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.9, "{}"),
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.85, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "内容2", 0.8, "{}")
            );
            List<SearchChannel.SearchResult> deduplicated = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.9, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "内容2", 0.8, "{}")
            );
            when(postProcessor.deduplicate(results)).thenReturn(deduplicated);

            List<SearchChannel.SearchResult> result = searchService.deduplicateResults(results);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("filterByScore Tests")
    class FilterByScoreTests {

        @Test
        @DisplayName("Should filter results by minimum score")
        void shouldFilterByScore() {
            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "高分", 0.95, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "低分", 0.3, "{}"),
                    new SearchChannel.SearchResult(3L, 100L, "中分", 0.7, "{}")
            );
            List<SearchChannel.SearchResult> filtered = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "高分", 0.95, "{}"),
                    new SearchChannel.SearchResult(3L, 100L, "中分", 0.7, "{}")
            );
            when(postProcessor.filterByScore(results, 0.5)).thenReturn(filtered);

            List<SearchChannel.SearchResult> result = searchService.filterByScore(results, 0.5);

            assertThat(result).hasSize(2);
        }
    }
}
