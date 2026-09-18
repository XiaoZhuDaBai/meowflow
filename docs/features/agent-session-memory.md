# Agent 会话记忆使用指南

## 功能说明

Agent 节点现在支持**会话记忆**功能，可以在多次执行之间保持对话上下文，实现真正的多轮对话。

### 核心特性

- ✅ **跨执行持久化**：会话状态存储在数据库，不随工作流执行结束而丢失
- ✅ **10 轮滚动窗口**：自动保留最近 10 轮对话，淘汰更早的消息
- ✅ **自动过期清理**：24 小时无活动的会话自动过期，7 天后彻底清理
- ✅ **用户隔离**：每个用户的会话独立存储，互不干扰
- ✅ **工具调用记忆**：不仅记录用户和助手消息，也记录工具调用历史

---

## 使用方式

### 1. 在节点配置中启用会话记忆

```json
{
  "type": "ai.agent",
  "data": {
    "query": "{{user_input}}",
    "instruction": "你是一个智能助手",
    "strategy": "function-calling",
    "maxIterations": 5,
    "enableMemory": true,
    "maxRounds": 10,
    "sessionId": "{{session_id}}"
  }
}
```

**参数说明**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `enableMemory` | Boolean | 否 | false | 是否启用会话记忆 |
| `maxRounds` | Integer | 否 | 10 | 保留的最大轮数（1轮=1用户消息+1助手回复） |
| `sessionId` | String | 否 | 自动生成 | 会话 ID，不传则自动创建新会话 |

### 2. 前端传递会话 ID

```typescript
// 首次调用（创建新会话）
const result1 = await executeWorkflow({
  workflowId: 'wf-001',
  inputs: {
    user_input: '你好，我叫张三',
    session_id: null  // 不传或传 null，后端自动创建
  }
});

// 获取会话 ID
const sessionId = result1.nodes['agent-001'].output.sessionId;

// 后续调用（复用会话）
const result2 = await executeWorkflow({
  workflowId: 'wf-001',
  inputs: {
    user_input: '我刚才说我叫什么名字？',
    session_id: sessionId  // 传递之前的会话 ID
  }
});
```

---

## API 接口

### 创建会话

```http
POST /api/agent/session/create
Content-Type: application/json

{
  "userId": 1001,
  "agentNodeId": "agent-001"
}
```

**响应**：
```json
{
  "code": 200,
  "data": "abc123def456",
  "message": "success"
}
```

### 获取会话历史

```http
GET /api/agent/session/{sessionId}/history?maxRounds=10
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "role": "user",
      "content": "你好"
    },
    {
      "role": "assistant",
      "content": "你好，有什么可以帮你的？"
    }
  ]
}
```

### 清空会话历史

```http
DELETE /api/agent/session/{sessionId}/clear
```

### 删除会话

```http
DELETE /api/agent/session/{sessionId}
```

---

## 数据库表结构

### mf_agent_session（会话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| session_id | VARCHAR(64) | 会话 ID（业务主键） |
| user_id | BIGINT | 用户 ID |
| agent_node_id | VARCHAR(128) | Agent 节点 ID |
| status | VARCHAR(32) | 状态：active/expired/archived |
| last_active_time | TIMESTAMP | 最后活跃时间 |
| expire_time | TIMESTAMP | 过期时间（24小时无活动） |

### mf_agent_message（消息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| session_id | VARCHAR(64) | 会话 ID |
| role | VARCHAR(32) | 角色：user/assistant/tool |
| content | TEXT | 消息内容 |
| tool_name | VARCHAR(128) | 工具名称（仅 role=tool） |
| tool_result | JSONB | 工具结果（仅 role=tool） |
| sequence_number | INT | 消息序号（用于排序和淘汰） |

---

## 自动清理机制

### 1. 会话过期（每小时执行）

- 将 24 小时未活跃的会话标记为 `expired`
- 不删除数据，仅更新状态

### 2. 会话清理（每天凌晨 3 点执行）

- 删除 7 天前过期的会话及其所有消息
- 彻底释放存储空间

### 3. 消息淘汰（每次追加消息时触发）

- 保留最近 30 条消息（10 轮 × 3 条）
- 自动删除更早的消息

---

## 最佳实践

### 1. 客服机器人场景

```javascript
// 用户首次进入聊天
const sessionId = await createSession(userId, 'customer-service-agent');

// 整个对话过程复用同一个 sessionId
while (userIsInChat) {
  const userMessage = await getUserInput();
  const response = await callAgent({
    query: userMessage,
    sessionId: sessionId,
    enableMemory: true
  });
  displayResponse(response.answer);
}

// 用户离开聊天，可选择清空或保留会话
await clearSession(sessionId);
```

### 2. 多 Agent 协作场景

```javascript
// 不同 Agent 使用不同的 sessionId
const session1 = await createSession(userId, 'agent-research');
const session2 = await createSession(userId, 'agent-writer');

// Agent 1：信息收集
const researchResult = await callAgent({
  query: '收集关于 AI 的资料',
  sessionId: session1,
  enableMemory: true
});

// Agent 2：内容创作（独立上下文）
const articleResult = await callAgent({
  query: '写一篇关于 AI 的文章',
  sessionId: session2,
  enableMemory: true
});
```

### 3. 长期记忆场景

```javascript
// 用户今天的对话
await callAgent({
  query: '我最近在学习 Python',
  sessionId: 'user-1001-longterm',
  enableMemory: true
});

// 第二天继续（会话未过期）
await callAgent({
  query: '昨天我说我在学什么？',
  sessionId: 'user-1001-longterm',
  enableMemory: true
});
// 回答：你昨天说你在学习 Python
```

---

## 注意事项

1. **Token 消耗**：会话历史会作为上下文发送给 LLM，`maxRounds` 设置过大会增加 Token 消耗
2. **隐私合规**：敏感对话需要遵守数据保护法规，必要时提前清理或匿名化
3. **并发安全**：同一 sessionId 并发调用可能导致消息顺序错乱，建议前端排队
4. **数据库容量**：高频场景建议定期清理无用会话，或缩短过期时间

---

## 常见问题

### Q1: 会话 ID 可以自定义吗？

可以。只要保证唯一性，可以传入任何字符串作为 sessionId。但推荐使用系统自动生成的 UUID。

### Q2: 会话过期后还能恢复吗？

过期后的会话在 7 天内仍保留数据，可以手动查询。超过 7 天后会被彻底删除，无法恢复。

### Q3: 如何实现"忘记之前的对话"？

调用清空会话 API：`DELETE /api/agent/session/{sessionId}/clear`

### Q4: 会话记忆对所有策略都生效吗？

是的。无论是 `function-calling` 还是 `ReAct` 策略，会话记忆都会生效。

---

## 版本历史

- **v1.5**（2026-09-06）：新增会话记忆功能，支持 10 轮滚动窗口和自动清理
