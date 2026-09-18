package com.meowflow.infra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.infra.embedding.EmbeddingClient;
import com.meowflow.infra.knowledge.DocumentParser;
import com.meowflow.infra.persistence.entity.ChunkEntity;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import com.meowflow.infra.persistence.mapper.ChunkMapper;
import com.meowflow.infra.persistence.mapper.DocumentMapper;
import com.meowflow.infra.persistence.mapper.KnowledgeBaseMapper;
import com.meowflow.infra.vector.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeService Tests")
class KnowledgeServiceTest {

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

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(
                knowledgeBaseMapper, documentMapper, chunkMapper,
                documentParser, embeddingClient, vectorStore, new ObjectMapper(), fileStorageService
        );
    }

    private KnowledgeBaseEntity stubInsert(KnowledgeBaseEntity kb) {
        when(knowledgeBaseMapper.insert(any(KnowledgeBaseEntity.class))).thenAnswer(invocation -> {
            KnowledgeBaseEntity arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        });
        return kb;
    }

    @Nested
    @DisplayName("createKnowledgeBase Tests")
    class CreateKnowledgeBaseTests {

        @Test
        @DisplayName("Should create knowledge base with default dimension when dimension is null")
        void shouldCreateKnowledgeBaseWithDefaultDimension() {
            when(knowledgeBaseMapper.insert(any(KnowledgeBaseEntity.class))).thenAnswer(invocation -> {
                KnowledgeBaseEntity arg = invocation.getArgument(0);
                arg.setId(10L);
                return 1;
            });

            KnowledgeBaseEntity kb = knowledgeService.createKnowledgeBase(
                    "kb-name", "kb-desc", "pgvector", null);

            assertThat(kb).isNotNull();
            assertThat(kb.getId()).isEqualTo(10L);
            assertThat(kb.getName()).isEqualTo("kb-name");
            assertThat(kb.getDescription()).isEqualTo("kb-desc");
            assertThat(kb.getVectorStoreType()).isEqualTo("pgvector");
            assertThat(kb.getDimension()).isEqualTo(1536);
            assertThat(kb.getStatus()).isEqualTo("active");

            verify(vectorStore).createCollection("kb_10", 1536);
        }

        @Test
        @DisplayName("Should not call vectorStore.createCollection for non-pgvector types")
        void shouldNotCreateCollectionForMilvus() {
            when(knowledgeBaseMapper.insert(any(KnowledgeBaseEntity.class))).thenAnswer(invocation -> {
                KnowledgeBaseEntity arg = invocation.getArgument(0);
                arg.setId(20L);
                return 1;
            });

            KnowledgeBaseEntity kb = knowledgeService.createKnowledgeBase(
                    "kb-milvus", "desc", "milvus", 768);

            assertThat(kb.getDimension()).isEqualTo(768);
            verify(vectorStore, never()).createCollection(anyString(), any(Integer.class));
        }

        @Test
        @DisplayName("Should use provided dimension when not null")
        void shouldUseProvidedDimension() {
            when(knowledgeBaseMapper.insert(any(KnowledgeBaseEntity.class))).thenAnswer(invocation -> {
                KnowledgeBaseEntity arg = invocation.getArgument(0);
                arg.setId(30L);
                return 1;
            });

            KnowledgeBaseEntity kb = knowledgeService.createKnowledgeBase(
                    "kb", "desc", "pgvector", 1024);

            assertThat(kb.getDimension()).isEqualTo(1024);
            verify(vectorStore).createCollection("kb_30", 1024);
        }
    }

    @Nested
    @DisplayName("listKnowledgeBases Tests")
    class ListKnowledgeBaseTests {

        @Test
        @DisplayName("Should return knowledge bases from mapper")
        void shouldListKnowledgeBases() {
            KnowledgeBaseEntity kb1 = new KnowledgeBaseEntity();
            kb1.setId(1L);
            kb1.setName("kb1");
            KnowledgeBaseEntity kb2 = new KnowledgeBaseEntity();
            kb2.setId(2L);
            kb2.setName("kb2");

            when(knowledgeBaseMapper.selectList(any())).thenReturn(List.of(kb1, kb2));

            List<KnowledgeBaseEntity> result = knowledgeService.listKnowledgeBases();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("kb1");
        }
    }

    @Nested
    @DisplayName("uploadDocument Tests")
    class UploadDocumentTests {

        @Test
        @DisplayName("Should upload document and process chunks successfully")
        void shouldUploadDocument() {
            KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
            kb.setId(5L);
            kb.setStatus("active");
            when(knowledgeBaseMapper.selectById(5L)).thenReturn(kb);

            when(documentParser.parseAndChunk(anyString(), anyString())).thenReturn(List.of("c1", "c2"));
            when(documentParser.estimateTokenCount(anyString())).thenReturn(10);
            when(embeddingClient.embed(anyString()))
                    .thenReturn(new EmbeddingClient.EmbeddingResult("text", new float[]{0.1f}, 10, 5));

            when(documentMapper.insert(any(DocumentEntity.class))).thenAnswer(invocation -> {
                DocumentEntity arg = invocation.getArgument(0);
                arg.setId(100L);
                return 1;
            });

            DocumentEntity doc = knowledgeService.uploadDocument(
                    5L, "title", "content body", "text/plain");

            assertThat(doc).isNotNull();
            assertThat(doc.getId()).isEqualTo(100L);
            assertThat(doc.getTitle()).isEqualTo("title");
            assertThat(doc.getKnowledgeBaseId()).isEqualTo(5L);
            assertThat(doc.getChunkCount()).isEqualTo(2);
            assertThat(doc.getStatus()).isEqualTo("completed");

            verify(chunkMapper, times(2)).insert(any(ChunkEntity.class));
            verify(vectorStore, times(2)).insert(any(VectorStore.VectorEntry.class));
            verify(documentMapper, times(2)).updateById(any(DocumentEntity.class));
        }

        @Test
        @DisplayName("Should throw exception when knowledge base not found")
        void shouldThrowForMissingKb() {
            when(knowledgeBaseMapper.selectById(99999L)).thenReturn(null);

            assertThatThrownBy(() -> knowledgeService.uploadDocument(
                    99999L, "title", "content", "text/plain"
            )).hasMessageContaining("知识库不存在");
        }

        @Test
        @DisplayName("Should mark document as failed when processing throws")
        void shouldMarkFailedOnError() {
            KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
            kb.setId(5L);
            kb.setStatus("active");
            when(knowledgeBaseMapper.selectById(5L)).thenReturn(kb);

            when(documentMapper.insert(any(DocumentEntity.class))).thenAnswer(invocation -> {
                DocumentEntity arg = invocation.getArgument(0);
                arg.setId(200L);
                return 1;
            });

            when(documentParser.parseAndChunk(anyString(), anyString()))
                    .thenThrow(new RuntimeException("parse-error"));

            assertThatThrownBy(() -> knowledgeService.uploadDocument(
                    5L, "title", "content", "text/plain"
            )).hasMessageContaining("parse-error");

            verify(documentMapper, atLeast(1)).updateById(any(DocumentEntity.class));
        }
    }

    @Nested
    @DisplayName("deleteDocument Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document and its chunks")
        void shouldDeleteDocument() {
            DocumentEntity doc = new DocumentEntity();
            doc.setId(50L);
            doc.setKnowledgeBaseId(5L);
            when(documentMapper.selectById(50L)).thenReturn(doc);

            ChunkEntity chunk = new ChunkEntity();
            chunk.setId(1L);
            chunk.setVectorKey("v1");
            when(chunkMapper.findByDocumentId(50L)).thenReturn(List.of(chunk));

            knowledgeService.deleteDocument(50L);

            verify(vectorStore).delete("kb_5", "v1");
            verify(chunkMapper).deleteById(1L);
            verify(documentMapper).deleteById(50L);
        }

        @Test
        @DisplayName("Should do nothing when document is null")
        void shouldDoNothingForMissingDoc() {
            when(documentMapper.selectById(50L)).thenReturn(null);

            knowledgeService.deleteDocument(50L);

            verify(chunkMapper, never()).findByDocumentId(any());
            verify(documentMapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getDocument / getChunk Tests")
    class ReadTests {

        @Test
        @DisplayName("getDocument should delegate to mapper")
        void shouldGetDocument() {
            DocumentEntity doc = new DocumentEntity();
            doc.setId(1L);
            when(documentMapper.selectById(1L)).thenReturn(doc);

            assertThat(knowledgeService.getDocument(1L)).isNotNull();
            assertThat(knowledgeService.getDocument(2L)).isNull();
        }

        @Test
        @DisplayName("getChunk should delegate to mapper")
        void shouldGetChunk() {
            ChunkEntity chunk = new ChunkEntity();
            chunk.setId(1L);
            chunk.setContent("text");
            when(chunkMapper.selectById(1L)).thenReturn(chunk);

            assertThat(knowledgeService.getChunk(1L).getContent()).isEqualTo("text");
        }

        @Test
        @DisplayName("getDocumentChunks should delegate to mapper")
        void shouldGetDocumentChunks() {
            ChunkEntity c = new ChunkEntity();
            c.setChunkIndex(0);
            when(chunkMapper.findByDocumentId(50L)).thenReturn(List.of(c));

            assertThat(knowledgeService.getDocumentChunks(50L)).hasSize(1);
        }
    }
}
