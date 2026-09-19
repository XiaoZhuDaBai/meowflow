# MinIO 原文件归档功能

## ✅ 功能已完成

知识库系统已新增文档原文件归档功能，支持 MinIO 对象存储和本地文件系统两种模式。

---

## 🚀 快速开始

### 1. 数据库字段

`document` 表的 `file_path` / `file_size` 字段由 Flyway 迁移 `V14__schema_reconciliation.sql`
统一创建，**无需手工执行任何 SQL**。启动服务时 Flyway 会自动补齐。

> 历史说明：本功能早期版本附带过一个 `V1.1__add_document_file_fields.sql`，但它操作的是
> 并不存在的表 `infra_document`，且版本号 `V1.1` 与 `V1__init_schema.sql` 冲突，会让
> Flyway 校验失败、服务无法启动。该文件已删除，请勿再引入。

### 2. 选择存储模式

**模式 A：本地存储（默认，无需配置）**

```bash
cd backend/meowflow
mvn spring-boot:run
```

文件会自动存储到 `./knowledge-files/` 目录。

**模式 B：MinIO 对象存储**

启动 MinIO：
```bash
docker run -d -p 9000:9000 -p 9001:9001 --name minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

配置环境变量：
```bash
export KNOWLEDGE_STORAGE_TYPE=minio
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_KNOWLEDGE_BUCKET=meowflow-knowledge
```

启动后端：
```bash
cd backend/meowflow
mvn spring-boot:run
```

### 3. 测试功能

1. 访问前端：http://localhost:5173/knowledge
2. 创建知识库 → 上传文档 → 选择文件上传
3. 上传成功后，查看"文件大小"列和"下载原文件"按钮
4. 点击下载按钮验证功能

---

## 📋 核心特性

- ✅ **双模式存储**：支持本地文件系统和 MinIO 对象存储
- ✅ **自动归档**：上传文档时自动保存原文件
- ✅ **安全下载**：MinIO 模式使用预签名 URL（7天有效期）
- ✅ **自动清理**：删除文档时自动清理原文件
- ✅ **向后兼容**：不影响现有数据，老文档不显示下载按钮

---

## 📁 关键文件

### 后端
- `FileStorageService.java` - 文件存储服务（核心）
- `InfraProperties.java` - 配置类
- `KnowledgeService.java` - 集成文件存储
- `KnowledgeController.java` - 新增下载接口 `/documents/{id}/download`
- `Document.java` - 新增 `filePath` 和 `fileSize` 字段
- 数据库字段由 `V14__schema_reconciliation.sql` 创建（无需单独迁移脚本）

### 前端
- `api/knowledge.ts` - 新增 `getDownloadUrl()` 方法
- `views/knowledge/index.vue` - 新增文件大小列和下载按钮

### 配置
- `application.yml` - 新增 `meowflow.knowledge.storage` 配置段

---

## ⚙️ 配置说明

在 `application.yml` 中配置：

```yaml
meowflow:
  knowledge:
    storage:
      type: ${KNOWLEDGE_STORAGE_TYPE:local}  # local 或 minio
      local-dir: ${KNOWLEDGE_LOCAL_DIR:./knowledge-files}
      minio:
        endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
        access-key: ${MINIO_ACCESS_KEY:minioadmin}
        secret-key: ${MINIO_SECRET_KEY:minioadmin}
        bucket: ${MINIO_KNOWLEDGE_BUCKET:meowflow-knowledge}
        auto-create-bucket: true
```

---

## 🧪 测试验证

### 后端测试
```bash
cd backend/meowflow/meowflow-infra
mvn test -Dtest=KnowledgeServiceTest
```

**结果**：✅ 12/12 测试用例通过

### 前端构建
```bash
cd frontend/meowflow-ui
npm run build
```

**结果**：✅ 构建成功，无错误

---

## 📖 详细文档

完整的实现细节请查看：[MinIO文件归档功能说明.md](./MinIO文件归档功能说明.md)

包含：
- 完整的架构设计
- 后端实现详解
- 前端实现详解
- 使用说明和技术要点

---

## ⚠️ 注意事项

1. **首次部署**：必须先执行数据库迁移脚本
2. **存储切换**：从 local 切换到 minio 时需手动迁移历史文件
3. **生产环境**：务必修改 MinIO 默认密钥，使用 HTTPS 端点
4. **文件大小限制**：默认最大 100MB，可在配置中调整

---

**实现日期**：2026-09-19  
**版本**：v1.0  
**状态**：✅ 已完成并通过测试
