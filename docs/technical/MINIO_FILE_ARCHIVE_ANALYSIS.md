# MinIO 原文件归档方案分析

> 分析时间：2026-09-18  
> 分析范围：知识库文件上传链路、MinIO 集成现状、原文件归档需求

---

## 一、当前上传链路分析

### 1.1 现状

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    当前知识库文件上传链路                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  前端上传文件                                                            │
│       │                                                                │
│       ▼                                                                │
│  POST /api/infra/knowledge/documents/upload                            │
│       │                                                                │
│       ▼                                                                │
│  KnowledgeController.uploadDocumentFile()                              │
│       │                                                                │
│       ├─ 接收 MultipartFile                                            │
│       ├─ 使用 Apache Tika 提取正文                                     │
│       └─ 调用 KnowledgeService.uploadDocument(text)                    │
│              │                                                        │
│              ▼                                                        │
│          DocumentEntity 入库                                           │
│              │                                                        │
│              ├─ title: 文件名                                         │
│              ├─ content: 提取的纯文本                                  │
│              ├─ contentType: MIME 类型                                │
│              ├─ filePath: NULL ❌                                     │
│              └─ fileSize: NULL ❌                                     │
│              │                                                        │
│              ▼                                                        │
│          DocumentParser.parseAndChunk(content)                         │
│              │                                                        │
│              ▼                                                        │
│          分块 → 向量化 → PGVector 存储                                 │
│              │                                                        │
│              ▼                                                        │
│          返回 DocumentEntity                                           │
│                                                                          │
│  ⚠️  原始文件丢失：Tika 提取后，原文件未保存                            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**关键问题**：
- ❌ 原始文件未保存到任何存储（本地/MinIO）
- ❌ `DocumentEntity.filePath` 字段为空
- ❌ `DocumentEntity.fileSize` 字段为空
- ❌ 用户无法下载原文件
- ❌ 无法重新解析（如果 Tika 解析错误）
- ❌ 无法审计原始文件内容

### 1.2 数据库表结构

**document 表**：
```sql
CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,                    -- Tika 提取的纯文本
    content_type VARCHAR(100),       -- MIME 类型
    file_size BIGINT,                -- ❌ 当前未填充
    file_path VARCHAR(500),          -- ❌ 当前未填充（设计预留）
    knowledge_base_id BIGINT,
    chunk_count INTEGER,
    status VARCHAR(20),
    error_message TEXT,
    metadata TEXT,
    create_by VARCHAR(64),
    create_time TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP,
    deleted BOOLEAN
);
```

**结论**：`file_path` 和 `file_size` 字段**已预留**，但未使用。

---

## 二、MinIO 集成现状

### 2.1 当前集成情况

| 模块 | MinIO 依赖 | 集成代码 | 配置 | 状态 |
|------|-----------|---------|------|------|
| `meowflow-workflow` | ✅ 已引入 `minio` | ✅ `FileUploadController` | ✅ `WorkflowProperties.File` | 已实现 |
| `meowflow-infra` | ❌ 未引入 | ❌ 无 | ❌ 无 | **未集成** |

### 2.2 workflow 模块的 MinIO 实现

**FileUploadController.java**（workflow 模块）：
```java
@RestController
@RequestMapping("/api/file")
public class FileUploadController {
    
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        // ...
        if ("minio".equalsIgnoreCase(workflowProperties.getFile().getStorageType())) {
            storeToMinio(storedName, file, reference);
        } else {
            // 本地文件系统存储
            Path target = dir.resolve(storedName);
            file.transferTo(target.toFile());
        }
        return Result.success(reference);
    }
    
    private void storeToMinio(String objectName, MultipartFile file, Map<String, Object> reference) {
        // 使用 MinioClient 上传
        minioClient.putObject(/* ... */);
        reference.put("path", bucket + "/" + objectName);
        reference.put("url", endpoint + "/" + bucket + "/" + objectName);
        reference.put("storage", "minio");
    }
}
```

**WorkflowProperties.File**：
```java
@Data
public static class File {
    private String storageDir = "./workflow-files";     // 本地目录
    private String storageType = "local";               // local | minio
    private String minioEndpoint = "http://localhost:9000";
    private String minioAccessKey = "minioadmin";
    private String minioSecretKey = "minioadmin";
    private String minioBucket = "meowflow-files";
    private long maxSize = 20L * 1024 * 1024;
}
```

**结论**：
- ✅ workflow 模块有完整的 MinIO 客户端封装
- ✅ 支持本地 / MinIO 双模式切换
- ❌ infra 模块**未复用**这套逻辑

---

## 三、是否需要 MinIO 原文件归档

### 3.1 需求分析

| 场景 | 是否需要原文件 | 优先级 |
|------|---------------|--------|
| **原文件下载** | ✅ 需要 | P0 |
| **审计溯源** | ✅ 需要 | P1 |
| **重新解析** | ✅ 需要（Tika 升级/参数调整） | P1 |
| **格式保留** | ✅ 需要（PDF 样式、DOCX 格式） | P2 |
| **法律合规** | ✅ 需要（原始证据保留） | P1 |
| **纯向量检索** | ❌ 不需要 | - |

### 3.2 PRD 要求

从 `docs/PRD.md` 和 `docs/technical/modules/infra.md` 中可以看到：

```markdown
## 六、知识库模块

### 6.1 核心组件

Document: 文档
├── id, title, content, contentType
├── knowledgeBaseId, chunkCount
└── status, errorMessage
```

**PRD 中未明确要求**：
- 原文件下载功能
- 原文件归档策略

但从**业务逻辑**和**数据完整性**角度：
- ✅ `file_path` 字段已预留，说明设计时**有考虑**
- ✅ 对象存储（MinIO）已在技术架构中规划
- ✅ 完整的文档管理应包含原文件追溯能力

### 3.3 建议决策

**推荐方案**：**实现 MinIO 原文件归档**

**理由**：
1. **数据完整性**：仅保留 Tika 提取文本，丢失原始格式、图片、表格
2. **审计合规**：企业场景需要原文件溯源（尤其是法律、财务领域）
3. **容错能力**：Tika 解析失败时，可保留原文件供人工处理
4. **成本可控**：MinIO 对象存储成本低，不会显著增加系统负担
5. **架构完整**：与 workflow 模块的文件上传形成统一存储策略

---

## 四、实现方案

### 4.1 方案设计

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    改进后的上传链路                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  前端上传文件                                                            │
│       │                                                                │
│       ▼                                                                │
│  POST /api/infra/knowledge/documents/upload                            │
│       │                                                                │
│       ▼                                                                │
│  KnowledgeController.uploadDocumentFile()                              │
│       │                                                                │
│       ├─ 1️⃣ 保存原文件到 MinIO                                        │
│       │    └─ 返回：filePath, fileSize, objectUrl                     │
│       │                                                                │
│       ├─ 2️⃣ 使用 Apache Tika 提取正文                                 │
│       │    └─ 返回：纯文本 content                                    │
│       │                                                                │
│       └─ 3️⃣ 调用 KnowledgeService.uploadDocument()                   │
│              │                                                        │
│              ▼                                                        │
│          DocumentEntity 入库（完整字段）                               │
│              ├─ title: 文件名                                         │
│              ├─ content: 提取的纯文本                                  │
│              ├─ contentType: MIME 类型                                │
│              ├─ filePath: MinIO 对象路径 ✅                           │
│              ├─ fileSize: 文件大小（字节）✅                          │
│              └─ metadata: {"minioUrl": "..."} ✅                      │
│              │                                                        │
│              ▼                                                        │
│          分块 → 向量化 → PGVector 存储                                 │
│              │                                                        │
│              ▼                                                        │
│          返回 DocumentEntity（含下载链接）                             │
│                                                                          │
│  ✅ 原始文件已归档，可随时下载                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 新增 API

**下载原文件**：
```
GET /api/infra/knowledge/documents/{id}/download
```

**返回**：
- 302 重定向到 MinIO presigned URL（临时访问链接）
- 或直接 Stream 文件内容

### 4.3 配置增强

**application.yml**（infra 模块新增）：
```yaml
meowflow:
  knowledge:
    storage:
      type: minio                      # local | minio
      local-dir: ./knowledge-files
      minio:
        endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
        access-key: ${MINIO_ACCESS_KEY:minioadmin}
        secret-key: ${MINIO_SECRET_KEY:minioadmin}
        bucket: meowflow-knowledge      # 单独的 bucket
        auto-create-bucket: true
```

### 4.4 实现步骤

#### 步骤 1：引入 MinIO 依赖（infra 模块）

**meowflow-infra/pom.xml**：
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
</dependency>
```

#### 步骤 2：新增配置类

**InfraProperties.java**：
```java
@Data
@Component
@ConfigurationProperties(prefix = "meowflow.knowledge")
public class InfraProperties {
    private Storage storage = new Storage();
    
    @Data
    public static class Storage {
        private String type = "local";          // local | minio
        private String localDir = "./knowledge-files";
        private Minio minio = new Minio();
        
        @Data
        public static class Minio {
            private String endpoint = "http://localhost:9000";
            private String accessKey = "minioadmin";
            private String secretKey = "minioadmin";
            private String bucket = "meowflow-knowledge";
            private boolean autoCreateBucket = true;
        }
    }
}
```

#### 步骤 3：新增文件存储服务

**FileStorageService.java**：
```java
@Service
@Slf4j
public class FileStorageService {
    
    private final InfraProperties infraProperties;
    private MinioClient minioClient;
    
    // 存储文件
    public FileMetadata store(MultipartFile file) throws Exception {
        String objectName = generateObjectName(file.getOriginalFilename());
        long fileSize = file.getSize();
        
        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            return storeToMinio(objectName, file, fileSize);
        } else {
            return storeToLocal(objectName, file, fileSize);
        }
    }
    
    // 生成预签名下载链接
    public String getDownloadUrl(String filePath) throws Exception {
        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(infraProperties.getStorage().getMinio().getBucket())
                    .object(filePath)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } else {
            // 本地文件返回相对路径或走文件流接口
            return "/api/infra/knowledge/documents/download/" + filePath;
        }
    }
    
    // 删除文件
    public void delete(String filePath) throws Exception {
        // ...
    }
    
    private FileMetadata storeToMinio(String objectName, MultipartFile file, long fileSize) {
        // MinIO 上传逻辑
    }
    
    private FileMetadata storeToLocal(String objectName, MultipartFile file, long fileSize) {
        // 本地文件系统存储
    }
    
    private String generateObjectName(String originalName) {
        return UUID.randomUUID().toString().replace("-", "") + 
               extractExtension(originalName);
    }
    
    @Data
    public static class FileMetadata {
        private String filePath;        // 对象路径
        private long fileSize;          // 文件大小
        private String downloadUrl;     // 下载链接（可选）
    }
}
```

#### 步骤 4：修改 KnowledgeController

**KnowledgeController.java**：
```java
@PostMapping(value = "/documents/upload", consumes = "multipart/form-data")
public Result<DocumentEntity> uploadDocumentFile(
        @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam("file") MultipartFile file) {
    try {
        // 1️⃣ 先保存原文件到 MinIO
        FileStorageService.FileMetadata fileMetadata = fileStorageService.store(file);
        
        // 2️⃣ 使用 Tika 解析文件内容
        String contentType = file.getContentType();
        String text = documentParser.extractFromInputStream(file.getInputStream(), contentType);
        String docTitle = (title != null && !title.isBlank()) ? title : file.getOriginalFilename();
        
        // 3️⃣ 创建文档实体（包含文件路径）
        DocumentEntity doc = knowledgeService.uploadDocumentWithFile(
                knowledgeBaseId, 
                docTitle, 
                text, 
                contentType,
                fileMetadata.getFilePath(),
                fileMetadata.getFileSize());
        
        return Result.success(doc);
    } catch (Exception e) {
        log.error("File upload and parse failed", e);
        return Result.error(ResultCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
    }
}

@GetMapping("/documents/{id}/download")
public ResponseEntity<Void> downloadDocument(@PathVariable Long id) {
    DocumentEntity doc = knowledgeService.getDocument(id);
    if (doc == null || doc.getFilePath() == null) {
        return ResponseEntity.notFound().build();
    }
    
    try {
        String downloadUrl = fileStorageService.getDownloadUrl(doc.getFilePath());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    } catch (Exception e) {
        log.error("Generate download URL failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

#### 步骤 5：修改 KnowledgeService

**KnowledgeService.java**：
```java
public DocumentEntity uploadDocumentWithFile(
        Long knowledgeBaseId, 
        String title, 
        String content, 
        String contentType,
        String filePath,
        Long fileSize) {
    
    KnowledgeBaseEntity kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
    if (kb == null || !"active".equals(kb.getStatus())) {
        throw new RuntimeException("知识库不存在或未激活");
    }

    DocumentEntity doc = new DocumentEntity();
    doc.setTitle(title);
    doc.setContent(content);
    doc.setContentType(contentType);
    doc.setFilePath(filePath);          // ✅ 新增
    doc.setFileSize(fileSize);          // ✅ 新增
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
```

#### 步骤 6：前端增加下载按钮

**knowledge/index.vue**：
```vue
<el-table-column label="操作" width="180" fixed="right">
  <template #default="scope">
    <el-button 
      v-if="scope.row.filePath" 
      size="small" 
      @click="handleDownload(scope.row)">
      下载原文件
    </el-button>
    <el-button 
      size="small" 
      type="danger" 
      @click="handleDeleteDoc(scope.row)">
      删除
    </el-button>
  </template>
</el-table-column>

<script setup>
function handleDownload(doc: Document) {
  window.open(`/api/infra/knowledge/documents/${doc.id}/download`, '_blank');
}
</script>
```

---

## 五、成本与权衡

### 5.1 存储成本

| 场景 | 文件量 | 平均大小 | 存储成本（MinIO） |
|------|--------|---------|------------------|
| 小团队 | 1000 个文档 | 500KB | ~500MB（几乎可忽略） |
| 中型企业 | 10000 个文档 | 1MB | ~10GB（<$1/月） |
| 大型企业 | 100000 个文档 | 2MB | ~200GB（~$5/月） |

**结论**：存储成本**极低**，不应成为阻碍。

### 5.2 性能影响

| 操作 | 当前耗时 | 增加后耗时 | 增量 |
|------|---------|-----------|------|
| 上传 + 解析 | ~2s | ~2.5s | +0.5s（MinIO 写入） |
| 向量检索 | ~100ms | ~100ms | 无影响 |
| 下载原文件 | N/A | ~1s | 新增功能 |

**结论**：性能影响**可接受**。

### 5.3 实现工作量

| 任务 | 预估工时 |
|------|---------|
| 1. 引入 MinIO 依赖 | 0.5h |
| 2. 新增配置类 | 1h |
| 3. 实现 FileStorageService | 3h |
| 4. 修改 Controller/Service | 2h |
| 5. 前端下载按钮 | 1h |
| 6. 测试（本地/MinIO 双模式） | 2h |
| **总计** | **9.5h（约 1.5 工作日）** |

---

## 六、决策建议

### 6.1 立即实施（推荐）✅

**理由**：
1. 数据完整性是基础需求，不应妥协
2. 实现工作量小（1.5 工作日）
3. 架构已预留字段，改动成本低
4. 与 workflow 模块形成统一存储策略

### 6.2 暂不实施（不推荐）❌

**条件**：
- PRD 明确不需要原文件下载
- 用户仅需要向量检索能力
- 存储预算极度受限

**风险**：
- ❌ 数据不完整，无法溯源
- ❌ Tika 解析错误时无法恢复
- ❌ 后期实施成本更高（存量文件无原文件）

---

## 七、总结

### 7.1 核心结论

| 维度 | 结论 |
|------|------|
| **是否需要？** | ✅ **强烈推荐**实施 |
| **技术可行性** | ✅ 高（workflow 模块已有参考实现） |
| **实现成本** | ✅ 低（1.5 工作日） |
| **存储成本** | ✅ 极低（<$5/月） |
| **性能影响** | ✅ 可忽略（+0.5s 上传耗时） |
| **业务价值** | ✅ 高（审计、溯源、容错） |

### 7.2 下一步行动

**如果决定实施**：
1. 确认 MinIO 部署（docker-compose 或独立服务）
2. 按照 **4.4 实现步骤** 逐步完成
3. 双模式测试（local / minio）
4. 前端 UI 增强（下载按钮、文件图标）

**如果暂不实施**：
- 在 PRD 中明确记录"原文件不归档"的决策理由
- 告知用户"文档仅保留文本内容，原文件不可下载"

---

**建议**：立即实施 MinIO 原文件归档，完善知识库模块的数据完整性。
