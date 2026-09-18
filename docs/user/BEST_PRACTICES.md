# 喵流最佳实践

> 20 个生产环境验证的最佳实践

---

## 📑 目录

1. [工作流设计原则](#设计原则)
2. [性能优化](#性能优化)
3. [成本控制](#成本控制)
4. [可靠性设计](#可靠性设计)
5. [安全合规](#安全合规)

---

## 设计原则

### 实践 1：单一职责

每个工作流只做一件事。

❌ **错误**: 一个工作流同时处理客服、营销、财务。

✅ **正确**: 拆分为多个工作流，通过 Webhook 串联。

```
[客服工作流] → Webhook → [工单工作流] → Webhook → [财务工作流]
```

---

### 实践 2：明确输入输出

每个工作流的**触发节点**应明确入参 schema，**结束节点**应明确返回值。

```json
{
  "input": {
    "type": "object",
    "properties": {
      "userId": { "type": "string" },
      "text": { "type": "string" }
    }
  },
  "output": {
    "type": "object",
    "properties": {
      "reply": { "type": "string" },
      "confidence": { "type": "number" }
    }
  }
}
```

---

### 实践 3：避免循环依赖

DAG 必须是**有向无环图**，循环依赖会导致死锁。

✅ **正确**: 用 **Loop 节点** + `$item` 循环变量代替节点间的循环引用。

---

### 实践 4：节点命名清晰

使用业务可读的命名：

❌ `Node1`、`LLM_v2`、`my_test_node`

✅ `意图分类`、`订单查询`、`发送通知`

---

## 性能优化

### 实践 5：并行执行独立节点

当多个下游节点**互不依赖**时，使用 **Fork 节点** 并行执行。

```
              ┌─ [知识库检索] ─┐
[触发] → Fork ┼─ [用户画像]   ─┼→ Join → [LLM 回答]
              └─ [订单查询]   ─┘
```

性能提升: **3x** (单线程 6s → 并行 2s)

---

### 实践 6：避免重复 LLM 调用

对相同输入做缓存 (默认 TTL 5 分钟)。

```yaml
# 工作流级别缓存配置
cache:
  enabled: true
  ttl: 300
  key: "{{trigger.text}}"
```

---

### 实践 7：大上下文分块处理

超过 100K token 的长文本：
1. 先用 **Split 节点** 分块
2. 并行处理每块
3. 用 **LLM 合并** 节点汇总

---

### 实践 8：流式输出提升用户体验

对长输出启用 **流式 (SSE)**：

```yaml
nodes:
  - type: ai.llm
    config:
      stream: true       # 开启流式
      outputMode: sse
```

首字节延迟: **3000ms → 200ms**

---

## 成本控制

### 实践 9：分层使用模型

| 任务 | 推荐模型 |
|------|---------|
| 简单分类/提取 | GPT-4o-mini / GLM-4-Flash |
| 复杂推理 | GPT-4o / Claude-3.5 |
| 长文本 | Claude-3.5-Sonnet (200K) |
| 代码 | DeepSeek-Coder |

---

### 实践 10：使用 token 限制

```yaml
- type: ai.llm
  config:
    maxTokens: 500      # 限制输出
    stopSequences: ["\n\n\n"]  # 提前停止
```

---

### 实践 11：监控成本异常

设置 **告警规则**：单日成本超阈值自动通知。

```yaml
alerts:
  - name: high-cost
    condition: cost.today > 100
    action: notify.feishu
```

---

## 可靠性设计

### 实践 12：超时控制

每个节点设置合理超时：

| 节点类型 | 推荐超时 |
|---------|---------|
| LLM | 30s |
| HTTP | 10s |
| 数据库 | 5s |
| 代码执行 | 3s |

---

### 实践 13：失败重试 + 退避

```yaml
- type: http.request
  config:
    timeoutMs: 5000
    retries: 3
    retryBackoff: exponential  # 1s, 2s, 4s
    retryOnFail: true
```

---

### 实践 14：兜底值

关键路径设置默认值，避免单点失败：

```yaml
- type: ai.llm
  config:
    fallbackOutput: "抱歉，我暂时无法回答，请稍后再试。"
    fallbackOn: timeout | error
```

---

### 实践 15：错误分支

为关键路径添加**错误边**：

```
[LLM 回答] ──error──→ [默认回复] ─→ [结束]
       │
       └──success──→ [结束]
```

---

### 实践 16：幂等性设计

Webhook 处理必须**幂等**：
- 用唯一 ID 去重
- 使用数据库事务
- 状态机管理

```yaml
- type: code.transform
  config:
    source: |
      const id = input.headers['x-event-id'];
      if (await isProcessed(id)) {
        return { skipped: true };
      }
      await markProcessed(id);
      return { processed: true };
```

---

## 安全合规

### 实践 17：敏感信息过滤

在 LLM 调用前先过滤 PII：

```
[输入] → [PII 检测] → IF (有敏感) → [脱敏] → [LLM]
                                  ↓
                              [拒绝]
```

---

### 实践 18：API Key 加密存储

喵流**自动加密**所有 API Key（基于 AES-256-GCM），无需手动处理。

---

### 实践 19：审计日志

所有执行记录自动入库，支持 6 个月回溯：

```sql
SELECT * FROM execution_log
WHERE user_id = ? AND created_at > ?
ORDER BY created_at DESC;
```

---

### 实践 20：权限最小化

```yaml
# 工作流配置
permissions:
  read: [user, admin]
  write: [admin]
  execute: [user, admin]
```

---

## 🎯 进阶技巧

### 多工作流协作

通过 Webhook 实现工作流编排：

```
工作流 A (数据采集) 
  → Webhook → 工作流 B (AI 处理) 
  → Webhook → 工作流 C (通知发送)
```

### A/B 测试

克隆工作流，50% 流量分配到不同版本。

### 灰度发布

发布时设置 5% 流量，逐步放大到 100%。

---

## 📚 参考案例

| 场景 | 模板 |
|------|------|
| 客服 | 智能客服 |
| 营销 | 多平台发布 |
| 办公 | 周报生成 |
| 数据 | 日志分析 |
| 金融 | 反欺诈检测 |
| 医疗 | 病例摘要 |

所有模板位于 **模板市场** → 选择对应分类。

---

**更多案例**: 参考 [../technical/scalability.md](../technical/scalability.md)
