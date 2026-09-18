# MeowFlow 更新日志

## [v1.5.0] - 2026-09-06

### 🎉 新增功能

#### Agent 会话记忆
- ✅ 实现跨执行持久化会话状态（`AgentSessionService`）
- ✅ 支持 10 轮滚动窗口自动淘汰旧消息
- ✅ 24 小时自动过期机制 + 7 天彻底清理
- ✅ 支持 user/assistant/tool 三种角色消息持久化
- ✅ 提供会话管理 REST API（创建/查询/清空/删除）
- ✅ 定时任务自动清理过期会话

**数据库变更**：
- 新增表：`mf_agent_session`（会话元信息）
- 新增表：`mf_agent_message`（消息历史）

**使用方式**：
```json
{
  "type": "ai.agent",
  "data": {
    "query": "{{user_input}}",
    "enableMemory": true,
    "maxRounds": 10,
    "sessionId": "{{session_id}}"
  }
}
```

**相关文件**：
- `backend/meowflow/meowflow-infra/src/main/java/com/meowflow/infra/agent/`
  - `AgentSession.java`
  - `AgentMessage.java`
  - `AgentSessionService.java`
  - `AgentSessionMapper.java`
  - `AgentMessageMapper.java`
  - `AgentSessionCleanupTask.java`
- `backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/`
  - `executor/ai/AgentExecutor.java`（集成会话记忆）
  - `controller/AgentSessionController.java`（API 接口）
- `docs/features/agent-session-memory.md`（使用文档）
- `deploy/sql/migration/V1.5__agent_session.sql`（数据库迁移）

---

## [v1.4.0] - 2026-09-05

### 🎉 新增功能

#### 调试功能集成
- ✅ 实时断点暂停执行
- ✅ 单步执行节点
- ✅ 变量快照查看
- ✅ 集成到前端编辑器 UI（`DebugBar.vue`）

#### 命令面板
- ✅ Ctrl+K 快捷键快速搜索节点
- ✅ 实时过滤节点类型
- ✅ 一键添加到画布

#### 模板市场扩充
- ✅ 从 17 个模板扩展到 53 个模板
- ✅ 覆盖营销、办公、数据分析、金融等 8 大场景

#### 用户文档体系
- ✅ 快速入门指南（`QUICKSTART.md`）
- ✅ 完整用户手册（`USER_GUIDE.md`）
- ✅ 节点参考文档（`NODE_REFERENCE.md`）
- ✅ 最佳实践（`BEST_PRACTICES.md`）
- ✅ 常见问题（`FAQ.md`）

---

## [v1.3.0] - 2026-09-03

### 🐛 Bug 修复

#### 端到端跑通修复（P0 级别）
- ✅ 修复 SSE 事件 JSON 序列化问题
- ✅ 修复 Redis Stream 游标实现
- ✅ 修复 SSE 游标类型混用
- ✅ 编辑器支持异步执行（`async: true`）
- ✅ 编辑器停止按钮取消后端执行
- ✅ 网关白名单放行 webhook/plugin 路径
- ✅ Vite 代理配置到后端 8080 端口
- ✅ 一键启动脚本（`start-all.ps1`）

**详细说明**：见 `docs/technical/E2E_RUN_GAPS_FIXES.md`

---

## [v1.2.0] - 2026-08-15

### 🎉 新增功能

#### 多模型管理
- ✅ 统一模型路由层（`ModelRoutingExecutor`）
- ✅ 熔断机制（`@CircuitBreaker`）
- ✅ 健康探测（`HealthChecker`）
- ✅ 故障自动降级（候选模型列表）
- ✅ 配置加密（`@Encrypted`）
- ✅ 成本可计量（`AIInvokeLog`）

#### RAG 检索
- ✅ 文档分块、向量化、入库完整链路
- ✅ 向量 + 关键词混合召回（`HybridSearchChannel`）
- ✅ 阈值过滤（`SearchPostProcessor.filterByScore`）
- ✅ RRF 融合重排（`SearchPostProcessor.rrfReRank`）
- ✅ 知识库搜索节点（`KnowledgeSearchExecutor`）

---

## [v1.1.0] - 2026-07-20

### 🎉 新增功能

#### Agent 系统
- ✅ Agent 作为工作流一等节点（`NodeType.AGENT`）
- ✅ Function-calling 策略（原生工具调用）
- ✅ ReAct 策略（JSON 格式循环）
- ✅ 自建 MCP 工具框架（`MCPToolRegistry`）

#### 执行可观测性
- ✅ TraceId 从网关贯穿到 AI 调用（`TraceContextHolder`）
- ✅ 异步日志批量入库（`@Async`）
- ✅ Micrometer 指标埋点（`workflow_execution_total`）
- ✅ 钉钉/邮件/短信告警（`AlertService`）

---

## [v1.0.0] - 2026-07-10

### 🎉 首次发布

#### DAG 执行引擎
- ✅ 37 类节点（6 触发 + 19 动作 + 7 控制 + 4 结束 + 1 注释）
- ✅ 可插拔扩展（`NodeRegistry`）
- ✅ 并行批次（`ForkExecutor` + `JoinExecutor`）
- ✅ 条件分支（`IfExecutor` / `SwitchExecutor`）
- ✅ 循环子图（`LoopExecutor` + `LoopSubgraphDriver`）
- ✅ 超时机制（`TimeoutScanner`）
- ✅ 重试逻辑（`LLMExecutor.maxRetries`）
- ✅ 取消机制（`CancellationToken`）
- ✅ 实时推送（`RunEvent` → Redis Stream → SSE）

#### 基础功能
- ✅ 用户认证与权限管理
- ✅ 工作流编辑器（Vue3 + TypeScript）
- ✅ 实时执行日志
- ✅ 17 个内置模板

---

## 版本规范

- **主版本号（Major）**：架构级别变更
- **次版本号（Minor）**：新增功能
- **修订号（Patch）**：Bug 修复

---

## 贡献者

感谢所有为 MeowFlow 贡献代码的开发者！

- @core-team - 核心团队
- @contributors - 社区贡献者

---

## 许可证

[MIT License](LICENSE)
