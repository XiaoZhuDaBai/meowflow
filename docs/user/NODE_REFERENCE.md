# 喵流节点参考手册

> 38 个节点完整参数说明

---

## 📑 目录

- [触发器 (6 个)](#触发器)
- [AI 节点 (8 个)](#ai-节点)
- [逻辑控制 (7 个)](#逻辑控制)
- [工具集成 (8 个)](#工具集成)
- [通知 (4 个)](#通知)
- [数据处理 (3 个)](#数据处理)
- [结束节点 (2 个)](#结束节点)

---

## 触发器

### 1. Webhook 触发器

**用途**: 接收外部 HTTP 请求触发流程

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| method | enum | ✅ | GET / POST / PUT / DELETE |
| path | string | ✅ | 监听路径 (如 `/hooks/incoming`) |
| authToken | string |  | 认证 Token |
| timeout | int |  | 等待响应超时 (ms) |

**输出**:
```json
{
  "body": { /* 请求体 */ },
  "headers": { /* 请求头 */ },
  "query": { /* 查询参数 */ },
  "method": "POST",
  "path": "/hooks/incoming"
}
```

**示例**: 接收 GitHub Webhook
```
method: POST
path: /hooks/github
authToken: ghp_xxxxx
```

---

### 2. 定时触发器

**用途**: 通过 Cron 表达式定时执行

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cron | string | ✅ | Cron 表达式 |
| timezone | string |  | 时区 (默认 Asia/Shanghai) |

**Cron 示例**:
```
0 9 * * *       # 每天 9:00
0 0 * * 1       # 每周一 0:00
*/30 * * * *    # 每 30 分钟
0 0 1 * *       # 每月 1 日
```

---

### 3. 表单触发器

**用途**: 用户提交表单时触发

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| formId | string | ✅ | 表单唯一标识 |
| fields | json |  | 字段定义 (JSON Schema) |

---

### 4. IM 消息触发器

**用途**: 接收钉钉/企微/飞书消息

**支持的平台**: 钉钉、企业微信、飞书、Slack

**输出**:
```json
{
  "text": "消息文本",
  "sender": "user_id",
  "platform": "dingtalk",
  "timestamp": "2026-09-05T10:30:00Z"
}
```

---

### 5. 邮件触发器

**用途**: 接收特定邮箱的邮件

**参数**:
- 邮箱地址
- 关键字过滤
- IMAP 配置

---

### 6. 事件触发器

**用途**: 监听内部业务事件

支持事件: `workflow.completed`、`execution.failed`、`alert.triggered`

---

## AI 节点

### 7. LLM 节点

**用途**: 调用大语言模型生成文本

**参数**:
| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| model | string | ✅ | - | 模型 ID |
| systemPrompt | string |  | - | 系统提示 |
| prompt | string | ✅ | - | 用户提示 (支持 `{{}}`) |
| temperature | number |  | 0.7 | 0-2 |
| maxTokens | int |  | - | 最大输出 token |
| topP | number |  | 1 | 0-1 |
| outputKey | string |  | text | 输出字段名 |

**支持的模型**:
- OpenAI: gpt-4o, gpt-4o-mini, gpt-3.5-turbo
- Anthropic: claude-3.5-sonnet, claude-3-haiku
- 国内: 文心一言、通义千问、智谱 GLM、Kimi
- 本地: Ollama

---

### 8. Agent 节点

**用途**: 让 LLM 自主选择工具完成任务

**策略**:
- `function-calling`: 原生工具调用 (推荐)
- `ReAct`: JSON 推理循环

**参数**:
- maxIterations: 最大循环次数 (默认 10)
- tools: 可用工具列表
- systemPrompt

---

### 9. 知识库检索

**用途**: 从知识库检索相关文档

**参数**:
| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| knowledgeBaseId | string | ✅ | - | KB ID |
| topK | int |  | 5 | 返回条数 |
| scoreThreshold | number |  | 0.5 | 最低分 |
| query | string |  | - | 查询 (默认 `{{trigger.text}}`) |
| rerank | bool |  | true | 是否重排序 |

**输出**:
```json
{
  "context": "拼接的上下文文本",
  "documents": [
    { "id": "...", "content": "...", "score": 0.92 }
  ]
}
```

---

### 10. 问题分类器

**用途**: 把问题分类到预定义类别

**参数**:
- categories: 类别列表 (逗号分隔)
- model: 使用的模型
- outputKey: 输出字段

---

### 11. 参数提取器

**用途**: 从文本中提取结构化字段

**参数**:
- schema: JSON Schema 定义
- model

---

### 12. 文本摘要

**用途**: 长文本摘要

**参数**:
- maxLength: 目标长度
- style: 摘要风格 (concise/detailed/bullets)

---

### 13. 翻译

**用途**: 多语言翻译

**参数**:
- targetLang: 目标语言
- sourceLang: 源语言 (auto 自动检测)

---

### 14. 内容审核

**用途**: 文本合规性检查

**检测**: 色情、暴力、政治敏感、广告、辱骂

**输出**: `{passed, violations, scores}`

---

## 逻辑控制

### 15. 条件分支 (IF)

**表达式**: `{{field}} === value`

支持的操作符: `===`, `!==`, `>`, `>=`, `<`, `<=`, `contains`, `startsWith`, `regex`

### 16. 多分支 (Switch)

基于字段值路由到不同分支。

### 17. 循环 (Iteration)

对数组逐个处理，循环变量 `$item`。

### 18. 并行 (Fork)

将下游拆为多个并行分支。

### 19. 汇聚 (Join)

等待多个上游完成后再继续 (all/any/n)。

### 20. 等待 (Wait)

暂停 N 秒/到指定时间。

### 21. 异常处理 (Try/Catch)

捕获下游节点异常。

---

## 工具集成

### 22. HTTP 请求

**参数**: method, url, headers, body, auth, timeout, retries

**认证方式**: None / Bearer / Basic / API Key

### 23. 数据库查询

支持 PostgreSQL / MySQL / ClickHouse。

**安全**: 默认只允许 SELECT，禁止 DDL/DML。

### 24. SQL 生成

自然语言 → SQL。

### 25. 代码执行

支持 JavaScript / Python (沙箱)。

### 26. JSON 转换

JPath 提取 / JSONPath。

### 27. MCP 工具

调用 Model Context Protocol 工具。

### 28. 自定义函数

注册自定义 JS 函数。

### 29. 文件读取

读取上传的文件内容。

---

## 通知

### 30. 飞书通知

### 31. 钉钉通知

### 32. 邮件发送

### 33. 短信发送

---

## 数据处理

### 34. 聚合器

合并多个上游输出。

### 35. 变量赋值

设置上下文变量。

### 36. 模板转换

字符串模板渲染。

---

## 结束节点

### 37. 结束

返回最终结果给调用方。

### 38. 答案

流式输出给用户 (用于 Chatflow)。

---

## 💡 节点选择指南

| 场景 | 推荐节点 |
|------|---------|
| 文本生成 | LLM |
| 多步任务 | Agent + 工具 |
| 知识问答 | 知识库检索 + LLM |
| 分类决策 | 问题分类器 / 条件分支 |
| 数据提取 | 参数提取器 |
| HTTP 调用 | HTTP 请求 |
| 数据库 | 数据库查询 / SQL 生成 |
| 长任务异步 | 异步执行节点 |

---

**完整参数请参考**: [API 接口文档](../frontend/API接口对接.md)
