# MeowFlow Executor - 执行器服务模块设计文档

> 本文档详细描述喵流平台的执行器服务模块（MeowFlow-executor）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MeowFlow-executor 模块定位                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-executor 是执行器服务模块，负责：                                  │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 任务调度与分发                                              │   │
│  │      • 接收执行任务                                              │   │
│  │      • 任务分发到执行节点                                        │   │
│  │                                                                   │   │
│  │   2. 执行节点管理                                                │   │
│  │      • 执行节点注册                                              │   │
│  │      • 节点健康检查                                              │   │
│  │      • 负载均衡                                                  │   │
│  │                                                                   │   │
│  │   3. 任务状态管理                                                │   │
│  │      • 任务状态同步                                              │   │
│  │      • 任务取消                                                  │   │
│  │      • 超时处理                                                  │   │
│  │                                                                   │   │
│  │   4. 远程执行（可选微服务架构）                                    │   │
│  │      • 远程节点调用                                              │   │
│  │      • 执行结果回调                                              │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  说明：在单体架构中，executor 模块的功能主要集成在 workflow 模块中          │
│       此模块主要负责执行器的分布式扩展能力                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 任务可追踪                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 每个任务有唯一 ID，全程可追踪                                       │
│  • 任务状态实时同步                                                  │
│  • 支持任务取消和超时处理                                            │
│                                                                          │
│  2. 执行可扩展                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 支持执行节点水平扩展                                              │
│  • 执行节点自动发现                                                  │
│  • 负载均衡分配任务                                                  │
│                                                                          │
│  3. 高可用保障                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 执行节点健康检查                                                  │
│  • 失败任务自动重试                                                  │
│  • 任务持久化保障                                                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
MeowFlow-executor/
├── pom.xml
└── src/main/java/com/meowflow/executor/
    │
    ├── controller/                      # 控制器层
    │   └── ExecutorController.java     # 执行器管理 REST API
    │
    ├── service/                         # 业务逻辑层
    │   ├── TaskService.java            # 任务管理
    │   ├── TaskDispatchService.java    # 任务分发（支持本地/分布式模式）
    │   └── ExecutorNodeRegistry.java    # 执行节点注册与管理
    │
    ├── model/                           # 数据模型
    │   ├── Task.java                   # 任务定义
    │   ├── TaskQueue.java              # 任务队列
    │   ├── TaskContext.java            # 任务上下文
    │   ├── TaskResult.java             # 任务结果
    │   ├── TaskStatus.java             # 任务状态枚举
    │   └── TaskType.java               # 任务类型枚举
    │
    ├── mq/                              # 消息队列
    │   ├── RabbitMQConfig.java         # RabbitMQ 配置属性
    │   ├── RabbitMQCoreConfig.java     # RabbitMQ 核心配置（队列/交换机）
    │   ├── DelayedMessageConfig.java   # 延迟消息配置
    │   ├── TaskProducer.java           # 任务消息生产者
    │   ├── TaskConsumer.java           # 任务消息消费者
    │   └── TaskMessage.java            # 任务消息定义
    │
    ├── client/                         # 远程调用
    │   ├── ExecutorClient.java         # 执行器 REST 客户端
    │   └── ExecutorNodeConfig.java    # 执行器节点配置
    │
    ├── loadbalancer/                    # 负载均衡
    │   ├── LoadBalancer.java           # 负载均衡接口
    │   ├── RandomLoadBalancer.java     # 随机策略
    │   ├── RoundRobinLoadBalancer.java # 轮询策略
    │   ├── WeightedLoadBalancer.java  # 加权策略
    │   └── LeastConnectionsLoadBalancer.java  # 最小连接数策略
    │
    ├── registry/                        # 节点注册
    │   ├── ExecutorNode.java           # 执行节点定义
    │   ├── HealthChecker.java          # 健康检查
    │   └── TimeoutScanner.java        # 超时扫描
    │
    └── config/                         # 配置
        └── ExecutorConfig.java         # Swagger OpenAPI 配置
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           模块依赖关系                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                              MeowFlow-executor                              │
│                                    │                                      │
│                                    ▼                                      │
│                         ┌─────────────────┐                             │
│                         │ MeowFlow-common  │                             │
│                         │  通用基础设施   │                             │
│                         └────────┬────────┘                             │
│                                  │                                       │
│                                  ▼                                       │
│                         ┌─────────────────┐                             │
│                         │ MeowFlow-workflow │                             │
│                         │  工作流服务    │                             │
│                         └─────────────────┘                             │
│                                                                          │
│  说明：executor 模块依赖 workflow 模块，提供分布式执行能力               │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、任务管理

### 3.1 任务定义

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          任务定义                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Task 任务定义                                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class Task implements Serializable {                       │    │
│  │                                                                   │    │
│  │      /** 任务ID */                                               │    │
│  │      private String taskId;                                       │    │
│  │                                                                   │    │
│  │      /** 执行ID */                                               │    │
│  │      private String executionId;                                  │    │
│  │                                                                   │    │
│  │      /** 节点ID */                                               │    │
│  │      private String nodeId;                                       │    │
│  │                                                                   │    │
│  │      /** 节点类型 */                                             │    │
│  │      private String nodeType;                                     │    │
│  │                                                                   │    │
│  │      /** 任务类型 */                                             │    │
│  │      private TaskType type;                                      │    │
│  │                                                                   │    │
│  │      /** 任务参数 */                                             │    │
│  │      private Map<String, Object> params;                          │    │
│  │                                                                   │    │
│  │      /** 执行节点ID */                                           │    │
│  │      private String executorNodeId;                                │    │
│  │                                                                   │    │
│  │      /** 优先级 */                                               │    │
│  │      private int priority = 0;                                    │    │
│  │                                                                   │    │
│  │      /** 超时时间（毫秒）*/                                      │    │
│  │      private long timeoutMs = 60000;                              │    │
│  │                                                                   │    │
│  │      /** 重试次数 */                                             │    │
│  │      private int retryCount = 0;                                 │    │
│  │      private int maxRetries = 3;                                  │    │
│  │                                                                   │    │
│  │      /** 创建时间 */                                             │    │
│  │      private long createTime;                                     │    │
│  │                                                                   │    │
│  │      /** 开始时间 */                                             │    │
│  │      private long startTime;                                      │    │
│  │                                                                   │    │
│  │      /** 结束时间 */                                             │    │
│  │      private long endTime;                                        │    │
│  │                                                                   │    │
│  │      /** 任务状态 */                                             │    │
│  │      private TaskStatus status;                                   │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public enum TaskType {                                           │    │
│  │      NODE_EXECUTION,     // 节点执行                              │    │
│  │      ASYNC_CALLBACK,     // 异步回调                              │    │
│  │      SCHEDULE_TRIGGER,   // 定时触发                              │    │
│  │      HEARTBEAT          // 心跳检测                              │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public enum TaskStatus {                                         │    │
│  │      PENDING,           // 等待中                                  │    │
│  │      QUEUED,           // 已入队                                  │    │
│  │      RUNNING,          // 执行中                                  │    │
│  │      SUCCESS,          // 成功                                    │    │
│  │      FAILED,           // 失败                                    │    │
│  │      CANCELLED,        // 已取消                                  │    │
│  │      TIMEOUT           // 超时                                    │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 任务队列

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          任务队列设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TaskQueue 任务队列                                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class TaskQueue {                                         │    │
│  │                                                                   │    │
│  │      private final BlockingQueue<Task> queue;                    │    │
│  │      private final Map<String, Task> taskMap;                    │    │
│  │      private final Map<String, Long> taskTimestamps;             │    │
│  │                                                                   │    │
│  │      public TaskQueue(int capacity) {                            │    │
│  │          this.queue = new LinkedBlockingQueue<>(capacity);       │    │
│  │          this.taskMap = new ConcurrentHashMap<>();             │    │
│  │          this.taskTimestamps = new ConcurrentHashMap<>();     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 入队                                                    │    │
│  │       */                                                        │    │
│  │      public boolean enqueue(Task task) {                         │    │
│  │          boolean success = queue.offer(task);                   │    │
│  │          if (success) {                                          │    │
│  │              taskMap.put(task.getTaskId(), task);               │    │
│  │              taskTimestamps.put(task.getTaskId(),                │    │
│  │                  System.currentTimeMillis());                    │    │
│  │              task.setStatus(TaskStatus.QUEUED);                  │    │
│  │          }                                                       │    │
│  │          return success;                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 出队                                                    │    │
│  │       */                                                        │    │
│  │      public Task dequeue() {                                     │    │
│  │          Task task = queue.poll();                               │    │
│  │          if (task != null) {                                     │    │
│  │              task.setStatus(TaskStatus.RUNNING);                 │    │
│  │              task.setStartTime(System.currentTimeMillis());       │    │
│  │          }                                                       │    │
│  │          return task;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 阻塞出队                                                │    │
│  │       */                                                        │    │
│  │      public Task dequeueWithWait(long timeout, TimeUnit unit)    │    │
│  │          throws InterruptedException {                           │    │
│  │          Task task = queue.poll(timeout, unit);                  │    │
│  │          if (task != null) {                                     │    │
│  │              task.setStatus(TaskStatus.RUNNING);                 │    │
│  │              task.setStartTime(System.currentTimeMillis());       │    │
│  │          }                                                       │    │
│  │          return task;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取任务                                                │    │
│  │       */                                                        │    │
│  │      public Task getTask(String taskId) {                        │    │
│  │          return taskMap.get(taskId);                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 取消任务                                                │    │
│  │       */                                                        │    │
│  │      public boolean cancel(String taskId) {                      │    │
│  │          Task task = taskMap.remove(taskId);                    │    │
│  │          if (task != null) {                                     │    │
│  │              task.setStatus(TaskStatus.CANCELLED);               │    │
│  │              taskTimestamps.remove(taskId);                      │    │
│  │              return true;                                        │    │
│  │          }                                                       │    │
│  │          return false;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取队列大小                                            │    │
│  │       */                                                        │    │
│  │      public int size() {                                        │    │
│  │          return queue.size();                                    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 检查超时任务                                            │    │
│  │       */                                                        │    │
│  │      public List<Task> checkTimeout(long timeoutMs) {            │    │
│  │          List<Task> timeoutTasks = new ArrayList<>();           │    │
│  │          long now = System.currentTimeMillis();                   │    │
│  │                                                                   │    │
│  │          for (Map.Entry<String, Long> entry :                    │    │
│  │                   taskTimestamps.entrySet()) {                   │    │
│  │              if (now - entry.getValue() > timeoutMs) {          │    │
│  │                  Task task = taskMap.get(entry.getKey());       │    │
│  │                  if (task != null && task.getStatus() ==       │    │
│  │                      TaskStatus.RUNNING) {                      │    │
│  │                      timeoutTasks.add(task);                     │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return timeoutTasks;                                    │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、执行节点管理

### 4.1 执行节点定义

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行节点定义                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutorNode 执行节点                                            │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class ExecutorNode implements Serializable {               │    │
│  │                                                                   │    │
│  │      /** 节点ID */                                               │    │
│  │      private String nodeId;                                       │    │
│  │                                                                   │    │
│  │      /** 节点名称 */                                             │    │
│  │      private String name;                                         │    │
│  │                                                                   │    │
│  │      /** 节点地址 */                                             │    │
│  │      private String host;                                         │    │
│  │                                                                   │    │
│  │      /** 端口 */                                                 │    │
│  │      private int port;                                            │    │
│  │                                                                   │    │
│  │      /** 节点标签 */                                             │    │
│  │      private Set<String> tags;                                    │    │
│  │                                                                   │    │
│  │      /** 支持的节点类型 */                                       │    │
│  │      private Set<String> supportedNodeTypes;                      │    │
│  │                                                                   │    │
│  │      /** 并发容量 */                                             │    │
│  │      private int concurrency;                                      │    │
│  │      private int currentLoad;                                      │    │
│  │                                                                   │    │
│  │      /** 状态 */                                                 │    │
│  │      private NodeStatus status;                                    │    │
│  │                                                                   │    │
│  │      /** 健康状态 */                                             │    │
│  │      private HealthStatus healthStatus;                            │    │
│  │      private long lastHeartbeat;                                   │    │
│  │                                                                   │    │
│  │      /** 注册时间 */                                             │    │
│  │      private long registerTime;                                    │    │
│  │                                                                   │    │
│  │      /** 版本号 */                                               │    │
│  │      private String version;                                       │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public enum NodeStatus {                                          │    │
│  │      ACTIVE,          // 活跃                                    │    │
│  │      INACTIVE,        // 不活跃                                  │    │
│  │      DRAINING,        // 排空中                                  │    │
│  │      OFFLINE          // 离线                                    │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  public enum HealthStatus {                                        │    │
│  │      HEALTHY,         // 健康                                    │    │
│  │      DEGRADED,        // 降级                                    │    │
│  │      UNHEALTHY        // 不健康                                  │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 节点注册中心

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          节点注册中心                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutorNodeRegistry 节点注册中心                               │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class ExecutorNodeRegistry                               │    │
│  │          implements InitializingBean, DisposableBean {           │    │
│  │                                                                   │    │
│  │      private final Map<String, ExecutorNode> nodes =             │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      private final Map<String, Set<String>> tagToNodes =         │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      private final ScheduledExecutorService scheduler =           │    │
│  │          Executors.newScheduledThreadPool(1);                   │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 注册节点                                                │    │
│  │       */                                                        │    │
│  │      public void register(ExecutorNode node) {                   │    │
│  │          ExecutorNode existing = nodes.get(node.getNodeId());    │    │
│  │          if (existing != null) {                                │    │
│  │              // 更新现有节点                                     │    │
│  │              existing.setHost(node.getHost());                  │    │
│  │              existing.setPort(node.getPort());                  │    │
│  │              existing.setStatus(NodeStatus.ACTIVE);             │    │
│  │              existing.setLastHeartbeat(System.currentTimeMillis());│   │
│  │          } else {                                                │    │
│  │              // 注册新节点                                       │    │
│  │              node.setRegisterTime(System.currentTimeMillis());    │    │
│  │              node.setLastHeartbeat(System.currentTimeMillis());  │    │
│  │              nodes.put(node.getNodeId(), node);                  │    │
│  │                                                                   │    │
│  │              // 更新标签索引                                     │    │
│  │              if (node.getTags() != null) {                      │    │
│  │                  for (String tag : node.getTags()) {            │    │
│  │                      tagToNodes.computeIfAbsent(tag,              │    │
│  │                          k -> new HashSet<>()).add(node.getNodeId());│ │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │          log.info("注册执行节点: {} -> {}:{}",                    │    │
│  │              node.getNodeId(), node.getHost(), node.getPort());  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 注销节点                                                │    │
│  │       */                                                        │    │
│  │      public void unregister(String nodeId) {                     │    │
│  │          ExecutorNode node = nodes.remove(nodeId);               │    │
│  │          if (node != null && node.getTags() != null) {          │    │
│  │              for (String tag : node.getTags()) {                │    │
│  │                  Set<String> nodeSet = tagToNodes.get(tag);     │    │
│  │                  if (nodeSet != null) {                          │    │
│  │                      nodeSet.remove(nodeId);                    │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │          log.info("注销执行节点: {}", nodeId);                    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取节点                                                │    │
│  │       */                                                        │    │
│  │      public ExecutorNode getNode(String nodeId) {                │    │
│  │          return nodes.get(nodeId);                               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取所有活跃节点                                        │    │
│  │       */                                                        │    │
│  │      public List<ExecutorNode> getActiveNodes() {                │    │
│  │          return nodes.values().stream()                          │    │
│  │              .filter(n -> n.getStatus() == NodeStatus.ACTIVE)  │    │
│  │              .collect(Collectors.toList());                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 按标签获取节点                                          │    │
│  │       */                                                        │    │
│  │      public List<ExecutorNode> getNodesByTag(String tag) {       │    │
│  │          Set<String> nodeIds = tagToNodes.get(tag);              │    │
│  │          if (nodeIds == null) {                                 │    │
│  │              return Collections.emptyList();                      │    │
│  │          }                                                       │    │
│  │          return nodeIds.stream()                                │    │
│  │              .map(nodes::get)                                   │    │
│  │              .filter(Objects::nonNull)                         │    │
│  │              .filter(n -> n.getStatus() == NodeStatus.ACTIVE)  │    │
│  │              .collect(Collectors.toList());                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 心跳更新                                                │    │
│  │       */                                                        │    │
│  │      public void heartbeat(String nodeId) {                      │    │
│  │          ExecutorNode node = nodes.get(nodeId);                  │    │
│  │          if (node != null) {                                    │    │
│  │              node.setLastHeartbeat(System.currentTimeMillis());  │    │
│  │              node.setStatus(NodeStatus.ACTIVE);                  │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostConstruct                                             │    │
│  │      public void init() {                                        │    │
│  │          // 启动心跳检测                                         │    │
│  │          scheduler.scheduleAtFixedRate(() -> {                   │    │
│  │              checkHeartbeats();                                  │    │
│  │          }, 30, 30, TimeUnit.SECONDS);                           │    │
│  │                                                                   │    │
│  │          // 启动健康检查                                         │    │
│  │          scheduler.scheduleAtFixedRate(() -> {                   │    │
│  │              checkHealth();                                      │    │
│  │          }, 60, 60, TimeUnit.SECONDS);                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void checkHeartbeats() {                             │    │
│  │          long now = System.currentTimeMillis();                   │    │
│  │          long timeout = 90 * 1000;  // 90秒超时                   │    │
│  │                                                                   │    │
│  │          for (ExecutorNode node : nodes.values()) {             │    │
│  │              if (now - node.getLastHeartbeat() > timeout) {     │    │
│  │                  log.warn("执行节点心跳超时: {}", node.getNodeId());│  │
│  │                  node.setStatus(NodeStatus.INACTIVE);           │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void checkHealth() {                                 │    │
│  │          for (ExecutorNode node : getActiveNodes()) {           │    │
│  │              try {                                               │    │
│  │                  boolean healthy = healthChecker.check(node);    │    │
│  │                  node.setHealthStatus(healthy ?                   │    │
│  │                      HealthStatus.HEALTHY : HealthStatus.UNHEALTHY);│   │
│  │              } catch (Exception e) {                             │    │
│  │                  node.setHealthStatus(HealthStatus.UNHEALTHY);   │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、任务分发

### 5.1 任务分发服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          任务分发服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TaskDispatchService 任务分发服务                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class TaskDispatchService {                               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutorNodeRegistry nodeRegistry;                    │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private LoadBalancer loadBalancer;                           │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutorClient executorClient;                        │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 分发任务                                                │    │
│  │       */                                                        │    │
│  │      public boolean dispatch(Task task) {                         │    │
│  │          // 1. 选择执行节点                                       │    │
│  │          ExecutorNode node = selectNode(task);                   │    │
│  │          if (node == null) {                                     │    │
│  │              log.warn("无可用执行节点: taskId={}",                 │    │
│  │                  task.getTaskId());                              │    │
│  │              return false;                                       │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 2. 更新任务节点                                       │    │
│  │          task.setExecutorNodeId(node.getNodeId());               │    │
│  │                                                                   │    │
│  │          // 3. 发送任务                                          │    │
│  │          try {                                                   │    │
│  │              executorClient.submitTask(node, task);               │    │
│  │              return true;                                         │    │
│  │          } catch (Exception e) {                                 │    │
│  │              log.error("任务分发失败: taskId={}, node={}",         │    │
│  │                  task.getTaskId(), node.getNodeId(), e);          │    │
│  │              return false;                                       │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 选择执行节点                                            │    │
│  │       */                                                        │    │
│  │      private ExecutorNode selectNode(Task task) {                 │    │
│  │          // 获取支持该节点类型的节点                              │    │
│  │          Set<String> supportedTypes = parseSupportedTypes(task);   │    │
│  │                                                                   │    │
│  │          List<ExecutorNode> candidates = nodeRegistry             │    │
│  │              .getActiveNodes().stream()                         │    │
│  │              .filter(n -> n.getHealthStatus() ==                  │    │
│  │                  HealthStatus.HEALTHY)                           │    │
│  │              .filter(n -> n.getCurrentLoad() <                    │    │
│  │                  n.getConcurrency())                             │    │
│  │              .filter(n -> supportedTypes.contains(               │    │
│  │                  n.getSupportedNodeTypes()))                      │    │
│  │              .collect(Collectors.toList());                      │    │
│  │                                                                   │    │
│  │          if (candidates.isEmpty()) {                              │    │
│  │              return null;                                         │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 负载均衡选择                                          │    │
│  │          return loadBalancer.select(candidates, task);           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private Set<String> parseSupportedTypes(Task task) {         │    │
│  │          // 如果任务指定了节点类型，直接返回                       │    │
│  │          if (StringUtils.isNotBlank(task.getNodeType())) {       │    │
│  │              return Collections.singleton(task.getNodeType());     │    │
│  │          }                                                       │    │
│  │          // 否则返回所有类型                                      │    │
│  │          return Collections.emptySet();                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 批量分发                                                │    │
│  │       */                                                        │    │
│  │      public Map<String, Boolean> dispatchBatch(List<Task> tasks) { │    │
│  │          Map<String, Boolean> results = new HashMap<>();         │    │
│  │          for (Task task : tasks) {                               │    │
│  │              results.put(task.getTaskId(), dispatch(task));       │    │
│  │          }                                                       │    │
│  │          return results;                                         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 负载均衡

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          负载均衡器                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  LoadBalancer 负载均衡器                                         │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public interface LoadBalancer {                                  │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 选择节点                                                │    │
│  │       */                                                        │    │
│  │      ExecutorNode select(List<ExecutorNode> nodes, Task task);   │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  // 随机负载均衡                                                │    │
│  │  public class RandomLoadBalancer implements LoadBalancer {       │    │
│  │      private final Random random = new Random();                │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public ExecutorNode select(List<ExecutorNode> nodes,         │    │
│  │                                Task task) {                      │    │
│  │          return nodes.get(random.nextInt(nodes.size()));        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  // 轮询负载均衡                                                │    │
│  │  public class RoundRobinLoadBalancer implements LoadBalancer {  │    │
│  │      private final AtomicInteger counter = new AtomicInteger(0);│    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public ExecutorNode select(List<ExecutorNode> nodes,         │    │
│  │                                Task task) {                      │    │
│  │          int index = counter.getAndIncrement() % nodes.size();  │    │
│  │          return nodes.get(index);                                │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  // 加权负载均衡                                                │    │
│  │  public class WeightedLoadBalancer implements LoadBalancer {    │    │
│  │      private final AtomicInteger sequence = new AtomicInteger(0);│    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public ExecutorNode select(List<ExecutorNode> nodes,         │    │
│  │                                Task task) {                      │    │
│  │          // 计算总权重                                            │    │
│  │          int totalWeight = nodes.stream()                        │    │
│  │              .mapToInt(n -> n.getConcurrency())                  │    │
│  │              .sum();                                             │    │
│  │                                                                   │    │
│  │          // 伪随机选择                                           │    │
│  │          int offset = sequence.getAndIncrement() % totalWeight;  │    │
│  │                                                                   │    │
│  │          for (ExecutorNode node : nodes) {                       │    │
│  │              offset -= node.getConcurrency();                    │    │
│  │              if (offset < 0) {                                   │    │
│  │                  return node;                                    │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return nodes.get(0);                                    │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  // 最少连接负载均衡                                            │    │
│  │  public class LeastConnectionLoadBalancer                         │    │
│  │          implements LoadBalancer {                                │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public ExecutorNode select(List<ExecutorNode> nodes,         │    │
│  │                                Task task) {                      │    │
│  │          return nodes.stream()                                   │    │
│  │              .min(Comparator.comparingInt(n ->                   │    │
│  │                  n.getCurrentLoad()))                             │    │
│  │              .orElse(nodes.get(0));                             │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、定时任务 - RabbitMQ 延时队列

### 6.1 设计概述

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    RabbitMQ 延时队列定时任务                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                                                                   │    │
│  │   用户请求定时任务                                                 │    │
│  │        │                                                         │    │
│  │        ▼                                                         │    │
│  │   ┌─────────────────┐                                            │    │
│  │   │ 计算延时时间      │  executeAt - currentTime                 │    │
│  │   └────────┬────────┘                                            │    │
│  │            │                                                      │    │
│  │            ▼                                                      │    │
│  │   ┌─────────────────────────────────────────────────────┐        │    │
│  │   │              RabbitMQ 延时交换机                       │        │    │
│  │   │   (x-delayed-message / x-message-ttl + DLX)        │        │    │
│  │   └─────────────────────┬───────────────────────────────┘        │    │
│  │                         │                                        │    │
│  │                         ▼                                        │    │
│  │   ┌─────────────────────────────────────────────────────┐        │    │
│  │   │              延时队列（等待到期）                      │        │    │
│  │   └─────────────────────┬───────────────────────────────┘        │    │
│  │                         │                                        │    │
│  │                         ▼  延时到期                              │    │
│  │   ┌─────────────────────────────────────────────────────┐        │    │
│  │   │              执行队列（立即消费）                      │        │    │
│  │   └─────────────────────┬───────────────────────────────┘        │    │
│  │                         │                                        │    │
│  │                         ▼                                        │    │
│  │   ┌─────────────────────────────────────────────────────┐        │    │
│  │   │              任务执行器（消费消息）                    │        │    │
│  │   └─────────────────────────────────────────────────────┘        │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  优点：                                                                  │
│  • 消息级延时，精度高（毫秒级）                                          │
│  • 无需轮询，资源占用少                                                 │
│  • RabbitMQ 持久化保障消息可靠                                          │
│  • 支持集群部署，高可用                                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 延时队列配置

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          RabbitMQ 配置                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  RabbitMQConfig 延时队列配置                                      │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Configuration                                                  │    │
│  │  public class DelayedMessageConfig {                              │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 延时交换机（使用 rabbitmq_delayed_message_exchange 插件）  │    │
│  │       */                                                        │    │
│  │      @Bean                                                       │    │
│  │      public CustomExchange delayedExchange() {                    │    │
│  │          Map<String, Object> args = new HashMap<>();             │    │
│  │          args.put("x-delayed-type", "direct");                   │    │
│  │          return new CustomExchange(                              │    │
│  │              "MeowFlow.delayed.exchange",                         │    │
│  │              "x-delayed-message",                                │    │
│  │              true,                                               │    │
│  │              false,                                              │    │
│  │              args                                                │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 延时队列（存放等待执行的消息）                             │    │
│  │       */                                                        │    │
│  │      @Bean                                                       │    │
│  │      public Queue delayedQueue() {                                │    │
│  │          return QueueBuilder                                     │    │
│  │              .durable("MeowFlow.delayed.queue")                   │    │
│  │              .build();                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 绑定延时交换机和队列                                       │    │
│  │       */                                                        │    │
│  │      @Bean                                                       │    │
│  │      public Binding delayedBinding(                               │    │
│  │              Queue delayedQueue,                                  │    │
│  │              CustomExchange delayedExchange) {                    │    │
│  │          return BindingBuilder                                   │    │
│  │              .bind(delayedQueue)                                  │    │
│  │              .to(delayedExchange)                                 │    │
│  │              .with("delayed.task").noargs();                     │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  注意：需要安装 rabbitmq_delayed_message_exchange 插件                   │
│  安装命令：rabbitmq-plugins enable rabbitmq_delayed_message_exchange      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.3 延时任务服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          延时任务服务                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  DelayedTaskMessage 延时任务消息                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class DelayedTaskMessage implements Serializable {       │    │
│  │                                                                   │    │
│  │      /** 任务ID */                                               │    │
│  │      private String taskId;                                       │    │
│  │                                                                   │    │
│  │      /** 任务类型 */                                             │    │
│  │      private String taskType;                                    │    │
│  │                                                                   │    │
│  │      /** 执行时间戳（毫秒）*/                                     │    │
│  │      private long executeAt;                                      │    │
│  │                                                                   │    │
│  │      /** 任务参数 */                                             │    │
│  │      private Map<String, Object> params;                          │    │
│  │                                                                   │    │
│  │      /** 重试次数 */                                             │    │
│  │      private int retryCount = 0;                                 │    │
│  │                                                                   │    │
│  │      /** 创建时间 */                                             │    │
│  │      private long createTime;                                     │    │
│  │                                                                   │    │
│  │      /** 执行节点ID（可选）*/                                     │    │
│  │      private String executorNodeId;                                │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  DelayedTaskService 延时任务服务                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class DelayedTaskService {                               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private RabbitTemplate rabbitTemplate;                        │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 发送延时任务                                             │    │
│  │       * @param taskType 任务类型                                 │    │
│  │       * @param executeAt 执行时间戳                              │    │
│  │       * @param params 任务参数                                   │    │
│  │       * @return 任务ID                                           │    │
│  │       */                                                        │    │
│  │      public String scheduleTask(String taskType,                 │    │
│  │                                long executeAt,                    │    │
│  │                                Map<String, Object> params) {       │    │
│  │          String taskId = UUID.randomUUID().toString();           │    │
│  │          long delay = executeAt - System.currentTimeMillis();     │    │
│  │                                                                   │    │
│  │          if (delay < 0) {                                       │    │
│  │              delay = 0;  // 已过期，立即执行                      │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          DelayedTaskMessage message = new DelayedTaskMessage();   │    │
│  │          message.setTaskId(taskId);                              │    │
│  │          message.setTaskType(taskType);                         │    │
│  │          message.setExecuteAt(executeAt);                        │    │
│  │          message.setParams(params);                             │    │
│  │          message.setCreateTime(System.currentTimeMillis());      │    │
│  │                                                                   │    │
│  │          sendDelayedMessage(message, delay);                     │    │
│  │                                                                   │    │
│  │          log.info("延时任务已调度: taskId={}, delay={}ms",       │    │
│  │              taskId, delay);                                     │    │
│  │          return taskId;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 发送延时消息（使用 x-delay 头）                           │    │
│  │       */                                                        │    │
│  │      private void sendDelayedMessage(DelayedTaskMessage message,  │    │
│  │                                        long delay) {              │    │
│  │          rabbitTemplate.convertAndSend(                          │    │
│  │              "MeowFlow.delayed.exchange",                         │    │
│  │              "delayed.task",                                      │    │
│  │              message,                                             │    │
│  │              msg -> {                                             │    │
│  │                  msg.getMessageProperties()                       │    │
│  │                      .setHeader("x-delay", delay);               │    │
│  │                  return msg;                                     │    │
│  │              }                                                   │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 取消延时任务（通过延迟取消标记）                           │    │
│  │       */                                                        │    │
│  │      public boolean cancelTask(String taskId) {                  │    │
│  │          // 记录取消标记，由执行器检查                            │    │
│  │          canceledTasks.add(taskId);                              │    │
│  │          log.info("延时任务已标记取消: taskId={}", taskId);      │    │
│  │          return true;                                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private final Set<String> canceledTasks =                   │    │
│  │          ConcurrentHashMap.newKeySet();                          │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.4 延时任务消费者

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          延时任务消费者                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  DelayedTaskConsumer 延时任务消费者                               │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class DelayedTaskConsumer {                              │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private DelayedTaskService delayedTaskService;               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TaskDispatchService taskDispatchService;             │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TaskMapper taskMapper;                              │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 消费延时任务消息                                         │    │
│  │       */                                                        │    │
│  │      @RabbitListener(queues = "MeowFlow.delayed.queue")            │    │
│  │      public void consumeDelayedTask(DelayedTaskMessage message) {  │    │
│  │          String taskId = message.getTaskId();                    │    │
│  │                                                                   │    │
│  │          // 1. 检查是否已取消                                     │    │
│  │          if (delayedTaskService.isCanceled(taskId)) {            │    │
│  │              log.info("延时任务已取消，跳过执行: taskId={}", taskId);│ │
│  │              return;                                              │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 2. 记录任务开始                                       │    │
│  │          Task task = new Task();                                 │    │
│  │          task.setTaskId(taskId);                                 │    │
│  │          task.setType(TaskType.SCHEDULE_TRIGGER);                │    │
│  │          task.setParams(message.getParams());                    │    │
│  │          task.setStartTime(System.currentTimeMillis());          │    │
│  │          task.setStatus(TaskStatus.RUNNING);                     │    │
│  │                                                                   │    │
│  │          // 3. 执行业务逻辑                                       │    │
│  │          try {                                                   │    │
│  │              executeTask(message);                               │    │
│  │              task.setStatus(TaskStatus.SUCCESS);                │    │
│  │              task.setEndTime(System.currentTimeMillis());        │    │
│  │              log.info("延时任务执行成功: taskId={}", taskId);     │    │
│  │                                                                   │    │
│  │          } catch (Exception e) {                                 │    │
│  │              log.error("延时任务执行失败: taskId={}", taskId, e);  │    │
│  │              handleTaskFailure(message, task, e);               │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 4. 保存任务记录                                       │    │
│  │          taskMapper.insert(task);                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 执行具体任务                                             │    │
│  │       */                                                        │    │
│  │      private void executeTask(DelayedTaskMessage message) {       │    │
│  │          String taskType = message.getTaskType();               │    │
│  │          Map<String, Object> params = message.getParams();        │    │
│  │                                                                   │    │
│  │          switch (taskType) {                                     │    │
│  │              case "WORKFLOW_TRIGGER":                            │    │
│  │                  // 触发工作流                                     │    │
│  │                  triggerWorkflow(params);                        │    │
│  │                  break;                                          │    │
│  │              case "NOTIFICATION":                                │    │
│  │                  // 发送通知                                       │    │
│  │                  sendNotification(params);                        │    │
│  │                  break;                                          │    │
│  │              case "DATA_SYNC":                                   │    │
│  │                  // 数据同步                                       │    │
│  │                  syncData(params);                                │    │
│  │                  break;                                          │    │
│  │              default:                                            │    │
│  │                  log.warn("未知任务类型: {}", taskType);          │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 处理任务失败                                             │    │
│  │       */                                                        │    │
│  │      private void handleTaskFailure(DelayedTaskMessage message,   │    │
│  │                                       Task task, Exception e) {   │    │
│  │          if (message.getRetryCount() < 3) {                     │    │
│  │              // 重试                                                                     │    │
│  │              message.setRetryCount(message.getRetryCount() + 1); │    │
│  │              delayedTaskService.retryTask(message);             │    │
│  │              log.info("延时任务重试: taskId={}, retry={}",       │    │
│  │                  message.getTaskId(), message.getRetryCount()); │   │
│  │          } else {                                               │    │
│  │              // 重试次数用尽，标记失败                             │    │
│  │              task.setStatus(TaskStatus.FAILED);                 │    │
│  │              task.setErrorMessage(e.getMessage());              │    │
│  │              log.error("延时任务重试次数用尽: taskId={}",         │    │
│  │                  message.getTaskId());                          │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.5 使用示例

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          使用示例                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowExecutionController 控制器示例                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/workflow")                             │    │
│  │  public class WorkflowExecutionController {                      │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private DelayedTaskService delayedTaskService;               │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 预约工作流执行（定时触发）                                  │    │
│  │       */                                                        │    │
│  │      @PostMapping("/schedule")                                   │    │
│  │      public Result<String> scheduleWorkflow(                     │    │
│  │              @RequestParam String workflowId,                    │    │
│  │              @RequestParam long executeAt,  // 时间戳            │    │
│  │              @RequestParam(required = false) Map<String,         │    │
│  │                  Object> params) {                              │    │
│  │                                                                   │    │
│  │          // 构建任务参数                                         │    │
│  │          Map<String, Object> taskParams = new HashMap<>();       │    │
│  │          taskParams.put("workflowId", workflowId);              │    │
│  │          if (params != null) {                                  │    │
│  │              taskParams.putAll(params);                         │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 调度延时任务                                         │    │
│  │          String taskId = delayedTaskService.scheduleTask(        │    │
│  │              "WORKFLOW_TRIGGER",                                  │    │
│  │              executeAt,                                          │    │
│  │              taskParams                                          │    │
│  │          );                                                      │    │
│  │                                                                   │    │
│  │          return Result.success(taskId);                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 取消预约执行                                             │    │
│  │       */                                                        │    │
│  │      @DeleteMapping("/schedule/{taskId}")                        │    │
│  │      public Result<Void> cancelSchedule(                        │    │
│  │              @PathVariable String taskId) {                     │    │
│  │          delayedTaskService.cancelTask(taskId);                  │    │
│  │          return Result.success();                               │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  调用示例：                                                              │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  // 预约 1 小时后执行工作流                                       │    │
│  │  POST /api/v1/workflow/schedule?workflowId=wf001&executeAt=...   │    │
│  │                                                                   │    │
│  │  // 预约 30 分钟后发送通知                                       │    │
│  │  POST /api/v1/workflow/schedule?taskType=NOTIFICATION&executeAt=..│   │
│  │                                                                   │    │
│  │  // 取消预约                                                     │    │
│  │  DELETE /api/v1/workflow/schedule/{taskId}                       │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.6 适用场景与限制

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    延时队列适用场景与限制                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ✓ 适用场景                                                            │
│  ─────────────────────────────────────────────────────────────────────  │
│  • 单次延时任务（"1小时后执行"、"明天10点执行"）                        │
│  • 延时精度要求高（毫秒级）                                             │
│  • 任务数量中等（几千到几万）                                           │
│  • 需要消息持久化和可靠投递                                             │
│  • 支持集群部署                                                         │
│                                                                          │
│  ✗ 不适用场景                                                          │
│  ─────────────────────────────────────────────────────────────────────  │
│  • 周期性的 cron 任务（建议使用 XXL-JOB、Quartz）                       │
│  • 超长时间延时（> 7天，建议数据库 + 定时扫描）                          │
│  • 超大量任务（> 百万级，建议分片 + 数据库）                            │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  最佳实践                                                         │    │
│  │  ─────────────────────────────────────────────────────────────  │    │
│  │                                                                   │    │
│  │  短延时（< 1小时）  → RabbitMQ 延时队列（当前方案）               │    │
│  │  中延时（1-24小时）  → RabbitMQ 延时队列 + 持久化备份              │    │
│  │  长延时（> 24小时）  → 数据库 + 定时扫描（每小时）                 │    │
│  │  周期任务           → XXL-JOB / Quartz 独立调度系统               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、远程调用

### 7.1 执行器客户端

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行器客户端                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutorClient 执行器客户端                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class ExecutorClient {                                   │    │
│  │                                                                   │    │
│  │      private final Map<String, RestTemplate> nodeClients =       │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 提交任务                                                │    │
│  │       */                                                        │    │
│  │      public void submitTask(ExecutorNode node, Task task) {       │    │
│  │          RestTemplate client = getClient(node);                   │    │
│  │          String url = buildUrl(node, "/internal/task/submit");    │    │
│  │                                                                   │    │
│  │          try {                                                   │    │
│  │              client.postForObject(url, task, TaskResult.class);  │    │
│  │          } catch (RestClientException e) {                       │    │
│  │              throw new TaskDispatchException(                      │    │
│  │                  "任务提交失败: " + e.getMessage(), e);          │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 取消任务                                                │    │
│  │       */                                                        │    │
│  │      public boolean cancelTask(ExecutorNode node, String taskId) {│    │
│  │          RestTemplate client = getClient(node);                   │    │
│  │          String url = buildUrl(node, "/internal/task/cancel");   │    │
│  │                                                                   │    │
│  │          try {                                                   │    │
│  │              Boolean result = client.postForObject(               │    │
│  │                  url, taskId, Boolean.class);                    │    │
│  │              return result != null && result;                     │    │
│  │          } catch (RestClientException e) {                       │    │
│  │              log.error("取消任务失败: {}", taskId, e);           │    │
│  │              return false;                                       │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 查询任务状态                                            │    │
│  │       */                                                        │    │
│  │      public TaskStatus queryTaskStatus(ExecutorNode node,       │    │
│  │                                            String taskId) {     │    │
│  │          RestTemplate client = getClient(node);                   │    │
│  │          String url = buildUrl(node, "/internal/task/status");   │    │
│  │                                                                   │    │
│  │          try {                                                   │    │
│  │              return client.postForObject(url, taskId,            │    │
│  │                  TaskStatus.class);                              │    │
│  │          } catch (RestClientException e) {                       │    │
│  │              log.error("查询任务状态失败: {}", taskId, e);        │    │
│  │              return TaskStatus.FAILED;                           │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 健康检查                                                │    │
│  │       */                                                        │    │
│  │      public boolean healthCheck(ExecutorNode node) {             │    │
│  │          RestTemplate client = getClient(node);                   │    │
│  │          String url = buildUrl(node, "/internal/health");        │    │
│  │                                                                   │    │
│  │          try {                                                   │    │
│  │              ResponseEntity<Map> response = client.getForEntity  │    │
│  │                  (url, Map.class);                              │    │
│  │              return response.getStatusCode().is2xxSuccessful(); │    │
│  │          } catch (RestClientException e) {                       │    │
│  │              return false;                                        │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private RestTemplate getClient(ExecutorNode node) {         │    │
│  │          return nodeClients.computeIfAbsent(node.getNodeId(),   │    │
│  │              k -> createClient(node));                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private RestTemplate createClient(ExecutorNode node) {       │    │
│  │          SimpleClientHttpRequestFactory factory =                │    │
│  │              new SimpleClientHttpRequestFactory();               │    │
│  │          factory.setConnectTimeout(5000);                       │    │
│  │          factory.setReadTimeout(10000);                         │    │
│  │                                                                   │    │
│  │          return new RestTemplate(factory);                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private String buildUrl(ExecutorNode node, String path) {   │    │
│  │          return String.format("http://%s:%d%s",                  │    │
│  │              node.getHost(), node.getPort(), path);             │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 八、任务状态管理

### 7.1 状态同步服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          任务状态管理                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TaskStatusService 任务状态管理服务                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class TaskStatusService {                               │    │
│  │                                                                   │    │
│  │      private final Map<String, TaskStatus> taskStatuses =        │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      private final Map<String, TaskResult> taskResults =          │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 更新状态                                                │    │
│  │       */                                                        │    │
│  │      public void updateStatus(String taskId, TaskStatus status) { │    │
│  │          taskStatuses.put(taskId, status);                       │    │
│  │                                                                   │    │
│  │          // 状态变化时触发回调                                    │    │
│  │          onStatusChange(taskId, status);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取状态                                                │    │
│  │       */                                                        │    │
│  │      public TaskStatus getStatus(String taskId) {                │    │
│  │          return taskStatuses.get(taskId);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 保存结果                                                │    │
│  │       */                                                        │    │
│  │      public void saveResult(String taskId, TaskResult result) {   │    │
│  │          taskResults.put(taskId, result);                        │    │
│  │                                                                   │    │
│  │          // 根据结果更新状态                                      │    │
│  │          if (result.isSuccess()) {                              │    │
│  │              updateStatus(taskId, TaskStatus.SUCCESS);          │    │
│  │          } else {                                                │    │
│  │              updateStatus(taskId, TaskStatus.FAILED);           │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取结果                                                │    │
│  │       */                                                        │    │
│  │      public TaskResult getResult(String taskId) {                 │    │
│  │          return taskResults.get(taskId);                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 取消任务                                                │    │
│  │       */                                                        │    │
│  │      public boolean cancelTask(String taskId) {                   │    │
│  │          TaskStatus current = taskStatuses.get(taskId);           │    │
│  │          if (current == TaskStatus.RUNNING ||                    │    │
│  │              current == TaskStatus.PENDING) {                   │    │
│  │              updateStatus(taskId, TaskStatus.CANCELLED);          │    │
│  │              return true;                                        │    │
│  │          }                                                       │    │
│  │          return false;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 检查超时                                                │    │
│  │       */                                                        │    │
│  │      public List<String> checkTimeouts(long timeoutMs) {          │    │
│  │          List<String> timeoutTaskIds = new ArrayList<>();         │    │
│  │          long now = System.currentTimeMillis();                   │    │
│  │                                                                   │    │
│  │          for (Map.Entry<String, TaskStatus> entry :             │    │
│  │                   taskStatuses.entrySet()) {                    │    │
│  │              if (entry.getValue() == TaskStatus.RUNNING) {      │    │
│  │                  // TODO: 需要记录开始时间来检查超时             │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return timeoutTaskIds;                                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void onStatusChange(String taskId, TaskStatus status) {│ │
│  │          if (status == TaskStatus.SUCCESS ||                      │    │
│  │              status == TaskStatus.FAILED ||                       │    │
│  │              status == TaskStatus.CANCELLED) {                    │    │
│  │              // 触发完成回调                                      │    │
│  │              TaskResult result = taskResults.get(taskId);        │    │
│  │              if (result != null) {                               │    │
│  │                  notifyCompletion(taskId, result);                │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void notifyCompletion(String taskId, TaskResult result) {│ │
│  │          // 通知任务完成                                         │    │
│  │          log.info("任务完成: taskId={}, success={}",              │    │
│  │              taskId, result.isSuccess());                        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 九、健康检查

### 8.1 健康检查器

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          健康检查器                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  HealthChecker 健康检查器                                         │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class HealthChecker {                                    │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutorClient executorClient;                        │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 检查节点健康状态                                        │    │
│  │       */                                                        │    │
│  │      public boolean check(ExecutorNode node) {                   │    │
│  │          try {                                                   │    │
│  │              return executorClient.healthCheck(node);             │    │
│  │          } catch (Exception e) {                                 │    │
│  │              log.warn("健康检查失败: nodeId={}", node.getNodeId());│ │
│  │              return false;                                       │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 批量检查                                                │    │
│  │       */                                                        │    │
│  │      public Map<String, Boolean> checkBatch(                      │    │
│  │          List<ExecutorNode> nodes) {                            │    │
│  │          Map<String, Boolean> results = new HashMap<>();         │    │
│  │          for (ExecutorNode node : nodes) {                       │    │
│  │              results.put(node.getNodeId(), check(node));         │    │
│  │          }                                                       │    │
│  │          return results;                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 深度健康检查                                            │    │
│  │       */                                                        │    │
│  │      public HealthReport deepCheck(ExecutorNode node) {          │    │
│  │          HealthReport report = new HealthReport();               │    │
│  │          report.setNodeId(node.getNodeId());                     │    │
│  │          report.setTimestamp(System.currentTimeMillis());         │    │
│  │                                                                   │    │
│  │          // 检查网络连通性                                        │    │
│  │          report.setNetworkOk(checkNetwork(node));                │    │
│  │                                                                   │    │
│  │          // 检查资源使用                                          │    │
│  │          report.setResourceInfo(checkResources(node));           │    │
│  │                                                                   │    │
│  │          // 检查任务处理能力                                      │    │
│  │          report.setTaskCapacityOk(checkTaskCapacity(node));      │    │
│  │                                                                   │    │
│  │          // 总体健康状态                                          │    │
│  │          boolean healthy = report.isNetworkOk() &&               │    │
│  │              report.getTaskCapacityOk() &&                        │    │
│  │              isResourcesOk(report.getResourceInfo());            │    │
│  │          report.setHealthy(healthy);                            │    │
│  │                                                                   │    │
│  │          return report;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean checkNetwork(ExecutorNode node) {            │    │
│  │          return executorClient.healthCheck(node);                 │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private ResourceInfo checkResources(ExecutorNode node) {     │    │
│  │          // TODO: 调用节点 API 获取资源信息                       │    │
│  │          ResourceInfo info = new ResourceInfo();                 │    │
│  │          info.setCpuUsage(0.5);  // 示例值                       │    │
│  │          info.setMemoryUsage(0.6);                              │    │
│  │          return info;                                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean checkTaskCapacity(ExecutorNode node) {        │    │
│  │          double usage = (double) node.getCurrentLoad() /         │    │
│  │              node.getConcurrency();                              │    │
│  │          return usage < 0.9;  // 负载超过 90% 认为不健康        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean isResourcesOk(ResourceInfo info) {            │    │
│  │          return info.getCpuUsage() < 0.9 &&                      │    │
│  │              info.getMemoryUsage() < 0.9;                        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public static class HealthReport implements Serializable {       │    │
│  │      private String nodeId;                                       │    │
│  │      private long timestamp;                                      │    │
│  │      private boolean healthy;                                     │    │
│  │      private boolean networkOk;                                   │    │
│  │      private boolean taskCapacityOk;                             │    │
│  │      private ResourceInfo resourceInfo;                           │    │
│  │      private String message;                                      │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public static class ResourceInfo implements Serializable {        │    │
│  │      private double cpuUsage;                                     │    │
│  │      private double memoryUsage;                                  │    │
│  │      private double diskUsage;                                    │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 十、API 接口

### 9.1 执行器管理接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行器管理 API                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutorController 接口定义                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/executors")                            │    │
│  │  @Api(tags = "执行器管理")                                      │    │
│  │  public class ExecutorController {                               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutorNodeRegistry nodeRegistry;                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private HealthChecker healthChecker;                        │    │
│  │                                                                   │    │
│  │      @PostMapping("/register")                                   │    │
│  │      @ApiOperation("注册执行节点")                               │    │
│  │      public Result<Void> register(                               │    │
│  │          @RequestBody ExecutorNode node) {                       │    │
│  │          nodeRegistry.register(node);                            │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/unregister")                                 │    │
│  │      @ApiOperation("注销执行节点")                               │    │
│  │      public Result<Void> unregister(                             │    │
│  │          @RequestParam String nodeId) {                          │    │
│  │          nodeRegistry.unregister(nodeId);                         │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping                                                 │    │
│  │      @ApiOperation("获取执行节点列表")                           │    │
│  │      public Result<List<ExecutorNode>> listNodes(                │    │
│  │          @RequestParam(required = false) String status) {       │    │
│  │          List<ExecutorNode> nodes = status == null ?            │    │
│  │              nodeRegistry.getActiveNodes() :                      │    │
│  │              getNodesByStatus(status);                           │    │
│  │          return Result.success(nodes);                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{nodeId}")                                    │    │
│  │      @ApiOperation("获取节点详情")                               │    │
│  │      public Result<ExecutorNode> getNode(                        │    │
│  │          @PathVariable String nodeId) {                          │    │
│  │          ExecutorNode node = nodeRegistry.getNode(nodeId);       │    │
│  │          return Result.success(node);                             │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{nodeId}/health")                             │    │
│  │      @ApiOperation("健康检查")                                   │    │
│  │      public Result<HealthChecker.HealthReport> checkHealth(      │    │
│  │          @PathVariable String nodeId) {                          │    │
│  │          ExecutorNode node = nodeRegistry.getNode(nodeId);       │    │
│  │          if (node == null) {                                     │    │
│  │              return Result.error(ResultCode.NOT_FOUND);          │    │
│  │          }                                                       │    │
│  │          HealthChecker.HealthReport report =                      │    │
│  │              healthChecker.deepCheck(node);                      │    │
│  │          return Result.success(report);                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{nodeId}/heartbeat")                        │    │
│  │      @ApiOperation("心跳")                                       │    │
│  │      public Result<Void> heartbeat(@PathVariable String nodeId) { │    │
│  │          nodeRegistry.heartbeat(nodeId);                         │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 十一、配置说明

### 10.1 YAML 配置

```yaml
MeowFlow:
  executor:
    registry:
      heartbeat-timeout: 90  # 心跳超时时间（秒）
      health-check-interval: 60  # 健康检查间隔（秒）
    dispatch:
      max-retry: 3  # 最大重试次数
      retry-delay: 1000  # 重试延迟（毫秒）
    load-balancer:
      type: least-connection  # 负载均衡策略
```

---

## 十二、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-07-10 | 初始版本 |
| v1.1 | 2026-07-10 | 新增第六章：RabbitMQ 延时队列定时任务设计 |

---

**文档版本：v1.1**
**最后更新：2026-07-10**
