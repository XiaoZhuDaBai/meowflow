# 环境变量配置指南 (Environment Setup Guide)

> 本文档汇总 MeowFlow 在「真实环境 · 关闭 Mock」模式下完整运行所需的全部环境变量，并对照源代码说明各变量的实际消费点。开发者在本地复现 LLM 对话、知识库检索、通知发送等链路时必须参考此文。

---

## 一、加载机制

MeowFlow 有 **3 层环境变量加载顺序**（覆盖关系 — 后者覆盖前者）：

| 优先级 | 来源 | 加载方式 |
|------|------|---------|
| 最低 | `application.yml` 中的硬编码默认值 | 编译进 JAR |
| 中 | 测试 profile (`application-integration.yml`) | `mvn test` 时自动激活 |
| 高 | 操作系统环境变量 / `.env` 文件 | Docker Compose 自动加载 `.env`，Java 通过系统环境读取 |

### 加载 `.env` 的方式

```powershell
# PowerShell: 把 .env 内容导入到当前会话
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
    }
}

# 或者用 docker-compose --env-file
docker compose --env-file .env.local -f docker-compose.dev.yml up -d
```

---

## 二、变量分类速查表

### ✅ 1. 基础设施 (Docker Compose 自带)

| 变量 | 默认值 | 消费位置 | 是否必需 |
|------|--------|---------|---------|
| `POSTGRES_HOST` | localhost | `datasource.url` (所有模块) | ✅ |
| `POSTGRES_PORT` | 5432 | 同上 | ✅ |
| `POSTGRES_DB` | meowflow | 同上 | ✅ |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | meowflow / meowflow123 | 同上 | ✅ |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | `spring.data.redis.*` | ✅ |
| `RABBITMQ_HOST` / `PORT` / `USER` / `PASSWORD` | localhost:5672 / guest | spring-rabbitmq + meowflow-mq | ✅ (executor) |
| `MINIO_ENDPOINT` / `ACCESS_KEY` / `SECRET_KEY` / `BUCKET` | localhost:9000 / minioadmin / meowflow-files | — | 可选 |
| `NACOS_HOST` / `PORT` | localhost:8848 | `spring.cloud.nacos.discovery` | ⚠️ 单机 false 可缺 |
| `SPRING_PROFILES_ACTIVE` | dev | 所有 `application.yml` | ✅ |

### ✅ 2. App 通用

| 变量 | 用途 | 消费位置 |
|------|------|---------|
| `CRYPTO_AES_KEY` | AES-256 加密敏感字段 | `meowflow.crypto.aes-key` (common-defaults.yml) |
| `SNOWFLAKE_WORKER_ID` / `SNOWFLAKE_DATACENTER_ID` | Snowflake 分布式 ID | SnowflakeIdGenerator |
| `MEOWFLOW_CRYPTO_AES_KEY` | 覆盖 common-defaults 中默认的 AES key | 同上 |

### ⚠️ 3. AI / LLM (缺失 → ChatClientController 报空列表)

所有变量对应 `ChatClientFactory.init()` 的注册分支：

| 变量 | 默认 base URL | 用途 | 缺失时表现 |
|------|--------------|------|----------|
| `meowflow.infra.chat.openai.api-key` | https://api.openai.com/v1 | OpenAI Chat | `models` 接口无 openai 项 |
| `meowflow.infra.chat.openai.base-url` | — | OpenAI 兼容 endpoint | — |
| `meowflow.infra.chat.openai.model` | gpt-4o-mini | OpenAI 模型名 | — |
| `meowflow.infra.chat.anthropic.api-key` | https://api.anthropic.com | Anthropic Claude | `models` 接口无 anthropic 项 |
| `meowflow.infra.chat.anthropic.model` | claude-3-5-sonnet-20240620 | Claude 模型名 | — |
| `meowflow.infra.chat.ali.api-key` | dashscope | 通义千问 (DashScope) | `models` 接口无 ali 项 |
| `meowflow.infra.chat.baidu.api-key` + `baidu.secret-key` | 文心一言 access token | `models` 接口无 baidu 项 |

**消费代码**：

```27:50:backend/meowflow/meowflow-infra/src/main/java/com/meowflow/infra/service/ChatClientFactory.java
@Value("${meowflow.infra.chat.openai.base-url:https://api.openai.com}")
private String openaiBaseUrl;
@Value("${meowflow.infra.chat.openai.api-key:}")
private String openaiApiKey;
@Value("${meowflow.infra.chat.openai.model:gpt-4o-mini}")
private String openaiModel;
...
```

> 💡 **本地零成本替代方案**：用 [Ollama](https://ollama.com) 启动 `qwen2.5:7b` 等模型，提供 OpenAI 兼容接口：
> ```ini
> meowflow.infra.chat.openai.base-url=http://host.docker.internal:11434/v1
> meowflow.infra.chat.openai.api-key=ollama
> meowflow.infra.chat.openai.model=qwen2.5:7b
> ```

### ⚠️ 4. Embedding (缺失 → KnowledgeController 上传/检索失败)

所有变量对应 `EmbeddingClientOpenAI` / `EmbeddingClientAliConfig` 两个 `@Configuration`：

| 变量 | 默认 base URL | 用途 | 缺失时表现 |
|------|--------------|------|----------|
| `embedding.openai.api-key` | https://api.openai.com | OpenAI Embedding | `uploadDocument` 抛 `BizException(SERVICE_UNAVAILABLE)` |
| `embedding.openai.model` | text-embedding-v2 | OpenAI text-embedding-3-small / v2 / v3 | — |
| `embedding.openai.dimension` | 1536 | 向量维度 | — |
| `embedding.ali.api-key` | dashscope | 通义 Embedding | 同上 |
| `embedding.ali.model` | text-embedding-ada-002 | Aliyun text-embedding-v2/v3 | — |
| `embedding.ali.dimension` | 1536 | Aliyun 1536 / 3072 | — |

**消费代码**：

```22:30:backend/meowflow/meowflow-infra/src/main/java/com/meowflow/infra/embedding/EmbeddingClientOpenAI.java
@Value("${embedding.openai.base-url:https://api.openai.com}")
private String baseUrl;
@Value("${embedding.openai.api-key:}")
private String apiKey;
@Value("${embedding.openai.model:text-embedding-v2}")
private String model;
```

### ⚠️ 5. 通知集成 (不是环境变量 — 通过 API 注册)

钉钉/飞书/企微 配置不是环境变量。系统启动后，通过 HTTP 接口写入 `IntegrationService.configs`（内存 `ConcurrentHashMap`）：

```bash
# 钉钉 (需要 webhook + 可选加签 secret)
curl -X POST http://localhost:8082/api/infra/integration/configs/dingtalk \
  -H 'Content-Type: application/json' \
  -d '{"webhookUrl":"https://oapi.dingtalk.com/robot/send?access_token=xxx","secret":"SEC..."}'

# 飞书
curl -X POST http://localhost:8082/api/infra/integration/configs/feishu \
  -H 'Content-Type: application/json' \
  -d '{"webhookUrl":"https://open.feishu.cn/hook/xxx"}'

# 企微
curl -X POST http://localhost:8082/api/infra/integration/configs/wxwork \
  -H 'Content-Type: application/json' \
  -d '{"webhookUrl":"https://qyapi.weixin.qq.com/cgi-bin/webhook/..."}'
```

然后调用 `POST /api/infra/integration/send` 发送消息。

### 📧 6. 邮件 / 短信

`EmailSender` / `SmsSender` 也是内存注册：
- Email 通过 SMTP host/port/user/pass 配置
- SMS 接入网关 SDK

配置通过 `POST /api/infra/integration/configs` 通用接口写入。

### ⚙️ 7. MCP (缺失 → executeTool 找不到子进程)

MCP 工具通过 `POST /api/infra/mcp/tools` 注册，存于内存 `MCPToolRegistry`：

```json
{
  "name": "filesystem",
  "description": "Read/write local files",
  "provider": "mcp-filesystem",
  "adapterType": "stdio",
  "endpoint": "npx -y @modelcontextprotocol/server-filesystem /tmp"
}
```

执行时 `StdioAdapter` / `SseAdapter` 会拉起子进程或连接 SSE endpoint。

---

## 三、Controller → 真实依赖关系

| Controller 路径 | 调用 | 必需凭证 / 依赖 |
|---|---|---|
| `POST/GET /api/infra/chat/*` | `ChatClientFactory.chat()` | OpenAI / Anthropic / Aliyun / Baidu api-key |
| `POST /api/infra/knowledge/bases` | `KnowledgeService.createKnowledgeBase()` | 仅 PG + Redis，**不需要 AI** |
| `GET /api/infra/knowledge/bases/*` | `knowledgeBaseMapper.selectById()` | 仅 PG |
| `POST /api/infra/knowledge/documents` | `KnowledgeService.uploadDocument()` → **Embedding API** | Embedding api-key + PGVector |
| `POST /api/infra/knowledge/search` | `SearchService.search()` → **Embedding API + 向量检索** | Embedding api-key + PG (pgvector) |
| `POST /api/infra/mcp/tools` | `MCPToolService.registerTool()` | 无（内存） |
| `POST /api/infra/mcp/tools/{name}/execute` | `StdioAdapter` / `SseAdapter` | 子进程 / SSE endpoint 可达 |
| `POST /api/infra/integration/configs/*` | `IntegrationService.saveConfig()` | 无（内存） |
| `POST /api/infra/integration/send` | `DingtalkSender.send()` / 等 | 真实钉钉/飞书 webhook |

---

## 四、常见错误与诊断

### `ChatClientController.chat` 返回 500

**日志关键词**：`OpenAI API 调用异常` / `routeAndChat` 没有可用 client

**修复**：
1. 确认环境变量 `meowflow.infra.chat.openai.api-key` 已设置
2. 确认模型名 `gpt-4o-mini` 在你的账号可用
3. 检查代理/网络是否能访问 `api.openai.com`

### `KnowledgeController.uploadDocument` 报 `BizException(SERVICE_UNAVAILABLE)`

**根因**：`embeddingClient.embed()` 失败（默认走 OpenAI Embedding）

**修复**：设置 `embedding.openai.api-key` 或者改用通义：
```ini
embedding.openai.api-key=
embedding.ali.api-key=${meowflow.infra.chat.ali.api-key:}
```

### `IntegrationController.send` 返回 `SendResult.failure("集成配置不存在: dingtalk")`

**根因**：尚未注册 webhook 配置

**修复**：调用 `POST /api/infra/integration/configs/dingtalk`

### `MCPToolController.executeTool` 抛 `IllegalStateException`

**根因**：外部 MCP 进程未运行 / SSE endpoint 不可达

**修复**：
- stdio: 确认 endpoint 命令在容器内 / 当前 OS 可执行
- sse: 确认 SSE 服务 URL 正确

---

## 五、最小可运行配置

只想让 Spring 启动不报错且能调用本地 PG/Redis 的最小配置：

```ini
# 基础设施 (默认即可)
POSTGRES_HOST=localhost
POSTGRES_PASSWORD=meowflow123
# Spring profile (启动 dev profile)
SPRING_PROFILES_ACTIVE=dev
# AES key
CRYPTO_AES_KEY=dGhpcyBpcyBhIDMyLWJ5dGUga2V5ISEh
```

启动后调用：
- ✅ `GET /api/infra/knowledge/bases` — 真实 PG 读写
- ✅ `POST /api/infra/mcp/tools` 注册
- ✅ `POST /api/infra/integration/configs/*` 注册
- ❌ `POST /api/infra/chat/chat` — 空 api-key，所有 ChatClient 未注册，会 500
- ❌ `POST /api/infra/knowledge/documents` — Embedding 失败

要让 AI 链路跑通，至少配置其中一个：

```ini
# 选 1: OpenAI 官方
OPENAI_API_KEY=sk-xxx

# 选 2: Aliyun DashScope (国内)
meowflow.infra.chat.ali.api-key=sk-xxx
embedding.ali.api-key=sk-xxx

# 选 3: 本地 Ollama (零成本)
meowflow.infra.chat.openai.base-url=http://host.docker.internal:11434/v1
meowflow.infra.chat.openai.api-key=ollama
meowflow.infra.chat.openai.model=qwen2.5:7b
embedding.openai.base-url=http://host.docker.internal:11434/v1
embedding.openai.api-key=ollama
embedding.openai.model=nomic-embed-text
```

---

## 六、相关文件

- 模板文件：[`backend/meowflow/deploy/docker/.env.example`](../backend/meowflow/deploy/docker/.env.example)
- 当前 `.env`：[`backend/meowflow/deploy/docker/.env`](../backend/meowflow/deploy/docker/.env)
- docker compose：[`backend/meowflow/deploy/docker/docker-compose.dev.yml`](../backend/meowflow/deploy/docker/docker-compose.dev.yml)
- infra 模块文档：[`modules/infra.md`](modules/infra.md)
