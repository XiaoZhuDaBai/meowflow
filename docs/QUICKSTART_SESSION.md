# MeowFlow Agent 会话记忆快速开始

## 5 分钟快速集成

### 第 1 步：执行数据库迁移

```bash
cd d:\Code\喵流\backend\meowflow\deploy\sql\migration
psql -U postgres -d meowflow -f V1.5__agent_session.sql
```

### 第 2 步：启动后端服务

```powershell
cd d:\Code\喵流\backend\meowflow
.\start-all.ps1
```

### 第 3 步：测试 API

#### 创建会话
```bash
curl -X POST "http://localhost:8080/workflow/api/agent/session/create?userId=1001&agentNodeId=agent-001"
```

**响应示例**：
```json
{
  "code": 200,
  "data": "abc123def456",
  "message": "success"
}
```

#### 执行 Agent（带会话记忆）

```bash
curl -X POST "http://localhost:8080/workflow/api/execution/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "wf-001",
    "inputs": {
      "agent_query": "你好，我叫张三",
      "agent_enableMemory": true,
      "agent_sessionId": "abc123def456"
    }
  }'
```

#### 继续对话（复用会话）

```bash
curl -X POST "http://localhost:8080/workflow/api/execution/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "wf-001",
    "inputs": {
      "agent_query": "我刚才说我叫什么名字？",
      "agent_enableMemory": true,
      "agent_sessionId": "abc123def456"
    }
  }'
```

**预期回答**：你刚才说你叫张三。

---

## 常用命令

### 查询会话历史
```bash
curl "http://localhost:8080/workflow/api/agent/session/abc123def456/history?maxRounds=10"
```

### 清空会话
```bash
curl -X DELETE "http://localhost:8080/workflow/api/agent/session/abc123def456/clear"
```

### 删除会话
```bash
curl -X DELETE "http://localhost:8080/workflow/api/agent/session/abc123def456"
```

---

## 前端集成示例

```typescript
import { ref } from 'vue';

const sessionId = ref<string | null>(null);

// 首次对话
async function startConversation(userMessage: string) {
  const response = await fetch('/workflow/api/execution/execute', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      workflowId: 'wf-agent-chat',
      inputs: {
        agent_query: userMessage,
        agent_enableMemory: true,
        agent_sessionId: sessionId.value
      }
    })
  });
  
  const result = await response.json();
  
  // 保存会话 ID 以便后续使用
  sessionId.value = result.nodes['agent-001'].output.sessionId;
  
  return result.nodes['agent-001'].output.answer;
}

// 继续对话
async function continueConversation(userMessage: string) {
  return startConversation(userMessage); // 自动复用 sessionId
}

// 重新开始（新会话）
function resetConversation() {
  sessionId.value = null;
}
```

---

## 验证效果

### 场景 1：记住用户信息
```
用户：我叫李明，今年 25 岁
Agent：你好李明，很高兴认识你！

[5 分钟后]

用户：我叫什么名字？
Agent：你叫李明。

用户：我今年多大？
Agent：你今年 25 岁。
```

### 场景 2：多轮任务规划
```
用户：帮我规划一个周末旅游
Agent：好的，请问你想去哪里旅游？

用户：我想去北京
Agent：北京是个好选择！你打算待几天？

用户：两天
Agent：收到，两天北京之旅。你更喜欢文化古迹还是现代景点？

用户：古迹
Agent：那我推荐：故宫、长城、颐和园...
```

### 场景 3：工具调用记忆
```
用户：查一下今天北京的天气
Agent：[调用天气工具] 北京今天晴，气温 15-25℃

用户：那上海呢？
Agent：[调用天气工具] 上海今天多云，气温 18-26℃

用户：刚才北京是多少度？
Agent：北京今天是 15-25℃
```

---

## 故障排查

### 问题 1：会话 ID 无效
**症状**：调用接口返回 404
**原因**：会话已过期或被删除
**解决**：传 `null` 重新创建会话

### 问题 2：历史记录为空
**症状**：历史接口返回空数组
**原因**：会话刚创建，还没有消息
**解决**：先执行一次 Agent，再查询历史

### 问题 3：消息丢失
**症状**：10 轮之前的对话查不到
**原因**：自动淘汰机制生效
**解决**：调整 `maxRounds` 参数（不建议超过 20）

---

## 性能建议

- **单会话并发**：避免同一 sessionId 并发调用，可能导致消息乱序
- **Token 控制**：`maxRounds=10` 约消耗 2000 tokens，根据预算调整
- **定期清理**：对于长期不活跃的用户，可手动调用删除接口释放空间

---

## 更多文档

- 📖 [完整功能文档](./features/agent-session-memory.md)
- 🔧 [实现技术细节](./technical/SESSION_MEMORY_IMPLEMENTATION.md)
- 📋 [版本更新日志](./CHANGELOG.md)

---

**版本**：v1.5.0  
**更新日期**：2026-09-06
