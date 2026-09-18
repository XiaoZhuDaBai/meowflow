# MeowFlow Common - 通用基础设施模块设计文档

> 本文档详细描述喵流平台的通用基础设施模块（MeowFlow-common）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MeowFlow-common 模块定位                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-common 是整个平台的基础设施层，为所有业务模块提供：                │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 通用能力下沉                                                 │   │
│  │      • 异常处理体系                                              │   │
│  │      • 统一响应封装                                              │   │
│  │      • 工具类集合                                                │   │
│  │                                                                   │   │
│  │   2. 基础设施能力（参考 Ragent Framework）                        │   │
│  │      • 线程池管理 + TTL 上下文透传                               │   │
│  │      • 限流熔断（基于 Resilience4j）                            │   │
│  │      • 链路追踪                                                  │   │
│  │      • 用户上下文                                                │   │
│  │                                                                   │   │
│  │   3. 跨模块共享                                                  │   │
│  │      • 常量定义                                                  │   │
│  │      • 枚举类                                                    │   │
│  │      • 全局配置                                                  │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  核心原则：业务无关的能力必须下沉到 common，业务逻辑不得侵入此模块          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 业务解耦                                                            │
│  ───────────────────────────────────────────────────────────────────────│
│  • 所有能力与业务逻辑解耦                                               │
│  • 业务模块无感知地使用基础设施能力                                       │
│  • 更换业务不影响基础设施                                               │
│                                                                          │
│  2. 生产优先                                                            │
│  ───────────────────────────────────────────────────────────────────────│
│  • 从第一天就考虑高可用、高并发、可观测                                   │
│  • 限流熔断、链路追踪、线程安全，一个都不能少                             │
│  • 参考 Ragent 等生产级项目的工程实践                                   │
│                                                                          │
│  3. 零依赖业务                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • common 模块不依赖任何业务模块                                         │
│  • 其他模块可以无限制地依赖 common                                       │
│  • 避免循环依赖                                                        │
│                                                                          │
│  4. 面向接口编程                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 核心能力都抽象为接口                                                 │
│  • 实现可插拔、可扩展                                                   │
│  • 默认实现满足大多数场景                                               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
MeowFlow-common/
├── pom.xml
└── src/main/java/com/meowflow/common/
    │
    ├── config/                      # 通用配置
    │   ├── ThreadPoolConfig.java    # 8个专用线程池配置
    │   ├── AsyncConfig.java         # 异步任务配置
    │   ├── TtlConfig.java          # TTL 上下文透传配置
    │   ├── Resilience4jConfig.java # 限流熔断配置
    │   ├── RedisConfig.java        # Redis 配置
    │   ├── MybatisPlusConfig.java  # MyBatis-Plus 配置
    │   ├── JacksonConfig.java      # JSON 配置
    │   ├── CryptoConfig.java       # 加密配置
    │   └── CommonAutoConfig.java   # 自动配置
    │
    ├── context/                     # 上下文管理（参考 Ragent）
    │   ├── UserContext.java         # 用户上下文
    │   ├── UserContextHolder.java   # 用户上下文持有者
    │   ├── TraceContext.java        # 链路追踪上下文
    │   └── TraceContextHolder.java  # 链路追踪上下文持有者
    │
    ├── exception/                   # 异常处理
    │   ├── BizException.java        # 业务异常
    │   ├── NodeException.java        # 节点执行异常
    │   ├── WorkflowException.java   # 工作流异常
    │   ├── ErrorCode.java          # 错误码定义（合并到 ResultCode）
    │   └── GlobalExceptionHandler.java  # 全局异常处理器
    │
    ├── rate/                        # 限流熔断（基于 Resilience4j）
    │   └── Resilience4jConfig.java  # Resilience4j 配置
    │
    ├── result/                      # 统一响应
    │   ├── Result.java              # 统一响应体
    │   └── ResultCode.java          # 响应码枚举（包含错误码）
    │
    ├── trace/                       # 链路追踪
    │   ├── TraceNode.java           # Trace 注解
    │   ├── TraceAspect.java         # Trace AOP 切面
    │   ├── TraceIdGenerator.java    # Trace ID 生成器
    │   └── TraceLog.java            # Trace 日志实体
    │
    ├── util/                        # 工具类（基于 Hutool）
    │   ├── IdGenerator.java         # Snowflake ID 生成器
    │   ├── IdGeneratorFactory.java  # ID 生成器工厂
    │   ├── IdGeneratorConfig.java   # 将 IdGenerator 注册为 Spring Bean
    │   ├── WorkerIdAssigner.java     # WorkerID 分配器
    │   ├── JsonUtils.java           # JSON 工具
    │   ├── DateUtils.java           # 日期工具
    │   ├── StringUtils.java         # 字符串工具
    │   ├── BeanUtils.java           # Bean 转换工具
    │   └── SecurityUtils.java       # 安全工具（密码/签名/脱敏）
    │
    ├── security/                    # 安全模块
    │   └── AESEncryptor.java       # AES-256-GCM 加密器
    │
    ├── redis/                       # Redis 服务
    │   └── RedisService.java       # Redis 通用操作服务
    │
    ├── mq/                          # 消息队列
    │   ├── RabbitConfig.java       # RabbitMQ 配置
    │   ├── MqProducer.java          # 消息生产者
    │   ├── MqMessage.java          # 消息基类
    │   └── NotifyMessage.java      # 通知消息
    │
    ├── webhook/                    # Webhook 签名验证
    │   ├── VerifyWebhook.java     # Webhook 验证注解
    │   ├── WebhookInterceptor.java # Webhook 拦截器
    │   ├── WebhookSignatureVerifier.java # 签名验证器
    │   └── WebhookVerifyAspect.java # 验证切面
    │
    ├── idempotent/                  # 幂等控制
    │   ├── Idempotent.java        # 幂等注解
    │   ├── IdempotentStore.java    # 幂等存储接口
    │   └── IdempotentAspect.java  # 幂等切面
    │
    ├── mybatis/                     # MyBatis 扩展
    │   ├── EncryptedStringTypeHandler.java # 加密字符串处理器
    │   └── EncryptedLongTypeHandler.java   # 加密长整型处理器
    │
    ├── annotation/                  # 自定义注解
    │   └── Encrypted.java          # 字段加密注解
    │
    ├── log/                         # 日志相关
    │   ├── LogbackConfig.java      # Logback 配置
    │   ├── LogEvent.java           # 日志事件
    │   ├── LogEventEntity.java     # 日志实体
    │   ├── LogPersistenceService.java # 日志持久化服务
    │   └── PostgresAppender.java    # PostgreSQL 日志Appender
    │
    ├── constant/                    # 常量
    │   └── CommonConstants.java    # 通用常量
    │
    └── web/                         # Web 上下文
        ├── WebContext.java         # Web 上下文工具
        └── ContextFilter.java      # 上下文过滤器
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           模块依赖关系                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                              MeowFlow-common                              │
│                                    │                                      │
│                                    ▼                                      │
│                         ┌─────────────────┐                             │
│                         │  Spring Boot    │                             │
│                         │  Spring Context │                             │
│                         └────────┬────────┘                             │
│                                  │                                       │
│                         ┌────────┴────────┐                             │
│                         │  Redis / Lettuce│                             │
│                         └────────┬────────┘                             │
│                                  │                                       │
│                         ┌────────┴────────┐                             │
│                         │ Resilience4j    │                             │
│                         │ 限流熔断库      │                             │
│                         └────────┬────────┘                             │
│                                  │                                       │
│                         ┌────────┴────────┐                             │
│                         │  Transmittable  │                             │
│                         │  ThreadLocal    │                             │
│                         └─────────────────┘                             │
│                                                                          │
│  说明：common 模块只依赖基础设施组件，不依赖任何业务模块                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、核心组件设计

### 3.1 线程池管理（8个专用线程池）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      8个专用线程池设计（参考 Ragent）                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  线程池划分原则                                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  1. 按业务隔离：不同类型的任务使用不同的线程池                    │    │
│  │  2. 资源隔离：一个任务阻塞不影响其他任务                          │    │
│  │  3. 动态配置：每个线程池的参数可独立配置                          │    │
│  │  4. TTL 透传：所有线程池都包装 TtlExecutors，确保上下文透传       │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  线程池配置详情                                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  ┌────────────────────┬───────────┬────────────┬────────────┐  │    │
│  │  │       名称          │ 核心线程  │  最大线程   │  队列容量  │  │    │
│  │  ├────────────────────┼───────────┼────────────┼────────────┤  │    │
│  │  │ workflow-executor │    10    │     20     │   1000    │  │    │
│  │  │ llm-call          │    20    │     50     │   2000    │  │    │
│  │  │ http-request      │    50    │    100     │   5000    │  │    │
│  │  │ mcp-batch         │    10    │     20     │    100    │  │    │
│  │  │ knowledge-search  │    50    │    100     │    500    │  │    │
│  │  │ notify-send       │    20    │     50     │    200    │  │    │
│  │  │ db-operation      │    15    │     30     │    200    │  │    │
│  │  │ schedule-task     │    10    │     20     │    100    │  │    │
│  │  └────────────────────┴───────────┴────────────┴────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ThreadPoolConfig.java 实现                                      │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Configuration                                                  │    │
│  │  public class ThreadPoolConfig {                                  │    │
│  │                                                                   │    │
│  │      @Bean("workflowExecutorPool")                               │    │
│  │      public Executor workflowExecutorPool() {                    │    │
│  │          return TtlExecutors.getTtlExecutor(                     │    │
│  │              new ThreadPoolExecutor(                             │    │
│  │                  10, 20, 60L, TimeUnit.SECONDS,                 │    │
│  │                  new LinkedBlockingQueue<>(1000),                 │    │
│  │                  new NamedThreadFactory("workflow-executor"),     │    │
│  │                  new CallerRunsPolicy()                          │    │
│  │              )                                                   │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Bean("llmCallPool")                                        │    │
│  │      public Executor llmCallPool() {                             │    │
│  │          return TtlExecutors.getTtlExecutor(                     │    │
│  │              new ThreadPoolExecutor(                             │    │
│  │                  20, 50, 60L, TimeUnit.SECONDS,                 │    │
│  │                  new LinkedBlockingQueue<>(2000),                 │    │
│  │                  new NamedThreadFactory("llm-call"),             │    │
│  │                  new CallerRunsPolicy()                          │    │
│  │              )                                                   │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      // ... 其他线程池配置                                         │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 上下文透传（参考 Ragent）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      上下文透传设计（参考 Ragent）                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  问题背景                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  主线程设置的用户信息、Trace ID 等，通过线程池传递到子线程时，     │    │
│  │  普通 ThreadLocal 会丢失。Ragent 框架通过 TransmittableThreadLocal│    │
│  │  解决了这个问题。                                                 │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  用户上下文设计                                                   │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class UserContext {                                      │    │
│  │      private String userId;          // 用户ID                    │    │
│  │      private String username;        // 用户名                    │    │
│  │      private String orgId;           // 组织ID                    │    │
│  │      private String orgName;         // 组织名称                  │    │
│  │      private List<String> roles;     // 角色列表                  │    │
│  │      private Map<String, Object> ext;  // 扩展数据                │    │
│  │  }                                                              │    │
│  │                                                                   │    │
│  │  public class UserContextHolder {                                │    │
│  │                                                                   │    │
│  │      private static final TransmittableThreadLocal<UserContext>  │    │
│  │          CONTEXT = new TransmittableThreadLocal<>();             │    │
│  │                                                                   │    │
│  │      public static void set(UserContext context) {                │    │
│  │          CONTEXT.set(context);                                    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static UserContext get() {                            │    │
│  │          return CONTEXT.get();                                    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static void remove() {                                │    │
│  │          CONTEXT.remove();                                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static String getUserId() {                          │    │
│  │          UserContext ctx = get();                                │    │
│  │          return ctx != null ? ctx.getUserId() : null;            │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  链路追踪上下文设计                                               │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class TraceContext {                                     │    │
│  │      private String traceId;         // 全链路追踪ID               │    │
│  │      private String spanId;         // 当前 Span ID               │    │
│  │      private String parentSpanId;   // 父 Span ID                │    │
│  │      private long startTime;        // 开始时间                   │    │
│  │      private Map<String, String> tags;  // 扩展标签              │    │
│  │  }                                                              │    │
│  │                                                                   │    │
│  │  public class TraceIdGenerator {                                │    │
│  │                                                                   │    │
│  │      public static String generate() {                          │    │
│  │          // 格式：IP + PID + 时间戳 + 随机数                      │    │
│  │          String ip = getIp();                                    │    │
│  │          String pid = ManagementFactory.getRuntimeMXBean()       │    │
│  │              .getName().split("@")[0];                          │    │
│  │          String time = String.valueOf(System.currentTimeMillis());│   │
│  │          String random = String.format("%04d",                   │    │
│  │              ThreadLocalRandom.current().nextInt(10000));       │    │
│  │                                                                   │    │
│  │          return ip + pid + time + random;                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  透传原理（参考 Ragent TransmittableThreadLocal）                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  ┌────────────────────────────────────────────────────────────┐  │    │
│  │  │                                                            │  │    │
│  │  │    主线程                                                  │  │    │
│  │  │         │                                                  │  │    │
│  │  │         │  设置 UserContext                                 │  │    │
│  │  │         ▼                                                  │  │    │
│  │  │    ┌─────────────┐                                        │  │    │
│  │  │    │ TTL wrapper │ ← 包装器持有值快照                    │  │    │
│  │  │    └──────┬──────┘                                        │  │    │
│  │  │           │                                               │  │    │
│  │  │           │ 提交到线程池                                   │  │    │
│  │  │           ▼                                               │  │    │
│  │  │    ┌─────────────┐                                        │  │    │
│  │  │    │ 子线程       │ ← 自动从 wrapper 恢复值                │  │    │
│  │  │    └─────────────┘                                        │  │    │
│  │  │                                                            │  │    │
│  │  └────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 限流熔断（基于 Resilience4j）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      限流熔断设计（基于 Resilience4j）                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  技术选型                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  为什么不自己实现滑动窗口？                                          │    │
│  │  • Resilience4j 是专门为 Java 8+ 设计的高性能熔断限流库             │    │
│  │  • 内置成熟的滑动窗口算法（计数/时间两种模式）                       │    │
│  │  • 与 Spring Boot 3.x 深度集成，开箱即用                          │    │
│  │  • 经过大量生产验证，社区活跃，文档完善                              │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  熔断器（Resilience4j Circuit Breaker）                           │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  Resilience4j 内置两种滑动窗口熔断器：                              │    │
│  │  • COUNT_BASED：基于调用次数的滑动窗口                             │    │
│  │  • TIME_BASED：基于时间的滑动窗口                                 │    │
│  │                                                                   │    │
│  │  ┌────────────────────────────────────────────────────────────┐  │    │
│  │  │  熔断器配置示例                                            │  │    │
│  │  │  ────────────────────────────────────────────────────────│  │    │
│  │  │                                                            │  │    │
│  │  │  resilience4j:                                            │  │    │
│  │  │    circuitbreaker:                                        │  │    │
│  │  │      instances:                                          │  │    │
│  │  │        llmCall:                                          │  │    │
│  │  │          slidingWindowType: COUNT_BASED  # 或 TIME_BASED  │  │    │
│  │  │          slidingWindowSize: 10           # 窗口大小       │  │    │
│  │  │          failureRateThreshold: 50         # 失败率阈值   │  │    │
│  │  │          waitDurationInOpenState: 30s     # 熔断持续时间  │  │    │
│  │  │          permittedNumberOfCallsInHalfOpenState: 3         │  │    │
│  │  │                                                            │  │    │
│  │  └────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  │  ┌────────────────────────────────────────────────────────────┐  │    │
│  │  │  CircuitBreaker 使用示例                                    │  │    │
│  │  │  ────────────────────────────────────────────────────────│  │    │
│  │  │                                                            │  │    │
│  │  │  @CircuitBreaker(name = "llmCall",                        │  │    │
│  │  │      fallbackMethod = "chatFallback")                      │  │    │
│  │  │  public String chat(String prompt) {                        │  │    │
│  │  │      return callLLM(prompt);                              │  │    │
│  │  │  }                                                        │  │    │
│  │  │                                                            │  │    │
│  │  │  private String chatFallback(String prompt, Throwable t) {   │  │    │
│  │  │      log.warn("LLM 熔断降级: {}", t.getMessage());        │  │    │
│  │  │      return "服务暂不可用，请稍后重试";                     │  │    │
│  │  │  }                                                        │  │    │
│  │  │                                                            │  │    │
│  │  └────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  限流器（Resilience4j Rate Limiter）                            │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  ┌────────────────────────────────────────────────────────────┐  │    │
│  │  │  限流器配置示例                                            │  │    │
│  │  │  ────────────────────────────────────────────────────────│  │    │
│  │  │                                                            │  │    │
│  │  │  resilience4j:                                            │  │    │
│  │  │    ratelimiter:                                          │  │    │
│  │  │      instances:                                          │  │    │
│  │  │        apiLimit:                                         │  │    │
│  │  │          limitForPeriod: 100           # 每周期请求数    │  │    │
│  │  │          limitRefreshPeriod: 1s         # 刷新周期      │  │    │
│  │  │          timeoutDuration: 5s            # 等待超时      │  │    │
│  │  │                                                            │  │    │
│  │  └────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  │  ┌────────────────────────────────────────────────────────────┐  │    │
│  │  │  RateLimiter 使用示例                                    │  │    │
│  │  │  ────────────────────────────────────────────────────────│  │    │
│  │  │                                                            │  │    │
│  │  │  @RateLimiter(name = "apiLimit", fallbackMethod =        │  │    │
│  │  │      "apiFallback")                                        │  │    │
│  │  │  public String apiCall() {                                │  │    │
│  │  │      return "success";                                    │  │    │
│  │  │  }                                                        │  │    │
│  │  │                                                            │  │    │
│  │  │  private String apiFallback(Throwable t) {                │  │    │
│  │  │      throw new BizException(ResultCode.RATE_LIMITED);     │  │    │
│  │  │  }                                                        │  │    │
│  │  │                                                            │  │    │
│  │  └────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.4 链路追踪（参考 Ragent）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      链路追踪设计（参考 Ragent）                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  @TraceNode 注解                                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Target(ElementType.METHOD)                                     │    │
│  │  @Retention(RetentionPolicy.RUNTIME)                            │    │
│  │  public @interface TraceNode {                                    │    │
│  │                                                                   │    │
│  │      /** 节点名称 */                                              │    │
│  │      String value();                                              │    │
│  │                                                                   │    │
│  │      /** 节点分类：ai/flow/tool/notify */                        │    │
│  │      String category() default "default";                        │    │
│  │                                                                   │    │
│  │      /** 是否记录输入 */                                          │    │
│  │      boolean recordInput() default true;                         │    │
│  │                                                                   │    │
│  │      /** 是否记录输出 */                                          │    │
│  │      boolean recordOutput() default true;                        │    │
│  │                                                                   │    │
│  │      /** 输入脱敏字段 */                                          │    │
│  │      String[] inputMaskFields() default {};                      │    │
│  │                                                                   │    │
│  │      /** 输出脱敏字段 */                                          │    │
│  │      String[] outputMaskFields() default {};                      │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TraceAspect 切面实现                                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Aspect                                                          │    │
│  │  @Component                                                       │    │
│  │  public class TraceAspect {                                        │    │
│  │                                                                   │    │
│  │      @Around("@annotation(traceNode)")                           │    │
│  │      public Object around(ProceedingJoinPoint joinPoint,          │    │
│  │                          TraceNode traceNode) throws Throwable { │    │
│  │                                                                   │    │
│  │          TraceContext ctx = TraceContextHolder.get();             │    │
│  │          String traceId = ctx != null ? ctx.getTraceId() :      │    │
│  │              TraceIdGenerator.generate();                         │    │
│  │          String spanId = TraceIdGenerator.generateSpanId();      │    │
│  │                                                                   │    │
│  │          TraceLog log = TraceLog.builder()                        │    │
│  │              .traceId(traceId)                                   │    │
│  │              .spanId(spanId)                                     │    │
│  │              .nodeName(traceNode.value())                         │    │
│  │              .category(traceNode.category())                       │    │
│  │              .startTime(startTime)                               │    │
│  │              .build();                                             │    │
│  │                                                                   │    │
│  │          // ... 执行业务逻辑，记录输入输出 ...                    │    │
│  │                                                                   │    │
│  │          asyncSaveTraceLog(log);  // 异步保存到日志              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void asyncSaveTraceLog(TraceLog log) {              │    │
│  │          // 异步输出到日志，格式化为 JSON 便于收集分析             │    │
│  │          log.info("[TRACE_LOG] {}", log.toJson());              │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  注意：TraceLog 默认异步写入日志文件，可扩展为数据库存储           │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  使用示例                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                        │    │
│  │  public class LLMService {                                        │    │
│  │                                                                   │    │
│  │      @TraceNode(value = "LLM 调用", category = "ai",             │    │
│  │                   inputMaskFields = {"apiKey", "password"})     │    │
│  │      public String chat(String prompt) {                        │    │
│  │          // LLM 调用逻辑                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @TraceNode(value = "HTTP 请求", category = "tool",          │    │
│  │                   inputMaskFields = {"apiKey"})                 │    │
│  │      public HttpResponse request(HttpRequest req) {             │    │
│  │          // HTTP 请求逻辑                                        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、统一响应与异常

### 4.1 统一响应体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          统一响应体设计                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Result.java                                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @JsonInclude(JsonInclude.Include.NON_NULL)                     │    │
│  │  public class Result<T> {                                        │    │
│  │                                                                   │    │
│  │      private int code;              // 状态码                    │    │
│  │      private String message;        // 消息                      │    │
│  │      private T data;               // 数据                      │    │
│  │      private long timestamp;        // 时间戳                    │    │
│  │      private String traceId;        // 链路追踪ID                │    │
│  │                                                                   │    │
│  │      public static <T> Result<T> success() {                     │    │
│  │          return success(null);                                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static <T> Result<T> success(T data) {               │    │
│  │          Result<T> result = new Result<>();                     │    │
│  │          result.setCode(ResultCode.SUCCESS.getCode());          │    │
│  │          result.setMessage(ResultCode.SUCCESS.getMessage());    │    │
│  │          result.setData(data);                                   │    │
│  │          result.setTimestamp(System.currentTimeMillis());        │    │
│  │          result.setTraceId(getTraceId());                        │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static <T> Result<T> error(ResultCode resultCode) { │    │
│  │          Result<T> result = new Result<>();                     │    │
│  │          result.setCode(resultCode.getCode());                  │    │
│  │          result.setMessage(resultCode.getMessage());            │    │
│  │          result.setTimestamp(System.currentTimeMillis());        │    │
│  │          result.setTraceId(getTraceId());                        │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static <T> Result<T> error(int code, String msg) {  │    │
│  │          Result<T> result = new Result<>();                     │    │
│  │          result.setCode(code);                                   │    │
│  │          result.setMessage(msg);                                 │    │
│  │          result.setTimestamp(System.currentTimeMillis());        │    │
│  │          result.setTraceId(getTraceId());                        │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  响应码枚举 ResultCode                                            │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public enum ResultCode {                                        │    │
│  │                                                                   │    │
│  │      // 成功                                                      │    │
│  │      SUCCESS(200, "操作成功"),                                   │    │
│  │                                                                   │    │
│  │      // 客户端错误 4xx                                           │    │
│  │      BAD_REQUEST(400, "请求参数错误"),                           │    │
│  │      UNAUTHORIZED(401, "未授权"),                                 │    │
│  │      FORBIDDEN(403, "禁止访问"),                                  │    │
│  │      NOT_FOUND(404, "资源不存在"),                                │    │
│  │                                                                   │    │
│  │      // 服务端错误 5xx                                           │    │
│  │      INTERNAL_ERROR(500, "服务器内部错误"),                       │    │
│  │      SERVICE_UNAVAILABLE(503, "服务不可用"),                     │    │
│  │                                                                   │    │
│  │      // 业务错误 1xxx                                            │    │
│  │      BIZ_ERROR(1000, "业务处理失败"),                             │    │
│  │      NODE_EXECUTE_ERROR(1001, "节点执行失败"),                   │    │
│  │      WORKFLOW_NOT_FOUND(1002, "工作流不存在"),                   │    │
│  │      WORKFLOW_EXECUTE_ERROR(1003, "工作流执行失败"),             │    │
│  │                                                                   │    │
│  │      // 限流熔断 2xxx                                            │    │
│  │      RATE_LIMITED(2001, "请求过于频繁"),                         │    │
│  │      CIRCUIT_BREAKER_OPEN(2002, "服务熔断中，请稍后重试"),       │    │
│  │                                                                   │    │
│  │      private final int code;                                    │    │
│  │      private final String message;                              │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 异常体系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          三级异常体系                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  异常继承结构                                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │                    RuntimeException                                │    │
│  │                          │                                        │    │
│  │                          ▼                                        │    │
│  │                   BizException                                    │    │
│  │                          │                                        │    │
│  │         ┌───────────────┼───────────────┐                        │    │
│  │         ▼               ▼               ▼                        │    │
│  │   NodeException   WorkflowException  Others...                   │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  BizException 基础业务异常                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class BizException extends RuntimeException {            │    │
│  │                                                                   │    │
│  │      private final int code;                                     │    │
│  │      private final String errorCode;                             │    │
│  │      private Map<String, Object> details;                        │    │
│  │                                                                   │    │
│  │      public BizException(ResultCode resultCode) {                │    │
│  │          super(resultCode.getMessage());                         │    │
│  │          this.code = resultCode.getCode();                        │    │
│  │          this.errorCode = resultCode.name();                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public BizException(ResultCode resultCode, String message) {│    │
│  │          super(message);                                          │    │
│  │          this.code = resultCode.getCode();                        │    │
│  │          this.errorCode = resultCode.name();                     │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public class NodeException extends BizException {                │    │
│  │      private final String nodeId;                                │    │
│  │      private final String nodeType;                              │    │
│  │                                                                   │    │
│  │      public NodeException(String nodeId, String nodeType,        │    │
│  │                          String message) {                        │    │
│  │          super(ResultCode.NODE_EXECUTE_ERROR, message);           │    │
│  │          this.nodeId = nodeId;                                   │    │
│  │          this.nodeType = nodeType;                               │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public class WorkflowException extends BizException {            │    │
│  │      private final String workflowId;                            │    │
│  │      private final String executionId;                           │    │
│  │                                                                   │    │
│  │      public WorkflowException(String workflowId, String message) │    │
│  │          super(ResultCode.WORKFLOW_EXECUTE_ERROR, message);      │    │
│  │          this.workflowId = workflowId;                            │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  GlobalExceptionHandler 全局异常处理器                            │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestControllerAdvice                                           │    │
│  │  public class GlobalExceptionHandler {                            │    │
│  │                                                                   │    │
│  │      @ExceptionHandler(BizException.class)                       │    │
│  │      public Result<Void> handleBizException(BizException e) {    │    │
│  │          log.warn("业务异常: code={}, msg={}", e.getCode(),     │    │
│  │              e.getMessage());                                    │    │
│  │          return Result.error(e.getCode(), e.getMessage());       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @ExceptionHandler(MethodArgumentNotValidException.class)    │    │
│  │      public Result<Void> handleValidException(                   │    │
│  │          MethodArgumentNotValidException e) {                    │    │
│  │          String message = e.getBindingResult().getFieldErrors()  │    │
│  │              .stream()                                           │    │
│  │              .map(FieldError::getDefaultMessage)                 │    │
│  │              .collect(Collectors.joining(", "));                │    │
│  │          return Result.error(ResultCode.BAD_REQUEST.getCode(),   │    │
│  │              message);                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @ExceptionHandler(RequestNotPermittedException.class)        │    │
│  │      public Result<Void> handleRateLimit(                        │    │
│  │          RequestNotPermittedException e) {                        │    │
│  │          log.warn("限流触发: {}", e.getMessage());              │    │
│  │          return Result.error(ResultCode.RATE_LIMITED);         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @ExceptionHandler(Exception.class)                          │    │
│  │      public Result<Void> handleException(Exception e) {           │    │
│  │          log.error("系统异常", e);                               │    │
│  │          return Result.error(ResultCode.INTERNAL_ERROR);         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、幂等设计

### 5.1 双维度幂等

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          双维度幂等设计                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  幂等维度                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  1. 接口幂等：基于 token + Redis                                  │    │
│  │     • 用户提交请求前先获取 token                                   │    │
│  │     • 提交时携带 token，Redis 中检查是否存在                      │    │
│  │     • 存在则执行，不存在则拒绝                                    │    │
│  │                                                                   │    │
│  │  2. 业务幂等：基于业务唯一键                                       │    │
│  │     • 根据业务特点生成唯一键（如订单号、工作流执行ID）               │    │
│  │     • 数据库唯一索引保证                                          │    │
│  │     • 重复执行返回之前结果                                        │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  接口幂等实现                                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class IdempotencyInterceptor extends HandlerInterceptor{│    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private StringRedisTemplate redisTemplate;                 │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public boolean preHandle(HttpServletRequest request,       │    │
│  │          HttpServletResponse response, Object handler) {         │    │
│  │                                                                   │    │
│  │          String idempotencyKey = request.getHeader(             │    │
│  │              "X-Idempotency-Key");                              │    │
│  │                                                                   │    │
│  │          if (StringUtils.isBlank(idempotencyKey)) {              │    │
│  │              return true;  // 没有幂等key，放行                    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          String fullKey = "idempotency:" + idempotencyKey;      │    │
│  │                                                                   │    │
│  │          // SETNX 保证原子性                                      │    │
│  │          Boolean success = redisTemplate.opsForValue()          │    │
│  │              .setIfAbsent(fullKey, "1", Duration.ofMinutes(30));│   │
│  │                                                                   │    │
│  │          if (!success) {                                         │    │
│  │              // 已存在，表示重复请求                               │    │
│  │              response.setStatus(429);                            │    │
│  │              return false;                                      │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return true;                                            │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、分布式ID

### 6.1 Snowflake ID 生成器

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Snowflake ID 生成器                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Snowflake 算法原理                                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │   1                   41                    12           1       │    │
│  │  ┌───────────────────┬────────────────────┬────────────┬────────┐ │    │
│  │  │    符号位（固定0） │     时间戳（毫秒）   │  机器ID    │ 序列号  │ │    │
│  │  └───────────────────┴────────────────────┴────────────┴────────┘ │    │
│  │                                                                   │    │
│  │  • 符号位：1位，固定为0                                           │    │
│  │  • 时间戳：41位，可以使用约69年                                   │    │
│  │  • 机器ID：10位，最多支持1024个节点                               │    │
│  │  • 序列号：12位，每毫秒最多生成4096个ID                           │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  IdGenerator 实现                                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class IdGenerator {                                      │    │
│  │                                                                   │    │
│  │      private final long workerId;                                │    │
│  │      private final long epoch = 1609459200000L; // 2021-01-01   │    │
│  │      private long sequence = 0L;                                │    │
│  │      private long lastTimestamp = -1L;                          │    │
│  │                                                                   │    │
│  │      public IdGenerator(long workerId) {                        │    │
│  │          this.workerId = workerId;                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public synchronized long nextId() {                        │    │
│  │          long timestamp = System.currentTimeMillis();           │    │
│  │                                                                   │    │
│  │          if (timestamp < lastTimestamp) {                       │    │
│  │              throw new RuntimeException("时钟回拨");            │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          if (timestamp == lastTimestamp) {                       │    │
│  │              sequence = (sequence + 1) & 4095;  // 12位掩码     │    │
│  │              if (sequence == 0) {                               │    │
│  │                  timestamp = waitNextMillis();                  │    │
│  │              }                                                   │    │
│  │          } else {                                                │    │
│  │              sequence = 0;                                       │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          lastTimestamp = timestamp;                             │    │
│  │                                                                   │    │
│  │          return ((timestamp - epoch) << 22)                     │    │
│  │               | (workerId << 12)                                 │    │
│  │               | sequence;                                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public String nextIdStr() {                                 │    │
│  │          return String.valueOf(nextId());                        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  注册方式                                                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  • IdGeneratorFactory：通过 @Component 自动扫描，作为单例容器         │    │
│  │    持有 IdGenerator 实例，可调用 nextId() / nextIdStr()            │    │
│  │  • IdGeneratorConfig：把 IdGenerator 注册为 Spring Bean，           │    │
│  │    便于在 @RequiredArgsConstructor 构造注入中直接使用              │    │
│  │  • WorkerIdAssigner：通过 Redis 分配 workerId（默认 0），           │    │
│  │    保证多节点 ID 不冲突                                             │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、使用指南

### 7.1 Maven 依赖

```xml
<dependency>
    <groupId>com.MeowFlow</groupId>
    <artifactId>MeowFlow-common</artifactId>
    <version>${MeowFlow.version}</version>
</dependency>
```

### 7.2 线程池使用

```java
@Service
public class MyService {

    @Autowired
    @Qualifier("llmCallPool")
    private Executor llmCallPool;

    public void callLLM() {
        llmCallPool.execute(() -> {
            // LLM 调用逻辑
            // 自动继承 UserContext 和 TraceContext
        });
    }
}
```

### 7.3 熔断使用

```java
@Service
public class LLMService {

    @CircuitBreaker(name = "llmCall", fallbackMethod = "chatFallback")
    public String chat(String prompt) {
        return callLLM(prompt);
    }

    private String chatFallback(String prompt, Throwable t) {
        log.warn("LLM 熔断降级: {}", t.getMessage());
        return "服务暂不可用，请稍后重试";
    }
}
```

### 7.4 限流使用

```java
@Service
public class ApiService {

    @RateLimiter(name = "apiLimit", fallbackMethod = "apiFallback")
    public String apiCall() {
        return "success";
    }

    private String apiFallback(Throwable t) {
        throw new BizException(ResultCode.RATE_LIMITED);
    }
}
```

### 7.5 链路追踪使用

```java
@Service
public class WorkflowService {

    @TraceNode(value = "工作流执行", category = "flow")
    public ExecutionResult execute(String workflowId) {
        // 自动记录 Trace 日志
    }
}
```

---

## 八、配置说明

### 8.1 公共配置（meowflow-common）

`meowflow-common/src/main/resources/common-defaults.yml` 提供所有业务模块共享的默认配置：

```yaml
# 通用默认值，通过 spring.config.import 导入
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# AES 加密密钥配置
# 开发环境默认值，生产环境必须通过环境变量或 Nacos 覆盖
meowflow:
  crypto:
    aes-key: ${MEOWFLOW_CRYPTO_AES_KEY:<your-base64-32bytes-key>}
    key-id: dev-local
```

### 8.2 配置加载顺序

1. 各模块 `application.yml`
2. `spring.config.import: optional:classpath:common-defaults.yml`（common 默认值）
3. Profile 特定配置 `application-{profile}.yml`
4. 环境变量或 Nacos 覆盖

### 8.3 新模块配置模板

```yaml
spring:
  application:
    name: meowflow-{module}
  config:
    import: optional:classpath:common-defaults.yml
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:meowflow}
    username: ${POSTGRES_USER:meowflow}
    password: ${POSTGRES_PASSWORD:meowflow}
```

### 8.4 线程池配置

```yaml
MeowFlow:
  thread-pool:
    workflow-executor:
      core-size: 10
      max-size: 20
      queue-capacity: 1000
    llm-call:
      core-size: 20
      max-size: 50
      queue-capacity: 2000
```

### 8.2 Resilience4j 配置

```yaml
resilience4j:
  circuitbreaker:
    instances:
      llmCall:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
  ratelimiter:
    instances:
      apiLimit:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 5s
```

---

## 九、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.3 | 2026-07-20 | 新增公共配置约定（common-defaults.yml、配置加载顺序、新模块配置模板） |
| v1.2 | 2026-07-19 | 补充 IdGeneratorConfig：把 IdGenerator 注册为 Spring Bean，便于构造注入 |
| v1.1 | 2026-07-15 | 完善目录结构：添加 AsyncConfig、TtlConfig、SecurityUtils、TraceLog |
| v1.0 | 2026-07-10 | 初始版本，参考 Ragent 框架设计，使用 Resilience4j 实现限流熔断 |

---

**文档版本：v1.3**
**基于 Ragent Framework 设计理念**
**限流熔断基于 Resilience4j 实现**
**最后更新：2026-07-20**
