package com.meowflow.infra.vector;

import com.meowflow.infra.vector.VectorStore.VectorEntry;
import com.meowflow.infra.vector.VectorStore.VectorFilter;
import com.meowflow.infra.vector.VectorStore.VectorSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PGVectorStore 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PGVectorStore Tests")
class PGVectorStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PGVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        when(jdbcTemplate.queryForObject(contains("EXISTS"), eq(Boolean.class), anyString()))
                .thenReturn(false);
        vectorStore = new PGVectorStore(jdbcTemplate);
    }

    @Nested
    @DisplayName("insert Tests")
    class InsertTests {

        @Test
        @DisplayName("Should insert single vector")
        void shouldInsertSingleVector() {
            VectorEntry entry = new VectorEntry(
                    "test_collection",
                    "key1",
                    new float[]{0.1f, 0.2f, 0.3f},
                    "{\"content\": \"test\"}"
            );

            vectorStore.insert(entry);

            verify(jdbcTemplate).update(
                    contains("INSERT INTO"),
                    eq("test_collection"),
                    eq("key1"),
                    any(),
                    eq("{\"content\": \"test\"}")
            );
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            VectorEntry entry = new VectorEntry(
                    "collection",
                    "key2",
                    new float[]{0.5f, 0.6f},
                    null
            );

            vectorStore.insert(entry);

            verify(jdbcTemplate).update(
                    contains("INSERT INTO"),
                    eq("collection"),
                    eq("key2"),
                    any(),
                    isNull()
            );
        }
    }

    @Nested
    @DisplayName("insertBatch Tests")
    class InsertBatchTests {

        @Test
        @DisplayName("Should batch insert vectors")
        void shouldBatchInsertVectors() {
            List<VectorEntry> entries = Arrays.asList(
                    new VectorEntry("col", "k1", new float[]{1, 2, 3}, "{}"),
                    new VectorEntry("col", "k2", new float[]{4, 5, 6}, "{}"),
                    new VectorEntry("col", "k3", new float[]{7, 8, 9}, "{}")
            );

            vectorStore.insertBatch(entries);

            verify(jdbcTemplate).batchUpdate(
                    contains("INSERT INTO"),
                    eq(entries),
                    eq(entries.size()),
                    any()
            );
        }

        @Test
        @DisplayName("Should skip empty batch")
        void shouldSkipEmptyBatch() {
            vectorStore.insertBatch(Collections.emptyList());

            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
        }
    }

    @Nested
    @DisplayName("search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should search with knowledge base filter")
        void shouldSearchWithKbFilter() {
            float[] queryVector = new float[]{0.1f, 0.2f, 0.3f};
            VectorFilter filter = new VectorFilter("kb123", null, null);

            List<VectorSearchResult> results = vectorStore.search(queryVector, 5, filter);

            assertThat(results).isNotNull();
            verify(jdbcTemplate).query(
                    contains("knowledgeBaseId"),
                    any(RowMapper.class),
                    any(Object[].class)
            );
        }

        @Test
        @DisplayName("Should search without filter")
        void shouldSearchWithoutFilter() {
            float[] queryVector = new float[]{0.1f, 0.2f};

            List<VectorSearchResult> results = vectorStore.search(queryVector, 10, null);

            assertThat(results).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete vector by key")
        void shouldDeleteVectorByKey() {
            vectorStore.delete("collection", "key1");

            verify(jdbcTemplate).update(
                    contains("DELETE FROM"),
                    eq("collection"),
                    eq("key1")
            );
        }

        @Test
        @DisplayName("Should delete collection")
        void shouldDeleteCollection() {
            vectorStore.deleteCollection("test_collection");

            verify(jdbcTemplate).update(
                    contains("DELETE FROM"),
                    eq("test_collection")
            );
        }
    }

    @Nested
    @DisplayName("collectionExists Tests")
    class CollectionExistsTests {

        @Test
        @DisplayName("Should return true for existing collection")
        void shouldReturnTrueForExisting() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("exists")))
                    .thenReturn(true);

            boolean exists = vectorStore.collectionExists("exists");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing collection")
        void shouldReturnFalseForNonExisting() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("not_exists")))
                    .thenReturn(false);

            boolean exists = vectorStore.collectionExists("not_exists");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("createCollection Tests")
    class CreateCollectionTests {

        @Test
        @DisplayName("Should log when collection exists")
        void shouldLogWhenCollectionExists() {
            when(jdbcTemplate.queryForObject(contains("EXISTS"), eq(Boolean.class), eq("existing")))
                    .thenReturn(true);

            vectorStore.createCollection("existing", 1536);

            verify(jdbcTemplate, never()).update(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("Should create collection with dimension")
        void shouldCreateCollectionWithDimension() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq("new")))
                    .thenReturn(false);

            vectorStore.createCollection("new", 1024);

            assertThat(vectorStore.collectionExists("new")).isFalse();
        }
    }
}
