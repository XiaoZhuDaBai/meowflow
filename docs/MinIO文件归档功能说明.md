# MinIO 原文件归档功能实现说明

## 功能概述

为知识库系统新增了原文件归档功能，支持将用户上传的文档原文件存储到 MinIO 或本地文件系统，并提供下载接口。

## 实现架构

### 1. 后端实现

#### 1.1 依赖引入

在 `meowflow-infra/pom.xml` 中新增 MinIO 客户端依赖：

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

#### 1.2 配置管理

新增配置类 `InfraProperties`（位置：`com.meowflow.infra.config.InfraProperties`）：

```java
@Data
@Component
@ConfigurationProperties(prefix = "meowflow.knowledge")
public class InfraProperties {
    private Storage storage = new Storage();
    
    @Data
    public static class Storage {
        private String type = "local";           // local | minio
        private String localDir = "./knowledge-files";
        private MinioConfig minio = new MinioConfig();
    }
    
    @Data
    public static class MinioConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private boolean autoCreateBucket = true;
    }
}
```

配置示例（`application.yml`）：

```yaml
meowflow:
  knowledge:
    storage:
      type: ${KNOWLEDGE_STORAGE_TYPE:local}  # local | minio
      local-dir: ${KNOWLEDGE_LOCAL_DIR:./knowledge-files}
      minio:
        endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
        access-key: ${MINIO_ACCESS_KEY:minioadmin}
        secret-key: ${MINIO_SECRET_KEY:minioadmin}
        bucket: ${MINIO_KNOWLEDGE_BUCKET:meowflow-knowledge}
        auto-create-bucket: true
```

#### 1.3 文件存储服务

新增 `FileStorageService`（位置：`com.meowflow.infra.service.FileStorageService`）：

**核心方法：**
- `saveFile(InputStream, String, String)`: 保存文件，返回存储路径
- `getFile(String)`: 获取文件输入流
- `getFileUrl(String, int)`: 生成预签名下载 URL（MinIO 模式）或本地下载 URL
- `deleteFile(String)`: 删除文件
- `getFileSize(String)`: 获取文件大小

**存储策略：**
- **Local 模式**：文件存储到本地目录（默认 `./knowledge-files`）
- **MinIO 模式**：文件存储到 MinIO 对象存储，支持自动创建 Bucket

#### 1.4 数据库扩展

在 `Document` 实体类中新增字段：

```java
@TableField("file_path")
private String filePath;     // 存储路径（MinIO key 或本地相对路径）

@TableField("file_size")
private Long fileSize;       // 文件大小（字节）
```

对应的数据库字段由 Flyway 迁移 `V14__schema_reconciliation.sql` 统一创建
（`document.file_path VARCHAR(512)` / `document.file_size BIGINT`），**无需手工执行 SQL**：

```sql
-- 仅作说明，实际由 V14__schema_reconciliation.sql 创建，不要手工执行
CREATE TABLE IF NOT EXISTS document (
    ...
    file_size         BIGINT,
    file_path         VARCHAR(512),
    ...
);
```

> 注意：早期版本曾给出针对 `infra_document` 表的 `ALTER TABLE` 脚本，该表并不存在
> （真实表名是 `document`），且版本号 `V1.1__*` 与 `V1__init_schema.sql` 冲突会导致
> Flyway 校验失败、服务起不来。该脚本已删除，请勿再引入。

#### 1.5 控制器与服务层

**KnowledgeController 新增接口：**

```java
@GetMapping("/documents/{id}/download")
public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
    // MinIO 模式：302 重定向到 7 天有效的预签名 URL
    // 本地模式：直接以流返回文件（本地没有可重定向的地址，
    //           早期实现返回文件名会导致 302 指向不存在的端点）
}
```

**KnowledgeService 更新：**

`uploadDocument` 方法扩展了参数签名，支持传入 `filePath` 和 `fileSize`：

```java
public Document uploadDocument(
    Long knowledgeBaseId, 
    String title, 
    String content,
    String contentType, 
    String filePath,      // 新增
    Long fileSize         // 新增
)
```

在 `uploadDocumentFile` 中调用 `FileStorageService` 保存文件：

```java
String storedPath = fileStorageService.saveFile(
    file.getInputStream(), 
    fileName, 
    file.getContentType()
);
long size = file.getSize();

// 传递给 uploadDocument
uploadDocument(knowledgeBaseId, title, content, contentType, storedPath, size);
```

删除文档时自动清理文件：

```java
public void deleteDocument(Long id) {
    Document doc = getDocument(id);
    if (doc.getFilePath() != null) {
        fileStorageService.deleteFile(doc.getFilePath());
    }
    // ... 继续删除向量和数据库记录
}
```

### 2. 前端实现

#### 2.1 类型定义

在 `Document` 接口中新增字段（`frontend/meowflow-ui/src/api/knowledge.ts`）：

```typescript
export interface Document {
  // ... 其他字段
  filePath?: string;
  fileSize?: number;
}
```

#### 2.2 API 方法

新增下载 URL 生成方法：

```typescript
export const knowledgeApi = {
  // ...
  
  /**
   * 下载文档原文件（如果已归档）
   * 后端返回 302 重定向到实际下载地址
   */
  getDownloadUrl(id: number | string): string {
    return url(`/documents/${id}/download`);
  },
}
```

#### 2.3 UI 组件

在文档列表表格中新增两列（`frontend/meowflow-ui/src/views/knowledge/index.vue`）：

```vue
<!-- 文件大小列 -->
<el-table-column label="文件大小" width="100">
  <template #default="scope">
    <span v-if="scope.row.fileSize">{{ formatFileSize(scope.row.fileSize) }}</span>
    <span v-else style="color: var(--text-tertiary)">-</span>
  </template>
</el-table-column>

<!-- 操作列中新增下载按钮 -->
<el-table-column label="操作" width="180" fixed="right">
  <template #default="scope">
    <el-button
      v-if="scope.row.filePath"
      size="small"
      @click="handleDownload(scope.row as Document)"
    >
      下载原文件
    </el-button>
    <el-button size="small" type="danger" @click="handleDeleteDoc(scope.row as Document)">
      删除
    </el-button>
  </template>
</el-table-column>
```

下载逻辑：

```typescript
function handleDownload(row: Document) {
  window.open(knowledgeApi.getDownloadUrl(row.id), '_blank');
}

function formatFileSize(size?: number): string {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}
```

## 使用说明

### 本地模式（默认）

无需额外配置，文件自动存储到 `./knowledge-files` 目录。

### MinIO 模式

1. 启动 MinIO 服务：

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

2. 配置环境变量：

```bash
export KNOWLEDGE_STORAGE_TYPE=minio
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_KNOWLEDGE_BUCKET=meowflow-knowledge
```

3. 重启应用即可。

### 文件上传与下载流程

1. **上传**：用户通过前端上传文件 → 后端通过 Tika 解析内容 → 原文件存储到 MinIO/本地 → 记录 `filePath` 和 `fileSize`
2. **下载**：用户点击"下载原文件"按钮 → 调用 `/documents/{id}/download`：
   - MinIO 模式：后端 302 重定向到预签名 URL，浏览器直接从对象存储下载
   - 本地模式：后端以 `octet-stream` 流直接返回文件内容

## 测试验证

### 后端单元测试

所有测试已通过：

```bash
& "D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd" \
  -f "D:\Code\喵流\backend\meowflow\pom.xml" \
  -pl meowflow-infra test -Dtest=KnowledgeServiceTest

# 结果：Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

### 前端构建验证

```bash
cd frontend/meowflow-ui
npm run build

# 结果：✓ built in 14.74s
```

## 数据库字段

`document.file_path` 与 `document.file_size` 由 Flyway 迁移 `V14__schema_reconciliation.sql`
创建，服务启动时自动补齐，**不需要手工执行任何 SQL**。

若确认某个库缺少这两个字段，应新增一个 Flyway 迁移文件（放在
`meowflow-common/src/main/resources/db/migration/`，版本号接在当前最大值之后），
而不是直接在生产库上手工 `ALTER TABLE` —— 否则 Flyway 校验会失败，服务无法启动。

## 技术要点

1. **存储抽象**：通过策略模式（`storage.type` 配置）支持本地和 MinIO 两种存储方式
2. **预签名 URL**：MinIO 模式下生成有效期 7 天的预签名下载链接，无需暴露文件服务器
3. **自动清理**：删除文档时自动清理对应的原文件
4. **兼容性**：老数据的 `filePath` 为空时，下载按钮不显示，不影响现有功能
5. **安全性**：文件路径以 UUID 命名，避免路径遍历攻击

## 文件清单

### 后端新增/修改文件

- `meowflow-infra/pom.xml` - 新增 MinIO 依赖
- `com.meowflow.infra.config.InfraProperties` - 新增配置类
- `com.meowflow.infra.service.FileStorageService` - 新增文件存储服务
- `com.meowflow.infra.controller.KnowledgeController` - 新增下载接口
- `com.meowflow.infra.service.KnowledgeService` - 扩展上传/删除逻辑
- `com.meowflow.infra.entity.Document` - 新增 `filePath` 和 `fileSize` 字段
- `application.yml` - 新增存储配置

### 前端修改文件

- `src/api/knowledge.ts` - 新增 `getDownloadUrl` 方法，扩展 `Document` 接口
- `src/views/knowledge/index.vue` - 新增文件大小列和下载按钮

## 总结

该功能已完整实现并通过测试验证，支持本地和 MinIO 两种存储模式，前后端联调即可使用。建议先在本地模式测试，稳定后再切换到 MinIO 生产环境。
