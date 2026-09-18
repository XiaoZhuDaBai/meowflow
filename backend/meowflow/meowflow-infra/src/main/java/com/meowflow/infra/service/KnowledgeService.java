package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.infra.embedding.EmbeddingClient;
import com.meowflow.infra.knowledge.DocumentParser;
import com.meowflow.infra.persistence.entity.ChunkEntity;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import com.meowflow.infra.persistence.mapper.ChunkMapper;
import com.meowflow.infra.persistence.mapper.DocumentMapper;
import com.meowflow.infra.persistence.mapper.KnowledgeBaseMapper;
import com.meowflow.infra.vector.VectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务（持久化版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final DocumentParser documentParser;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    /**
     * 创建知识库
     */
    public KnowledgeBaseEntity createKnowledgeBase(String name, String description, String vectorStoreType, Integer dimension) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName(name);
        kb.setDescription(description);
        kb.setVectorStoreType(vectorStoreType);
        kb.setDimension(dimension != null ? dimension : 1536);
        kb.setStatus("active");
        kb.setCreateBy(currentUserIdAsString());

        knowledgeBaseMapper.insert(kb);

        if ("pgvector".equals(vectorStoreType)) {
            vectorStore.createCollection("kb_" + kb.getId(), kb.getDimension());
        }

        log.info("Created knowledge base: id={}, name={}", kb.getId(), name);
        return kb;
    }

    /**
     * 获取知识库
     */
    public KnowledgeBaseEntity getKnowledgeBase(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    /**
     * 列出知识库
     */
    public List<KnowledgeBaseEntity> listKnowledgeBases() {
        return knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseEntity>()
                        .eq(KnowledgeBaseEntity::getStatus, "active")
                        .orderByDesc(KnowledgeBaseEntity::getCreateTime)
        );
    }

    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        List<DocumentEntity> docs = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getKnowledgeBaseId, id)
        );

        for (DocumentEntity doc : docs) {
            deleteDocument(doc.getId());
        }

        vectorStore.deleteCollection("kb_" + id);
        knowledgeBaseMapper.deleteById(id);
        log.info("Deleted knowledge base: id={}", id);
    }

    /**
     * 上传文档
     */
    @Transactional
    public DocumentEntity uploadDocument(Long knowledgeBaseId, String title, String content, String contentType) {
        return uploadDocument(knowledgeBaseId, title, content, contentType, null, null);
    }

    /**
     * 上传文档（含原文件归档信息）
     *
     * @param filePath 原文件在存储中的路径（本地路径或 MinIO 对象名），无原文件时为 null
     * @param fileSize 原文件大小（字节），无原文件时为 null
     */
    @Transactional
    public DocumentEntity uploadDocument(Long knowledgeBaseId, String title, String content, String contentType,
                                          String filePath, Long fileSize) {
        KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null || !"active".equals(kb.getStatus())) {
            throw new RuntimeException("知识库不存在或未激活");
        }

        DocumentEntity doc = new DocumentEntity();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setContentType(contentType);
        doc.setFilePath(filePath);
        doc.setFileSize(fileSize);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setStatus("pending");
        doc.setCreateBy(currentUserIdAsString());

        documentMapper.insert(doc);

        try {
            processDocument(doc, kb);
        } catch (Exception e) {
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
            throw e;
        }

        return doc;
    }

    /**
     * 处理文档（分块 + 向量化）
     */
    @Transactional
    public void processDocument(DocumentEntity doc, KnowledgeBaseEntity kb) {
        doc.setStatus("processing");
        documentMapper.updateById(doc);

        List<String> chunks = documentParser.parseAndChunk(doc.getContent(), doc.getContentType());

        for (int i = 0; i < chunks.size(); i++) {
            String chunkContent = chunks.get(i);

            EmbeddingClient.EmbeddingResult embedding = embeddingClient.embed(chunkContent);
            int tokenCount = documentParser.estimateTokenCount(chunkContent);

            ChunkEntity chunk = new ChunkEntity();
            chunk.setDocumentId(doc.getId());
            chunk.setContent(chunkContent);
            chunk.setChunkIndex(i);
            chunk.setTokenCount(tokenCount);
            chunk.setVectorKey(String.format("%s:%d", doc.getId(), i));
            chunk.setCreateBy(currentUserIdAsString());

            Map<String, Object> metadata = Map.of(
                    "documentId", doc.getId(),
                    "knowledgeBaseId", kb.getId(),
                    "content", chunkContent
            );
            chunk.setMetadata(toJson(metadata));

            chunkMapper.insert(chunk);

            vectorStore.insert(new VectorStore.VectorEntry(
                    "kb_" + kb.getId(),
                    chunk.getVectorKey(),
                    embedding.vector(),
                    chunk.getMetadata()
            ));
        }

        doc.setChunkCount(chunks.size());
        doc.setStatus("completed");
        documentMapper.updateById(doc);

        log.info("Processed document: id={}, chunks={}", doc.getId(), chunks.size());
    }

    /**
     * 列出知识库下的文档
     */
    public List<DocumentEntity> listDocuments(Long knowledgeBaseId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(DocumentEntity::getCreateTime)
        );
    }

    /**
     * 获取文档
     */
    public DocumentEntity getDocument(Long id) {
        return documentMapper.selectById(id);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        DocumentEntity doc = documentMapper.selectById(id);
        if (doc == null) return;

        List<ChunkEntity> chunks = chunkMapper.findByDocumentId(id);
        for (ChunkEntity chunk : chunks) {
            vectorStore.delete("kb_" + doc.getKnowledgeBaseId(), chunk.getVectorKey());
            chunkMapper.deleteById(chunk.getId());
        }

        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            try {
                fileStorageService.delete(doc.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete archived file for document id={}, filePath={}: {}",
                        id, doc.getFilePath(), e.getMessage());
            }
        }

        documentMapper.deleteById(id);
        log.info("Deleted document: id={}", id);
    }

    /**
     * 获取文档分块
     */
    public List<ChunkEntity> getDocumentChunks(Long documentId) {
        return chunkMapper.findByDocumentId(documentId);
    }

    /**
     * 获取分块
     */
    public ChunkEntity getChunk(Long id) {
        return chunkMapper.selectById(id);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String currentUserIdAsString() {
        Long userId = UserContextHolder.getUserId();
        return userId == null ? null : String.valueOf(userId);
    }
}
