package com.meowflow.infra.knowledge;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChunkTest {

    @Test
    void chunk_shouldHaveRequiredFields() {
        Chunk chunk = new Chunk();
        chunk.setId(1L);
        chunk.setDocumentId(100L);
        chunk.setContent("This is a test chunk content");
        chunk.setChunkIndex(0);
        chunk.setTokenCount(10);

        assertEquals(1L, chunk.getId());
        assertEquals(100L, chunk.getDocumentId());
        assertEquals("This is a test chunk content", chunk.getContent());
        assertEquals(0, chunk.getChunkIndex());
        assertEquals(10, chunk.getTokenCount());
    }

    @Test
    void chunk_shouldSupportVector() {
        Chunk chunk = new Chunk();
        Float[] vector = new Float[]{0.1f, 0.2f, 0.3f};
        chunk.setVector(vector);

        assertNotNull(chunk.getVector());
        assertEquals(3, chunk.getVector().length);
        assertEquals(0.1f, chunk.getVector()[0]);
        assertEquals(0.2f, chunk.getVector()[1]);
        assertEquals(0.3f, chunk.getVector()[2]);
    }

    @Test
    void chunk_shouldSupportVectorKey() {
        Chunk chunk = new Chunk();
        chunk.setVectorKey("embedding-key-123");

        assertEquals("embedding-key-123", chunk.getVectorKey());
    }

    @Test
    void chunk_shouldSupportMetadata() {
        Chunk chunk = new Chunk();
        chunk.setMetadata("{\"source\":\"pdf\",\"page\":1}");

        assertNotNull(chunk.getMetadata());
        assertTrue(chunk.getMetadata().contains("pdf"));
    }

    @Test
    void chunk_shouldTrackTimestamps() {
        Chunk chunk = new Chunk();
        LocalDateTime now = LocalDateTime.now();
        chunk.setCreateTime(now);
        chunk.setUpdateTime(now);

        assertEquals(now, chunk.getCreateTime());
        assertEquals(now, chunk.getUpdateTime());
    }

    @Test
    void chunk_shouldSupportMultipleChunks() {
        Chunk chunk1 = new Chunk();
        chunk1.setId(1L);
        chunk1.setChunkIndex(0);

        Chunk chunk2 = new Chunk();
        chunk2.setId(2L);
        chunk2.setChunkIndex(1);

        assertEquals(0, chunk1.getChunkIndex());
        assertEquals(1, chunk2.getChunkIndex());
    }
}
