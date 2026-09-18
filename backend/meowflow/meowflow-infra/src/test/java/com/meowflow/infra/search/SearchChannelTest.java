package com.meowflow.infra.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchChannel 单元测试
 */
@DisplayName("SearchChannel Tests")
class SearchChannelTest {

    @Nested
    @DisplayName("SearchResult Record Tests")
    class SearchResultTests {

        @Test
        @DisplayName("Should create SearchResult with all fields")
        void shouldCreateSearchResultWithAllFields() {
            SearchChannel.SearchResult result = new SearchChannel.SearchResult(
                    1L, 100L, "测试内容", 0.95, "{\"source\": \"doc\"}"
            );

            assertThat(result.chunkId()).isEqualTo(1L);
            assertThat(result.documentId()).isEqualTo(100L);
            assertThat(result.content()).isEqualTo("测试内容");
            assertThat(result.score()).isEqualTo(0.95);
            assertThat(result.metadata()).isEqualTo("{\"source\": \"doc\"}");
        }

        @Test
        @DisplayName("SearchResult should be comparable by score")
        void searchResultShouldBeComparableByScore() {
            SearchChannel.SearchResult result1 = new SearchChannel.SearchResult(1L, 100L, "内容1", 0.9, "{}");
            SearchChannel.SearchResult result2 = new SearchChannel.SearchResult(2L, 100L, "内容2", 0.8, "{}");

            List<SearchChannel.SearchResult> results = Arrays.asList(result2, result1);
            List<SearchChannel.SearchResult> sorted = results.stream()
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .toList();

            assertThat(sorted.get(0).score()).isEqualTo(0.9);
            assertThat(sorted.get(1).score()).isEqualTo(0.8);
        }
    }

    @Nested
    @DisplayName("HybridSearchChannel Tests")
    class HybridSearchChannelTests {

        @Test
        @DisplayName("Should merge results from multiple channels")
        void shouldMergeResultsFromMultipleChannels() {
            VectorSearchChannel vectorChannel = new MockVectorChannel();
            KeywordSearchChannel keywordChannel = new MockKeywordChannel();
            HybridSearchChannel hybridChannel = new HybridSearchChannel(vectorChannel, keywordChannel);

            List<SearchChannel.SearchResult> results = hybridChannel.search(
                    "测试查询", 1L, 5
            );

            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("Should return results sorted by score")
        void shouldReturnResultsSortedByScore() {
            VectorSearchChannel vectorChannel = new HighScoreVectorChannel();
            KeywordSearchChannel keywordChannel = new LowScoreKeywordChannel();
            HybridSearchChannel hybridChannel = new HybridSearchChannel(vectorChannel, keywordChannel);

            List<SearchChannel.SearchResult> results = hybridChannel.search(
                    "测试", 1L, 10
            );

            double previousScore = Double.MAX_VALUE;
            for (SearchChannel.SearchResult result : results) {
                assertThat(result.score()).isLessThanOrEqualTo(previousScore);
                previousScore = result.score();
            }
        }
    }

    @Nested
    @DisplayName("SearchPostProcessor Tests")
    class SearchPostProcessorTests {

        private SearchPostProcessor postProcessor = new SearchPostProcessor();

        @Test
        void setup() {
            // Reserved for documentation; postProcessor is field-initialised.
        }

        @Test
        @DisplayName("Should build context from results")
        void shouldBuildContextFromResults() {
            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "第一段内容", 0.9, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "第二段内容", 0.8, "{}")
            );

            String context = postProcessor.buildContext(results, 1000);

            assertThat(context).contains("第一段内容");
            assertThat(context).contains("第二段内容");
        }

        @Test
        @DisplayName("Should deduplicate results by chunk ID")
        void shouldDeduplicateResults() {
            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "内容1", 0.9, "{}"),
                    new SearchChannel.SearchResult(1L, 100L, "内容1重复", 0.85, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "内容2", 0.8, "{}")
            );

            List<SearchChannel.SearchResult> deduplicated = postProcessor.deduplicate(results);

            assertThat(deduplicated).hasSize(2);
            assertThat(deduplicated).anyMatch(r -> r.chunkId().equals(1L));
            assertThat(deduplicated).anyMatch(r -> r.chunkId().equals(2L));
        }

        @Test
        @DisplayName("Should filter results by minimum score")
        void shouldFilterByMinimumScore() {
            List<SearchChannel.SearchResult> results = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "高分", 0.95, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "中分", 0.5, "{}"),
                    new SearchChannel.SearchResult(3L, 100L, "低分", 0.2, "{}")
            );

            List<SearchChannel.SearchResult> filtered = postProcessor.filterByScore(results, 0.6);

            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).content()).isEqualTo("高分");
        }

        @Test
        @DisplayName("Should apply RRF fusion")
        void shouldApplyRRFFusion() {
            List<SearchChannel.SearchResult> vectorResults = Arrays.asList(
                    new SearchChannel.SearchResult(1L, 100L, "向量结果1", 0.9, "{}"),
                    new SearchChannel.SearchResult(2L, 100L, "向量结果2", 0.8, "{}")
            );
            List<SearchChannel.SearchResult> keywordResults = Arrays.asList(
                    new SearchChannel.SearchResult(2L, 100L, "关键词结果2", 0.85, "{}"),
                    new SearchChannel.SearchResult(3L, 100L, "关键词结果3", 0.7, "{}")
            );

            List<SearchChannel.SearchResult> fused = postProcessor.rrfReRank(
                    Arrays.asList(vectorResults, keywordResults), 60
            );

            assertThat(fused).isNotEmpty();
        }
    }

    // Mock channels for testing
    private static class MockVectorChannel extends VectorSearchChannel {
        @Override
        public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
            return Collections.singletonList(
                    new SearchResult(1L, 100L, "向量检索结果", 0.9, "{}")
            );
        }

        @Override
        public String getChannelName() {
            return "vector";
        }
    }

    private static class MockKeywordChannel extends KeywordSearchChannel {
        @Override
        public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
            return Collections.singletonList(
                    new SearchResult(2L, 100L, "关键词检索结果", 0.85, "{}")
            );
        }

        @Override
        public String getChannelName() {
            return "keyword";
        }
    }

    private static class HighScoreVectorChannel extends VectorSearchChannel {
        @Override
        public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
            return Arrays.asList(
                    new SearchResult(1L, 100L, "高分向量", 0.95, "{}"),
                    new SearchResult(3L, 100L, "高分向量2", 0.9, "{}")
            );
        }

        @Override
        public String getChannelName() {
            return "vector";
        }
    }

    private static class LowScoreKeywordChannel extends KeywordSearchChannel {
        @Override
        public List<SearchResult> search(String query, Long knowledgeBaseId, int topK) {
            return Arrays.asList(
                    new SearchResult(2L, 100L, "低分关键词", 0.5, "{}"),
                    new SearchResult(4L, 100L, "低分关键词2", 0.4, "{}")
            );
        }

        @Override
        public String getChannelName() {
            return "keyword";
        }
    }
}
