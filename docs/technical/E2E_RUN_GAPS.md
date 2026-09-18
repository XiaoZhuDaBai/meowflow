# MeowFlow 端到端跑通缺口清单

更新日期：2026-08-30

## 目标链路

前端画布 -> 保存/发布 -> 后端执行 -> SSE/日志实时反馈 -> 取消/中断 -> 外部触发器 -> 人工输入 -> 最终状态落库。

## 当前结论

主链路代码大部分已经存在，但尚未真正端到端跑通。以下按“必须修复才能跑通”到“跑通后应补齐”的顺序列出。

## P0：必须先修，否则主链路不可用

| # | 问题 | 现状 | 影响 | 修复方向 |
|---|------|------|------|----------|
| 1 | SSE 事件 data 不是合法 JSON | [ExecutionStreamController.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/controller/ExecutionStreamController.java:119) 手动拼字符串，`event.getData().toString()` 会输出 `{status=success, output={...}}` | 前端 `JSON.parse` 失败，节点完成/执行成功/失败/取消事件不生效，日志和节点状态缺失 | 用 ObjectMapper/JsonUtils 序列化整个事件 |
| 2 | Redis Stream 游标没有真正生效 | [RedisStreamEventSink.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/event/RedisStreamEventSink.java:170) 调用 `streamRange`，但 [RedisService.java](D:/Code/喵流/backend/meowflow/meowflow-common/src/main/java/com/meowflow/common/redis/RedisService.java:418) 忽略 start/end，始终从 Stream 头读 | SSE 每 200ms 重复推送旧事件，日志重复，游标恢复不可用 | 改成真实 `XRANGE`/`opsForStream().range(...)`，返回 Stream ID |
| 3 | SSE 游标类型混用 | 前端把 `event_id` 当作 `after` 传回，但后端需要的是 Redis Stream ID；`RunEvent.eventId` 是自定义 `millis-seq`，不是 Stream `_id` | 即使修好 range，断线续传仍可能错位 | SSE 返回/记录真实 Stream `_id`，前端用该值继续 |
| 4 | 编辑器执行是同步阻塞 | [workflow.ts](D:/Code/喵流/frontend/meowflow-ui/src/api/workflow.ts:222) 和 [useWorkflowRun.ts](D:/Code/喵流/frontend/meowflow-ui/src/composables/useWorkflowRun.ts:192) 都传 `async: false` | 长工作流在 HTTP 返回前前端看不到日志，也无法从编辑器中断 | 改用 `async: true` 或新增 `POST /api/execution/execute-async`，拿到 executionId 后立即订阅 SSE |
| 5 | 编辑器“停止”没有取消后端 | [useWorkflowRun.ts](D:/Code/喵流/frontend/meowflow-ui/src/composables/useWorkflowRun.ts:54) 的 `stop()` 只关 SSE，不调用 `executionApi.cancel()` | 用户以为停止了，后端仍在执行 | 在编辑器加入停止按钮，`stop()` 先调 cancel，再关 SSE |
| 6 | 网关白名单路径错误 | [SaTokenConfig.java](D:/Code/喵流/backend/meowflow/meowflow-gateway/src/main/java/com/meowflow/gateway/filter/SaTokenConfig.java:48) 只放行 `/workflow/api/v1/webhook/**`，真实路径是 `/workflow/api/webhook/...`；plugin 触发也没放行 | 外部 webhook/plugin 触发会 401 | 放行 `/workflow/api/webhook/**` 和 `/workflow/api/plugin/trigger/**` |
| 7 | 开发环境默认走 mock，且 Vite 没有后端代理 | `.env.development` 中 `VITE_USE_MOCK=true`；[vite.config.ts](D:/Code/喵流/frontend/meowflow-ui/vite.config.ts) 没有 `/workflow` 代理 | `npm run dev` 默认不会请求真实后端；手动关掉 mock 后请求落到 5173 同源，打不到 8080 | 增加 Vite proxy 到 `http://localhost:8080`，并给真实模式单独环境文件 |
| 8 | 没有全部服务的一键编排 | [docker-compose.dev.yml](D:/Code/喵流/backend/meowflow/deploy/docker/docker-compose.dev.yml) 只启动基础设施和一个默认 gateway 容器 | workflow/user/infra/monitor/executor 需要手工构建启动，无法一键跑通 | 增加完整服务编排，或提供启动脚本按依赖顺序启动 |

## P1：影响完整性和稳定性，跑通前应一并处理

| # | 问题 | 现状 | 影响 | 修复方向 |
|---|------|------|------|----------|
| 9 | 发布后的草稿版本会污染执行版本 | [WorkflowService.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/service/WorkflowService.java:220) 保存草稿直接改 `currentVersion`，工作流仍保持 running | 执行会跑到未发布的草稿 | 保存草稿不切换当前版本，或发布时才把草稿提升为当前版本 |
| 10 | 执行日志接口依赖 monitor 服务 | [ExecutionController.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/controller/ExecutionController.java:72) 通过 `MonitorFeignClient` 拿日志 | 只启动 workflow 时日志接口失败 | 日志落库/查询放到 workflow 内，或确保 monitor 随 workflow 一起启动 |
| 11 | 编辑器测试运行没有人工输入提交入口 | 提交入口只在 [Detail.vue](D:/Code/喵流/frontend/meowflow-ui/src/views/log/Detail.vue:367) | 画布直接跑 human-input 会一直等到超时 | 在编辑器日志区加入“等待人工输入”提交框，复用 `submitHumanInput` |
| 12 | 取消状态可能被异步线程覆盖 | [ExecutionService.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/service/ExecutionService.java:129) cancel 后，异步线程仍可能继续 `updateExecutionFromResult` | 用户取消后最终可能显示 failed，而不是 cancelled | 取消后设置终态并跳过后续覆盖，或让引擎统一返回 cancelled |
| 13 | 定义解析缺少空值保护 | [ExecutionService.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/service/ExecutionService.java:208) 空 JSON 返回 null，引擎后续直接操作 definition | 脏版本会 NPE | 解析后校验非空并给出可读错误 |
| 14 | 孤立节点只告警不拦截 | [WorkflowCompiler.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/compiler/WorkflowCompiler.java:177) 只 `log.warn` | 用户可发布永远不执行的节点 | 发布时根据策略阻止或明确标记 |
| 15 | RunEventSink 快照/状态接口没有被调用 | `updateExecutionStatus/createSnapshot/getLatestSnapshot` 只有接口实现，没有执行链路调用 | 中断恢复、崩溃续跑、实时状态查询不可用 | 在执行关键节点接入快照和状态更新 |
| 16 | E2E 测试是占位实现 | [WorkflowFullLifecycleE2ETest.java](D:/Code/喵流/backend/meowflow/meowflow-e2e/src/test/java/com/meowflow/e2e/WorkflowFullLifecycleE2ETest.java) 只断言 context 能启动 | 没有真实创建/发布/执行/取消断言 | 写真实 HTTP E2E：登录 -> 创建 -> 发布 -> 执行 -> SSE -> 取消 |

## P2：跑通后建议补齐

| # | 问题 | 说明 |
|---|------|------|
| 17 | `ExecutionLogStream` 引用不存在的 `/logs/stream` | [executionLogStream.ts](D:/Code/喵流/frontend/meowflow-ui/src/utils/executionLogStream.ts:66) 指向后端没有的端点；该 class 目前未被使用，删除或接真实端点 |
| 18 | 事件 ID 是 JVM 内存计数器 | [RunEvent.java](D:/Code/喵流/backend/meowflow/meowflow-workflow/src/main/java/com/meowflow/workflow/event/RunEvent.java:193) 多实例下可能重复 |
| 19 | 条件/IF-ELSE 多分支仍需前端语义闭环 | 已加多 handle 和自动标签，但 case 改名后旧边标签不会自动同步 |
| 20 | 编辑器运行结果与日志页信息割裂 | 编辑器内没有执行结果表格、节点输入输出明细、重试原因展示 |
| 21 | 没有正式压测/并发执行验证 | 需要验证同一工作流并发执行、同 executionId 重复 cancel、Redis Stream 断连重连 |

## 验收标准：满足以下才算“跑通”

1. 前端 `npm run dev` 以真实后端模式启动，`VITE_USE_MOCK=false`，`/workflow/**` 能代理到 gateway 8080。
2. 登录后从画布创建节点、保存、发布成功。
3. 从编辑器测试运行，SSE 能看到 `execution_started -> node_started -> node_finished -> execution_succeeded`，且事件不重复。
4. 节点状态、最终输出、执行日志能在前端实时看到。
5. 编辑器停止按钮能真正取消后端执行，最终状态为 `cancelled`，不会被异步线程覆盖。
6. 外部 webhook 和 plugin 触发不需要登录鉴权即可命中工作流。
7. 工作流包含 human-input 时，能在编辑器直接提交输入并继续执行。
8. 发布后再次编辑画布保存，不会让未发布的草稿版本被运行。
9. 执行日志接口 `/api/execution/{id}/logs` 能返回节点日志。
10. 完整服务编排能一键启动，并通过一条真实 HTTP E2E 测试验证完整生命周期。

## 验证命令

```powershell
# 基础设施
cd D:\Code\喵流\backend\meowflow\deploy\docker
docker compose -f docker-compose.dev.yml up -d

# 后端模块构建
cd D:\Code\喵流\backend\meowflow
$env:JAVA_HOME='C:\Users\11057\.jdks\ms-17.0.19'
& 'D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd' -pl meowflow-workflow,meowflow-gateway,meowflow-monitor,meowflow-infra -am "-Dmaven.test.skip=true" package

# 前端真实模式
cd D:\Code\喵流\frontend\meowflow-ui
$env:VITE_USE_MOCK='false'
npm run dev
```
