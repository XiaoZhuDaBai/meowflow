package com.meowflow.infra.controller;

import com.meowflow.common.result.Result;
import com.meowflow.infra.persistence.entity.ChunkEntity;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import com.meowflow.infra.service.FileStorageService;
import com.meowflow.infra.service.KnowledgeService;
import com.meowflow.infra.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import com.meowflow.infra.knowledge.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/knowledge")
@Tag(name = "知识库管理", description = "知识库、文档、检索管理接口")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final SearchService searchService;
    private final DocumentParser documentParser;
    private final FileStorageService fileStorageService;

    public KnowledgeController(KnowledgeService knowledgeService, SearchService searchService,
                               DocumentParser documentParser, FileStorageService fileStorageService) {
        this.knowledgeService = knowledgeService;
        this.searchService = searchService;
        this.documentParser = documentParser;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/bases")
    @Operation(summary = "创建知识库", description = "创建一个新的知识库")
    public Result<KnowledgeBaseEntity> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity kb = knowledgeService.createKnowledgeBase(
                request.getName(),
                request.getDescription(),
                request.getVectorStoreType(),
                request.getDimension());
        return Result.success(kb);
    }

    @GetMapping("/bases/{id}")
    @Operation(summary = "获取知识库", description = "根据 ID 获取知识库信息")
    public Result<KnowledgeBaseEntity> getKnowledgeBase(@PathVariable Long id) {
        return Result.success(knowledgeService.getKnowledgeBase(id));
    }

    @GetMapping("/bases")
    @Operation(summary = "列出知识库", description = "列出所有知识库")
    public Result<List<KnowledgeBaseEntity>> listKnowledgeBases() {
        return Result.success(knowledgeService.listKnowledgeBases());
    }

    @DeleteMapping("/bases/{id}")
    @Operation(summary = "删除知识库", description = "删除指定的知识库")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeService.deleteKnowledgeBase(id);
        return Result.success();
    }

    @PostMapping("/documents")
    @Operation(summary = "上传文档", description = "向知识库上传文档")
    public Result<DocumentEntity> uploadDocument(@RequestBody UploadDocumentRequest request) {
        DocumentEntity doc = knowledgeService.uploadDocument(
                request.getKnowledgeBaseId(),
                request.getTitle(),
                request.getContent(),
                request.getContentType());
        return Result.success(doc);
    }

    @PostMapping(value = "/documents/upload", consumes = "multipart/form-data")
    @Operation(summary = "上传文件（自动解析）", description = "上传文件并使用 Apache Tika 自动解析内容，支持 PDF/DOCX/PPTX/TXT/HTML 等格式")
    public Result<DocumentEntity> uploadDocumentFile(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            // 1️⃣ 先保存原文件到存储（MinIO 或本地）
            FileStorageService.FileMetadata fileMetadata = fileStorageService.store(file);

            // 2️⃣ 使用 Tika 解析文件内容
            String contentType = file.getContentType();
            String text = documentParser.extractFromInputStream(file.getInputStream(), contentType);
            String docTitle = (title != null && !title.isBlank()) ? title : file.getOriginalFilename();
            String detectedType = contentType != null ? contentType : "application/octet-stream";

            // 3️⃣ 创建文档实体（包含文件路径和大小）
            DocumentEntity doc = knowledgeService.uploadDocument(
                    knowledgeBaseId,
                    docTitle,
                    text,
                    detectedType,
                    fileMetadata.getFilePath(),
                    fileMetadata.getFileSize());
            return Result.success(doc);
        } catch (Exception e) {
            log.error("File upload and parse failed", e);
            return com.meowflow.common.result.Result.error(
                    com.meowflow.common.result.ResultCode.INTERNAL_ERROR,
                    "文件解析失败: " + e.getMessage());
        }
    }

    @GetMapping("/documents")
    @Operation(summary = "列出知识库文档", description = "按知识库 ID 列出文档")
    public Result<List<DocumentEntity>> listDocuments(@RequestParam Long knowledgeBaseId) {
        return Result.success(knowledgeService.listDocuments(knowledgeBaseId));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "获取文档", description = "根据 ID 获取文档信息")
    public Result<DocumentEntity> getDocument(@PathVariable Long id) {
        return Result.success(knowledgeService.getDocument(id));
    }

    @GetMapping("/documents/{id}/chunks")
    @Operation(summary = "获取文档分块", description = "获取文档的所有分块")
    public Result<List<ChunkEntity>> getDocumentChunks(@PathVariable Long id) {
        return Result.success(knowledgeService.getDocumentChunks(id));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档", description = "删除指定的文档及其分块")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return Result.success();
    }

    @GetMapping("/documents/{id}/download")
    @Operation(summary = "下载原文件", description = "下载文档的原始文件（如果已归档）")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadDocument(@PathVariable Long id) {
        DocumentEntity doc = knowledgeService.getDocument(id);
        if (doc == null || doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        String filePath = doc.getFilePath();

        // MinIO 模式：重定向到预签名 URL，由浏览器直接从对象存储下载
        if (fileStorageService.isMinioMode()) {
            try {
                String downloadUrl = fileStorageService.getDownloadUrl(filePath);
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(java.net.URI.create(downloadUrl))
                        .build();
            } catch (Exception e) {
                log.error("Generate download URL failed for document id={}", id, e);
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // 本地模式：没有可重定向的地址，直接回传文件内容
        try {
            org.springframework.core.io.InputStreamResource resource =
                    new org.springframework.core.io.InputStreamResource(fileStorageService.openLocalStream(filePath));
            // 归档文件名是 UUID（不含原始文件名），用文档标题 + 原扩展名作为下载名
            String extension = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".")) : "";
            String downloadName = String.valueOf(doc.getId()) + extension;
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            org.springframework.http.ContentDisposition.attachment()
                                    .filename(downloadName, java.nio.charset.StandardCharsets.UTF_8)
                                    .build().toString())
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("Read archived file failed for document id={}, filePath={}", id, filePath, e);
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/search")
    @Operation(summary = "知识检索", description = "在知识库中检索相关内容")
    public Result<List<com.meowflow.infra.search.SearchChannel.SearchResult>> search(
            @RequestBody SearchRequest request) {
        List<com.meowflow.infra.search.SearchChannel.SearchResult> results =
                searchService.search(request.getQuery(), request.getKnowledgeBaseId(), request.getTopK());
        return Result.success(results);
    }

    @PostMapping("/search/context")
    @Operation(summary = "构建检索上下文", description = "检索并构建 AI 上下文")
    public Result<String> buildSearchContext(@RequestBody SearchRequest request) {
        String context = searchService.buildSearchContext(
                request.getQuery(),
                request.getKnowledgeBaseId(),
                request.getTopK(),
                request.getMaxLength());
        return Result.success(context);
    }

    @Data
    public static class CreateKnowledgeBaseRequest {
        @Parameter(description = "知识库名称")
        private String name;

        @Parameter(description = "知识库描述")
        private String description;

        @Parameter(description = "向量库类型: milvus/pgvector/redis/elasticsearch")
        private String vectorStoreType;

        @Parameter(description = "向量维度")
        private Integer dimension;
    }

    @Data
    public static class UploadDocumentRequest {
        @Parameter(description = "知识库 ID")
        private Long knowledgeBaseId;

        @Parameter(description = "文档标题")
        private String title;

        @Parameter(description = "文档内容")
        private String content;

        @Parameter(description = "内容类型: text/plain/text/html/application/json等")
        private String contentType;
    }

    @Data
    public static class SearchRequest {
        @Parameter(description = "查询文本")
        private String query;

        @Parameter(description = "知识库 ID")
        private Long knowledgeBaseId;

        @Parameter(description = "返回数量")
        private int topK = 10;

        @Parameter(description = "最大上下文长度")
        private int maxLength = 4000;
    }
}
