# 喵流 (MeowFlow) - 数据一致性测试验证文档

> 本文档用于在系统全流程跑通后，系统性地验证数据一致性相关的风险点
> 每个测试用例都包含：测试目的、前置条件、测试步骤、预期结果、异常处理、修复建议

> 📌 **交叉引用**：测试用例与模块/技术栈的对应关系，详见 [`CROSS_REFERENCE.md`](CROSS_REFERENCE.md)
> 📌 **技术栈汇总**：每个技术组件的用途说明，详见 [`../modules/STACK_SUMMARY.md`](../modules/STACK_SUMMARY.md)

---

## 一、测试总览

### 1.1 风险等级与测试优先级

| 优先级 | 编号 | 测试场景 | 风险等级 | 预计耗时 |
|-------|------|---------|---------|---------|
| **P0** | TC-001 | HTTP 节点重复执行（邮件/POST） | 🔴 高 | 30min |
| **P0** | TC-002 | 用户权限缓存与数据库不一致 | 🔴 高 | 20min |
| **P0** | TC-003 | 分布式事务部分失败回滚 | 🔴 高 | 45min |
| **P1** | TC-004 | RabbitMQ 消息丢失 | 🟡 中 | 30min |
| **P1** | TC-005 | RabbitMQ 消息重复消费 | 🟡 中 | 30min |
| **P2** | TC-006 | 工作流执行中切换版本 | 🟠 中 | 30min |
| **P2** | TC-007 | 节点重试副作用 | 🟡 中 | 30min |
| **P3** | TC-008 | 雪花 ID 时钟回拨 | 🟢 低 | 15min |
| **P3** | TC-009 | 链路追踪日志丢失 | 🟢 低 | 20min |
| **P3** | TC-010 | 用户上下文异步透传 | 🟢 低 | 20min |
| **P4** | TC-011 | 未知节点类型（NodeRegistry 未注册） | 🟠 中 | 15min |
| **P4** | TC-012 | 节点配置校验失败 | 🟢 低 | 15min |
| **P4** | TC-013 | DAG 环依赖导致死锁 | 🔴 高 | 20min |
| **P4** | TC-014 | 并行节点部分失败 | 🟠 中 | 25min |
| **P4** | TC-015 | Wait 节点超时 | 🟢 低 | 15min |
| **P4** | TC-016 | LLM 模型路由全部熔断 | 🔴 高 | 30min |
| **P4** | TC-017 | LLM 首包探测超时 | 🟠 中 | 20min |
| **P4** | TC-018 | LLM SSE 流式响应中断 | 🟡 中 | 25min |
| **P4** | TC-019 | Embedding 服务不可用 | 🟡 中 | 20min |
| **P4** | TC-020 | 向量数据库（Milvus）不可达 | 🟠 中 | 20min |
| **P4** | TC-021 | 关键词检索无结果 | 🟢 低 | 10min |
| **P4** | TC-022 | 后处理器链异常（重排模型挂掉） | 🟠 中 | 20min |
| **P4** | TC-023 | Webhook 签名验证失败 | 🟡 中 | 15min |
| **P4** | TC-024 | Webhook 时间戳过期（重放攻击） | 🟡 中 | 15min |
| **P4** | TC-025 | Schedule 触发器堆积 | 🟡 中 | 20min |
| **P4** | TC-026 | 网关限流触发 | 🟢 低 | 15min |
| **P4** | TC-027 | JWT Token 过期/无效 | 🟢 低 | 10min |
| **P4** | TC-028 | Nginx 502/504 后端不可用 | 🟠 中 | 20min |
| **P4** | TC-029 | 线程池队列打满（拒绝策略触发） | 🟠 中 | 20min |
| **P4** | TC-030 | Redis Lua 脚本执行失败 | 🟢 低 | 15min |
| **P4** | TC-031 | Snowflake ID 序列耗尽 | 🟢 低 | 10min |
| **P4** | TC-032 | 告警静默期内不应触发 | 🟢 低 | 15min |
| **P4** | TC-033 | 告警渠道全部失败 | 🟡 中 | 20min |
| **P4** | TC-034 | Trace 链路丢失（AOP 未生效） | 🟢 低 | 15min |

### 1.2 测试环境准备

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          测试环境清单                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 系统服务                                                             │
│     ├─ MeowFlow-workflow（启动日志收集模式）                              │
│     ├─ MeowFlow-infra（启动 Mock LLM 服务）                              │
│     ├─ MeowFlow-user                                                       │
│     ├─ MeowFlow-monitor                                                    │
│     └─ MeowFlow-executor                                                   │
│                                                                          │
│  2. Mock 服务（用于模拟外部依赖）                                        │
│     ├─ Mock HTTP 服务（可注入延迟/失败）                                │
│     ├─ Mock 邮件服务（记录调用次数）                                    │
│     ├─ Mock 钉钉服务                                                     │
│     └─ Mock 数据库（用于测试分布式事务）                                │
│                                                                          │
│  3. 监控工具                                                             │
│     └─ PostgreSQL 慢查询日志                                             │
│     ├─ Redis 监控面板                                                    │
│     └─ RabbitMQ 管理界面（15672）                                        │
│                                                                          │
│  4. 工具脚本                                                             │
│     ├─ chaos-blade（故障注入）                                          │
│     ├─ wrk/jmeter（压力测试）                                           │
│     └─ tcpdump（网络抓包）                                              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、P0 级测试用例

### TC-001：HTTP 节点重复执行（最危险场景）

**测试目的**：验证含副作用的 HTTP 节点（邮件/POST）在网络异常时是否会被重复执行

#### 前置条件
- [ ] 系统已部署并能正常执行工作流
- [ ] Mock 邮件服务已部署（端口 18080），可记录调用次数
- [ ] Mock HTTP POST 服务已部署（端口 18081），可注入延迟
- [ ] 数据库已准备好执行记录查询

#### 测试数据准备

```sql
-- 创建测试工作流定义
INSERT INTO workflow (id, name, definition, version, status)
VALUES (
  'test-tc001',
  'HTTP节点重复测试',
  '{
    "nodes": [
      {"id": "start-1", "type": "start", "name": "开始"},
      {"id": "http-1", "type": "http", "name": "发送邮件",
       "config": {
         "method": "POST",
         "url": "http://localhost:18080/mock-email/send",
         "body": {"to": "test@MeowFlow.com", "subject": "测试邮件"},
         "timeoutMs": 5000,
         "maxRetries": 3
       }
      },
      {"id": "end-1", "type": "end", "name": "结束"}
    ],
    "edges": [
      {"source": "start-1", "target": "http-1"},
      {"source": "http-1", "target": "end-1"}
    ]
  }',
  1,
  'active'
);
```

#### 测试步骤

**子用例 1.1：网络超时导致的重复发送**

```
步骤 1：在 Mock HTTP 服务中配置 5 秒延迟 + 返回成功
步骤 2：触发工作流 test-tc001 执行
步骤 3：观察 Mock 邮件服务的调用日志
步骤 4：查询执行记录 wf_execution 表
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • Mock 邮件服务收到 1 次请求                                           │
│  • wf_execution.status = SUCCESS                                        │
│  • wf_node_execution.retry_count = 0                                    │
│  • 用户收到 1 封邮件                                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常（需要修复）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • Mock 邮件服务收到 N 次请求（N > 1）                                  │
│  • 用户收到多封重复邮件                                                  │
│  • wf_node_execution.status 多次切换 PENDING → RUNNING                  │
└─────────────────────────────────────────────────────────────────────────┘
```

**子用例 1.2：HTTP 503 服务不可用**

```
步骤 1：Mock 服务返回 503 错误
步骤 2：触发工作流
步骤 3：检查最终状态
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 节点按配置重试 3 次（每次间隔递增）                                  │
│  • 最终 wf_node_execution.status = FAILED                                │
│  • wf_execution.status = FAILED                                          │
│  • Mock 服务收到 3 次请求（重试 3 次）                                  │
│  • 不应该超过 3 次                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 数据校验 SQL

```sql
-- 查询执行记录
SELECT id, status, retry_count, error_msg, start_time, end_time
FROM wf_execution
WHERE workflow_id = 'test-tc001'
ORDER BY create_time DESC
LIMIT 1;

-- 查询节点执行记录
SELECT id, status, retry_count, duration_ms, error_msg
FROM wf_node_execution
WHERE execution_id = 'xxx'
ORDER BY create_time;
```

#### 异常处理流程

```
发现重复执行后：
1. 立即停止相关测试
2. 记录 Mock 服务的请求次数和详细日志
3. 复现问题，提供：
   - 完整执行链路（Trace ID）
   - 重试日志
   - Mock 服务收到的所有请求
4. 进入修复阶段（参考 1.3 修复建议）
```

#### 修复建议（测试失败时参考）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      HTTP 节点幂等修复方案                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  方案 A：请求幂等键（推荐）                                               │
│  ───────────────────────────────────────────────────────────────────────│
│  • 每个节点生成唯一 execution_node_id                                    │
│  • 作为 Idempotency-Key 传给外部服务                                     │
│  • 外部服务做去重                                                         │
│                                                                          │
│  方案 B：响应缓存                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 第一次成功响应缓存到 Redis（TTL = 24h）                               │
│  • 重试时先查缓存，有则直接返回                                          │
│                                                                          │
│  方案 C：明确区分可重试/不可重试                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • HTTP 节点增加 isIdempotent 配置                                       │
│  • 默认 false，写操作必须显式开启                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-002：用户权限缓存与数据库不一致

**测试目的**：验证用户角色/权限变更后，Redis 缓存是否及时失效

#### 前置条件
- [ ] 测试用户 test_user_001 已存在，角色为 `user`
- [ ] 系统有 Redis 监控
- [ ] 已准备好权限相关 API 的调用脚本

#### 测试步骤

**子用例 2.1：修改角色后立即访问**

```
步骤 1：使用 test_user_001 登录，获取 access_token
步骤 2：调用 GET /api/admin/users（管理员接口，预期 403）
步骤 3：管理员调用 PUT /api/admin/users/test_user_001/role
        body: { "roleId": "admin_role_id" }
步骤 4：【关键】立刻（< 1秒）使用 test_user_001 的 token 调用管理员接口
步骤 5：观察返回结果
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • 步骤 4 返回 200 OK，能访问管理员接口                                 │
│  • 角色变更实时生效                                                       │
│  • Redis 中的用户权限 key 被删除或更新                                   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常（需要修复）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • 步骤 4 仍返回 403，权限未生效                                         │
│  • 等待 30 分钟后才能访问                                                 │
│  • Redis 中的旧权限缓存仍存在                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

**子用例 2.2：撤销权限**

```
步骤 1：test_user_002 当前是管理员
步骤 2：撤销其管理员角色
步骤 3：立即使用 test_user_002 的 token 访问管理员接口
步骤 4：观察返回结果
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 立即返回 403，撤销实时生效                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 监控验证

```bash
# 查看 Redis 中的权限缓存
redis-cli KEYS "user:permission:*"

# 步骤 3 后立即执行
redis-cli KEYS "user:permission:test_user_001"
# 预期：返回空（已失效）

# 查看是否在 30 分钟后还有缓存
# 预期：完全清除
```

#### 修复建议

```java
// UserService.java 修改
@Transactional
public void updateUserRole(String userId, String roleId) {
    // 1. 更新数据库
    userRoleRepository.updateRole(userId, roleId);

    // 2. 立即清除 Redis 缓存（强一致）
    String cacheKey = "user:permission:" + userId;
    redisTemplate.delete(cacheKey);

    // 3. 通知其他实例清除本地缓存（如果有）
    // 通过 MQ 广播失效消息
    mqTemplate.send("permission-change", userId);
}
```

---

### TC-003：分布式事务部分失败

**测试目的**：验证工作流执行过程中，多节点部分成功部分失败时的数据一致性

#### 前置条件
- [ ] 准备测试工作流：含 3 个数据库写入节点 + 1 个 HTTP 节点
- [ ] Mock 数据库可注入异常
- [ ] 已准备好事务日志查询脚本

#### 测试数据准备

```sql
-- 创建测试工作流：DB1 → DB2 → DB3 → HTTP
INSERT INTO workflow (id, name, definition, version)
VALUES (
  'test-tc003',
  '分布式事务测试',
  '{
    "nodes": [
      {"id": "start-1", "type": "start"},
      {"id": "db-1", "type": "database", "name": "DB1写入",
       "config": {"sql": "INSERT INTO order_a (id, amount) VALUES ({execution_id}, 100)"}},
      {"id": "db-2", "type": "database", "name": "DB2写入",
       "config": {"sql": "INSERT INTO order_b (id, amount) VALUES ({execution_id}, 200)"}},
      {"id": "db-3", "type": "database", "name": "DB3写入",
       "config": {"sql": "INSERT INTO order_c (id, amount) VALUES ({execution_id}, 300)"}},
      {"id": "http-1", "type": "http", "name": "通知",
       "config": {"url": "http://localhost:18081/notify"}},
      {"id": "end-1", "type": "end"}
    ],
    "edges": [
      {"source": "start-1", "target": "db-1"},
      {"source": "db-1", "target": "db-2"},
      {"source": "db-2", "target": "db-3"},
      {"source": "db-3", "target": "http-1"},
      {"source": "http-1", "target": "end-1"}
    ]
  }',
  1
);
```

#### 测试步骤

**子用例 3.1：DB2 节点失败**

```
步骤 1：触发工作流 test-tc003
步骤 2：在 DB2 节点执行时，通过 Mock 工具注入异常
步骤 3：观察 DB1、DB2、DB3 的数据状态
步骤 4：观察工作流执行状态
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现 - Saga 模式）                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  • DB1 有数据（DB1 节点已成功）                                          │
│  • DB2 无数据（DB2 节点失败，未提交）                                    │
│  • DB3 无数据（后续节点未执行）                                          │
│  • wf_execution.status = FAILED                                          │
│  • wf_node_execution[db-1].status = SUCCESS                              │
│  • wf_node_execution[db-2].status = FAILED                               │
│  • wf_node_execution[db-3].status = PENDING (未执行)                     │
│  • 补偿节点（如果有）执行 DB1 的回滚                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常（需要修复）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • DB1 有数据、DB2 有数据（部分提交但未回滚）                            │
│  • DB3 也可能被错误执行                                                  │
│  • wf_execution.status = SUCCESS（错误状态）                             │
│  • 整体事务回滚了但部分外部副作用已发生                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

**子用例 3.2：HTTP 节点超时但前面节点已成功**

```
步骤 1：触发工作流
步骤 2：Mock HTTP 服务延迟 10 秒
步骤 3：工作流超时
步骤 4：观察 DB 数据 + HTTP 是否调用
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • DB1、DB2、DB3 都有数据                                                │
│  • HTTP 调用可能已发生（已发送请求，但响应超时）                         │
│  • wf_execution.status = FAILED                                          │
│  • 提供明确的错误信息                                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 数据校验 SQL

```sql
-- 检查各表是否有数据
SELECT 'order_a' AS tbl, COUNT(*) FROM order_a WHERE id = '{execution_id}'
UNION ALL
SELECT 'order_b', COUNT(*) FROM order_b WHERE id = '{execution_id}'
UNION ALL
SELECT 'order_c', COUNT(*) FROM order_c WHERE id = '{execution_id}';

-- 检查执行状态
SELECT status, error_msg FROM wf_execution WHERE id = '{execution_id}';

-- 检查节点状态
SELECT node_id, status, retry_count, error_msg
FROM wf_node_execution
WHERE execution_id = '{execution_id}'
ORDER BY sort_order;
```

#### 修复建议

```
┌─────────────────────────────────────────────────────────────────────────┐
│                分布式事务改造方案                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  方案 A：Saga 模式（推荐）                                               │
│  ───────────────────────────────────────────────────────────────────────│
│  • 每个节点独立事务                                                       │
│  • 节点状态机：PENDING → RUNNING → SUCCESS/FAILED/COMPENSATED            │
│  • 失败时调用反向补偿节点                                                 │
│  • 补偿任务也是工作流节点                                                 │
│                                                                          │
│  方案 B：本地消息表（最终一致）                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • 业务表 + 消息表同一事务                                              │
│  • 后台轮询发送 MQ                                                       │
│  • 消费端幂等处理                                                         │
│                                                                          │
│  方案 C：TCC 模式                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • Try-Confirm-Cancel 三阶段                                            │
│  • 实现复杂，性能较好                                                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、P1 级测试用例

### TC-004：RabbitMQ 消息丢失

**测试目的**：验证 RabbitMQ 在异常情况下是否丢失消息

#### 前置条件
- [ ] RabbitMQ 已开启管理界面（http://localhost:15672）
- [ ] 工作流触发走 MQ 异步
- [ ] 已准备好队列监控脚本

#### 测试步骤

**子用例 4.1：消费者宕机时的消息丢失**

```
步骤 1：记录当前队列消息数：rabbitmqctl list_queues name messages
步骤 2：触发 10 个工作流执行
步骤 3：在消费者拉到消息但未 ACK 时，kill -9 消费者进程
步骤 4：重启消费者进程
步骤 5：观察队列消息数和工作流执行情况
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • 队列消息被消费者重新拉取                                             │
│  • 所有 10 个工作流都成功执行                                           │
│  • 没有消息丢失                                                           │
│  • wf_execution 表有 10 条记录                                          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 部分工作流没有执行                                                     │
│  • 队列消息数 = 0，但 wf_execution 缺失                                 │
│  • 用户看不到执行记录                                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

**子用例 4.2：RabbitMQ 重启**

```
步骤 1：触发 5 个工作流
步骤 2：在消息未被消费时，重启 RabbitMQ
        docker restart rabbitmq
步骤 3：等待 RabbitMQ 启动完成
步骤 4：观察消息和工作流执行情况
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 消息从磁盘恢复                                                         │
│  • 所有 5 个工作流都执行成功                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 监控命令

```bash
# 查看队列消息数
rabbitmqctl list_queues name messages_ready messages_unacknowledged

# 查看消费者数量
rabbitmqctl list_consumers

# 查看消息是否持久化
rabbitmqctl list_queues name durable
```

#### 修复建议

```java
// RabbitMQ 配置 - 开启持久化
@Bean
public Queue workflowQueue() {
    return QueueBuilder.durable("workflow.execute.queue")
        .withArgument("x-queue-type", "quorum")  // 使用仲裁队列
        .build();
}

// 生产者 - 开启 publisher confirms
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
    RabbitTemplate template = new RabbitTemplate(factory);
    template.setMandatory(true);
    template.setConfirmCallback((data, ack, cause) -> {
        if (!ack) {
            log.error("消息发送失败: {}", cause);
            // 重发或记录到本地消息表
        }
    });
    return template;
}

// 消费者 - 手动 ACK
@RabbitListener(queues = "workflow.execute.queue")
public void handle(Channel channel, Message message) {
    try {
        process(message);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }
}
```

---

### TC-005：RabbitMQ 消息重复消费

**测试目的**：验证消费者重复收到消息时的幂等性

#### 前置条件
- [ ] 已有 RabbitMQ 监控
- [ ] 准备好幂等性测试脚本

#### 测试步骤

**子用例 5.1：手动重发消息**

```
步骤 1：触发一个工作流
步骤 2：在 RabbitMQ 管理界面找到该消息
步骤 3：手动 republish 该消息
步骤 4：观察工作流是否被重复执行
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • 第二次消息被识别为重复                                                │
│  • 不创建新的 wf_execution 记录                                         │
│  • 返回之前的结果                                                        │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 创建了 2 条 wf_execution 记录                                        │
│  • 工作流被执行了 2 次                                                   │
│  • 用户收到重复的邮件/通知                                               │
└─────────────────────────────────────────────────────────────────────────┘
```

**子用例 5.2：消费超时导致重发**

```
步骤 1：触发工作流
步骤 2：消费者处理时 sleep 30 秒（超过 MQ 默认超时）
步骤 3：观察 MQ 重发 + 工作流重复执行
```

#### 修复建议

```java
// 消费端幂等处理
@RabbitListener(queues = "workflow.execute.queue")
public void handle(Message message) {
    String executionId = extractExecutionId(message);

    // 1. 检查幂等性
    String idempotentKey = "mq:processed:" + executionId;
    Boolean firstTime = redisTemplate.opsForValue()
        .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));

    if (Boolean.FALSE.equals(firstTime)) {
        log.info("消息已处理，跳过: {}", executionId);
        return;
    }

    // 2. 执行业务逻辑
    try {
        workflowEngine.execute(executionId);
    } catch (Exception e) {
        redisTemplate.delete(idempotentKey);  // 失败时清除，允许重试
        throw e;
    }
}
```

---

## 四、P2 级测试用例

### TC-006：工作流执行中切换版本

**测试目的**：验证正在执行的工作流，在版本切换后是否会出现状态错乱

#### 前置条件
- [ ] 准备一个含 10 个节点的长时间工作流
- [ ] 每个节点 sleep 1 秒，模拟长时间执行

#### 测试数据准备

```sql
-- 创建测试工作流 v1（含 10 个节点）
INSERT INTO workflow (id, name, definition, version) VALUES ('test-tc006', '长时间工作流', '{...}', 1);
```

#### 测试步骤

```
步骤 1：触发工作流执行（v1）
步骤 2：等待执行到第 5 个节点（约 5 秒后）
步骤 3：修改工作流定义，更新到 v2
        PUT /api/workflows/test-tc006
        body: { "definition": "{新的工作流定义}" }
步骤 4：观察后续节点（6-10）是按 v1 还是 v2 执行
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现 - 执行快照）                                          │
├─────────────────────────────────────────────────────────────────────────┤
│  • 第 1-10 个节点全部按 v1 执行                                          │
│  • v2 不影响正在执行的实例                                                │
│  • 新的执行实例使用 v2                                                   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 中间某些节点按 v1 执行，某些按 v2 执行                                │
│  • 节点参数错乱                                                           │
│  • 执行结果不一致                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 数据校验 SQL

```sql
-- 查询执行的快照版本
SELECT id, workflow_version, workflow_definition
FROM wf_execution
WHERE id = '{execution_id}';

-- 对比实际执行和快照是否一致
SELECT node_id, config
FROM wf_node_execution
WHERE execution_id = '{execution_id}';
```

#### 修复建议

```java
// WorkflowEngine.java - 执行时保存快照
public void execute(String workflowId, String executionId) {
    // 1. 加载工作流定义
    Workflow workflow = workflowRepository.findById(workflowId);

    // 2. 保存快照到 execution 表（关键！）
    Execution execution = new Execution();
    execution.setId(executionId);
    execution.setWorkflowId(workflowId);
    execution.setWorkflowVersion(workflow.getVersion());
    execution.setWorkflowDefinition(workflow.getDefinition());  // 保存快照
    execution.setStatus("RUNNING");
    executionRepository.save(execution);

    // 3. 后续节点执行都基于快照
    // 而不是重新加载 workflow
}
```

---

### TC-007：节点重试副作用

**测试目的**：验证不同节点类型的重试机制是否安全

#### 测试矩阵

| 节点类型 | 配置 maxRetries | Mock 行为 | 预期调用次数 | 是否安全 |
|---------|----------------|-----------|------------|---------|
| HTTP GET | 3 | 5s 延迟 | 3 次 | ✅ |
| HTTP POST | 3 | 5s 延迟 | 1 次（有幂等键）| ✅ |
| 邮件节点 | 3 | 5s 延迟 | 1 次（幂等）| ⚠️ |
| 数据库 INSERT | 3 | 5s 延迟 | 1 次（唯一约束）| ⚠️ |
| LLM 调用 | 3 | 5s 延迟 | 3 次 | ✅ |
| 数据库 UPDATE | 3 | 5s 延迟 | 1 次 | ✅ |

#### 测试步骤

```
对每种节点类型：
步骤 1：创建工作流，配置 maxRetries = 3
步骤 2：Mock 服务注入 5 秒延迟
步骤 3：触发工作流
步骤 4：观察 Mock 服务的调用次数
```

#### 修复建议

```java
// NodeDefinition.java 增加幂等性配置
public class NodeDefinition {
    private boolean idempotent = false;  // 是否可安全重试

    // 对于 POST/邮件/INSERT 类节点，强制要求幂等键
    @JsonIgnore
    public boolean isRetrySafe() {
        return idempotent || isReadOnly();
    }
}

// HttpExecutor.java
public NodeResult execute(NodeDefinition node, ExecutionContext context) {
    if (!node.isRetrySafe()) {
        // 生成幂等键
        String idempotencyKey = generateIdempotencyKey(node, context);
        request.setHeader("Idempotency-Key", idempotencyKey);
    }
    // ...
}
```

---

## 五、P3 级测试用例

### TC-008：雪花 ID 时钟回拨

**测试目的**：验证系统时间回退时 Snowflake ID 生成器的行为

#### 测试步骤

```
步骤 1：正常生成 10 个 ID，记录
步骤 2：将系统时间向前调整 1 小时
步骤 3：再生成 10 个 ID
步骤 4：将系统时间向后调整 5 秒（模拟回拨）
步骤 5：生成 1 个 ID，观察行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期（正确实现）                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│  • 时钟回拨时系统等待或拒绝服务                                         │
│  • 不生成重复 ID                                                         │
│  • 有清晰的错误日志                                                       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  ❌ 异常                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 时钟回拨时整个系统崩溃                                                 │
│  • 生成重复 ID 导致主键冲突                                              │
│  • 无任何告警                                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// IdGenerator.java - 改进时钟回拨处理
public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();

    if (timestamp < lastTimestamp) {
        long offset = lastTimestamp - timestamp;
        if (offset <= 5) {  // 小幅回拨，等待
            try {
                Thread.sleep(offset << 1);
                timestamp = System.currentTimeMillis();
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException("时钟回拨过大");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("线程中断");
            }
        } else {  // 大幅回拨，报警
            log.error("检测到严重时钟回拨: {}ms", offset);
            throw new RuntimeException("时钟回拨过大，请检查NTP");
        }
    }
    // ...
}
```

---

### TC-009：链路追踪日志丢失（PG 写入方案）

**测试目的**：高并发下链路追踪数据写入 PostgreSQL 的完整性

#### 测试步骤

```
步骤 1：使用 JMeter 发起 1000 并发工作流触发
步骤 2：每个工作流触发后立即查询 PostgreSQL 的 trace_log 表
步骤 3：统计缺失率
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • PG trace_log 表中存在所有执行实例的 Trace 记录                       │
│  • 缺失率 < 0.1%                                                         │
│  • 不阻塞业务流程                                                         │
│  • LogbackDbAppender 异步批量写入正常                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

**验证 SQL**

```sql
-- 查询近 1 小时的总条数
SELECT COUNT(*) AS total_count
FROM trace_log
WHERE create_time > NOW() - INTERVAL '1 hour';

-- 按 trace_id 查询应该有多少条
SELECT trace_id, COUNT(*) AS span_count
FROM trace_log
WHERE create_time > NOW() - INTERVAL '1 hour'
GROUP BY trace_id
ORDER BY span_count DESC
LIMIT 10;

-- 抽样检查完整链路
SELECT trace_id, span_id, service_name, operation_name, duration_ms, status
FROM trace_log
WHERE trace_id = '{测试中记录的 trace_id}'
ORDER BY start_time;
```

#### 修复建议

```java
// LogbackDbAppender.java - 异步批量写入 + 失败降级
public class LogbackDbAppender extends AsyncAppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> buffer = new ArrayList<>(1000);
    private final JdbcTemplate jdbcTemplate;

    public LogbackDbAppender(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        // 启动定时刷盘线程（兜底）
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::flush, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    protected void append(ILoggingEvent event) {
        synchronized (buffer) {
            buffer.add(event);
            if (buffer.size() >= batchSize) {
                flush();
            }
        }
    }

    private void flush() {
        List<ILoggingEvent> toFlush;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        try {
            // 批量 INSERT 到 PG
            jdbcTemplate.batchUpdate(
                "INSERT INTO trace_log(trace_id, span_id, ...) VALUES(?,?,?,...)",
                toFlush.stream().map(this::toArgs).collect(Collectors.toList())
            );
        } catch (Exception e) {
            // 降级：保存到本地文件
            logToFile("/tmp/trace-log-fallback.log", toFlush);
            // 告警
            alertService.sendAlert("追踪日志写入 PG 失败");
        }
    }
}
```

---

### TC-010：用户上下文异步透传

**测试目的**：验证 SSE 流式响应等长连接场景下的上下文传递

#### 测试步骤

```
步骤 1：创建含 LLM 流式输出节点的工作流
步骤 2：使用用户 A 的 token 触发
步骤 3：在 SSE 流式响应过程中，检查日志中的 userId 是否为 A
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 所有日志的 userId 都正确                                              │
│  • 异步任务能正确识别用户                                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、P4 级架构异常测试用例

> 本章节测试用例来源于架构文档（`architecture.md`）中识别的 24 个异常场景
> 主要覆盖：节点执行层、LLM 路由、知识库检索、触发器、网关、基础设施、监控告警

### TC-011：未知节点类型（NodeRegistry 未注册）

**测试目的**：当工作流定义中引用了未注册的节点类型时，系统应优雅报错而非崩溃

**来源**：架构文档 § 4.1.2 NodeRegistry

#### 测试步骤

```
步骤 1：创建一个含未知节点的 workflow 定义
        nodes: [{ id: "n1", type: "unknown-node-type" }]
步骤 2：触发执行
步骤 3：观察 NodeRegistry.get() 的行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 抛出 NodeNotFoundException（业务异常，非系统异常）                    │
│  • 返回统一错误码（ResultCode.NODE_NOT_FOUND）                          │
│  • 工作流状态变为 FAILED                                                 │
│  • trace_log 中有完整异常堆栈                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// NodeRegistry.java - 节点未注册时抛业务异常而非 NPE
public NodeExecutor get(String type) {
    NodeExecutor executor = executors.get(type);
    if (executor == null) {
        throw new NodeNotFoundException(type);  // BizException
    }
    return executor;
}
```

---

### TC-012：节点配置校验失败

**测试目的**：节点配置不合法时，应在校验阶段拦截，不进入执行

**来源**：架构文档 § 4.1.2 NodeExecutor.validate()

#### 测试步骤

```
步骤 1：创建 LLM 节点配置，但 model 字段为空
步骤 2：保存工作流
步骤 3：触发执行
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 保存时 validate() 抛 ConfigurationException                          │
│  • HTTP 400 返回明确错误信息                                              │
│  • 执行阶段不会再触发无效配置                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// LLMExecutor.validate() 示例
public void validate(NodeDefinition node) {
    LLMNodeConfig config = parseConfig(node);
    if (config.getModel() == null || config.getModel().isBlank()) {
        throw new ConfigurationException("LLM 节点必须指定 model");
    }
    if (config.getCandidates() == null || config.getCandidates().isEmpty()) {
        throw new ConfigurationException("LLM 节点必须至少配置 1 个候选模型");
    }
}
```

---

### TC-013：DAG 环依赖导致死锁

**测试目的**：工作流定义存在环依赖时，DAG 拓扑排序应能识别并报错

**来源**：架构文档 § 4.4 WorkflowEngine.dagSort()（Kahn 算法）

#### 测试步骤

```
步骤 1：创建 DAG 定义：A → B → C → A（环）
步骤 2：保存工作流
步骤 3：触发执行
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 保存时检测到环依赖，抛 CircularDependencyException                    │
│  • 即使保存成功，执行时也应快速识别（BFS 完成后 result 大小 < nodes.size）│
│  • 工作流状态 FAILED，不死锁                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// DAGSorter.java - 环依赖检测
private List<Node> dagSort(List<Node> nodes, List<Edge> edges) {
    // Kahn 算法 BFS
    while (!queue.isEmpty()) { ... }

    if (result.size() < nodes.size()) {
        // 剩余未访问的节点构成环
        List<String> cycleNodes = nodes.stream()
            .filter(n -> !visited.contains(n.getId()))
            .map(Node::getId).collect(toList());
        throw new CircularDependencyException(
            "检测到环依赖，节点: " + cycleNodes);
    }
    return result;
}
```

---

### TC-014：并行节点部分失败

**测试目的**：ParallelExecutor 中多个分支执行，部分失败时应有明确语义

**来源**：架构文档 § 4.1.2 ParallelExecutor

#### 测试步骤

```
步骤 1：创建 Parallel 节点，含 3 个分支：A、B、C
步骤 2：A、C 成功，B 失败（Mock 注入失败）
步骤 3：观察整体结果
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 整体 Parallel 节点状态遵循配置的 failPolicy                            │
│  • failPolicy=any_fail（默认）：整体 FAILED                               │
│  • failPolicy=all_success：要求全部成功                                    │
│  • failPolicy=ignore：忽略失败继续                                        │
│  • 各分支独立记录日志到 trace_log                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// ParallelExecutor.java
public NodeResult execute(ExecutionContext ctx, NodeDefinition node) {
    String failPolicy = node.getConfig().getString("failPolicy", "any_fail");
    List<CompletableFuture<NodeResult>> futures = branches.stream()
        .map(b -> CompletableFuture.supplyAsync(
            () -> executeBranch(ctx, b), executor))
        .collect(toList());

    List<NodeResult> results = futures.stream()
        .map(CompletableFuture::join).collect(toList());

    if ("any_fail".equals(failPolicy) && hasFailed(results)) {
        return NodeResult.failed("并行分支存在失败: " + failedBranches(results));
    }
    return NodeResult.success(results);
}
```

---

### TC-015：Wait 节点超时

**测试目的**：Wait 节点等待超时后，工作流能继续执行后续节点

**来源**：架构文档 § 4.1.2 WaitExecutor

#### 测试步骤

```
步骤 1：创建 Wait 节点，配置 waitSeconds=10
步骤 2：触发工作流执行
步骤 3：在 5 秒时手动 Kill wait 线程（或改 waitSeconds=1）
步骤 4：观察后续节点是否执行
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • Wait 节点超时后状态变 TIMEOUT（不是 FAILED）                          │
│  • onTimeout 配置生效：continue（继续执行）/ fail（终止工作流）          │
│  • 后续节点按配置执行                                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// WaitExecutor.java
public NodeResult execute(ExecutionContext ctx, NodeDefinition node) {
    long waitMs = node.getConfig().getLong("waitSeconds") * 1000;
    String onTimeout = node.getConfig().getString("onTimeout", "fail");

    try {
        Thread.sleep(waitMs);
        return NodeResult.success();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        if ("continue".equals(onTimeout)) {
            return NodeResult.timeout();  // 特殊状态
        }
        return NodeResult.failed("Wait 节点超时");
    }
}
```

---

### TC-016：LLM 模型路由全部熔断

**测试目的**：所有候选 LLM 模型都熔断时，应有降级策略

**来源**：架构文档 § 4.2 LLM 节点设计

#### 测试步骤

```
步骤 1：配置 3 个候选模型（GPT-4o、Claude、阿里）
步骤 2：通过故障注入让所有模型健康检查返回 DOWN
步骤 3：触发 LLM 节点
步骤 4：等待熔断器全部 OPEN
步骤 5：再次触发
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • FallbackChain 兜底：返回默认降级文案（如"服务暂时不可用"）            │
│  • 告警：所有模型熔断，立即通知运维                                       │
│  • 工作流不无限重试熔断的模型                                             │
│  • HealthChecker 定时探测，半 OPEN → CLOSED 后自动恢复                    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// ModelRouter.java + FallbackChain
public ChatResponse route(ChatRequest request) {
    for (ModelCandidate candidate : candidates) {
        if (!circuitBreaker.tryAcquirePermission(candidate)) continue;
        try {
            return candidate.getClient().chat(request);
        } catch (Exception e) {
            circuitBreaker.recordFailure(candidate);
        }
    }
    // 全部熔断
    alertService.sendAlert("所有 LLM 模型熔断", "CRITICAL");
    return fallbackChain.execute(request);
}
```

---

### TC-017：LLM 首包探测超时

**测试目的**：模型响应慢时，首包探测机制应能自动切换模型

**来源**：架构文档 § 4.2 首包探测

#### 测试步骤

```
步骤 1：配置首选模型（GPT-4o）和备选（Claude）
步骤 2：让 GPT-4o 响应延迟 5 秒
步骤 3：设置 firstTokenTimeout=3s
步骤 4：触发 LLM 节点
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 3 秒内 GPT-4o 无首包 → 自动切换到 Claude                              │
│  • 用户无感知（透明降级）                                                │
│  • 触发慢请求告警                                                        │
│  • 该次请求超时计入 GPT-4o 的失败统计                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// StreamCallbackDecorator.java（装饰器模式）
public void onFirstToken(long timeoutMs, Runnable switchAction) {
    ScheduledExecutorService.schedule(() -> {
        if (!firstTokenReceived.get()) {
            log.warn("LLM 首包超时，切换模型");
            switchAction.run();
        }
    }, timeoutMs, TimeUnit.MILLISECONDS);
}
```

---

### TC-018：LLM SSE 流式响应中断

**测试目的**：SSE 长连接中途断开，服务器端应能感知并释放资源

**来源**：架构文档 § 4.2 SSE 流输出

#### 测试步骤

```
步骤 1：触发 SSE 流式 LLM 节点
步骤 2：接收 50% 内容后客户端断开
步骤 3：观察服务器端行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 服务器端检测到客户端断开（心跳超时/读返回 -1）                        │
│  • 立即取消下游 LLM 调用（避免浪费 Token）                                │
│  • 线程池 worker 释放                                                    │
│  • 记录异常流（保存前 50% 内容到 trace_log）                              │
│  • HTTP 连接器不再持有大对象（防止内存泄漏）                               │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// SseEmitter 包装（线程安全）
public class TraceableSseEmitter extends SseEmitter {
    @Override
    public void send(Object object) throws IOException {
        try {
            super.send(object);
        } catch (IOException e) {
            // 客户端断开
            cleanup();
            throw e;
        }
    }

    private void cleanup() {
        cancel();
        // 释放线程、清理临时变量
    }
}
```

---

### TC-019：Embedding 服务不可用

**测试目的**：Embedding API 不可用时，知识库检索节点应有降级

**来源**：架构文档 § 4.3 多路检索（向量通道）

#### 测试步骤

```
步骤 1：触发 knowledge-search 节点
步骤 2：让 Embedding API 返回 503
步骤 3：观察节点行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 向量检索通道失败，但不抛错                                             │
│  • 关键词检索通道继续执行（多路并行）                                     │
│  • 后处理器收到部分结果（仅关键词）                                       │
│  • 返回降级结果（标记 partial=true）                                      │
│  • Embedding API 失败计入熔断统计                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// VectorSearchChannel.search() 容错
public SearchResult search(SearchQuery query, int topK) {
    try {
        float[] embedding = embeddingClient.embed(query);
        return vectorDB.search(embedding, topK);
    } catch (Exception e) {
        log.warn("Embedding 失败，降级到关键词检索: {}", e.getMessage());
        return SearchResult.empty();  // 不抛错
    }
}
```

---

### TC-020：向量数据库（Milvus）不可达

**测试目的**：Milvus 集群宕机时，知识库检索节点不应卡死

**来源**：架构文档 § 4.3 VectorSearchChannel

#### 测试步骤

```
步骤 1：触发知识库检索节点
步骤 2：通过 iptables/firewall 切断 Milvus 连接
步骤 3：观察超时行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • Milvus 调用超时（默认 5s）后返回失败                                  │
│  • 不影响其他检索通道                                                     │
│  • 整体检索返回 partial 结果                                              │
│  • 告警：Milvus 不可达                                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// VectorSearchChannel 增加超时控制
public SearchResult search(SearchQuery query, int topK) {
    try {
        Future<SearchResult> future = executor.submit(
            () -> milvusClient.search(embedding, topK));
        return future.get(5, TimeUnit.SECONDS);  // 硬超时
    } catch (TimeoutException e) {
        future.cancel(true);
        return SearchResult.empty();
    }
}
```

---

### TC-021：关键词检索无结果

**测试目的**：关键词检索返回空时，整体检索结果不应为空

**来源**：架构文档 § 4.3 KeywordSearchChannel

#### 测试步骤

```
步骤 1：构造一个完全无匹配的问题（如"xyzabc123"）
步骤 2：触发知识库检索
步骤 3：观察返回结果
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 向量检索可能返回少量相似结果（基于语义）                                │
│  • 关键词检索返回空列表（不抛错）                                         │
│  • 整体返回结果非空（空列表 + 相似结果合并）                               │
│  • 客户端收到结构化响应（success=true, data=[]）                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-022：后处理器链异常（重排模型挂掉）

**测试目的**：后处理器链中某个环节失败，不应影响整个检索

**来源**：架构文档 § 4.3 PostProcessorChain（责任链模式）

#### 测试步骤

```
步骤 1：触发检索，配置包含 Deduplicate + Rerank + QualityFilter
步骤 2：让 Rerank 模型返回 500
步骤 3：观察链路
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • RerankProcessor 失败 → 跳过重排步骤，继续走 QualityFilter              │
│  • 最终结果缺少 rerank 分数标记                                           │
│  • 不抛错，节点整体 SUCCESS                                               │
│  • 告警：Rerank 模型异常                                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// PostProcessorChain.execute() 容错
public List<SearchResult> execute(List<SearchResult> results) {
    for (PostProcessor processor : processors) {
        try {
            results = processor.process(results);
        } catch (Exception e) {
            log.warn("后处理器 {} 失败，跳过", processor.getClass().getSimpleName(), e);
            // 继续下一步，不中断
        }
    }
    return results;
}
```

---

### TC-023：Webhook 签名验证失败

**测试目的**：Webhook 调用方签名错误时，应拒绝执行

**来源**：架构文档 § 8.2 Webhook 安全设计

#### 测试步骤

```
步骤 1：调用方构造一个错误签名的 Webhook 请求
        X-Webhook-Signature: sha256=wrong-signature
步骤 2：触发工作流
步骤 3：观察服务端响应
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 返回 HTTP 401 Unauthorized                                            │
│  • 错误信息：签名不匹配                                                   │
│  • 工作流不被触发                                                         │
│  • 记录安全审计日志                                                       │
│  • 同一 IP 连续失败 N 次后触发风控告警                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// WebhookTriggerExecutor.execute()
public NodeResult execute(ExecutionContext ctx, NodeDefinition node) {
    String signature = request.getHeader("X-Webhook-Signature");
    String timestamp = request.getHeader("X-Webhook-Timestamp");
    String body = request.getBody();

    long ts = Long.parseLong(timestamp);
    if (Math.abs(System.currentTimeMillis()/1000 - ts) > 300) {
        throw new SecurityException("时间戳过期");
    }
    String expected = "sha256=" + hmacSha256(secret, timestamp + "." + body);
    if (!expected.equals(signature)) {
        throw new SecurityException("签名不匹配");
    }
    return NodeResult.success();
}
```

---

### TC-024：Webhook 时间戳过期（重放攻击）

**测试目的**：使用超过 5 分钟的旧请求重放，应被拒绝

**来源**：架构文档 § 8.2 时间戳校验

#### 测试步骤

```
步骤 1：构造一个 10 分钟前的请求
        X-Webhook-Timestamp: now-600
步骤 2：签名正确，但时间戳过期
步骤 3：观察服务端响应
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 返回 HTTP 401 Unauthorized                                            │
│  • 错误信息：时间戳过期（超过 5 分钟）                                    │
│  • 工作流不被触发（防止重放）                                              │
│  • 审计日志记录                                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-025：Schedule 触发器堆积

**测试目的**：当工作流处理慢于触发频率时，Quartz 任务堆积应有限度

**来源**：架构文档 § 4.1.2 ScheduleTriggerExecutor

#### 测试步骤

```
步骤 1：创建定时工作流，每 10 秒触发一次
步骤 2：让节点执行人为延迟 30 秒
步骤 3：观察 5 分钟后 Quartz 任务状态
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • Quartz 不堆积（misfire 策略：fireOnceNow 丢弃积压）                    │
│  • 监控告警：执行耗时 > 触发间隔                                          │
│  • 工作流执行次数 = 时间窗口/触发间隔（不重复）                            │
│  • 数据库 wf_execution 表行数符合预期                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// Quartz 配置
@Bean
public Trigger workflowTrigger(Scheduler scheduler) {
    SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule()
        .withIntervalInSeconds(10)
        .repeatForever()
        .withMisfireHandlingInstructionNextWithRemainingCount();
    // 不丢弃后续任务，但也不无限堆积
}
```

---

### TC-026：网关限流触发

**测试目的**：用户高频请求触发限流时，应返回明确错误码

**来源**：架构文档 § 2.1 Nginx 限流 + § 4.1.3 限流熔断

#### 测试步骤

```
步骤 1：单个用户 1 秒内发起 200 次 API 请求
步骤 2：观察限流触发点
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 第 101 次请求返回 HTTP 429                                            │
│  • 错误信息：请求过于频繁                                                 │
│  • 用户上下文保留（限流不丢用户）                                         │
│  • 监控：单用户 QPS 异常告警                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```nginx
# nginx.conf
limit_req_zone $binary_remote_addr zone=one:10m rate=100r/s;
limit_req zone=one burst=20 nodelay;
```

---

### TC-027：JWT Token 过期/无效

**测试目的**：JWT Token 过期或被篡改时，请求应被拒绝

**来源**：架构文档 § 8.1 认证授权

#### 测试步骤

```
步骤 1：使用过期的 access_token（8 小时前签发）
步骤 2：使用错误签名的 token
步骤 3：观察响应
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 过期 token：返回 401 + 提示 token 过期                                 │
│  • 错误签名：返回 401 + 提示无效 token                                    │
│  • 客户端能识别 401，自动用 refresh_token 续签                             │
│  • refresh_token 也过期：返回 401 + 强制重新登录                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-028：Nginx 502/504 后端不可用

**测试目的**：后端服务全部宕机时，Nginx 返回正确错误码

**来源**：架构文档 § 7.2 生产环境（Nginx 集群）

#### 测试步骤

```
步骤 1：Kill 所有 MeowFlow-workflow 服务进程
步骤 2：发起 API 请求
步骤 3：观察 Nginx 返回
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 返回 HTTP 502（Bad Gateway）或 503                                     │
│  • 自定义错误页（友好提示，非 nginx 默认页）                              │
│  • 健康检查自动剔除挂掉的 Pod                                             │
│  • K8s 自动重启挂掉的 Pod                                                │
│  • 告警：服务不可达                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```nginx
# nginx.conf
upstream MeowFlow-backend {
    server backend1:8080 max_fails=3 fail_timeout=30s;
    server backend2:8080 max_fails=3 fail_timeout=30s;
    server backend3:8080 max_fails=3 fail_timeout=30s;
}

error_page 502 503 504 /50x.html;
location = /50x.html {
    root /usr/share/nginx/html;
    internal;
}
```

---

### TC-029：线程池队列打满（拒绝策略触发）

**测试目的**：线程池满载后，新任务应被合理处理（拒绝/丢弃/降级）

**来源**：架构文档 § 4.1.3 8 个专用线程池

#### 测试步骤

```
步骤 1：让 LLM 调用线程池（max=50, queue=2000）满载
步骤 2：发起第 2251 个 LLM 调用请求
步骤 3：观察行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 拒绝策略生效（默认 CallerRunsPolicy，由调用线程执行）                  │
│  • 用户响应延迟增加但任务不丢失                                           │
│  • 线程池队列监控告警（队列使用率 > 80%）                                  │
│  • LLM 节点快速失败（不无限等待）                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// ThreadPoolConfig.java
ThreadPoolExecutor llmExecutor = new ThreadPoolExecutor(
    20, 50, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(2000),
    new ThreadPoolExecutor.CallerRunsPolicy());  // 调用方执行
```

---

### TC-030：Redis Lua 脚本执行失败

**测试目的**：Redis 限流 Lua 脚本异常时，应有降级

**来源**：架构文档 § 4.1.3 队列式限流（Redis ZSET + Lua）

#### 测试步骤

```
步骤 1：故意发送错误的 Lua 脚本（如 SHUTDOWN 命令）
步骤 2：触发限流逻辑
步骤 3：观察行为
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • Lua 脚本返回错误时，业务侧捕获异常                                    │
│  • 降级：放行请求（fail-open，不限流）                                   │
│  • 告警：Redis Lua 异常                                                  │
│  • 不影响正常业务（限流丢失 < 100ms）                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 修复建议

```java
// RateLimitByRedis.java
public boolean tryAcquire(String key, int limit, int windowSec) {
    try {
        Long count = redisTemplate.execute(LIMIT_SCRIPT, keys, args);
        return count != null && count <= limit;
    } catch (Exception e) {
        log.warn("Redis 限流异常，降级放行", e);
        return true;  // 降级：放行
    }
}
```

---

### TC-031：Snowflake ID 序列耗尽

**测试目的**：单节点 ID 序列号用尽后应等待下一毫秒

**来源**：架构文档 § 4.1.3 Snowflake ID

#### 测试步骤

```
步骤 1：高并发场景下，1 毫秒内生成 5000+ ID
步骤 2：观察 ID 生成是否阻塞
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┷────┤
│  • 序列号满后自旋等待下一毫秒（而非返回错误）                              │
│  • ID 连续递增，无重复                                                    │
│  • 单机 QPS > 4 万时考虑多 workerId 或改进算法                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-032：告警静默期内不应触发

**测试目的**：静默期内重复触发告警应被忽略

**来源**：架构文档 § 9.1 监控告警

#### 测试步骤

```
步骤 1：触发执行失败告警
步骤 2：30 秒内连续触发 5 次失败
步骤 3：观察告警次数
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • 仅发送 1 次告警（首次触发）                                            │
│  • 静默期内重复触发不发送通知                                             │
│  • alert_history 表记录所有触发（含被静默的）                              │
│  • 静默期结束后，新触发才发送通知                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-033：告警渠道全部失败

**测试目的**：钉钉/邮件/短信全部失败时，告警不应丢失

**来源**：架构文档 § 9.1 告警渠道

#### 测试步骤

```
步骤 1：让钉钉、邮件、短信 SDK 都抛异常
步骤 2：触发告警
步骤 3：观察告警记录
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • alert_history 表完整记录告警内容                                       │
│  • 各渠道失败状态记录到 notify_status 字段                                │
│  • 告警不丢失（即使通知失败）                                              │
│  • 失败计数累加，连续失败触发"告警系统异常"元告警                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### TC-034：Trace 链路丢失（@TraceNode 未生效）

**测试目的**：当 AOP 失效时，能通过兜底机制发现并告警

**来源**：架构文档 § 9.2 链路追踪

#### 测试步骤

```
步骤 1：禁用 Spring AOP（关闭 @EnableAspectJAutoProxy）
步骤 2：执行工作流
步骤 3：查询 trace_log 表
```

**预期结果**

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ✅ 预期                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  • biz_execution_log 表仍记录了基础执行信息（即使 trace 缺失）            │
│  • 监控告警：trace_log 写入量骤降                                         │
│  • 服务启动时检查 AOP 状态（健康检查）                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、测试结果记录表

### 7.1 测试执行记录

| 用例 ID | 测试日期 | 测试人 | 结果 | 失败原因 | 修复状态 |
|---------|---------|--------|------|---------|---------|
| TC-001  |         |        |      |         |         |
| TC-002  |         |        |      |         |         |
| TC-003  |         |        |      |         |         |
| TC-004  |         |        |      |         |         |
| TC-005  |         |        |      |         |         |
| TC-006  |         |        |      |         |         |
| TC-007  |         |        |      |         |         |
| TC-008  |         |        |      |         |         |
| TC-009  |         |        |      |         |         |
| TC-010  |         |        |      |         |         |
| TC-011  |         |        |      |         |         |
| TC-012  |         |        |      |         |         |
| TC-013  |         |        |      |         |         |
| TC-014  |         |        |      |         |         |
| TC-015  |         |        |      |         |         |
| TC-016  |         |        |      |         |         |
| TC-017  |         |        |      |         |         |
| TC-018  |         |        |      |         |         |
| TC-019  |         |        |      |         |         |
| TC-020  |         |        |      |         |         |
| TC-021  |         |        |      |         |         |
| TC-022  |         |        |      |         |         |
| TC-023  |         |        |      |         |         |
| TC-024  |         |        |      |         |         |
| TC-025  |         |        |      |         |         |
| TC-026  |         |        |      |         |         |
| TC-027  |         |        |      |         |         |
| TC-028  |         |        |      |         |         |
| TC-029  |         |        |      |         |         |
| TC-030  |         |        |      |         |         |
| TC-031  |         |        |      |         |         |
| TC-032  |         |        |      |         |         |
| TC-033  |         |        |      |         |         |
| TC-034  |         |        |      |         |         |

### 7.2 缺陷记录模板

```markdown
## 缺陷编号：BUG-XXX
**发现时间**：YYYY-MM-DD HH:MM
**测试用例**：TC-XXX
**严重程度**：P0 / P1 / P2 / P3
**影响范围**：

**复现步骤**：
1.
2.
3.

**预期结果**：

**实际结果**：

**错误日志**：
```
```

**修复负责人**：
**修复方案**：
**修复 PR**：
**验证日期**：
```

---

## 八、测试环境清理

```bash
# 清理测试数据
psql -U postgres -d MeowFlow -c "DELETE FROM wf_execution WHERE workflow_id LIKE 'test-%';"
psql -U postgres -d MeowFlow -c "DELETE FROM workflow WHERE id LIKE 'test-%';"
psql -U postgres -d MeowFlow -c "DELETE FROM order_a; DELETE FROM order_b; DELETE FROM order_c;"

# 清理 Redis 测试缓存
redis-cli FLUSHDB

# 清理 RabbitMQ 测试队列
rabbitmqctl delete_queue workflow.test.queue

# 清理 PG 测试日志
psql -U postgres -d MeowFlow -c "DELETE FROM trace_log WHERE trace_id LIKE 'test-%';"
psql -U postgres -d MeowFlow -c "DELETE FROM app_log WHERE trace_id LIKE 'test-%';"
psql -U postgres -d MeowFlow -c "DELETE FROM llm_call_log WHERE trace_id LIKE 'test-%';"
psql -U postgres -d MeowFlow -c "DELETE FROM biz_execution_log WHERE execution_id LIKE 'test-%';"

# 清理本地兜底日志文件
rm -f /tmp/trace-log-fallback.log
```

---

## 九、附录

### 9.1 推荐测试工具

| 工具 | 用途 |
|------|------|
| **Postman** | API 接口测试 |
| **JMeter** | 压力测试 |
| **ChaosBlade** | 故障注入 |
| **wrk** | HTTP 压测 |
| **tcpdump** | 网络抓包分析 |
| **Arthas** | 线上诊断 |

### 9.2 推荐的 Mock 服务

```java
// MockController.java - 用于注入延迟和失败
@RestController
@RequestMapping("/mock-email")
public class MockEmailController {

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody EmailRequest request,
                                  @RequestParam(defaultValue = "0") long delay,
                                  @RequestParam(defaultValue = "200") int status) {
        // 记录调用
        log.info("Mock email received: from={}, callCount={}", request, callCount.incrementAndGet());

        // 注入延迟
        if (delay > 0) {
            try { Thread.sleep(delay); } catch (InterruptedException e) {}
        }

        // 注入失败
        if (status != 200) {
            return ResponseEntity.status(status).body("Mock error");
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}
```

### 9.3 测试 Checklist

- [ ] Mock 服务已部署
- [ ] 数据库已准备好测试数据
- [ ] Redis 已清理
- [ ] RabbitMQ 队列已清空
- [ ] PostgreSQL 扩展已启用（pg_trgm + zhparser）
- [ ] 监控告警已开启
- [ ] 测试脚本已准备好
- [ ] 异常处理预案已就绪

---

**文档版本：v1.1**
**最后更新：2026-07-11**
**适用于：喵流 (MeowFlow) 全流程跑通后的数据一致性验证**

**v1.1 变更**：
- 新增 P4 级架构异常测试用例 TC-011 ~ TC-034（共 24 个）
- 测试来源：架构文档 `architecture.md` 中识别的异常场景
- 章节结构调整：原「六、测试结果记录表」改为「七、」，新增「六、P4 级架构异常测试用例」