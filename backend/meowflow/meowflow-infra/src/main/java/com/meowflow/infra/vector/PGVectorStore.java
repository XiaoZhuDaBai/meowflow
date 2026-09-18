package com.meowflow.infra.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PGVector 向量存储实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PGVectorStore implements VectorStore {

    private final JdbcTemplate jdbcTemplate;

    private static final String VECTOR_TABLE = "vector_store";
    private static final int DEFAULT_DIMENSION = 1536;

    @PostConstruct
    public void init() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        // 1. 基础表
        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                id BIGSERIAL PRIMARY KEY,
                collection_name VARCHAR(255) NOT NULL,
                vector_key VARCHAR(255) NOT NULL,
                vector REAL[] NOT NULL,
                metadata JSONB DEFAULT '{}',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(collection_name, vector_key)
            )
            """.formatted(VECTOR_TABLE);

        // 2. B-tree 索引：按 collection_name 过滤时加速
        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_vector_store_collection
            ON %s(collection_name)
            """.formatted(VECTOR_TABLE);

        try {
            jdbcTemplate.execute(sql);
            jdbcTemplate.execute(indexSql);
            log.info("PGVector store initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to create vector table/collection index: {}", e.getMessage());
            return;
        }

        // 3. 向量 ANN 索引：仅在启用了 pgvector 扩展（即 vector_cosine_ops 操作符存在）时创建
        // 当前 schema 用的是 REAL[]，不属于 pgvector 的 vector 类型，无法直接使用 ivfflat / hnsw。
        // 如需 ANN 检索能力，请：
        //   a) docker-compose.dev.yml 改用 pgvector/pgvector 镜像；
        //   b) 把 vector 列迁移到 vector(N) 类型；
        //   c) 把 toPgArray() 改为返回 '[x,y,z]::vector' 字面量。
        // 这里做一个能力探测：能创建则创建，不能则跳过。
        try {
            String embeddingIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
                ON %s USING ivfflat (vector vector_cosine_ops)
                WITH (lists = 100)
                """.formatted(VECTOR_TABLE);
            jdbcTemplate.execute(embeddingIndexSql);
            log.info("Created ivfflat embedding index (pgvector enabled)");
        } catch (Exception e) {
            log.info("Skip ivfflat index (pgvector not installed or vector type mismatch): {}",
                    e.getMessage().split("\n")[0]);
        }
    }

    @Override
    public void insert(VectorEntry entry) {
        String sql = """
            INSERT INTO %s (collection_name, vector_key, vector, metadata)
            VALUES (?, ?, CAST(? AS real[]), CAST(? AS jsonb))
            ON CONFLICT (collection_name, vector_key)
            DO UPDATE SET vector = EXCLUDED.vector, metadata = EXCLUDED.metadata
            """.formatted(VECTOR_TABLE);

        jdbcTemplate.update(sql,
                entry.collectionName(),
                entry.key(),
                toPgArray(entry.vector()),
                entry.metadata()
        );
    }

    @Override
    public void insertBatch(List<VectorEntry> entries) {
        if (entries.isEmpty()) return;

        String sql = """
            INSERT INTO %s (collection_name, vector_key, vector, metadata)
            VALUES (?, ?, ?, ?::jsonb)
            ON CONFLICT (collection_name, vector_key)
            DO UPDATE SET vector = EXCLUDED.vector, metadata = EXCLUDED.metadata
            """.formatted(VECTOR_TABLE);

        jdbcTemplate.batchUpdate(sql, entries, entries.size(),
                (ps, entry) -> {
                    ps.setString(1, entry.collectionName());
                    ps.setString(2, entry.key());
                    ps.setObject(3, toPgArray(entry.vector()));
                    ps.setString(4, entry.metadata());
                });

        log.info("Batch inserted {} vectors", entries.size());
    }

    @Override
    public List<VectorSearchResult> search(float[] queryVector, int topK, VectorFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT vector_key, vector, metadata FROM ")
                .append(VECTOR_TABLE)
                .append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filter != null) {
            if (filter.knowledgeBaseId() != null) {
                sql.append(" AND metadata->>'knowledgeBaseId' = ?");
                params.add(filter.knowledgeBaseId());
            }
            if (filter.documentId() != null) {
                sql.append(" AND metadata->>'documentId' = ?");
                params.add(filter.documentId());
            }
            if (filter.tags() != null && !filter.tags().isEmpty()) {
                sql.append(" AND metadata->'tags' ?| ?");
                params.add(filter.tags().toArray(new String[0]));
            }
        }

        List<VectorSearchResult> candidates = jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new VectorSearchResult(
                        rs.getString("vector_key"),
                        toFloatArray((Object[]) rs.getArray("vector").getArray()),
                        0f,
                        rs.getString("metadata")
                ),
                params.toArray()
        );

        return candidates.stream()
                .map(item -> new VectorSearchResult(
                        item.key(),
                        item.vector(),
                        cosineSimilarity(queryVector, item.vector()),
                        item.metadata()))
                .sorted(Comparator.comparingDouble(VectorSearchResult::score).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    @Override
    public void delete(String collectionName, String vectorKey) {
        String sql = """
            DELETE FROM %s
            WHERE collection_name = ? AND vector_key = ?
            """.formatted(VECTOR_TABLE);

        jdbcTemplate.update(sql, collectionName, vectorKey);
    }

    @Override
    public void deleteCollection(String collectionName) {
        String sql = "DELETE FROM " + VECTOR_TABLE + " WHERE collection_name = ?";
        jdbcTemplate.update(sql, collectionName);
        log.info("Deleted collection: {}", collectionName);
    }

    @Override
    public boolean collectionExists(String collectionName) {
        String sql = """
            SELECT EXISTS(
                SELECT 1 FROM %s WHERE collection_name = ? LIMIT 1
            )
            """.formatted(VECTOR_TABLE);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, collectionName));
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        if (collectionExists(collectionName)) {
            log.warn("Collection {} already exists", collectionName);
            return;
        }
        log.info("Collection {} created with dimension {}", collectionName, dimension);
    }

    @Override
    public CollectionInfo getCollectionInfo(String collectionName) {
        String sql = """
            SELECT
                COUNT(*) as count,
                MAX(created_at) as created_at
            FROM %s
            WHERE collection_name = ?
            """.formatted(VECTOR_TABLE);

        return jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> new CollectionInfo(
                        collectionName,
                        DEFAULT_DIMENSION,
                        rs.getLong("count"),
                        rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toInstant().toEpochMilli()
                                : System.currentTimeMillis()
                ),
                collectionName
        );
    }

    private String toPgArray(float[] vector) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    private float[] toFloatArray(Object[] array) {
        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] instanceof Number number ? number.floatValue() : 0f;
        }
        return result;
    }

    private float cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) return 0f;
        return (float) (dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }
}
