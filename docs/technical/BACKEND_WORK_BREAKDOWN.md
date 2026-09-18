# 猪小能（MeowFlow）后端工作拆解

> 本文基于 `docs/PRD.md`、`docs/technical/architecture.md`、`docs/technical/database.md`、`docs/technical/scalability.md`、`docs/technical/modules/*.md` 以及 `scripts/sql/01-schema.sql` 整理而成，作为后端工程实施的待办清单。
>
> 不重复 PRD/架构文档原文，只做任务拆分与重组；具体技术细节请回到对应模块文档查阅。

---

## 1. 当前 backend 现状（基线）

| 项目 | 现状 |
|---|---|
| `backend/meowflow/pom.xml` | 已存在，定义父 POM：Java 17、Spring Boot 3.2.0、Spring Cloud 2023.0.0、Spring Cloud Alibaba 2022.0.0.0、MyBatis-Plus 3.5.5、Sa-Token 1.37.0、Knife4j 4.3.0、Hutool 5.8.23、Transmittable ThreadLocal 2.14.5、Resilience4j 2.2.0 |
| `backend/meowflow/meowflow-*` 子模块 | **未创建**（pom 中只声明了 `<modules>`，未生成目录） |
| Java 源码（Controller / Service / Entity / Mapper） | **全部空白** |
| 数据库脚本 | `scripts/sql/01-schema.sql` 已存在，按 `mf_<module>_<entity>` 前缀定义 23 张业务表 |
| 配置文件 `application*.yml` | **未生成** |
| Docker / Kubernetes 部署文件 | **未生成** |
| 单元测试 / 集成测试 | **未生成** |

**结论：当前 backend 仅有 Maven 父 POM 骨架，尚无任何可运行的服务；所有功能模块都需从零搭建。**

模块预期（pom 声明 6 个，与 `STACK_SUMMARY.md` 第 5 节一致，但 `meowflow-infra` 在 pom 中**尚未声明**，需补建）：
- `meowflow-common` —— 公共基础设施（线程池、上下文、统一响应、异常、限流熔断、Snowflake、Trace）
- `meowflow-user` —— 用户/认证/权限
- `meowflow-workflow` —— 工作流管理 + 执行引擎 + 节点执行器
- `meowflow-template` —— 模板市场
- `meowflow-executor` —— 分布式执行节点管理（单体阶段内嵌于 workflow）
- `meowflow-monitor` —— 日志、指标、告警
- `meowflow-infra`（**待补**）—— AI 模型客户端、模型路由、知识库检索、MCP 工具

---

## 2. 后端技术栈摘要

> 来源：`docs/technical/architecture.md` §三、`docs/technical/modules/STACK_SUMMARY.md` §十。

| 维度 | 选型 |
|---|---|
| 编程语言 | Java 17 |
| 核心框架 | Spring Boot 3.2.x、Spring Cloud 2023.0.x、Spring Cloud Alibaba 2022.0.x |
| 认证授权 | Sa-Token（JWT/StpInterface），未来可选 OAuth2 |
| 持久层 | MyBatis-Plus 3.5.5 + PostgreSQL 15（JSONB、`pg_trgm`/`zhparser` 全文检索） |
| 缓存 | Redis 7+（Session、限流、分布式锁） |
| 消息队列 | RabbitMQ 3.12（含 `rabbitmq_delayed_message_exchange` 插件） |
| 对象存储 | MinIO（文件、日志归档） |
| HTTP 客户端 | OkHttp / Spring `RestTemplate` / `WebClient` |
| 限流熔断 | Resilience4j（滑动窗口限流、三态熔断器） |
| 上下文透传 | Alibaba Transmittable ThreadLocal（TTL）+ `TtlExecutors` |
| 分布式 ID | Snowflake |
| 日志 | Logback 本地文件 + PostgreSQL `JSONB + GIN` 全文检索 |
| API 文档 | Knife4j（OpenAPI 3） |
| 工具库 | Hutool、Jackson、Guava |
| AI 抽象 | 统一 `ChatClient` + 多模型路由（OpenAI / Anthropic / 通义 / 文心）+ Rerank |
| 检索 | 向量（pgvector 或外部向量库）+ 关键词 + 图谱 |
| MCP | Model Context Protocol 工具注册与执行 |
| 部署 | Docker Compose（开发）/ Kubernetes（生产微服务） |
| 监控 | 自建指标 + Micrometer + PostgreSQL 日志表 + 钉钉/邮件/短信告警 |

数据库表前缀约定：`mf_<module>_<entity>`（见 `docs/RENAME_LOG.md` 与 `scripts/sql/01-schema.sql`）。

---

## 3. 工作项总览（按模块 + 优先级）

> 估时按"人·天"计（1 天 = 8 工时），颗粒度 0.5 ~ 2 天。

### 3.1 汇总表

| 编号 | 标题 | 模块 | 优先级 | 估时(天) |
|---|---|---|---|---|
| BE-001 | 创建 7 个 Maven 子模块骨架与父 POM 升级 | 公共基础 | P0 | 1.0 |
| BE-002 | 配置统一 `application*.yml`（dev/staging/prod） | 公共基础 | P0 | 0.5 |
| BE-003 | 引入 MyBatis-Plus + PostgreSQL + 多数据源配置 | 公共基础 | P0 | 1.0 |
| BE-004 | 引入 Redis 与 `RedisTemplate` 统一封装 | 公共基础 | P0 | 0.5 |
| BE-005 | 引入 RabbitMQ 与延时交换机声明 | 公共基础 | P0 | 1.0 |
| BE-006 | 8 个专用线程池 + TtlExecutors 封装 | 公共基础 | P0 | 1.0 |
| BE-007 | UserContext / TraceContext + TTL 透传 | 公共基础 | P0 | 1.0 |
| BE-008 | 链路追踪 TraceId 生成、AOP 切面 | 公共基础 | P0 | 1.0 |
| BE-009 | 统一响应体 `Result<T>` + 错误码枚举 | 公共基础 | P0 | 0.5 |
| BE-010 | 三层异常体系（Biz/Node/Workflow）+ GlobalExceptionHandler | 公共基础 | P0 | 1.0 |
| BE-011 | 两维度幂等（接口 token + 业务唯一键） | 公共基础 | P0 | 1.0 |
| BE-012 | Snowflake ID 生成器（数据中心+机器 ID 可配置） | 公共基础 | P0 | 0.5 |
| BE-013 | Resilience4j 限流（Redis 滑动窗口）+ 三态熔断 | 公共基础 | P1 | 1.5 |
| BE-014 | Knife4j / OpenAPI 聚合配置 | 公共基础 | P1 | 0.5 |
| BE-015 | 统一日志组件（Logback JSON + PG 写入器） | 公共基础 | P1 | 1.5 |
| BE-016 | Sa-Token 整合 + StpInterface 实现 | 用户 | P0 | 1.0 |
| BE-017 | 登录/刷新/登出/当前用户接口 | 用户 | P0 | 1.0 |
| BE-018 | 用户 CRUD + 密码修改/重置/启停接口 | 用户 | P0 | 1.5 |
| BE-019 | 组织 / 岗位 树形接口 | 用户 | P0 | 1.0 |
| BE-020 | 角色与权限（菜单/按钮）CRUD | 用户 | P0 | 1.5 |
| BE-021 | 数据权限（全部/本部门/本部门及以下）过滤 | 用户 | P1 | 1.5 |
| BE-022 | 登录失败限流（Redis 计数）+ 登录日志 | 用户 | P0 | 1.0 |
| BE-023 | 操作审计日志（`mf_sys_audit_log`）落库与查询 | 用户 | P1 | 1.0 |
| BE-024 | 工作流 CRUD + 版本管理（创建/发布/停止/复制/回滚） | 工作流 | P0 | 2.0 |
| BE-025 | 工作流分类（`mf_wf_category`）+ 分组（`mf_wf_group`） | 工作流 | P0 | 1.0 |
| BE-026 | 工作流服务 `WorkflowService` + Controller（`/api/v1/workflows`） | 工作流 | P0 | 1.5 |
| BE-027 | 执行接口（同步/异步/取消/详情/节点/日志） | 工作流 | P0 | 1.5 |
| BE-028 | 触发器：手动 + Webhook（签名校验 + IP 白名单） | 工作流 | P0 | 1.5 |
| BE-029 | 触发器：定时（Cron + RabbitMQ 延时队列） | 工作流 | P1 | 1.5 |
| BE-030 | 触发器：表单 / 消息（v2，MVP 仅保留接口骨架） | 工作流 | P2 | 0.5 |
| BE-031 | `NodeDefinition` + `WorkflowDefinition` 实体与 JSON Schema 校验 | 工作流 | P0 | 1.5 |
| BE-032 | DAG 拓扑排序（Kahn）+ 循环依赖检测 | 工作流 | P0 | 1.0 |
| BE-033 | `WorkflowEngine` 执行引擎（上下文 + 调度 + 重试） | 工作流 | P0 | 2.0 |
| BE-034 | `NodeRegistry`（Spring Bean 自动注册）+ `AbstractNodeExecutor` | 工作流 | P0 | 1.5 |
| BE-035 | 触发器节点执行器（manual/webhook/cron） | 工作流 | P0 | 1.0 |
| BE-036 | LLM 节点执行器（含 ChatClient、模型路由、降级） | 工作流 | P0 | 2.0 |
| BE-037 | 条件 / 分支 / 循环 节点执行器 | 工作流 | P0 | 1.5 |
| BE-038 | HTTP 节点执行器（OkHttp + 鉴权 + 重试） | 工作流 | P0 | 1.0 |
| BE-039 | 知识库检索节点执行器（多路融合 + Rerank） | 工作流 | P0 | 2.0 |
| BE-040 | 工具节点（MCP 工具调用）执行器 | 工作流 | P0 | 1.5 |
| BE-041 | 通知节点（钉钉/飞书/企微/邮件/短信）执行器 | 工作流 | P1 | 1.5 |
| BE-042 | 结束 / 聚合 / 变量赋值 节点执行器 | 工作流 | P0 | 1.0 |
| BE-043 | 执行上下文 `ExecutionContext`（变量解析 `$.var`） | 工作流 | P0 | 1.0 |
| BE-044 | `mf_wf_execution` / `mf_wf_node_execution` / `mf_wf_execution_log` 落库 | 工作流 | P0 | 1.0 |
| BE-045 | 模板 CRUD + 分类/标签 + 提交审核 | 模板 | P0 | 1.5 |
| BE-046 | 模板审核流（pending/approved/rejected） | 模板 | P0 | 1.0 |
| BE-047 | 模板评价（评分 + 评论 + 防重复） | 模板 | P1 | 1.0 |
| BE-048 | 模板搜索（关键词 + 分类 + 排序 + 分页） | 模板 | P0 | 1.0 |
| BE-049 | 一键使用模板创建工作流 + 复制模板 | 模板 | P0 | 1.0 |
| BE-050 | 模板热门 / 最新 / 个性化推荐（基础版按评分 + 使用次数） | 模板 | P2 | 1.0 |
| BE-051 | `Task` / `TaskQueue` / `TaskContext` 模型 | 执行器 | P1 | 1.0 |
| BE-052 | `ExecutorNodeRegistry`（节点注册 + 心跳 + 健康） | 执行器 | P1 | 1.5 |
| BE-053 | 任务分发 `TaskDispatchService`（按能力 + 负载过滤） | 执行器 | P1 | 1.5 |
| BE-054 | `LoadBalancer`（随机 / 轮询 / 加权 / 最少连接） | 执行器 | P1 | 1.0 |
| BE-055 | 任务状态管理 + 超时检查 + 取消 | 执行器 | P1 | 1.5 |
| BE-056 | `ExecutorClient`（RestTemplate）+ 内部鉴权 | 执行器 | P1 | 1.0 |
| BE-057 | 执行器管理 API（注册/心跳/查询） | 执行器 | P2 | 0.5 |
| BE-058 | 执行日志写入（`mf_wf_execution_log` 流式追加） | 监控 | P0 | 1.0 |
| BE-059 | 执行日志查询接口（按 executionId + level 分页） | 监控 | P0 | 0.5 |
| BE-060 | 业务指标聚合（按工作流/日）+ API | 监控 | P1 | 1.5 |
| BE-061 | 系统指标采集（JVM / 线程池 / DB 连接） | 监控 | P1 | 1.0 |
| BE-062 | 告警规则 CRUD + 规则引擎（条件匹配） | 监控 | P1 | 1.5 |
| BE-063 | 告警通知（钉钉/邮件/短信 Webhook） | 监控 | P1 | 1.5 |
| BE-064 | 告警沉默 / 升级 / 历史查询 | 监控 | P2 | 1.0 |
| BE-065 | 每日聚合指标表（`mf_mon_metric_daily`）定时写入 | 监控 | P2 | 0.5 |
| BE-066 | `ChatClient` 接口 + OpenAI / Anthropic / 通义 / 文心 实现 | AI 基础设施 | P0 | 2.0 |
| BE-067 | `EmbeddingClient` + 向量库（pgvector / 外部）对接 | AI 基础设施 | P0 | 1.5 |
| BE-068 | `ModelRouter` 多模型路由 + 健康检查 + 降级链 | AI 基础设施 | P0 | 1.5 |
| BE-069 | 知识库管理（库 / 文档 / 分块 CRUD + 解析） | AI 基础设施 | P0 | 2.0 |
| BE-070 | 多路检索通道（向量 / 关键词 / 图谱）+ 混合 + 后处理 | AI 基础设施 | P0 | 2.0 |
| BE-071 | MCP 工具注册中心 + 执行器 + stdio/sse 适配 | AI 基础设施 | P0 | 1.5 |
| BE-072 | AI 调用日志（`mf_ai_invoke_log`）与成本统计 | AI 基础设施 | P1 | 1.0 |
| BE-073 | 集成配置（钉钉/飞书/企微/邮件/SMS）`mf_int_config` | AI 基础设施 | P1 | 1.0 |
| BE-074 | 集成发送日志 `mf_int_send_log` + 失败重试 | AI 基础设施 | P1 | 1.0 |
| BE-075 | 初始化数据库与 DDL（执行 `01-schema.sql` + 索引/触发器） | 数据库 | P0 | 1.0 |
| BE-076 | 写 Flyway / Liquibase 迁移脚本（`mf_*` 全表初始化） | 数据库 | P1 | 1.0 |
| BE-077 | 数据库账号 / Schema / 索引 / 物化视图评审 | 数据库 | P1 | 0.5 |
| BE-078 | `docker-compose.dev.yml`（PG + Redis + RabbitMQ + MinIO） | 部署 | P0 | 1.0 |
| BE-079 | Dockerfile（多阶段构建 + 镜像瘦身） | 部署 | P0 | 0.5 |
| BE-080 | Kubernetes 部署文件（Deployment / Service / Ingress / ConfigMap） | 部署 | P1 | 2.0 |
| BE-081 | 健康检查 `/actuator/health` + Prometheus 指标暴露 | 部署 | P1 | 0.5 |
| BE-082 | CI（GitHub Actions / GitLab CI）：编译 + 测试 + 构建镜像 | 部署 | P1 | 1.0 |
| BE-083 | 前端联调：Swagger 评审 + CORS + 跨域 Cookie | 前端对接 | P0 | 1.0 |
| BE-084 | Webhook 签名校验 + 幂等 token 下发 | 安全 | P0 | 1.0 |
| BE-085 | 字段级加密（敏感字段 AES）+ 密钥管理 | 安全 | P1 | 1.0 |
| BE-086 | 接口幂等 + 防重放（Redis token + 时间戳窗口） | 安全 | P1 | 1.0 |
| BE-087 | 单元测试框架（JUnit 5 + Mockito）+ 测试覆盖率门槛 | 质量 | P1 | 1.0 |
| BE-088 | 关键流程集成测试（执行引擎、鉴权、幂等） | 质量 | P1 | 1.5 |
| BE-089 | 性能压测（JMeter / wrk）+ 关键接口 P95 指标 | 质量 | P2 | 1.5 |
| BE-090 | OpenAPI 契约生成 TS SDK 给前端 | 前端对接 | P2 | 1.0 |

**合计**：90 个工作项；模块分布——公共基础 14 / 用户 8 / 工作流 21 / 模板 6 / 执行器 7 / 监控 8 / AI 基础设施 9 / 数据库 3 / 部署 5 / 安全 3 / 质量 3 / 前端对接 2；优先级——P0 共 37 项，P1 共 35 项，P2 共 10 项。

### 3.2 工作量按里程碑（见 §5）

| 里程碑 | 工作项数 | 估时(天) |
|---|---|---|
| M1 基础骨架 | 23 | ~23 |
| M2 核心业务 | 27 | ~38 |
| M3 完善加固 | 27 | ~32 |
| M4 部署上线 | 14 | ~13 |
| **合计** | **91** | **~106 人·天**（约 5 ~ 6 人 4 周工作量） |

---

## 4. 工作项明细（按模块分组）

> 每条格式：`编号 / 标题 / 优先级 / 简述 / 关键产出物 / 依赖`。

### 4.1 公共基础（meowflow-common）

- **BE-001** 创建 7 个 Maven 子模块骨架与父 POM 升级 / **P0**
  - 在 `backend/meowflow/` 下创建 `meowflow-common / user / workflow / template / executor / monitor / infra` 七个子模块的 `pom.xml` 和 `src/main/java` 目录结构；将父 POM 中 `<modules>` 补齐 `meowflow-infra`；定义子模块统一依赖（common-utils、web、security、mybatis-plus、sa-token、knife4j、hutool、ttl、resilience4j）。
  - 产出：`meowflow-common/pom.xml` … `meowflow-infra/pom.xml`。
  - 依赖：—

- **BE-002** 配置统一 `application*.yml` / **P0**
  - 提供 `application.yml`、`application-dev.yml`、`application-staging.yml`、`application-prod.yml`，涵盖 server.port、servlet context-path、logging、mybatis-plus、redis、rabbit、sa-token 配置。
  - 产出：`src/main/resources/application*.yml`。
  - 依赖：BE-001。

- **BE-003** MyBatis-Plus + PostgreSQL + 多数据源 / **P0**
  - 集成 `mybatis-plus-boot-starter`、`postgresql` 驱动；配置分页插件、逻辑删除、字段填充（create_by/update_by/create_time/update_time）；预留多数据源（biz、log）切换。
  - 产出：`MybatisPlusConfig.java`、`DataSourceConfig.java`、`MetaObjectHandler.java`。
  - 依赖：BE-001。

- **BE-004** Redis 与 `RedisTemplate` 封装 / **P0**
  - 配置 Lettuce 连接池；提供 `RedisService`（string/hash/list/set/zset/分布式锁）；统一 Key 前缀与序列化方式（Jackson2JsonRedisSerializer）。
  - 产出：`RedisConfig.java`、`RedisService.java`、`<待确认：参考 common.md §3.3 限流实现>`。
  - 依赖：BE-001。

- **BE-005** RabbitMQ 与延时交换机声明 / **P0**
  - 引入 `spring-boot-starter-amqp`；声明 `meowflow.delayed.exchange`（x-delayed-message）+ 业务队列（workflow.execute / workflow.callback / schedule.delayed）。
  - 产出：`RabbitConfig.java`、`DelayedMessageConfig.java`、`RabbitTemplate` 增强。
  - 依赖：BE-001。

- **BE-006** 8 个专用线程池 + TtlExecutors 封装 / **P0**
  - 按 `common.md` §3.1 定义：`workflow-executor / llm-call / http-request / mcp-batch / knowledge-search / notify-send / db-operation / schedule-task`，统一用 `TtlExecutors.getTtlExecutorService` 包装。
  - 产出：`ThreadPoolConfig.java`、`TtlConfig.java`、`ThreadPoolMdcInterceptor`。
  - 依赖：BE-007。

- **BE-007** UserContext / TraceContext + TTL 透传 / **P0**
  - 基于 `TransmittableThreadLocal` 定义 `UserContext`（userId / username / orgId / roleIds）、`TraceContext`（traceId / spanId / parentSpanId）；提供 Holder 与 AOP 自动设置/清理。
  - 产出：`UserContext.java`、`UserContextHolder.java`、`TraceContext.java`、`TraceContextHolder.java`、`ContextFilter.java`。
  - 依赖：BE-001。

- **BE-008** 链路追踪 TraceId 生成 + AOP / **P0**
  - 基于 `common.md` §3.4 实现 `@TraceNode` 注解 + `TraceAspect`，自动生成 traceId/Span 并写入 MDC；对外响应头返回 `X-Trace-Id`。
  - 产出：`TraceNode.java`、`TraceAspect.java`、`TraceIdGenerator.java`、`TraceMDCFilter`。
  - 依赖：BE-007。

- **BE-009** 统一响应体 `Result<T>` + 错误码枚举 / **P0**
  - `Result<T> { code, message, data, traceId, timestamp }`；`ResultCode` 枚举按业务分类（1xxx 通用、2xxx 用户、3xxx 工作流、4xxx 模板、5xxx 执行器、6xxx 监控、9xxx 系统）。
  - 产出：`Result.java`、`ResultCode.java`、`PageResult.java`。
  - 依赖：—。

- **BE-010** 三层异常体系 + GlobalExceptionHandler / **P0**
  - `BizException` / `NodeException` / `WorkflowException`，由 `GlobalExceptionHandler` 统一捕获并转换为 `Result`。
  - 产出：`BizException.java`、`NodeException.java`、`WorkflowException.java`、`GlobalExceptionHandler.java`。
  - 依赖：BE-009。

- **BE-011** 两维度幂等 / **P0**
  - 接口幂等：`@Idempotent` 注解 + 拦截器，调用前先获取 token，调用时校验；业务幂等：基于唯一键/唯一索引在 Service 层校验。
  - 产出：`@Idempotent` 注解、`IdempotentAspect` / `IdempotentInterceptor`、`IdempotentStore`（Redis SETNX）。
  - 依赖：BE-004、BE-007。

- **BE-012** Snowflake ID 生成器 / **P0**
  - 基于 Hutool 或自研实现；workerId 通过 Redis 启动时分配；提供 `IdGenerator` Bean。
  - 产出：`IdGenerator.java`、`WorkerIdAssigner.java`、`application.yml` 中配置 `meowflow.snowflake.*`。
  - 依赖：BE-004。

- **BE-013** Resilience4j 限流 + 三态熔断 / **P1**
  - 配置 `RateLimiterRegistry`（按业务 tag 划分）+ `CircuitBreakerRegistry`（CLOSED/OPEN/HALF_OPEN）；提供 `@RateLimit`、`@CircuitBreaker` 注解及 AOP。
  - 产出：`RateLimit.java` 注解、`CircuitBreaker.java` 注解、`Resilience4jConfig.java`、`application.yml` 中 `resilience4j.*` 配置块。
  - 依赖：BE-001。

- **BE-014** Knife4j / OpenAPI 聚合 / **P1**
  - 引入 `knife4j-openapi3-jakarta-spring-boot-starter`；配置分组（user/workflow/template/executor/monitor/infra）；启用 OAuth2 / Sa-Token 安全定义；统一 Bearer token 认证。
  - 产出：`OpenApiConfig.java`、`application.yml` 中 `knife4j.*` 配置块。
  - 依赖：BE-016。

- **BE-015** 统一日志组件（Logback JSON + PG 写入器） / **P1**
  - Logback 输出 JSON；自定义 `PostgresAppender` 异步批量写入 `mf_sys_oper_log` / `mf_wf_execution_log` / `mf_ai_invoke_log`，加 GIN 索引支持全文检索（`pg_trgm`）。
  - 产出：`logback-spring.xml`、`PostgresAppender.java`、`LogEvent.java`、`LogPersistenceService.java`。
  - 依赖：BE-003。

### 4.2 用户模块（meowflow-user）

- **BE-016** Sa-Token 整合 + StpInterface 实现 / **P0**
  - 引入 `sa-token-spring-boot3-starter`；实现 `StpInterface` 返回当前用户角色与权限集合；注册 Sa-Token 拦截器、跨域配置。
  - 产出：`SaTokenConfig.java`、`UserStpInterface.java`、`application.yml` 中 `sa-token.*`。
  - 依赖：BE-014、BE-007。

- **BE-017** 登录 / 刷新 / 登出 / 当前用户 / **P0**
  - 接口：`POST /api/v1/auth/login`、`POST /api/v1/auth/refresh`、`POST /api/v1/auth/logout`、`GET /api/v1/auth/me`。
  - 产出：`AuthController.java`、`AuthService.java`、`LoginRequest/Response`、`RefreshRequest`。
  - 依赖：BE-016、BE-022。

- **BE-018** 用户 CRUD + 密码 / 启停 / **P0**
  - 接口：`POST/GET/PUT/DELETE /api/v1/users`、`PUT /api/v1/users/{id}/password`、`PUT /api/v1/users/{id}/status`、`PUT /api/v1/users/{id}/reset-password`。
  - 产出：`UserController.java`、`UserService.java`、`User/Role/Permission` 实体（对应 `mf_sys_user / mf_sys_role / mf_sys_permission / mf_sys_user_role / mf_sys_role_permission`）、`UserRepository`、DTO。
  - 依赖：BE-003、BE-016。

- **BE-019** 组织 / 岗位 树形接口 / **P0**
  - 接口：`GET /api/v1/orgs/tree`、`POST/PUT/DELETE /api/v1/orgs`、`GET /api/v1/posts`、`POST /api/v1/posts`；构建组织树（含祖级列表）。
  - 产出：`OrgController.java`、`OrgService.java`、`Org` 实体（`mf_sys_org` / `mf_sys_post`）。
  - 依赖：BE-003。

- **BE-020** 角色与权限 CRUD / **P0**
  - 接口：`POST/GET/PUT/DELETE /api/v1/roles`、`POST /api/v1/roles/{id}/permissions`、`GET /api/v1/permissions/menu`、`GET /api/v1/permissions/button`；构建菜单树。
  - 产出：`RoleController.java`、`PermissionController.java`、`MenuTree DTO`。
  - 依赖：BE-018。

- **BE-021** 数据权限（全部/本部门/本部门及以下） / **P1**
  - 在 `PermissionService` 中根据角色 `dataScope` 注入 MyBatis-Plus 拦截器；提供 `@DataScope` 注解。
  - 产出：`DataScopeInterceptor.java`、`@DataScope` 注解、`DataScopeContextHolder`。
  - 依赖：BE-020。

- **BE-022** 登录失败限流 + 登录日志 / **P0**
  - Redis `INCR login:fail:{username}`，>5 次锁定 1h；记录到 `mf_sys_login_log`；异步更新 `mf_sys_user.last_login_*`。
  - 产出：`LoginLogService.java`、`LoginFailLimiter.java`、对应 Mapper。
  - 依赖：BE-004。

- **BE-023** 操作审计日志 + 查询 / **P1**
  - `@OperationLog` 注解 + AOP 切面，捕获请求 URL/参数/响应/耗时/IP，写入 `mf_sys_audit_log`（异步、批量）；提供 `GET /api/v1/audit-logs` 分页查询接口（仅管理员）。
  - 产出：`OperationLog.java` 注解、`OperationLogAspect.java`、`AuditLogController.java`。
  - 依赖：BE-015、BE-016。

### 4.3 工作流模块（meowflow-workflow）——核心

- **BE-024** 工作流 CRUD + 版本管理 / **P0**
  - 创建/更新/删除/查询/分页/发布/停止/复制/版本历史/回滚；版本表 `mf_wf_workflow_version.definition` 为 JSONB，画布定义。
  - 产出：`WorkflowController.java`（`/api/v1/workflows` 全套）、`WorkflowService.java`、`WorkflowVersionService.java`、`Workflow/WorkflowVersion` 实体。
  - 依赖：BE-003、BE-024。

- **BE-025** 工作流分类 + 分组 / **P0**
  - 分类（公共，`mf_wf_category`）树形 CRUD；分组（用户私有，`mf_wf_group`）CRUD。
  - 产出：`WorkflowCategoryController.java`、`WorkflowGroupController.java`、Service。
  - 依赖：BE-003。

- **BE-026** 工作流服务 + Controller（`/api/v1/workflows`） / **P0**
  - 提供 10 个工作流管理接口（POST/PUT/DELETE/GET 单/GET 列表/POST publish/stop/copy/GET versions/POST rollback）。
  - 产出：`WorkflowController.java`、`WorkflowService.java`、DTO。
  - 依赖：BE-024、BE-025。

- **BE-027** 执行接口（同步/异步/取消/详情/节点/日志） / **P0**
  - `POST /api/v1/executions/{workflowId}/execute`、`.../execute-async`、`GET /{executionId}`、`/{executionId}/nodes`、`/workflow/{workflowId}`、`POST /{executionId}/cancel`、`GET /{executionId}/logs`。
  - 产出：`ExecutionController.java`、`ExecutionService.java`、DTO。
  - 依赖：BE-026、BE-033。

- **BE-028** 触发器：手动 + Webhook / **P0**
  - Webhook 接口：`POST /api/v1/webhooks/{workflowId}`；签名校验（HMAC-SHA256 over body + secret）+ IP 白名单 + 时间戳防重放。
  - 产出：`WebhookController.java`、`WebhookSignatureVerifier.java`、`WebhookIpWhiteList` 配置。
  - 依赖：BE-084、BE-033。

- **BE-029** 触发器：定时（Cron + RabbitMQ 延时队列） / **P1**
  - 支持一次性和 cron；写入 `mf_wf_workflow_version.input_schema`；任务落地到 `meowflow.delayed.exchange`；消费者触发 `ExecutionService.executeAsync`。
  - 产出：`ScheduleController.java`、`DelayedTaskService.java`、`DelayedTaskConsumer.java`、`xxl-job` 集成（可选）。
  - 依赖：BE-005、BE-027。

- **BE-030** 触发器：表单 / 消息（v2 接口骨架） / **P2**
  - 保留 controller 入口与实体；具体业务逻辑留待 v2 实施。
  - 产出：`FormTriggerController.java`（占位）、`MessageTriggerController.java`（占位）。
  - 依赖：BE-027。

- **BE-031** `NodeDefinition` + `WorkflowDefinition` JSON 校验 / **P0**
  - Java 侧定义 `WorkflowDefinition(List<NodeDefinition> nodes, List<Edge> edges)`；提供 JSON Schema（v3）；启动时与保存时均做 schema 校验；支持 `$.var` 表达式解析。
  - 产出：`WorkflowDefinition.java`、`NodeDefinition.java`、`JsonSchemaValidator.java`、资源文件 `workflow.schema.json`。
  - 依赖：—。

- **BE-032** DAG 拓扑排序（Kahn） / **P0**
  - 实现 `DAGSorter.sort(WorkflowDefinition)` 返回分层执行的节点列表；含循环依赖检测，异常 `BizException(DAG_HAS_CYCLE)`。
  - 产出：`DAGSorter.java`、`DAGHasCycleException.java`。
  - 依赖：BE-031。

- **BE-033** `WorkflowEngine` 执行引擎 / **P0**
  - 引擎入口负责：构建 `ExecutionContext` → DAG 分层 → 按层并发执行 → 失败重试 → 上下文变量传递 → 写 `mf_wf_execution`/`mf_wf_node_execution`/`mf_wf_execution_log`。
  - 产出：`WorkflowEngine.java`、`ExecutionContext.java`、`NodeResult.java`、`ExecutionResult.java`。
  - 依赖：BE-032、BE-034、BE-043、BE-044。

- **BE-034** `NodeRegistry` + `AbstractNodeExecutor` / **P0**
  - Spring `ApplicationListener<ContextRefreshedEvent>` 扫描所有 `NodeExecutor` Bean，按 `nodeType` 注册；`AbstractNodeExecutor` 模板方法提供 beforeExecute/doExecute/afterExecute；`resolveVariable("$.var")` 支持 JsonPath。
  - 产出：`NodeExecutor.java` 接口、`AbstractNodeExecutor.java`、`NodeRegistry.java`、`NodeType` 枚举。
  - 依赖：BE-001。

- **BE-035** 触发器节点执行器（manual / webhook / cron） / **P0**
  - 三个 `NodeExecutor` 实现；触发器节点出参就是 `input`。
  - 产出：`ManualTriggerExecutor.java`、`WebhookTriggerExecutor.java`、`CronTriggerExecutor.java`。
  - 依赖：BE-034。

- **BE-036** LLM 节点执行器（含 ChatClient、模型路由、降级） / **P0**
  - 通过 `ChatClient` 抽象调用 LLM，支持 prompt 模板、`$.context.*` 变量注入；通过 `ModelRouter` 选择模型并支持降级；写 `mf_ai_invoke_log`。
  - 产出：`LLMExecutor.java`、`PromptTemplate.java`、`LLMNodeConfig` DTO。
  - 依赖：BE-034、BE-066、BE-068。

- **BE-037** 条件 / 分支 / 循环 节点执行器 / **P0**
  - 表达式：`a > b`、`a in [..]`、`contains`；分支用 `if/elif/else` 出边；循环支持 `for i in range` / `for item in list`。
  - 产出：`ConditionExecutor.java`、`BranchExecutor.java`、`LoopExecutor.java`、`ExpressionEvaluator.java`（参考 Aviator/AviatorPlus）。
  - 依赖：BE-034。

- **BE-038** HTTP 节点执行器 / **P0**
  - 支持 GET/POST/PUT/DELETE，Header/Query/Body 模板，超时、鉴权（Bearer/Basic/自定义）；调用失败重试（指数退避）。
  - 产出：`HttpExecutor.java`、`HttpClient.java`、`AuthStrategy` 接口。
  - 依赖：BE-034、BE-013。

- **BE-039** 知识库检索节点执行器（多路 + Rerank） / **P0**
  - 调用 `SearchChannel`（向量 / 关键词 / 混合），合并 → `DeduplicateProcessor` → `RerankProcessor` → `QualityFilterProcessor` → 输出 top-K。
  - 产出：`KnowledgeSearchExecutor.java`、`SearchContext.java`、`SearchResult.java`。
  - 依赖：BE-034、BE-070。

- **BE-040** 工具节点（MCP）执行器 / **P0**
  - 调用 `MCPToolRegistry.get(name)` → `MCPToolExecutor.execute`；支持 stdio / sse 传输。
  - 产出：`ToolExecutor.java`、MCP 适配器。
  - 依赖：BE-034、BE-071。

- **BE-041** 通知节点（钉钉/飞书/企微/邮件/短信）执行器 / **P1**
  - 通过 `IntegrationConfigService` 获取配置，按 channel 分发；失败重试。
  - 产出：`NotifyExecutor.java`、`DingtalkSender.java`、`EmailSender.java`、`SmsSender.java`（基础骨架）。
  - 依赖：BE-034、BE-073。

- **BE-042** 结束 / 聚合 / 变量赋值 节点执行器 / **P0**
  - End：把 `output` 写入 `Execution.output` JSONB；Aggregator：合并多分支输出；SetVariable：执行表达式赋值。
  - 产出：`EndExecutor.java`、`AggregatorExecutor.java`、`SetVariableExecutor.java`。
  - 依赖：BE-034。

- **BE-043** 执行上下文 `ExecutionContext`（变量解析） / **P0**
  - 维护 `Map<String,Object> variables`（含 `input`、`node.{nodeId}.output` 等）；提供 `getVariable(String jsonPath)`、`setVariable(String key, Object value)`。
  - 产出：`ExecutionContext.java`、`VariableResolver.java`。
  - 依赖：BE-007。

- **BE-044** `mf_wf_execution` 等三表落库 / **P0**
  - 创建/更新 Execution 记录（status、input、output、error_message、cost_ms、cost_token、started_at/finished_at）；按节点写 `mf_wf_node_execution`；流式追加 `mf_wf_execution_log`。
  - 产出：`ExecutionRepository.java`、`NodeExecutionRepository.java`、`ExecutionLogRepository.java`、`ExecutionPersistenceService.java`。
  - 依赖：BE-003、BE-033。

### 4.4 模板模块（meowflow-template）

- **BE-045** 模板 CRUD + 分类/标签 / **P0**
  - 接口：`POST/PUT/DELETE /api/v1/templates`、`GET /api/v1/templates/{id}`；分类/标签使用 `mf_wf_category` 与 JSONB。
  - 产出：`TemplateController.java`、`TemplateService.java`、`Template` 实体（`mf_tpl_template`）。
  - 依赖：BE-025。

- **BE-046** 模板审核流 / **P0**
  - 接口：`POST /api/v1/templates/{id}/submit`、`POST /api/v1/reviews/{id}/approve`、`POST /api/v1/reviews/{id}/reject`、`GET /api/v1/reviews/pending`。
  - 产出：`ReviewController.java`、`TemplateReviewService.java`、`TemplateReview` 实体（`mf_tpl_review`）。
  - 依赖：BE-016、BE-045。

- **BE-047** 模板评价（评分 + 评论） / **P1**
  - 接口：`POST /api/v1/templates/{id}/reviews`、`GET /api/v1/templates/{id}/reviews`；提交时防重（unique on template_id+user_id）；更新 `mf_tpl_template.score / review_count`。
  - 产出：`ReviewService.java`、`TemplateReviewController.java`。
  - 依赖：BE-046。

- **BE-048** 模板搜索 / **P0**
  - 接口：`GET /api/v1/templates?keyword=&category=&industry=&page=&size=`；PG JSONB + `pg_trgm` 全文索引；支持按 use_count / score / create_time 排序。
  - 产出：`TemplateSearchService.java`、索引 DDL。
  - 依赖：BE-045、BE-075。

- **BE-049** 一键使用模板 + 复制 / **P0**
  - 接口：`POST /api/v1/templates/{id}/use?workflowName=`（克隆为 `mf_wf_workflow`）、`POST /api/v1/templates/{id}/copy`。
  - 产出：`TemplateService.useTemplate / copyTemplate`、`WorkflowService.createWorkflow` 复用。
  - 依赖：BE-045、BE-024。

- **BE-050** 模板热门 / 最新 / 推荐（基础版） / **P2**
  - 接口：`GET /api/v1/templates/hot?limit=10`、`.../latest?limit=10`、`.../recommended?userId=`；MVP 用 `ORDER BY use_count desc / create_time desc / score desc`。
  - 产出：`TemplateSearchService.hot/latest/recommended` 方法。
  - 依赖：BE-048。

### 4.5 执行器模块（meowflow-executor）

> 文档说明：单体阶段 executor 能力内嵌于 workflow，本模块保留骨架并提供独立部署能力。

- **BE-051** `Task` / `TaskQueue` / `TaskContext` 模型 / **P1**
  - 基础数据结构 + 内存 `BlockingQueue` 封装。
  - 产出：`Task.java`、`TaskType` 枚举、`TaskStatus` 枚举、`TaskQueue.java`、`TaskContext.java`、`TaskResult.java`。
  - 依赖：—。

- **BE-052** `ExecutorNodeRegistry`（注册 + 心跳 + 健康） / **P1**
  - 注册表使用 `ConcurrentHashMap<String, ExecutorNode>`；定时心跳检查（30s）+ 健康检查（60s）；超时 90s 标记 INACTIVE。
  - 产出：`ExecutorNodeRegistry.java`、`ExecutorNode.java`、`HealthChecker.java`。
  - 依赖：BE-051。

- **BE-053** `TaskDispatchService`（按能力 + 负载） / **P1**
  - 选择支持节点类型 + 健康 + 负载未满的执行节点；调用 `ExecutorClient.submitTask`。
  - 产出：`TaskDispatchService.java`。
  - 依赖：BE-052、BE-056。

- **BE-054** `LoadBalancer`（随机 / 轮询 / 加权 / 最少连接） / **P1**
  - 策略接口 + 4 个实现；通过 SPI 或 Bean 配置选择。
  - 产出：`LoadBalancer.java` 接口 + 4 个实现。
  - 依赖：BE-053。

- **BE-055** 任务状态管理 + 超时 + 取消 / **P1**
  - `TaskStatusService` 维护状态机；`cancelTask` 仅对 PENDING/RUNNING 生效；定时检查超时任务。
  - 产出：`TaskStatusService.java`、`TimeoutScanner`。
  - 依赖：BE-051。

- **BE-056** `ExecutorClient`（RestTemplate） / **P1**
  - `submitTask / cancelTask / queryTaskStatus / healthCheck`；连接/读超时；失败重试（可关闭）。
  - 产出：`ExecutorClient.java`、`ExecutorConfig.java`。
  - 依赖：BE-004。

- **BE-057** 执行器管理 API / **P2**
  - `POST /api/v1/executors/{register,unregister,heartbeat}`、`GET /api/v1/executors`、`GET /api/v1/executors/{nodeId}/health`。
  - 产出：`ExecutorController.java`。
  - 依赖：BE-052、BE-053。

### 4.6 监控模块（meowflow-monitor）

- **BE-058** 执行日志写入（流式） / **P0**
  - 通过 `mf_wf_execution_log` 流式追加；提供 `ExecutionLogService.log(level, type, message, payload)`。
  - 产出：`ExecutionLogService.java`、`ExecutionLog` 实体（`mf_wf_execution_log`）。
  - 依赖：BE-003。

- **BE-059** 执行日志查询接口 / **P0**
  - `GET /api/v1/monitor/logs/{executionId}?level=`、`.../page?page=&size=`。
  - 产出：`ExecutionLogController.java`、DTO。
  - 依赖：BE-058。

- **BE-060** 业务指标聚合 + API / **P1**
  - 按工作流/日统计 total/success/failed/avg_duration/token/cost；`GET /api/v1/monitor/metrics/executions?workflowId=&from=&to=`。
  - 产出：`MetricsService.java`、`ExecutionStatisticsDTO`、`TrendDataDTO`、`MetricsController.java`。
  - 依赖：BE-027、BE-044。

- **BE-061** 系统指标采集（JVM / 线程池 / DB） / **P1**
  - 接入 Micrometer，定期上报 `jvm.memory.used`、`thread.pool.active`、`hikari.connections.active` 等。
  - 产出：`SystemMetricsCollector.java`、`/actuator/prometheus` 暴露。
  - 依赖：BE-006、BE-081。

- **BE-062** 告警规则 CRUD + 规则引擎 / **P1**
  - 接口：`POST/GET/PUT/DELETE /api/v1/monitor/alerts/rules`；`AlertRuleEngine` 支持 eq/ne/gt/lt/gte/lte；沉默周期。
  - 产出：`AlertRuleController.java`、`AlertRuleEngine.java`、`AlertRule / AlertRecord` 实体（`mf_mon_alert_rule / mf_mon_alert`）。
  - 依赖：BE-058。

- **BE-063** 告警通知（钉钉/邮件/短信） / **P1**
  - `AlertNotifyService` 调度 `DingtalkNotifier / EmailNotifier / SmsNotifier`；失败重试 + 通知日志。
  - 产出：`AlertNotifyService.java` + 三个 Notifier。
  - 依赖：BE-073。

- **BE-064** 告警沉默 / 升级 / 历史查询 / **P2**
  - 沉默期内不重复发送；未在 N 分钟内 ack 自动升级；历史查询接口 `GET /api/v1/monitor/alerts/records?status=`。
  - 产出：`AlertSilenceService.java`、`AlertEscalationService.java`、`AlertController` 扩展。
  - 依赖：BE-062、BE-063。

- **BE-065** 每日聚合指标写入 `mf_mon_metric_daily` / **P2**
  - 凌晨任务批量聚合，写入日表（保留 30 天）。
  - 产出：`DailyMetricAggregator.java`、`@Scheduled` 任务。
  - 依赖：BE-060。

### 4.7 AI 基础设施（meowflow-infra——**待补**）

- **BE-066** `ChatClient` + 4 个模型实现 / **P0**
  - 接口：`complete / streamComplete / embed`；实现：`ChatClientOpenAI / Anthropic / Ali(通义) / Baidu(文心)`；统一超时、重试、降级。
  - 产出：`ChatClient.java` 接口 + 4 个实现 + `ChatClientFactory`。
  - 依赖：BE-001。

- **BE-067** `EmbeddingClient` + 向量库对接 / **P0**
  - OpenAI Embedding + 阿里 DashScope Embedding；向量库优先 pgvector（`mf_ai_chunk.embedding vector(1536)`），降级到外部 Milvus/Qdrant。
  - 产出：`EmbeddingClient.java` 接口 + 2 个实现 + `VectorStore` 抽象。
  - 依赖：BE-066、BE-069。

- **BE-068** `ModelRouter` + 健康检查 + 降级链 / **P0**
  - 根据 `mf_ai_model.priority + is_default + enabled` 排序，结合 `HealthChecker`（最近 5 分钟成功率、错误率）；失败时走 `FallbackChain`。
  - 产出：`ModelRouter.java`、`ModelCandidate.java`、`HealthChecker.java`、`FallbackChain.java`。
  - 依赖：BE-066。

- **BE-069** 知识库管理 / **P0**
  - 接口：`POST/GET/PUT/DELETE /api/v1/infra/knowledge-bases`、`.../documents`、`POST /api/v1/infra/knowledge-bases/{id}/rebuild`；实体 `mf_ai_knowledge / mf_ai_document / mf_ai_chunk`。
  - 产出：`KnowledgeController.java`、`DocumentParser`（PDF/Word/Markdown/TXT）。
  - 依赖：BE-067。

- **BE-070** 多路检索通道 + 后处理 / **P0**
  - 三个 `SearchChannel`：Vector、Keyword、Hybrid；后处理：Deduplicate、Rerank、QualityFilter。
  - 产出：`SearchChannel.java` 接口 + 3 个实现 + 后处理链。
  - 依赖：BE-067、BE-068。

- **BE-071** MCP 工具注册中心 + 执行器 / **P0**
  - 接口：`POST/GET/DELETE /api/v1/infra/mcp-tools`、`POST /api/v1/infra/mcp-tools/{id}/test`；实体 `mf_int_mcp_tool`。
  - 产出：`MCPToolController.java`、`MCPToolRegistry.java`、`MCPToolExecutor.java`、stdio/sse 适配。
  - 依赖：BE-003。

- **BE-072** AI 调用日志 + 成本统计 / **P1**
  - 写入 `mf_ai_invoke_log`（prompt、completion、tokens、cost_ms、cost_amount、status）；提供 `GET /api/v1/infra/invoke-logs?userId=&executionId=`。
  - 产出：`AIInvokeLogService.java`、`AICostCalculator.java`（按模型单价表）。
  - 依赖：BE-036。

- **BE-073** 集成配置（钉钉/飞书/企微/邮件/SMS） / **P1**
  - 接口：`POST/GET/PUT/DELETE /api/v1/infra/integrations`；实体 `mf_int_config`；支持 webhook URL + secret + 加签。
  - 产出：`IntegrationController.java`、`IntegrationService.java`。
  - 依赖：BE-003。

- **BE-074** 集成发送日志 + 失败重试 / **P1**
  - 写入 `mf_int_send_log`；失败按指数退避重试 3 次。
  - 产出：`IntegrationSender.java`（抽象）、具体 Sender 实现。
  - 依赖：BE-073。

### 4.8 数据库

- **BE-075** 初始化数据库与 DDL / **P0**
  - 在 dev/staging 环境执行 `scripts/sql/01-schema.sql`；创建 PostgreSQL 扩展（`pg_trgm`、`zhparser`、`pgvector` 可选）；创建应用账号与最小权限。
  - 产出：`scripts/sql/00-init-extensions.sql`（新增）、执行手册 `docs/runbooks/db-init.md`。
  - 依赖：—。

- **BE-076** Flyway / Liquibase 迁移脚本 / **P1**
  - 把 `01-schema.sql` 拆为 V1__init.sql、V2__indexes.sql、V3__seed.sql 等；启动自动执行。
  - 产出：`db/migration/V*.sql`、`FlywayConfig.java`。
  - 依赖：BE-003、BE-075。

- **BE-077** DB 账号 / Schema / 索引 / 物化视图评审 / **P1**
  - 设计 `mf_app`（读写）、`mf_ro`（只读）、`mf_log`（日志表专用）账号；为高频查询（execution 按 workflow_id + create_time）建组合索引；为模板搜索建 GIN。
  - 产出：索引 DDL 补丁、账号 SQL。
  - 依赖：BE-075。

### 4.9 部署 / DevOps

- **BE-078** `docker-compose.dev.yml` / **P0**
  - 编排 postgres + redis + rabbitmq（含 delayed 插件）+ minio + meowflow-app；提供 `.env.example`。
  - 产出：`deploy/docker/docker-compose.dev.yml`、`Dockerfile`、`init-rabbitmq.sh`（启用插件）。
  - 依赖：BE-091。

- **BE-079** Dockerfile（多阶段） / **P0**
  - builder 阶段用 eclipse-temurin:17-jdk；runtime 用 distroless 或 alpine；JVM 参数模板；非 root 用户运行。
  - 产出：`Dockerfile`。
  - 依赖：BE-001。

- **BE-080** Kubernetes 部署 / **P1**
  - Deployment / Service / Ingress（Nginx Ingress）/ ConfigMap / Secret / HPA；按微服务拆分（gateway / workflow / executor / monitor / user / template / infra）。
  - 产出：`deploy/k8s/*.yaml`、Kustomize overlay。
  - 依赖：BE-079、BE-081。

- **BE-081** `/actuator/health` + Prometheus / **P1**
  - 暴露 `/actuator/health`（含 DB、Redis、MQ 健康）、`/actuator/prometheus`；K8s liveness/readiness probe 指向 `/actuator/health/liveness` `/readiness`。
  - 产出：`application.yml` 配置、`HealthIndicator` 自定义（钉钉/MCP 等外部依赖）。
  - 依赖：BE-003、BE-004、BE-005。

- **BE-082** CI（编译 + 测试 + 构建镜像） / **P1**
  - GitHub Actions：`mvn -B verify`、`docker build`、推送镜像到 Registry；缓存 Maven 依赖。
  - 产出：`.github/workflows/ci.yml`。
  - 依赖：BE-079、BE-087。

### 4.10 安全 / 质量 / 前端对接

- **BE-083** 前端联调（CORS / Swagger） / **P0**
  - 允许前端 dev server 跨域；Cookie SameSite=Lax；统一 `/api/v1` 前缀；提供 Swagger UI 给前端查阅。
  - 产出：`CorsConfig.java`、`application.yml` 中 `meowflow.web.cors.allowed-origins`。
  - 依赖：BE-014。

- **BE-084** Webhook 签名校验 + 幂等 token 下发 / **P0**
  - HMAC-SHA256(body, secret) 校验；提供 `GET /api/v1/webhooks/{workflowId}/token` 下发幂等 token。
  - 产出：`WebhookSignatureVerifier.java`、`WebhookController.token`。
  - 依赖：BE-011、BE-028。

- **BE-085** 字段级加密（AES） / **P1**
  - MyBatis-Plus TypeHandler 处理 `mf_int_config.api_key`、`mf_ai_model.api_key` 等敏感字段；密钥来自环境变量 + KMS。
  - 产出：`EncryptTypeHandler.java`、`Encryptor.java`、`application.yml` 中 `meowflow.crypto.*`。
  - 依赖：BE-003。

- **BE-086** 接口幂等 + 防重放 / **P1**
  - 在 `BE-011` 基础上扩展：写操作要求 `Idempotency-Key` 请求头；时间戳窗口 ±5 分钟。
  - 产出：`IdempotencyAspect` 扩展、`ReplayProtectFilter`。
  - 依赖：BE-011。

- **BE-087** 单元测试框架 + 覆盖率门槛 / **P1**
  - JUnit 5 + Mockito + Testcontainers（PG/Redis/MQ）；JaCoCo 覆盖率门槛 60%（行）+ 50%（分支）。
  - 产出：`pom.xml` test 依赖、`src/test/java` 基础测试、`.github/workflows/ci.yml` 覆盖率门禁。
  - 依赖：—。

- **BE-088** 集成测试（执行引擎 / 鉴权 / 幂等） / **P1**
  - 用 Testcontainers 跑端到端：`POST /api/v1/workflows` → `POST /api/v1/executions/.../execute` → 校验 DB 行 + 日志；模拟多用户登录、过期 Token、重复提交。
  - 产出：`IntegrationTest` 类。
  - 依赖：BE-024、BE-027、BE-033。

- **BE-089** 性能压测 / **P2**
  - JMeter 场景：登录 100 并发、列表查询 200 并发、执行引擎 50 并发；记录 P95/P99；落 baseline 报告。
  - 产出：`scripts/perf/*.jmx`、`docs/runbooks/perf-baseline.md`。
  - 依赖：BE-087、BE-078。

- **BE-090** OpenAPI 契约生成 TS SDK / **P2**
  - 使用 `openapi-generator` 在 CI 中生成 `frontend/src/api/client.ts`，提供给前端联调；与前端 review 字段一致性。
  - 产出：`frontend/src/api/client.ts`（生成产物）、CI 步骤。
  - 依赖：BE-014、BE-082。

---

## 5. 里程碑划分

### 5.1 M1 — 基础骨架（~23 人·天）

**目标**：可启动的空壳服务，能跑 `mvn spring-boot:run`，统一日志/异常/响应体，建立 CI 与本地开发环境。

**包含工作项**：BE-001、BE-002、BE-003、BE-004、BE-005、BE-006、BE-007、BE-008、BE-009、BE-010、BE-011、BE-012、BE-014、BE-075、BE-078、BE-079、BE-083、BE-084、BE-016、BE-017、BE-022、BE-018、BE-019

**验收标准**：
- `docker compose -f deploy/docker/docker-compose.dev.yml up` 起 PG/Redis/MQ/MinIO
- 登录接口可调用，返回 JWT
- `Result<T>` 全局统一；异常被 `GlobalExceptionHandler` 转译

---

### 5.2 M2 — 核心业务（~38 人·天）

**目标**：跑通端到端工作流——创建工作流 → 触发执行 → 节点调度 → LLM/检索/通知节点工作 → 执行日志落库。

**包含工作项**：BE-024、BE-025、BE-026、BE-027、BE-028、BE-031、BE-032、BE-033、BE-034、BE-035、BE-036、BE-037、BE-038、BE-039、BE-040、BE-042、BE-043、BE-044、BE-045、BE-046、BE-048、BE-049、BE-058、BE-059、BE-066、BE-067、BE-068、BE-069、BE-070、BE-071、BE-020、BE-029

**验收标准**：
- 创建一个含「Webhook 触发 → LLM 节点 → 钉钉通知」3 节点的工作流
- 调用 Webhook 接口后，`mf_wf_execution` 状态 SUCCESS，`mf_wf_execution_log` 记录每一步
- 模板可一键克隆为工作流
- 工作流执行接口全部通过 Swagger 可调通

---

### 5.3 M3 — 完善加固（~32 人·天）

**目标**：补齐权限/数据安全/监控告警/性能基线/质量门禁，达到生产可用水平。

**包含工作项**：BE-013、BE-015、BE-021、BE-023、BE-030、BE-041、BE-047、BE-050、BE-051、BE-052、BE-053、BE-054、BE-055、BE-056、BE-060、BE-061、BE-062、BE-063、BE-072、BE-073、BE-074、BE-076、BE-077、BE-081、BE-085、BE-086、BE-087、BE-088

**验收标准**：
- 数据权限生效：不同 `dataScope` 的用户看到不同工作流列表
- 告警规则触发后，钉钉/邮件收到通知
- 单元测试覆盖率 ≥ 60%；CI 拒绝不达标 PR
- `curl http://localhost:8080/actuator/prometheus` 返回指标

---

### 5.4 M4 — 部署上线（~13 人·天）

**目标**：可灰度部署到测试/预发环境，运行 K8s + Prometheus + 告警闭环，发布 MVP。

**包含工作项**：BE-057、BE-064、BE-065、BE-080、BE-082、BE-089、BE-090

**验收标准**：
- `kubectl apply -k deploy/k8s/overlays/staging` 一键起环境
- GitHub Actions 主分支推送自动构建镜像 + 推送 Registry
- 性能压测报告产出 P95 < 300ms（列表查询）/ < 2s（单节点 LLM 调用）
- 前端能拿到自动生成的 TS SDK

---

## 6. 风险与依赖

| 风险 | 说明 | 应对 |
|---|---|---|
| `meowflow-infra` 模块在 pom 中未声明 | 与 `STACK_SUMMARY.md` §五描述不一致 | BE-001 阶段补建 |
| 单体 vs 微服务边界模糊 | 文档同时描述两种部署形态 | M1~M3 维持单体（按模块分包），M4 评估拆微服务 |
| `pgvector` 依赖未确认 | 文档给出 fallback（BYTEA） | BE-067 提供实现切换开关 |
| RabbitMQ delayed-message-exchange 插件 | 需要运维启用 | BE-005 + BE-078 init-rabbitmq.sh 一并处理 |
| 前端字段名与服务端字段一致性 | 缺乏契约 | BE-090 用 OpenAPI 生成 |
| 现有 `database.md` 与 `01-schema.sql` 部分命名不一致（`sys_` vs `mf_`） | 历史遗留 | 以 `01-schema.sql` 为准（已被 `RENAME_LOG.md` 覆盖） |

---

## 7. 参考索引（需要时回看）

- PRD / 总览：`docs/PRD.md`、`docs/SUMMARY.md`
- 架构：`docs/technical/architecture.md`
- 数据库：`docs/technical/database.md`、`scripts/sql/01-schema.sql`、`docs/RENAME_LOG.md`
- 可扩展性：`docs/technical/scalability.md`
- 模块设计：`docs/technical/modules/SUMMARY.md`、`common.md`、`user.md`、`workflow.md`、`template.md`、`executor.md`、`monitor.md`、`STACK_SUMMARY.md`
- 前端接口：`docs/frontend/API接口对接.md`

---

**文档版本**：v1.0
**最后更新**：2026-07-12