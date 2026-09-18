# 喵流 (MeowFlow) - 交叉引用文档

> 本文档建立 **测试用例 ↔ 技术栈 ↔ 模块** 三者之间的交叉引用关系
> 用于在数据一致性测试中快速定位风险点对应的技术组件和代码模块
>
> 📌 **日志存储说明**：本系统所有日志统一存储到 PostgreSQL（JSONB + GIN 索引 + tsvector 全文检索），不再使用 Elasticsearch。详见 [`../modules/STACK_SUMMARY.md`](../modules/STACK_SUMMARY.md) 第十节。

---

## 一、文档导航

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        三份文档的关系                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   ┌────────────────────────┐                                           │
│   │  STACK_SUMMARY.md      │  ← 技术栈与模块文档                        │
│   │  (技术栈汇总)          │                                           │
│   └───────────┬────────────┘                                           │
│               │                                                          │
│               │ 引用                                                     │
│               ▼                                                          │
│   ┌────────────────────────┐         ┌────────────────────────┐       │
│   │  CROSS_REFERENCE.md    │ ◄────►  │ DATA_CONSISTENCY_TEST.md│       │
│   │  (本文档 - 交叉引用)   │  引用   │  (数据一致性测试)        │       │
│   └────────────────────────┘         └────────────────────────┘       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、测试用例 → 技术栈 → 模块 映射表

### 2.1 完整映射关系

| 测试用例 | 风险点 | 涉及技术栈 | 涉及模块 | 涉及类/组件 |
|---------|-------|----------|---------|-----------|
| **TC-001** | HTTP 节点重复执行 | Resilience4j（重试）、MyBatis-Plus | MeowFlow-workflow | `HttpExecutor`、`WorkflowEngine` |
| **TC-002** | 用户权限缓存不一致 | Redis、Sa-Token | MeowFlow-user | `UserService`、`PermissionService`、`JwtAuthenticationFilter` |
| **TC-003** | 分布式事务部分失败 | Spring Transaction、MyBatis-Plus | MeowFlow-workflow | `WorkflowService`、`NodeExecutor` |
| **TC-004** | RabbitMQ 消息丢失 | RabbitMQ、Spring AMQP | MeowFlow-executor | `TaskProducer`、`TaskConsumer` |
| **TC-005** | RabbitMQ 消息重复消费 | Redis（幂等）、RabbitMQ | MeowFlow-executor | `TaskConsumer`、`IdempotentService` |
| **TC-006** | 工作流版本切换原子性 | MyBatis-Plus、Redis（缓存） | MeowFlow-workflow | `WorkflowEngine`、`WorkflowVersionService` |
| **TC-007** | 节点重试副作用 | Resilience4j、Redis | MeowFlow-workflow | `AbstractNodeExecutor`、各节点 Executor |
| **TC-008** | 雪花 ID 时钟回拨 | - | MeowFlow-common | `IdGenerator` |
| **TC-009** | 链路追踪日志丢失 | PostgreSQL（JSONB + GIN 索引）、TTL | MeowFlow-common | `TraceAspect`、`LogbackDbAppender` |
| **TC-010** | 用户上下文异步透传 | Alibaba TTL、Spring Async | MeowFlow-common | `UserContextHolder`、`TraceContextHolder` |
| **TC-011** | 未知节点类型 | NodeRegistry | MeowFlow-workflow | `NodeRegistry.get()` |
| **TC-012** | 节点配置校验失败 | NodeExecutor 接口 | MeowFlow-workflow | `NodeExecutor.validate()` |
| **TC-013** | DAG 环依赖 | MyBatis-Plus | MeowFlow-workflow | `DAGSorter.dagSort()` |
| **TC-014** | 并行节点部分失败 | Java 并发 | MeowFlow-workflow | `ParallelExecutor` |
| **TC-015** | Wait 节点超时 | Java Thread | MeowFlow-workflow | `WaitExecutor` |
| **TC-016** | LLM 模型路由全部熔断 | Resilience4j | MeowFlow-infra | `ModelRouter`、`FallbackChain`、`CircuitBreaker` |
| **TC-017** | LLM 首包探测超时 | ScheduledExecutor | MeowFlow-infra | `StreamCallbackDecorator` |
| **TC-018** | LLM SSE 流式响应中断 | Spring WebFlux | MeowFlow-workflow | `TraceableSseEmitter` |
| **TC-019** | Embedding 服务不可用 | Spring AI | MeowFlow-infra | `EmbeddingClient`、`VectorSearchChannel` |
| **TC-020** | 向量数据库不可达 | Milvus SDK | MeowFlow-infra | `VectorSearchChannel.search()` |
| **TC-021** | 关键词检索无结果 | PostgreSQL tsvector | MeowFlow-infra | `KeywordSearchChannel` |
| **TC-022** | 后处理器链异常 | Resilience4j | MeowFlow-infra | `PostProcessorChain.execute()` |
| **TC-023** | Webhook 签名验证失败 | Sa-Token | MeowFlow-workflow | `WebhookTriggerExecutor` |
| **TC-024** | Webhook 时间戳过期 | - | MeowFlow-workflow | `WebhookTriggerExecutor` |
| **TC-025** | Schedule 触发器堆积 | Quartz | MeowFlow-workflow | `ScheduleTriggerExecutor` |
| **TC-026** | 网关限流触发 | Spring Cloud Gateway | MeowFlow-common | `RateLimiter` |
| **TC-027** | JWT Token 过期 | Sa-Token | MeowFlow-user | `JwtAuthenticationFilter` |
| **TC-028** | Nginx 502/504 | Nginx | 运维层 | `nginx.conf` upstream 配置 |
| **TC-029** | 线程池队列打满 | ThreadPoolExecutor | MeowFlow-common | `ThreadPoolConfig` |
| **TC-030** | Redis Lua 脚本失败 | Redis、Spring Data Redis | MeowFlow-common | `RateLimitByRedis` |
| **TC-031** | Snowflake ID 序列耗尽 | - | MeowFlow-common | `IdGenerator` |
| **TC-032** | 告警静默期 | PostgreSQL | MeowFlow-monitor | `AlertService` |
| **TC-033** | 告警渠道全部失败 | 钉钉/邮件/短信 SDK | MeowFlow-monitor | `AlertService.sendAlert()` |
| **TC-034** | Trace 链路丢失 | Spring AOP | MeowFlow-common | `TraceAspect` |

### 2.2 风险等级与修复难度

| 用例 | 风险等级 | 修复难度 | 涉及代码文件数 |
|------|---------|---------|--------------|
| TC-001 | 🔴 高 | ⭐⭐ 中 | 3-5 个 |
| TC-002 | 🔴 高 | ⭐ 简单 | 1-2 个 |
| TC-003 | 🔴 高 | ⭐⭐⭐⭐ 高 | 5-10 个（架构改造） |
| TC-004 | 🟡 中 | ⭐⭐ 中 | 2-3 个（配置 + 代码） |
| TC-005 | 🟡 中 | ⭐⭐ 中 | 2-3 个 |
| TC-006 | 🟠 中 | ⭐⭐ 中 | 2-3 个 |
| TC-007 | 🟡 中 | ⭐⭐⭐ 高 | 5-8 个 |
| TC-008 | 🟢 低 | ⭐ 简单 | 1 个 |
| TC-009 | 🟢 低 | ⭐⭐ 中 | 2-3 个 |
| TC-010 | 🟢 低 | ⭐⭐ 中 | 2-3 个 |

---

## 三、按技术栈维度查询

### 3.1 PostgreSQL 相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│              PostgreSQL 涉及的所有风险点（核心数据 + 日志存储）            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  PostgreSQL 在系统中的用途：                                            │
│  • 业务数据主存储（工作流定义、执行记录、用户、权限）                    │
│  • 日志存储（业务执行日志、链路追踪、应用日志、LLM 调用日志）            │
│  • 全文检索（pg_trgm + zhparser）                                       │
│  • 审计日志                                                            │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-003   │ 分布式事务部分失败                              │          │
│  │ TC-006   │ 工作流执行快照存储                              │          │
│  │ TC-007   │ 数据库 INSERT 幂等性                           │          │
│  │ TC-009   │ 链路追踪日志写入 PG 失败/丢失                  │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  PostgreSQL 配置检查项：                                                 │
│  • 隔离级别设置（READ COMMITTED）                                       │
│  • 连接池大小（HikariCP）                                              │
│  • 慢查询日志                                                            │
│  • 唯一约束/索引是否覆盖幂等键                                          │
│  • JSONB 字段是否建 GIN 索引                                            │
│  • 是否启用 pg_trgm + zhparser 扩展                                    │
│  • 日志表是否按月分区                                                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Redis 相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Redis 涉及的所有风险点                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Redis 在系统中的用途：                                                  │
│  • 用户会话/Token 缓存                                                   │
│  • 用户权限缓存                                                          │
│  • 限流计数                                                              │
│  • 幂等键存储                                                            │
│  • 工作流定义缓存                                                        │
│  • 分布式锁                                                              │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-002   │ 用户权限缓存与 DB 不一致                      │          │
│  │ TC-005   │ MQ 重复消费 → 依赖 Redis 幂等键              │          │
│  │ TC-006   │ 工作流定义缓存版本切换                         │          │
│  │ TC-007   │ 节点重试 → 部分场景依赖 Redis                │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  Redis 配置检查项：                                                      │
│  • 是否开启 AOF 持久化                                                   │
│  • 主从复制策略                                                          │
│  • 集群模式下的 Slot 分配                                                │
│  • 内存淘汰策略                                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 RabbitMQ 相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RabbitMQ 涉及的所有风险点                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  RabbitMQ 在系统中的用途：                                              │
│  • 工作流异步触发                                                        │
│  • 节点执行任务分发                                                      │
│  • 通知消息发送                                                          │
│  • 分布式事件广播                                                        │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-004   │ 消息丢失（消费者宕机、Broker 重启）            │          │
│  │ TC-005   │ 消息重复消费                                   │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  RabbitMQ 配置检查项：                                                   │
│  • 队列是否持久化（durable=true）                                       │
│  • 消息是否持久化（deliveryMode=2）                                     │
│  • 是否使用 Quorum 队列（替代 Classic）                                 │
│  • Publisher Confirms 是否开启                                           │
│  • Consumer 是否手动 ACK                                                │
│  • 死信队列配置                                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.4 PostgreSQL 日志写入相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│           PostgreSQL 存日志 涉及的所有风险点                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  PG 存日志的关键组件：                                                   │
│  • `LogbackDbAppender`：Logback 自定义 Appender，异步批量写日志         │
│  • `TraceAspect`：AOP 拦截 @TraceNode 注解，写入 trace_log 表           │
│  • `biz_execution_log`、`app_log`、`llm_call_log` 表                  │
│  • `pg_trgm` + `zhparser` 全文检索                                      │
│  • `GIN` 索引加速 JSONB 和 tsvector 查询                                │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-009   │ 链路追踪日志丢失（写入 PG 失败）              │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  检查项：                                                                │
│  • 异步批量写入是否配置（避免阻塞业务）                                  │
│  • 写日志失败时是否有降级方案（本地文件兜底）                            │
│  • JSONB 字段是否建 GIN 索引                                            │
│  • 日志表是否按月分区（避免单表过大）                                    │
│  • 大字段（Prompt/Response）是否单独存储                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.5 Resilience4j 相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  Resilience4j 涉及的所有风险点                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Resilience4j 在系统中的用途：                                          │
│  • 限流（RateLimiter）                                                  │
│  • 熔断（CircuitBreaker）                                              │
│  • 重试（Retry）                                                        │
│  • 舱壁隔离（Bulkhead）                                                │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-001   │ HTTP 节点重复执行（重试机制副作用）            │          │
│  │ TC-007   │ 节点重试副作用                                 │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  Resilience4j 配置检查项：                                              │
│  • 各节点是否正确配置重试次数                                            │
│  • 幂等性检查是否到位                                                    │
│  • 熔断器降级方法是否完善                                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.6 Alibaba TTL 相关风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Alibaba TTL 涉及的所有风险点                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  TTL 在系统中的用途：                                                    │
│  • 用户上下文透传                                                        │
│  • 链路追踪上下文透传                                                    │
│  • 线程池任务上下文传递                                                  │
│                                                                          │
│  对应测试用例：                                                          │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-010   │ 用户上下文异步透传                             │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  TTL 配置检查项：                                                        │
│  • 所有线程池是否都用 TtlExecutors 包装                                 │
│  • CompletableFuture 是否处理上下文                                     │
│  • SSE/WebSocket 长连接场景                                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、按模块维度查询

### 4.1 MeowFlow-common 涉及风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  MeowFlow-common 涉及的测试用例                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  模块职责：通用基础设施、线程池、限流熔断、链路追踪、日志写入组件        │
│                                                                          │
│  涉及的测试用例：                                                        │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-008   │ 雪花 ID 时钟回拨（IdGenerator）               │          │
│  │ TC-009   │ 链路追踪日志丢失（LogbackDbAppender）         │          │
│  │ TC-010   │ 用户上下文异步透传（UserContextHolder）       │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  相关代码路径：                                                          │
│  • src/main/java/com/MeowFlow/common/util/IdGenerator.java             │
│  • src/main/java/com/MeowFlow/common/trace/TraceAspect.java            │
│  • src/main/java/com/MeowFlow/common/context/UserContextHolder.java    │
│  • src/main/java/com/MeowFlow/common/config/ThreadPoolConfig.java      │
│  • src/main/java/com/MeowFlow/common/log/LogbackDbAppender.java        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 MeowFlow-workflow 涉及风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                MeowFlow-workflow 涉及的测试用例                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  模块职责：工作流定义、执行引擎、节点执行器                               │
│                                                                          │
│  涉及的测试用例：                                                        │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-001   │ HTTP 节点重复执行（HttpExecutor）             │          │
│  │ TC-003   │ 分布式事务（WorkflowEngine）                  │          │
│  │ TC-006   │ 版本切换原子性（WorkflowVersionService）       │          │
│  │ TC-007   │ 节点重试副作用（AbstractNodeExecutor）         │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  相关代码路径：                                                          │
│  • src/main/java/com/MeowFlow/workflow/engine/WorkflowEngine.java     │
│  • src/main/java/com/MeowFlow/workflow/engine/executor/               │
│      ├─ tool/HttpExecutor.java                                       │
│      ├─ tool/DatabaseExecutor.java                                   │
│      ├─ notify/EmailNotifyExecutor.java                             │
│      └─ AbstractNodeExecutor.java                                    │
│  • src/main/java/com/MeowFlow/workflow/service/                       │
│      ├─ WorkflowService.java                                        │
│      └─ WorkflowVersionService.java                                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.3 MeowFlow-user 涉及风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  MeowFlow-user 涉及的测试用例                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  模块职责：用户管理、权限管理、认证授权                                  │
│                                                                          │
│  涉及的测试用例：                                                        │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-002   │ 用户权限缓存与 DB 不一致                      │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  相关代码路径：                                                          │
│  • src/main/java/com/MeowFlow/user/service/UserService.java            │
│  • src/main/java/com/MeowFlow/user/service/PermissionService.java      │
│  • src/main/java/com/MeowFlow/user/security/JwtAuthenticationFilter.java│
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.4 MeowFlow-executor 涉及风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│                MeowFlow-executor 涉及的测试用例                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  模块职责：任务调度、节点管理、分布式执行                                │
│                                                                          │
│  涉及的测试用例：                                                        │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-004   │ RabbitMQ 消息丢失                              │          │
│  │ TC-005   │ RabbitMQ 消息重复消费                          │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  相关代码路径：                                                          │
│  • src/main/java/com/MeowFlow/executor/mq/TaskProducer.java            │
│  • src/main/java/com/MeowFlow/executor/mq/TaskConsumer.java            │
│  • src/main/java/com/MeowFlow/executor/service/IdempotentService.java  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.5 MeowFlow-monitor 涉及风险点

```
┌─────────────────────────────────────────────────────────────────────────┐
│              MeowFlow-monitor 涉及的测试用例                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  模块职责：日志查询、告警管理、指标统计                                  │
│                                                                          │
│  涉及的测试用例：                                                        │
│  ┌──────────┬──────────────────────────────────────────────┐          │
│  │ TC-009   │ 链路追踪日志查询性能/完整性                   │          │
│  └──────────┴──────────────────────────────────────────────┘          │
│                                                                          │
│  相关代码路径：                                                          │
│  • src/main/java/com/MeowFlow/monitor/service/LogQueryService.java     │
│  • src/main/java/com/MeowFlow/monitor/service/AlertService.java        │
│  • src/main/java/com/MeowFlow/monitor/repository/                      │
│      ├─ BizExecutionLogRepository.java                                │
│      ├─ TraceLogRepository.java                                       │
│      └─ AppLogRepository.java                                         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、按代码修复点查询

### 5.1 修复点 1：HTTP 节点幂等（TC-001）

```
┌─────────────────────────────────────────────────────────────────────────┐
│  涉及文件：                                                              │
│  • MeowFlow-workflow/src/main/java/com/MeowFlow/workflow/                │
│      engine/executor/tool/HttpExecutor.java                           │
│  • MeowFlow-common/src/main/java/com/MeowFlow/common/                    │
│      util/IdempotencyKeyGenerator.java                                │
│                                                                          │
│  修复方案：                                                              │
│  • 为每个节点生成 Idempotency-Key                                      │
│  • 通过 HTTP Header 传给外部服务                                       │
│  • 外部服务根据 Key 去重                                                │
│                                                                          │
│  测试时验证：                                                            │
│  • Mock 服务收到重复 Key 时返回原响应                                   │
│  • 检查 wf_node_execution.retry_count 字段                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 修复点 2：缓存失效策略（TC-002）

```
┌─────────────────────────────────────────────────────────────────────────┐
│  涉及文件：                                                              │
│  • MeowFlow-user/src/main/java/com/MeowFlow/user/service/                │
│      UserService.java                                                  │
│  • MeowFlow-user/src/main/java/com/MeowFlow/user/security/               │
│      JwtAuthenticationFilter.java                                      │
│                                                                          │
│  修复方案：                                                              │
│  • 修改用户角色时立即删除 Redis 缓存                                    │
│  • 通过 MQ 广播失效消息给其他实例                                       │
│  • 本地缓存（Caffeine）设置短 TTL                                      │
│                                                                          │
│  测试时验证：                                                            │
│  • 修改角色后立即访问，看权限是否生效                                   │
│  • 检查 Redis 中的缓存是否被删除                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.3 修复点 3：Saga 事务（TC-003）

```
┌─────────────────────────────────────────────────────────────────────────┐
│  涉及文件：                                                              │
│  • MeowFlow-workflow/src/main/java/com/MeowFlow/workflow/                │
│      engine/WorkflowEngine.java                                       │
│  • MeowFlow-workflow/src/main/java/com/MeowFlow/workflow/                │
│      engine/executor/AbstractNodeExecutor.java                        │
│  • 新增：                                                                │
│      engine/CompensationEngine.java                                   │
│      engine/state/NodeExecutionStateMachine.java                      │
│                                                                          │
│  修复方案：                                                              │
│  • 每个节点独立事务                                                     │
│  • 节点状态机：PENDING → RUNNING → SUCCESS/FAILED/COMPENSATED         │
│  • 失败时调用反向补偿节点                                               │
│  • 引入本地消息表保证最终一致性                                         │
│                                                                          │
│  测试时验证：                                                            │
│  • 模拟中间节点失败                                                     │
│  • 检查后续节点是否执行                                                 │
│  • 检查前面节点是否回滚（如果有补偿）                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.4 修复点 4：MQ 可靠性配置（TC-004、TC-005）

```
┌─────────────────────────────────────────────────────────────────────────┐
│  涉及文件：                                                              │
│  • MeowFlow-executor/src/main/java/com/MeowFlow/executor/                │
│      config/RabbitMQConfig.java                                       │
│      mq/TaskProducer.java                                             │
│      mq/TaskConsumer.java                                             │
│                                                                          │
│  修复方案：                                                              │
│  • 队列使用 Quorum 模式                                                 │
│  • 开启 Publisher Confirms                                              │
│  • Consumer 手动 ACK                                                    │
│  • 消费端幂等处理（基于 Redis）                                         │
│  • 死信队列配置                                                         │
│                                                                          │
│  测试时验证：                                                            │
│  • Kill 消费者进程，看消息是否重发                                      │
│  • 重发相同消息，看是否被去重                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.5 修复点 5：PG 日志写入降级（TC-009）

```
┌─────────────────────────────────────────────────────────────────────────┐
│  涉及文件：                                                              │
│  • MeowFlow-common/src/main/java/com/MeowFlow/common/                    │
│      log/LogbackDbAppender.java                                       │
│  • src/main/resources/logback-spring.xml                              │
│                                                                          │
│  修复方案：                                                              │
│  • 异步批量写入 PG（batchSize=500, flushIntervalMs=2000）             │
│  • 写入失败时降级到本地文件（/tmp/trace-log-fallback.log）             │
│  • 异常时告警                                                            │
│  • 后台线程定时把兜底文件重新写入 PG                                    │
│                                                                          │
│  测试时验证：                                                            │
│  • 压测时观察 PG 日志是否齐全                                          │
│  • 模拟 PG 不可用，验证兜底文件能记录                                  │
│  • 恢复后验证兜底数据能补回                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、快速查询清单

### 6.1 当出现以下问题时，参考对应测试用例

| 现象 | 可能风险 | 参考测试 |
|------|---------|---------|
| 用户收到重复邮件 | HTTP 节点重试副作用 | TC-001 |
| 改完权限后立刻访问仍被拒绝 | 缓存未失效 | TC-002 |
| 工作流状态错乱，部分节点执行部分未执行 | 分布式事务问题 | TC-003 |
| 触发工作流但没有执行记录 | MQ 消息丢失 | TC-004 |
| 工作流被执行了 2 次 | MQ 重复消费 | TC-005 |
| 工作流执行到一半突然用了新版本 | 版本切换原子性 | TC-006 |
| 节点重试导致外部副作用被触发多次 | 重试机制 | TC-007 |
| 系统时间回退后整个服务不可用 | 雪花 ID 时钟回拨 | TC-008 |
| PG 中查不到某个执行的链路日志 | 追踪日志丢失 | TC-009 |
| 异步任务日志中的 userId 错误 | 上下文透传失败 | TC-010 |

### 6.2 测试执行顺序建议

```
推荐执行顺序（由浅入深）：

第一步：环境验证（TC-009、TC-010）
  └─ 这些是基础能力，先确认能跑通

第二步：单服务风险（TC-002、TC-008）
  └─ 修复简单，影响范围明确

第三步：MQ 可靠性（TC-004、TC-005）
  └─ 涉及配置修改，需要重启服务

第四步：核心业务流（TC-001、TC-006、TC-007）
  └─ 涉及核心工作流引擎

第五步：架构级修复（TC-003）
  └─ 改动最大，最后处理
```

---

## 七、文档版本

| 文档 | 路径 | 版本 | 最后更新 |
|------|------|------|---------|
| 测试总览 | `docs/technical/test/README.md` | v1.0 | 2026-07-13 |
| 数据一致性测试 | `docs/technical/test/DATA_CONSISTENCY_TEST.md` | v1.1 | 2026-07-11 |
| 交叉引用（本文档） | `docs/technical/test/CROSS_REFERENCE.md` | v1.3 | 2026-07-13 |
| 单元测试规范 | `docs/technical/test/UNIT_TEST_GUIDE.md` | v1.0 | 2026-07-13 |
| 集成测试规范 | `docs/technical/test/INTEGRATION_TEST_GUIDE.md` | v1.0 | 2026-07-13 |
| API 契约测试 | `docs/technical/test/CONTRACT_TEST_GUIDE.md` | v1.0 | 2026-07-13 |
| 性能测试 | `docs/technical/test/PERFORMANCE_TEST_GUIDE.md` | v1.0 | 2026-07-13 |
| 覆盖率指南 | `docs/technical/test/COVERAGE_GUIDE.md` | v1.0 | 2026-07-13 |
| 最佳实践 | `docs/technical/test/BEST_PRACTICES.md` | v1.0 | 2026-07-13 |
| 常见问题 FAQ | `docs/technical/test/FAQ.md` | v1.0 | 2026-07-13 |

**变更说明 v1.3（2026-07-13）**：
- 新增 8 份测试文档（README、UNIT_TEST_GUIDE、INTEGRATION_TEST_GUIDE、CONTRACT_TEST_GUIDE、PERFORMANCE_TEST_GUIDE、COVERAGE_GUIDE、BEST_PRACTICES、FAQ）
- 测试文档体系从 2 份扩展到 10 份
- 文档结构按层次组织：总览 → 规范 → 实践 → FAQ

**变更说明 v1.2**：
- 测试用例从 10 个扩展到 **34 个**（新增 P4 级架构异常测试用例 TC-011 ~ TC-034）
- 测试来源：架构文档（`architecture.md`）识别的异常场景
- 涵盖：节点执行层、LLM 路由、知识库检索、触发器、网关、基础设施、监控告警 7 大类

**变更说明 v1.1**：
- 移除所有 Elasticsearch 相关内容和引用
- 日志存储方案改为 PostgreSQL（JSONB + GIN 索引 + tsvector 全文检索）
- TC-009 从「ES 链路追踪日志丢失」改为「PG 链路追踪日志丢失」
- 修复点 5 新增「PG 日志写入降级」方案

---

**文档版本：v1.3**
**最后更新：2026-07-13**
**作用：测试用例 ↔ 技术栈 ↔ 模块 三向交叉引用**