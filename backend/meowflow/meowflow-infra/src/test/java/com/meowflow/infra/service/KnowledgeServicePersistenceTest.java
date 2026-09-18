package com.meowflow.infra.service;

import com.meowflow.infra.persistence.entity.ChunkEntity;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import com.meowflow.infra.persistence.mapper.ChunkMapper;
import com.meowflow.infra.persistence.mapper.DocumentMapper;
import com.meowflow.infra.persistence.mapper.KnowledgeBaseMapper;
import com.meowflow.infra.embedding.EmbeddingClient;
import com.meowflow.infra.knowledge.DocumentParser;
import com.meowflow.infra.vector.VectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeService 持久化版单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeService Persistence Tests")
class KnowledgeServicePersistenceTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private ChunkMapper chunkMapper;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private FileStorageService fileStorageService;

    private KnowledgeService knowledgeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        knowledgeService = new KnowledgeService(
                knowledgeBaseMapper,
                documentMapper,
                chunkMapper,
                documentParser,
                embeddingClient,
                vectorStore,
                objectMapper,
                fileStorageService
        );
    }

    @Nested
    @DisplayName("createKnowledgeBase Tests")
    class CreateKnowledgeBaseTests {

        @Test
        @DisplayName("Should create knowledge base with pgvector")
        void shouldCreateKnowledgeBaseWithPgvector() {
            when(knowledgeBaseMapper.insert(any())).thenAnswer(inv -> {
                KnowledgeBaseEntity kb = inv.getArgument(0);
                kb.setId(1L);
                return 1;
            });

            KnowledgeBaseEntity result = knowledgeService.createKnowledgeBase(
                    "测试知识库", "描述", "pgvector", 1536
            );

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("测试知识库");
            assertThat(result.getVectorStoreType()).isEqualTo("pgvector");
            verify(vectorStore).createCollection(eq("kb_1"), eq(1536));
        }

        @Test
        @DisplayName("Should use default dimension when not specified")
        void shouldUseDefaultDimension() {
            when(knowledgeBaseMapper.insert(any())).thenAnswer(inv -> {
                KnowledgeBaseEntity kb = inv.getArgument(0);
                kb.setId(2L);
                return 1;
            });

            KnowledgeBaseEntity result = knowledgeService.createKnowledgeBase(
                    "知识库", "描述", "pgvector", null
            );

            assertThat(result.getDimension()).isEqualTo(1536);
        }
    }

    @Nested
    @DisplayName("uploadDocument Tests")
    class UploadDocumentTests {

        @Test
        @DisplayName("Should upload and process document")
        void shouldUploadAndProcessDocument() {
            KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
            kb.setId(1L);
            kb.setStatus("active");
            when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);

            DocumentEntity savedDoc = new DocumentEntity();
            savedDoc.setId(100L);
            when(documentMapper.insert(any())).thenAnswer(inv -> {
                DocumentEntity doc = inv.getArgument(0);
                doc.setId(100L);
                return 1;
            });

            when(documentParser.parseAndChunk(anyString(), anyString()))
                    .thenReturn(Arrays.asList("chunk1", "chunk2"));
            when(documentParser.estimateTokenCount(anyString())).thenReturn(10);

            when(embeddingClient.embed(anyString()))
                    .thenReturn(new EmbeddingClient.EmbeddingResult("text", new float[]{0.1f}, 10, 5));

            DocumentEntity result = knowledgeService.uploadDocument(
                    1L, "测试文档", "文档内容", "text/plain"
            );

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo("completed");
            verify(chunkMapper, times(2)).insert(any());
            verify(vectorStore, times(2)).insert(any());
        }

        @Test
        @DisplayName("Should throw when knowledge base not found")
        void shouldThrowWhenKbNotFound() {
            when(knowledgeBaseMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> knowledgeService.uploadDocument(
                    999L, "文档", "内容", "text"
            )).hasMessageContaining("知识库不存在");
        }

        @Test
        @DisplayName("Should mark document as failed on error")
        void shouldMarkDocumentAsFailedOnError() {
            KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
            kb.setId(1L);
            kb.setStatus("active");
            when(knowledgeBaseMapper.selectById(1L)).thenReturn(kb);

            when(documentMapper.insert(any())).thenAnswer(inv -> {
                DocumentEntity doc = inv.getArgument(0);
                doc.setId(100L);
                return 1;
            });
            when(documentParser.parseAndChunk(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Parse error"));

            assertThatThrownBy(() -> knowledgeService.uploadDocument(
                    1L, "文档", "内容", "text"
            )).isInstanceOf(RuntimeException.class);

            verify(documentMapper, atLeastOnce()).updateById(argThat(doc ->
                "failed".equals(((DocumentEntity)doc).getStatus())
            ));
        }
    }

    @Nested
    @DisplayName("deleteKnowledgeBase Tests")
    class DeleteKnowledgeBaseTests {

        @Test
        @DisplayName("Should delete knowledge base and related data")
        void shouldDeleteKbAndRelatedData() {
            DocumentEntity doc1 = new DocumentEntity();
            doc1.setId(1L);
            doc1.setKnowledgeBaseId(100L);
            DocumentEntity doc2 = new DocumentEntity();
            doc2.setId(2L);
            doc2.setKnowledgeBaseId(100L);

            when(documentMapper.selectList(any())).thenReturn(Arrays.asList(doc1, doc2));
            when(documentMapper.selectById(anyLong())).thenAnswer(inv -> {
                Long id = inv.getArgument(0);
                if (id == 1L) return doc1;
                if (id == 2L) return doc2;
                return null;
            });
            when(chunkMapper.findByDocumentId(anyLong())).thenReturn(List.of());

            knowledgeService.deleteKnowledgeBase(100L);

            verify(vectorStore).deleteCollection("kb_100");
            verify(documentMapper).deleteById(1L);
            verify(documentMapper).deleteById(2L);
            verify(knowledgeBaseMapper).deleteById(100L);
        }
    }

    @Nested
    @DisplayName("getDocumentChunks Tests")
    class GetDocumentChunksTests {

        @Test
        @DisplayName("Should return chunks in order")
        void shouldReturnChunksInOrder() {
            ChunkEntity chunk1 = new ChunkEntity();
            chunk1.setId(1L);
            chunk1.setChunkIndex(0);
            chunk1.setContent("第一块");

            ChunkEntity chunk2 = new ChunkEntity();
            chunk2.setId(2L);
            chunk2.setChunkIndex(1);
            chunk2.setContent("第二块");

            when(chunkMapper.findByDocumentId(100L))
                    .thenReturn(Arrays.asList(chunk1, chunk2));

            List<ChunkEntity> chunks = knowledgeService.getDocumentChunks(100L);

            assertThat(chunks).hasSize(2);
            assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
            assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
        }
    }
}
