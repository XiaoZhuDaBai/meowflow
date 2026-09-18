# 喵流 (MeowFlow) - 性能测试指南

> 本文档定义性能测试的工具选型、关键场景、基线指标与执行流程

---

## 一、性能测试层次

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       性能测试金字塔                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                              /\                                          │
│                             /  \         压力测试（>2x 预期峰值）        │
│                            / ST \        评估系统极限                    │
│                           /______\                                       │
│                          /        \      负载测试（预期峰值）            │
│                         /   LT    \     验证 SLA                         │
│                        /____________\                                    │
│                       /              \   基准测试（稳态）                │
│                      /   Benchmark  \  性能回归检测                     │
│                     /__________________\                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、关键性能指标（SLO）

### 2.1 API 响应时间

| 场景 | P50 | P95 | P99 | 错误率 |
|------|-----|-----|-----|--------|
| **简单 GET 查询** | < 50ms | < 100ms | < 200ms | < 0.1% |
| **列表分页查询** | < 100ms | < 300ms | < 500ms | < 0.1% |
| **复杂工作流触发** | < 200ms | < 500ms | < 1s | < 0.5% |
| **LLM 节点执行** | < 5s | < 15s | < 30s | < 1% |
| **同步 SSE 流（首包）** | < 500ms | < 2s | < 3s | < 0.5% |
| **异步任务提交** | < 100ms | < 200ms | < 500ms | < 0.1% |

### 2.2 吞吐量

| 服务 | 稳态 QPS | 峰值 QPS |
|------|---------|---------|
| meowflow-workflow | 100 | 500 |
| meowflow-executor | 200 | 1000 |
| meowflow-user | 500 | 2000 |
| meowflow-infra（LLM 路由） | 50 | 200 |

### 2.3 资源利用率

| 资源 | 稳态使用率 | 告警阈值 |
|------|----------|---------|
| CPU | < 60% | > 80% |
| 内存 | < 70% | > 85% |
| 数据库连接池 | < 50% | > 80% |
| Redis 连接池 | < 50% | > 80% |
| RabbitMQ 队列长度 | < 1000 | > 5000 |
| 线程池队列长度 | < 50% | > 80% |

---

## 三、性能测试工具

### 3.1 JMeter（推荐用于 HTTP API）

**安装**：项目内已下载 Apache JMeter，建议作为标准工具。

**测试计划位置**：
```
backend/meowflow/performance/
├── api-load-test.jmx           # API 负载测试
├── llm-routing-test.jmx        # LLM 路由压测
├── workflow-execution.jmx      # 工作流执行压测
└── data/
    ├── workflow-create.csv     # 参数化数据
    └── user-credentials.csv
```

**执行命令**：

```bash
# 命令行运行（无 GUI）
jmeter -n -t api-load-test.jmx -l results.jtl -e -o html-report

# 指定线程数和循环次数
jmeter -n -t api-load-test.jmx -Jthreads=100 -Jloop=10 -l results.jtl
```

### 3.2 Gatling（推荐用于复杂场景）

**优势**：Scala DSL、实时报告、CI 友好。

**添加依赖**：

```xml
<plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>4.6.0</version>
</plugin>
```

**测试脚本示例**：

```scala
// src/test/scala/WorkflowLoadTest.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class WorkflowLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  val createWorkflow = scenario("Create Workflow")
    .exec(http("Create Workflow")
      .post("/api/workflows")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"name":"Load Test Workflow","code":"load-${counter}"}"""))
      .asJson
      .check(status.is(200))
      .check(jsonPath("$.data.id").saveAs("workflowId"))
    )

  val executeWorkflow = scenario("Execute Workflow")
    .exec(http("Execute Workflow")
      .post("/api/executions")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"workflowId":"${workflowId}"}"""))
      .asJson
      .check(status.is(200))
    )

  setUp(
    createWorkflow.inject(atOnceUsers(50))
      .andThen(
        executeWorkflow.inject(constantUsersPerSec(100).during(60.seconds))
      )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(95).lt(500),
     global.successfulRequests.percent.gte(99.5)
   )
}
```

### 3.3 工具对比

| 工具 | 学习曲线 | 报告质量 | CI 集成 | 适用场景 |
|------|---------|---------|---------|---------|
| **JMeter** | 中 | 良好 | 一般 | 简单 HTTP 压测 |
| **Gatling** | 中 | 优秀 | 优秀 | 复杂场景、CI |
| **wrk** | 低 | 简单 | 良好 | 极简 HTTP 压测 |
| **k6** | 低 | 良好 | 优秀 | 现代化、JS 脚本 |

**推荐**：项目初期使用 JMeter（团队熟悉），后续迁移到 Gatling。

---

## 四、关键测试场景

### 4.1 场景 1：工作流触发链路（高优先级）

```
目标：验证工作流从触发到执行的完整链路性能
并发：500 用户
持续：5 分钟
SLO：
  - P95 < 1s
  - P99 < 2s
  - 错误率 < 0.5%
```

**JMeter 配置**：
- Thread Group: 500 threads, ramp-up 30s, loop forever (300s)
- HTTP Request: POST /api/executions
- Body: 工作流定义
- Assertion: Response code = 200, Response time < 2000ms

### 4.2 场景 2：LLM 路由（高优先级）

```
目标：验证多模型路由性能
并发：50 RPS（稳态）、200 RPS（峰值）
SLO：
  - P95 < 5s（含 LLM 响应）
  - 首包探测 P95 < 2s
  - 熔断器不误触发
```

### 4.3 场景 3：知识库检索（中优先级）

```
目标：验证多路检索（向量 + 关键词）的并行性能
并发：100 RPS
SLO：
  - P95 < 800ms
  - 后处理器链 P95 < 200ms
```

### 4.4 场景 4：MQ 消费者吞吐（中优先级）

```
目标：验证 RabbitMQ 消费者并发处理能力
生产者：1000 msg/s
消费者：4 实例 × 50 线程
SLO：
  - 队列堆积 < 1000
  - 消息处理 P95 < 100ms
  - 无消息丢失
```

### 4.5 场景 5：限流降级（关键场景）

```
目标：验证限流触发时的降级体验
并发：2000 RPS（远超限流阈值 100 RPS）
SLO：
  - 触发限流返回 429
  - 降级响应时间 < 50ms
  - 业务不崩溃
```

---

## 五、性能基线（Baseline）

### 5.1 建立基线流程

```
1. 选择稳定版本（如 v1.0.0）
2. 配置标准测试环境（4C8G × 3 节点 + PG/Redis/RabbitMQ）
3. 运行基准场景 30 分钟
4. 记录关键指标作为基线
5. 每次发版前对比基线，差异 > 10% 必须分析
```

### 5.2 基线文档模板

```yaml
# baseline-v1.0.0.yml
version: v1.0.0
date: 2026-07-13
environment:
  cpu: 4 cores
  memory: 8 GB
  nodes: 3
  pg: 16 vCPU, 32 GB
  redis: 4 GB

scenarios:
  workflow_trigger:
    qps_stable: 100
    qps_peak: 500
    p50_ms: 120
    p95_ms: 380
    p99_ms: 720
    error_rate: 0.05%

  llm_routing:
    qps_stable: 50
    p95_ms: 4200
    p99_ms: 8500
```

---

## 六、性能测试执行流程

### 6.1 预发布环境验证

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  性能测试执行流程                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   1. 准备阶段                                                            │
│      └─► 准备测试环境（与生产配置一致）                                  │
│      └─► 准备测试数据（种子数据 + 清理脚本）                            │
│      └─► 预热系统（运行 5 分钟空负载）                                  │
│                                                                          │
│   2. 基准测试                                                            │
│      └─► 运行基准场景 30 分钟                                           │
│      └─► 记录指标作为基线                                                │
│                                                                          │
│   3. 负载测试                                                            │
│      └─► 逐步加压（50% → 100% → 150%）                                 │
│      └─► 观察资源利用率拐点                                              │
│                                                                          │
│   4. 压力测试                                                            │
│      └─► 持续峰值负载 10 分钟                                           │
│      └─► 验证系统不崩溃                                                  │
│                                                                          │
│   5. 恢复测试                                                            │
│      └─► 负载降到 0                                                      │
│      └─► 验证资源释放、连接池恢复                                        │
│                                                                          │
│   6. 报告                                                                │
│      └─► 生成 HTML 报告                                                  │
│      └─► 与基线对比                                                      │
│      └─► 输出改进建议                                                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 CI 集成（轻量级冒烟测试）

```yaml
# .github/workflows/perf-smoke.yml
name: Performance Smoke Test

on:
  workflow_dispatch:  # 手动触发

jobs:
  smoke-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Start services
        run: docker-compose up -d

      - name: Run JMeter smoke test
        run: |
          jmeter -n -t performance/api-smoke.jmx \
            -Jthreads=10 -Jduration=60 \
            -l results.jtl

      - name: Check SLO
        run: |
          ./scripts/check-slo.sh results.jtl

      - name: Upload report
        uses: actions/upload-artifact@v4
        with:
          name: perf-report
          path: html-report/
```

---

## 七、性能问题排查

### 7.1 慢查询分析

```sql
-- 查看慢查询
SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

### 7.2 线程转储

```bash
# Java 线程转储（找出阻塞线程）
jstack <pid> > thread-dump.txt

# 使用 fastthread.io 在线分析
```

### 7.3 Arthas 在线诊断（推荐）

```bash
# 查看方法执行耗时
trace com.meowflow.workflow.service.WorkflowService create

# 查看方法被调用情况
monitor com.meowflow.workflow.service.WorkflowService create

# 查看线程状态
thread -n 3
```

### 7.4 常见性能问题

| 问题 | 表现 | 解决方案 |
|------|------|---------|
| N+1 查询 | 100 个对象查询 100 次 | 批量查询 + JOIN |
| 缓存未命中 | DB QPS 飙高 | 加热点 Key 缓存 |
| 线程池满 | 任务排队延迟 | 扩容或异步解耦 |
| 数据库连接池满 | 请求阻塞 | 调大连接池或减少慢查询 |
| GC 频繁 | 长 STW | 调大堆内存或优化对象 |
| 锁竞争 | TPS 不随并发提升 | 减小锁粒度或换无锁结构 |

---

## 八、参考资源

- [JMeter 官方文档](https://jmeter.apache.org/usermanual/index.html)
- [Gatling 官方文档](https://gatling.io/docs/current/)
- [Google SRE Book - Performance](https://sre.google/sre-book/monitoring-distributed-systems/)
- [Brendan Gregg - Performance Methodology](https://www.brendangregg.com/methodology.html)