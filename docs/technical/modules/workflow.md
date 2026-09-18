# MeowFlow Workflow - 工作流服务模块设计文档

> 本文档详细描述喵流平台的工作流服务模块（MeowFlow-workflow）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MeowFlow-workflow 模块定位                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-workflow 是整个平台的核心业务模块，负责：                          │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 工作流管理                                                  │   │
│  │      • 工作流 CRUD 操作                                         │   │
│  │      • 版本管理                                                │   │
│  │      • 导入导出                                                │   │
│  │                                                                   │   │
│  │   2. 执行引擎（参考 Ragent Pipeline 编排）                       │   │
│  │      • DAG 拓扑排序                                            │   │
│  │      • 节点调度                                                │   │
│  │      • 上下文管理                                              │   │
│  │                                                                   │   │
│  │   3. 节点执行层（参考 Ragent Infra-AI）                          │   │
│  │      • NodeRegistry 节点注册中心                                │   │
│  │      • 各类型节点执行器                                         │   │
│  │                                                                   │   │
│  │   4. 触发器管理                                                  │   │
│  │      • 手动触发                                                │   │
│  │      • Webhook 触发                                            │   │
│  │      • 定时触发                                                │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  核心位置：工作流模块是连接业务层和执行层的桥梁                            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 节点可插拔（参考 Ragent NodeRegistry）                               │
│  ───────────────────────────────────────────────────────────────────────│
│  • 新增节点类型只需实现 NodeExecutor 接口                                │
│  • 自动注册到注册中心，零配置                                          │
│  • 节点之间无依赖，通过上下文传递数据                                    │
│                                                                          │
│  2. 执行流程标准化                                                      │
│  ───────────────────────────────────────────────────────────────────────│
│  • DAG 拓扑排序确保执行顺序正确                                        │
│  • 执行上下文贯穿整个流程                                              │
│  • 统一的异常处理和重试机制                                            │
│                                                                          │
│  3. 执行过程可追溯                                                      │
│  ───────────────────────────────────────────────────────────────────────│
│  • 完整记录每个节点的输入输出                                          │
│  • 支持执行回放和调试                                                  │
│  • 链路追踪 Trace ID 贯穿始终                                          │
│                                                                          │
│  4. 分层解耦（参考 Ragent 三层架构）                                    │
│  ───────────────────────────────────────────────────────────────────────│
│  • Controller 层：接收请求，参数校验                                    │
│  • Service 层：业务逻辑，事务管理                                       │
│  • Engine 层：执行引擎，流程编排                                        │
│  • Executor 层：节点执行，具体实现                                      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
MeowFlow-workflow/
├── pom.xml
└── src/main/java/com/MeowFlow/workflow/
    │
    ├── controller/                      # 控制器层
    │   ├── WorkflowController.java     # 工作流管理
    │   └── ExecutionController.java    # 执行管理
    │
    ├── service/                         # 业务逻辑层
    │   ├── WorkflowService.java       # 工作流服务
    │   ├── WorkflowVersionService.java # 版本管理服务
    │   └── WorkflowImportExportService.java  # 导入导出服务
    │
    ├── engine/                         # 执行引擎（核心）
    │   ├── WorkflowEngine.java        # 工作流引擎
    │   ├── DAGSorter.java             # DAG 拓扑排序
    │   ├── ExecutionContext.java      # 执行上下文
    │   │
    │   ├── executor/                  # 节点执行器
    │   │   ├── NodeExecutor.java      # 执行器接口
    │   │   ├── AbstractNodeExecutor.java  # 执行器抽象基类
    │   │   ├── NodeRegistry.java     # 节点注册中心
    │   │   │
    │   │   ├── trigger/              # 触发器节点
    │   │   │   ├── TriggerExecutor.java
    │   │   │   ├── ManualTriggerExecutor.java
    │   │   │   ├── WebhookTriggerExecutor.java
    │   │   │   └── ScheduleTriggerExecutor.java
    │   │   │
    │   │   ├── ai/                   # AI 节点
    │   │   │   ├── LLMExecutor.java
    │   │   │   ├── LLMClassifyExecutor.java
    │   │   │   ├── LLMExtractExecutor.java
    │   │   │   └── LLMSummarizeExecutor.java
    │   │   │
    │   │   ├── flow/                 # 流程控制节点
    │   │   │   ├── ConditionExecutor.java
    │   │   │   ├── LoopExecutor.java
    │   │   │   ├── ParallelExecutor.java
    │   │   │   └── WaitExecutor.java
    │   │   │
    │   │   ├── tool/                 # 工具节点
    │   │   │   ├── HTTPExecutor.java
    │   │   │   ├── DatabaseExecutor.java
    │   │   │   ├── CodeExecutor.java
    │   │   │   ├── VariableExecutor.java
    │   │   │   └── TemplateExecutor.java
    │   │   │
    │   │   ├── search/              # 知识库检索节点
    │   │   │   └── KnowledgeSearchExecutor.java
    │   │   │
    │   │   ├── notify/              # 通知节点
    │   │   │   ├── DingtalkNotifyExecutor.java
    │   │   │   ├── EmailNotifyExecutor.java
    │   │   │   ├── SMSNotifyExecutor.java
    │   │   │   └── WxWorkNotifyExecutor.java
    │   │   │
    │   │   └── end/                 # 结束节点
    │   │       └── EndExecutor.java
    │   │
    │   └── result/                   # 执行结果
    │       ├── NodeResult.java
    │       └── ExecutionResult.java
    │
    ├── repository/                     # 数据访问层
    │   ├── WorkflowRepository.java
    │   ├── WorkflowVersionRepository.java
    │   ├── ExecutionRepository.java
    │   └── NodeExecutionRepository.java
    │
    ├── entity/                        # 实体类
    │   ├── Workflow.java
    │   ├── WorkflowVersion.java
    │   ├── Execution.java
    │   └── NodeExecution.java
    │
    ├── dto/                          # 数据传输对象
    │   ├── WorkflowDTO.java
    │   ├── WorkflowDefinitionDTO.java
    │   ├── ExecutionRequestDTO.java
    │   └── ExecutionResultDTO.java
    │
    └── definition/                    # 工作流定义
        ├── WorkflowDefinition.java   # 工作流定义
        ├── NodeDefinition.java      # 节点定义
        └── EdgeDefinition.java      # 连接线定义
```

### 2.2 依赖关系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           模块依赖关系                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                              MeowFlow-workflow                              │
│                                    │                                      │
│                                    ▼                                      │
│                         ┌─────────────────┐                             │
│                         │ MeowFlow-common  │                             │
│                         │  通用基础设施   │                             │
│                         └────────┬────────┘                             │
│                                  │                                       │
│                                  ▼                                       │
│                         ┌─────────────────┐                             │
│                         │ MeowFlow-infra   │                             │
│                         │  AI 基础设施    │                             │
│                         └─────────────────┘                             │
│                                                                          │
│  内部依赖：                                                              │
│  • Controller → Service → Engine → Executor                            │
│  • Service → Repository                                                │
│  • Engine → Executor (通过 NodeRegistry)                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、核心组件设计

### 3.1 工作流定义

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          工作流定义结构                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowDefinition 工作流定义                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  {                                                             │    │
│  │    "name": "用户问题分类处理",                                  │    │
│  │    "description": "自动分类用户问题并路由",                      │    │
│  │    "version": 1,                                               │    │
│  │    "nodes": [                                                  │    │
│  │      {                                                         │    │
│  │        "id": "start-1",                                        │    │
│  │        "type": "start",                                        │    │
│  │        "name": "开始",                                         │    │
│  │        "position": {"x": 100, "y": 200},                       │    │
│  │        "config": {}                                            │    │
│  │      },                                                        │    │
│  │      {                                                         │    │
│  │        "id": "llm-classify-1",                                │    │
│  │        "type": "llm-classify",                                │    │
│  │        "name": "问题分类",                                     │    │
│  │        "position": {"x": 300, "y": 200},                      │    │
│  │        "config": {                                             │    │
│  │          "model": "gpt-4",                                    │    │
│  │          "prompt": "将用户问题分类：技术/售后/投诉",            │    │
│  │          "categories": ["技术", "售后", "投诉"]                 │    │
│  │        }                                                       │    │
│  │      },                                                        │    │
│  │      {                                                         │    │
│  │        "id": "condition-1",                                    │    │
│  │        "type": "condition",                                   │    │
│  │        "name": "分类判断",                                     │    │
│  │        "position": {"x": 500, "y": 200},                      │    │
│  │        "config": {                                             │    │
│  │          "conditions": [                                       │    │
│  │            {"field": "$.category", "op": "eq", "value": "技术"}│    │
│  │          ]                                                     │    │
│  │        }                                                       │    │
│  │      },                                                        │    │
│  │      {                                                         │    │
│  │        "id": "http-1",                                        │    │
│  │        "type": "http",                                        │    │
│  │        "name": "转工单系统",                                   │    │
│  │        "position": {"x": 700, "y": 100},                      │    │
│  │        "config": {                                             │    │
│  │          "url": "https://api.example.com/ticket",             │    │
│  │          "method": "POST",                                     │    │
│  │          "body": {"type": "$.category", "content": "$.content"}│  │
│  │        }                                                       │    │
│  │      },                                                        │    │
│  │      {                                                         │    │
│  │        "id": "email-1",                                       │    │
│  │        "type": "email",                                       │    │
│  │        "name": "发送邮件",                                     │    │
│  │        "position": {"x": 700, "y": 300},                      │    │
│  │        "config": {                                             │    │
│  │          "to": "support@example.com",                         │    │
│  │          "subject": "新投诉：{{content}}"                      │    │
│  │        }                                                       │    │
│  │      },                                                        │    │
│  │      {                                                         │    │
│  │        "id": "end-1",                                         │    │
│  │        "type": "end",                                         │    │
│  │        "name": "结束",                                         │    │
│  │        "position": {"x": 900, "y": 200},                      │    │
│  │        "config": {}                                            │    │
│  │      }                                                         │    │
│  │    ],                                                          │    │
│  │    "edges": [                                                  │    │
│  │      {"source": "start-1", "target": "llm-classify-1"},       │    │
│  │      {"source": "llm-classify-1", "target": "condition-1"},   │    │
│  │      {"source": "condition-1", "target": "http-1",             │    │
│  │       "label": "技术"},                                        │    │
│  │      {"source": "condition-1", "target": "email-1",           │    │
│  │       "label": "投诉"},                                        │    │
│  │      {"source": "http-1", "target": "end-1"},                 │    │
│  │      {"source": "email-1", "target": "end-1"}                  │    │
│  │    ]                                                           │    │
│  │  }                                                             │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 节点定义

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          节点定义结构                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  NodeDefinition 节点定义                                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class NodeDefinition implements Serializable {           │    │
│  │                                                                   │    │
│  │      /** 节点唯一ID */                                           │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      /** 节点类型 */                                              │    │
│  │      private String type;                                         │    │
│  │                                                                   │    │
│  │      /** 节点名称 */                                              │    │
│  │      private String name;                                         │    │
│  │                                                                   │    │
│  │      /** 节点描述 */                                              │    │
│  │      private String description;                                  │    │
│  │                                                                   │    │
│  │      /** 节点图标 */                                              │    │
│  │      private String icon;                                         │    │
│  │                                                                   │    │
│  │      /** 节点颜色 */                                              │    │
│  │      private String color;                                        │    │
│  │                                                                   │    │
│  │      /** 节点在画布上的位置 */                                    │    │
│  │      private Position position;                                  │    │
│  │                                                                   │    │
│  │      /** 节点配置（JSON）*/                                      │    │
│  │      private Map<String, Object> config;                          │    │
│  │                                                                   │    │
│  │      /** 是否为关键节点（失败会影响整体）*/                        │    │
│  │      private boolean critical = true;                            │    │
│  │                                                                   │    │
│  │      /** 最大重试次数 */                                          │    │
│  │      private int maxRetries = 0;                                 │    │
│  │                                                                   │    │
│  │      /** 超时时间（秒）*/                                         │    │
│  │      private int timeoutSeconds = 60;                            │    │
│  │                                                                   │    │
│  │      /** 输入定义 */                                              │    │
│  │      private List<PortDefinition> inputs;                        │    │
│  │                                                                   │    │
│  │      /** 输出定义 */                                              │    │
│  │      private List<PortDefinition> outputs;                       │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class Position implements Serializable {                  │    │
│  │      private double x;                                           │    │
│  │      private double y;                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class PortDefinition implements Serializable {           │    │
│  │      private String id;                                          │    │
│  │      private String name;                                        │    │
│  │      private String type;   // string/object/array/number       │    │
│  │      private boolean required;                                   │    │
│  │      private Object defaultValue;                                │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  节点分类                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  ┌─────────────┬────────────────────────────────────────────┐   │    │
│  │  │   分类      │              节点类型                      │   │    │
│  │  ├─────────────┼────────────────────────────────────────────┤   │    │
│  │  │ 触发器     │  start, webhook, schedule, manual          │   │    │
│  │  │ AI 节点    │  llm, llm-classify, llm-extract, llm-summarize│ │    │
│  │  │ 流程控制   │  condition, loop, parallel, wait, switch   │   │    │
│  │  │ 工具节点   │  http, database, code, variable, template │   │    │
│  │  │ 知识库     │  knowledge-search, knowledge-retrieval   │   │    │
│  │  │ 通知节点   │  email, dingtalk, sms, wxwork            │   │    │
│  │  │ 结束节点   │  end, return                             │   │    │
│  │  └─────────────┴────────────────────────────────────────────┘   │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、节点执行器设计

### 4.1 节点注册中心（参考 Ragent NodeRegistry）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      节点注册中心设计（参考 Ragent）                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  NodeExecutor 接口                                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public interface NodeExecutor {                                  │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取节点类型                                            │    │
│  │       */                                                        │    │
│  │      String getNodeType();                                      │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取节点分类                                            │    │
│  │       */                                                        │    │
│  │      String getCategory();                                      │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 执行节点                                                │    │
│  │       * @param context 执行上下文                                │    │
│  │       * @param node 节点定义                                    │    │
│  │       * @return 执行结果                                        │    │
│  │       */                                                        │    │
│  │      NodeResult execute(ExecutionContext context,                 │    │
│  │                        NodeDefinition node);                      │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 校验节点配置                                            │    │
│  │       */                                                        │    │
│  │      void validate(NodeDefinition node);                          │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取节点配置项定义                                       │    │
│  │       */                                                        │    │
│  │      List<NodeConfigOption> getConfigOptions();                  │    │
│  │  }                                                              │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  节点注册中心实现                                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class NodeRegistry implements InitializingBean {           │    │
│  │                                                                   │    │
│  │      private final Map<String, NodeExecutor> executors =          │    │
│  │          new ConcurrentHashMap<>();                              │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private List<NodeExecutor> nodeExecutors;                   │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public void afterPropertiesSet() {                           │    │
│  │          // 自动注册所有实现了 NodeExecutor 的 Bean              │    │
│  │          for (NodeExecutor executor : nodeExecutors) {           │    │
│  │              register(executor);                                 │    │
│  │          }                                                       │    │
│  │          log.info("节点注册中心初始化完成，共注册 {} 个节点",     │    │
│  │              executors.size());                                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public void register(NodeExecutor executor) {                │    │
│  │          String nodeType = executor.getNodeType();               │    │
│  │          if (executors.containsKey(nodeType)) {                  │    │
│  │              log.warn("节点类型 {} 已存在，将被覆盖", nodeType); │    │
│  │          }                                                       │    │
│  │          executors.put(nodeType, executor);                     │    │
│  │          log.debug("注册节点: {} -> {}", nodeType,               │    │
│  │              executor.getClass().getSimpleName());              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public NodeExecutor get(String nodeType) {                   │    │
│  │          NodeExecutor executor = executors.get(nodeType);         │    │
│  │          if (executor == null) {                                 │    │
│  │              throw new NodeNotFoundException(nodeType);          │    │
│  │          }                                                       │    │
│  │          return executor;                                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public boolean contains(String nodeType) {                   │    │
│  │          return executors.containsKey(nodeType);                 │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public Map<String, NodeExecutor> getAll() {                  │    │
│  │          return Collections.unmodifiableMap(executors);          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public List<NodeExecutor> getByCategory(String category) {   │    │
│  │          return executors.values().stream()                      │    │
│  │              .filter(e -> category.equals(e.getCategory()))      │    │
│  │              .collect(Collectors.toList());                      │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 节点执行器基类

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      节点执行器抽象基类                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AbstractNodeExecutor 抽象基类                                   │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public abstract class AbstractNodeExecutor                       │    │
│  │      implements NodeExecutor {                                   │    │
│  │                                                                   │    │
│  │      protected final Log log = LogFactory.getLog(getClass());  │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      protected NodeRegistry nodeRegistry;                        │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      protected ObjectMapper objectMapper;                        │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public NodeResult execute(ExecutionContext context,          │    │
│  │                              NodeDefinition node) {               │    │
│  │                                                                   │    │
│  │          // 1. 前置处理                                          │    │
│  │          beforeExecute(context, node);                           │    │
│  │                                                                   │    │
│  │          // 2. 执行节点                                          │    │
│  │          NodeResult result;                                      │    │
│  │          try {                                                   │    │
│  │              result = doExecute(context, node);                   │    │
│  │              result.setSuccess(true);                            │    │
│  │          } catch (Exception e) {                                 │    │
│  │              log.error("节点执行失败: {}", node.getId(), e);      │    │
│  │              result = NodeResult.fail(e.getMessage());           │    │
│  │              result.setErrorStack(ExceptionUtils.getStackTrace(e));│   │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 3. 后置处理                                          │    │
│  │          afterExecute(context, node, result);                    │    │
│  │                                                                   │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 前置处理（可重写）                                        │    │
│  │       */                                                        │    │
│  │      protected void beforeExecute(ExecutionContext context,     │    │
│  │                                  NodeDefinition node) {           │    │
│  │          // 默认实现为空                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 核心执行逻辑（子类必须实现）                               │    │
│  │       */                                                        │    │
│  │      protected abstract NodeResult doExecute(                     │    │
│  │          ExecutionContext context, NodeDefinition node);          │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 后置处理（可重写）                                        │    │
│  │       */                                                        │    │
│  │      protected void afterExecute(ExecutionContext context,       │    │
│  │                                  NodeDefinition node,             │    │
│  │                                  NodeResult result) {             │    │
│  │          // 默认实现为空                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 从上下文获取输入值                                        │    │
│  │       */                                                        │    │
│  │      protected <T> T getInput(ExecutionContext context,           │    │
│  │                            String key, Class<T> clazz) {          │    │
│  │          Object value = context.getVariable(key);                │    │
│  │          if (value == null) {                                   │    │
│  │              return null;                                        │    │
│  │          }                                                       │    │
│  │          return objectMapper.convertValue(value, clazz);          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 设置输出值                                                │    │
│  │       */                                                        │    │
│  │      protected void setOutput(ExecutionContext context,           │    │
│  │                             String key, Object value) {           │    │
│  │          context.setVariable(key, value);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 解析变量引用                                              │    │
│  │       * 支持格式：$.variableName 或 $.nodeId.output               │    │
│  │       */                                                        │    │
│  │      protected Object resolveVariable(ExecutionContext context,   │    │
│  │                                   Object expression) {            │    │
│  │          if (expression == null) {                              │    │
│  │              return null;                                       │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          String str = expression.toString();                     │    │
│  │          if (!str.startsWith("$.")) {                           │    │
│  │              return expression;                                  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          String path = str.substring(2);                        │    │
│  │          return JsonPath.read(context.getAllVariables(), path);  │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.3 各类节点执行器

#### 4.3.1 LLM 节点执行器

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      LLM 节点执行器                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  LLMExecutor 实现                                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class LLMExecutor extends AbstractNodeExecutor {          │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getNodeType() {                               │    │
│  │          return "llm";                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getCategory() {                               │    │
│  │          return "ai";                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      protected NodeResult doExecute(ExecutionContext context,    │    │
│  │                                      NodeDefinition node) {       │    │
│  │                                                                   │    │
│  │          // 1. 获取配置                                           │    │
│  │          Map<String, Object> config = node.getConfig();          │    │
│  │          String model = (String) config.get("model");           │    │
│  │          String prompt = (String) resolveVariable(context,       │    │
│  │              config.get("prompt"));                              │    │
│  │          String inputVar = (String) config.get("inputVariable");│    │
│  │                                                                   │    │
│  │          // 2. 获取输入                                           │    │
│  │          String input = "";                                     │    │
│  │          if (StringUtils.isNotBlank(inputVar)) {                │    │
│  │              input = (String) resolveVariable(context,          │    │
│  │                  config.get("inputVariable"));                  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 3. 调用 LLM（通过 ModelRouter）                       │    │
│  │          String response = modelRouter.chat(                    │    │
│  │              model, prompt, input                               │    │
│  │          );                                                      │    │
│  │                                                                   │    │
│  │          // 4. 设置输出                                           │    │
│  │          setOutput(context, node.getId() + ".output", response);│    │
│  │          setOutput(context, "llm.output", response);             │    │
│  │                                                                   │    │
│  │          NodeResult result = NodeResult.success();               │    │
│  │          result.setOutput(Collections.singletonMap("response",    │    │
│  │              response));                                         │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public List<NodeConfigOption> getConfigOptions() {          │    │
│  │          return Arrays.asList(                                    │    │
│  │              NodeConfigOption.builder()                          │    │
│  │                  .key("model").name("模型").type("model").build(),│   │
│  │              NodeConfigOption.builder()                          │    │
│  │                  .key("prompt").name("系统提示词").type("textarea").build(),│  │
│  │              NodeConfigOption.builder()                          │    │
│  │                  .key("inputVariable").name("输入变量").         │    │
│  │                      .type("variable").build()                   │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  LLMClassifyExecutor 分类节点                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class LLMClassifyExecutor extends AbstractNodeExecutor { │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getNodeType() {                               │    │
│  │          return "llm-classify";                                   │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      protected NodeResult doExecute(ExecutionContext context,    │    │
│  │                                      NodeDefinition node) {       │    │
│  │                                                                   │    │
│  │          Map<String, Object> config = node.getConfig();          │    │
│  │          String input = (String) resolveVariable(context,         │    │
│  │              config.get("inputVariable"));                       │    │
│  │          List<String> categories = (List<String>) config         │    │
│  │              .get("categories");                                  │    │
│  │                                                                   │    │
│  │          // 调用分类                                              │    │
│  │          String category = modelRouter.classify(                 │    │
│  │              input, categories                                   │    │
│  │          );                                                      │    │
│  │                                                                   │    │
│  │          setOutput(context, "category", category);               │    │
│  │          setOutput(context, node.getId() + ".category", category);│  │
│  │                                                                   │    │
│  │          NodeResult result = NodeResult.success();               │    │
│  │          result.setOutput(Collections.singletonMap("category",   │    │
│  │              category));                                          │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 4.3.2 条件分支节点执行器

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      条件分支节点执行器                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ConditionExecutor 实现                                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class ConditionExecutor extends AbstractNodeExecutor {    │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getNodeType() {                               │    │
│  │          return "condition";                                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getCategory() {                               │    │
│  │          return "flow";                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      protected NodeResult doExecute(ExecutionContext context,    │    │
│  │                                      NodeDefinition node) {       │    │
│  │                                                                   │    │
│  │          Map<String, Object> config = node.getConfig();          │    │
│  │          List<Map<String, Object>> conditions =                 │    │
│  │              (List<Map<String, Object>>) config.get("conditions");│  │
│  │                                                                   │    │
│  │          // 遍历条件，找到第一个满足的                             │    │
│  │          for (int i = 0; i < conditions.size(); i++) {            │    │
│  │              Map<String, Object> condition = conditions.get(i);  │    │
│  │              if (evaluateCondition(context, condition)) {        │    │
│  │                  setOutput(context, "matchedBranch", i);          │    │
│  │                  setOutput(context, "branchIndex", i);           │    │
│  │                                                                   │    │
│  │                  NodeResult result = NodeResult.success();      │    │
│  │                  result.setOutput(Collections.singletonMap(      │    │
│  │                      "matchedBranch", i));                       │    │
│  │                  return result;                                  │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 没有匹配的条件                                        │    │
│  │          setOutput(context, "matchedBranch", -1);                │    │
│  │          return NodeResult.success();                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean evaluateCondition(ExecutionContext context,  │    │
│  │                                  Map<String, Object> condition) { │    │
│  │          String field = (String) condition.get("field");         │    │
│  │          String op = (String) condition.get("op");               │    │
│  │          Object value = condition.get("value");                  │    │
│  │                                                                   │    │
│  │          Object actual = resolveVariable(context, field);         │    │
│  │          value = resolveVariable(context, value);                │    │
│  │                                                                   │    │
│  │          switch (op) {                                           │    │
│  │              case "eq":   return Objects.equals(actual, value);  │    │
│  │              case "ne":   return !Objects.equals(actual, value);│    │
│  │              case "gt":   return compare(actual, value) > 0;     │    │
│  │              case "gte":  return compare(actual, value) >= 0;    │    │
│  │              case "lt":   return compare(actual, value) < 0;    │    │
│  │              case "lte":  return compare(actual, value) <= 0;    │    │
│  │              case "contains":                                    │    │
│  │                  return actual != null && actual.toString()     │    │
│  │                      .contains(value.toString());                │    │
│  │              case "startsWith":                                  │    │
│  │                  return actual != null && actual.toString()     │    │
│  │                      .startsWith(value.toString());              │    │
│  │              case "endsWith":                                    │    │
│  │                  return actual != null && actual.toString()     │    │
│  │                      .endsWith(value.toString());                │    │
│  │              case "isNull":  return actual == null;             │    │
│  │              case "isNotNull": return actual != null;           │    │
│  │              case "isEmpty":                                     │    │
│  │                  return actual == null ||                       │    │
│  │                      (actual instanceof String &&                │    │
│  │                          ((String) actual).isEmpty()) ||        │    │
│  │                      (actual instanceof Collection &&            │    │
│  │                          ((Collection) actual).isEmpty());      │    │
│  │              default: return false;                            │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private int compare(Object a, Object b) {                    │    │
│  │          if (a instanceof Number && b instanceof Number) {      │    │
│  │              return Double.compare(                              │    │
│  │                  ((Number) a).doubleValue(),                     │    │
│  │                  ((Number) b).doubleValue()                     │    │
│  │              );                                                  │    │
│  │          }                                                       │    │
│  │          return String.valueOf(a).compareTo(String.valueOf(b)); │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 4.3.3 HTTP 请求节点执行器

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      HTTP 请求节点执行器                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  HTTPExecutor 实现                                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Component                                                     │    │
│  │  public class HTTPExecutor extends AbstractNodeExecutor {        │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private RestTemplate restTemplate;                          │    │
│  │                                                                   │    │
│  │      @Bean("httpExecutorPool")                                  │    │
│  │      public Executor httpExecutorPool() {                         │    │
│  │          return TtlExecutors.getTtlExecutor(                      │    │
│  │              new ThreadPoolExecutor(50, 100, 60L, TimeUnit.SECONDS,│  │
│  │                  new LinkedBlockingQueue<>(5000)));              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      public String getNodeType() {                               │    │
│  │          return "http";                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  public String getCategory() {                                       │    │
│  │          return "tool";                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                   │    │
│  │      protected NodeResult doExecute(ExecutionContext context,    │    │
│  │                                      NodeDefinition node) {       │    │
│  │                                                                   │    │
│  │          Map<String, Object> config = node.getConfig();          │    │
│  │                                                                   │    │
│  │          // 1. 解析配置                                           │    │
│  │          String url = (String) resolveVariable(context,           │    │
│  │              config.get("url"));                                 │    │
│  │          String method = (String) config.getOrDefault(          │    │
│  │              "method", "GET");                                   │    │
│  │          Map<String, Object> headers = (Map<String, Object>)     │    │
│  │              config.get("headers");                              │    │
│  │          Object body = resolveVariable(context,                  │    │
│  │              config.get("body"));                                │    │
│  │          int timeout = (int) config.getOrDefault("timeout", 30); │    │
│  │                                                                   │    │
│  │          // 2. 构建请求                                           │    │
│  │          HttpHeaders httpHeaders = new HttpHeaders();           │    │
│  │          if (headers != null) {                                  │    │
│  │              headers.forEach((k, v) ->                           │    │
│  │                  httpHeaders.add(k, String.valueOf(              │    │
│  │                      resolveVariable(context, v))));             │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          HttpEntity<Object> entity = new HttpEntity<>(body,      │    │
│  │              httpHeaders);                                       │    │
│  │                                                                   │    │
│  │          // 3. 发送请求                                           │    │
│  │          long startTime = System.currentTimeMillis();            │    │
│  │          ResponseEntity<String> response;                        │    │
│  │          try {                                                   │    │
│  │              response = restTemplate.exchange(                   │    │
│  │                  url,                                            │    │
│  │                  HttpMethod.valueOf(method.toUpperCase()),       │    │
│  │                  entity,                                         │    │
│  │                  String.class                                    │    │
│  │              );                                                  │    │
│  │          } catch (RestClientException e) {                        │    │
│  │              return NodeResult.fail("HTTP 请求失败: " +          │    │
│  │                  e.getMessage());                                │    │
│  │          }                                                       │    │
│  │          long duration = System.currentTimeMillis() - startTime;│    │
│  │                                                                   │    │
│  │          // 4. 解析响应                                           │    │
│  │          Map<String, Object> output = new HashMap<>();          │    │
│  │          output.put("statusCode", response.getStatusCode()       │    │
│  │              .value());                                         │    │
│  │          output.put("body", response.getBody());                 │    │
│  │          output.put("headers", response.getHeaders());           │    │
│  │          output.put("duration", duration);                       │    │
│  │                                                                   │    │
│  │          setOutput(context, node.getId() + ".output", output);  │    │
│  │          setOutput(context, "http.output", output);             │    │
│  │                                                                   │    │
│  │          NodeResult result = NodeResult.success();               │    │
│  │          result.setOutput(output);                              │    │
│  │          return result;                                         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、执行引擎设计

### 5.1 执行引擎核心

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      执行引擎设计（参考 Ragent Pipeline）                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowEngine 工作流引擎                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class WorkflowEngine {                                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private NodeRegistry nodeRegistry;                          │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private WorkflowRepository workflowRepository;               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutionRepository executionRepository;             │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private UserContextHolder userContextHolder;                 │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 同步执行工作流                                            │    │
│  │       */                                                         │    │
│  │      public ExecutionResult execute(String workflowId,            │    │
│  │                                   Map<String, Object> input) {    │    │
│  │                                                                   │    │
│  │          // 1. 加载工作流                                        │    │
│  │          Workflow workflow = workflowRepository.findById(workflowId);│  │
│  │          if (workflow == null) {                                  │    │
│  │              throw new WorkflowNotFoundException(workflowId);    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          WorkflowDefinition definition = parseDefinition(        │    │
│  │              workflow.getDefinition());                          │    │
│  │                                                                   │    │
│  │          // 2. 创建执行记录                                      │    │
│  │          Execution execution = createExecution(                  │    │
│  │              workflow, input, "manual");                         │    │
│  │                                                                   │    │
│  │          // 3. 执行工作流                                        │    │
│  │          try {                                                   │    │
│  │              ExecutionContext context = ExecutionContext.builder()│  │
│  │                  .workflowId(workflowId)                         │    │
│  │                  .executionId(execution.getId())                 │    │
│  │                  .input(input)                                   │    │
│  │                  .variables(new HashMap<>(input))                │    │
│  │                  .traceId(TraceContext.get().getTraceId())      │    │
│  │                  .userId(userContextHolder.getUserId())          │    │
│  │                  .build();                                        │    │
│  │                                                                   │    │
│  │              List<Node> executionOrder = dagSort(                 │    │
│  │                  definition.getNodes(),                           │    │
│  │                  definition.getEdges()                           │    │
│  │              );                                                  │    │
│  │                                                                   │    │
│  │              executeNodes(context, executionOrder, definition);  │    │
│  │                                                                   │    │
│  │              // 4. 更新执行记录                                  │    │
│  │              execution.setStatus("success");                     │    │
│  │              execution.setOutputData(context.getOutput());      │    │
│  │              executionRepository.save(execution);                │    │
│  │                                                                   │    │
│  │              return ExecutionResult.success(                     │    │
│  │                  context.getOutput());                            │    │
│  │          } catch (Exception e) {                                 │    │
│  │              execution.setStatus("failed");                      │    │
│  │              execution.setErrorMessage(e.getMessage());          │    │
│  │              executionRepository.save(execution);                │    │
│  │              throw e;                                            │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 异步执行工作流                                            │    │
│  │       */                                                         │    │
│  │      @Async("workflowExecutorPool")                               │    │
│  │      public void executeAsync(String workflowId,                  │    │
│  │                               Map<String, Object> input) {         │    │
│  │          try {                                                   │    │
│  │              execute(workflowId, input);                         │    │
│  │          } catch (Exception e) {                                 │    │
│  │              log.error("异步执行工作流失败: {}", workflowId, e); │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 执行节点列表                                              │    │
│  │       */                                                         │    │
│  │      private void executeNodes(ExecutionContext context,         │    │
│  │                                List<Node> nodes,                  │    │
│  │                                WorkflowDefinition definition) {    │    │
│  │                                                                   │    │
│  │          Map<String, Node> nodeMap = nodes.stream()             │    │
│  │              .collect(Collectors.toMap(Node::getId, n -> n));   │    │
│  │                                                                   │    │
│  │          Map<String, List<String>> adjacency = buildAdjacency(    │    │
│  │              nodes, definition.getEdges());                       │    │
│  │                                                                   │    │
│  │          for (Node node : nodes) {                               │    │
│  │              // 检查是否为开始节点                                │    │
│  │              if (!"start".equals(node.getType()) &&             │    │
│  │                  !canExecute(node, context, adjacency)) {        │    │
│  │                  continue;                                       │    │
│  │              }                                                   │    │
│  │                                                                   │    │
│  │              NodeExecutor executor = nodeRegistry.get(           │    │
│  │                  node.getType());                               │    │
│  │                                                                   │    │
│  │              // 记录开始                                          │    │
│  │              NodeExecution nodeExec = createNodeExecution(       │    │
│  │                  context.getExecutionId(), node);               │    │
│  │                                                                   │    │
│  │              try {                                               │    │
│  │                  NodeResult result = executor.execute(           │    │
│  │                      context, node);                              │    │
│  │                                                                   │    │
│  │                  nodeExec.setStatus(result.isSuccess() ?         │    │
│  │                      "success" : "failed");                       │    │
│  │                  nodeExec.setOutputData(result.getOutput());     │    │
│  │                                                                   │    │
│  │                  if (!result.isSuccess() && node.isCritical()) { │    │
│  │                      throw new NodeExecuteException(              │    │
│  │                          node.getId(), node.getType(),          │    │
│  │                          result.getErrorMessage());               │    │
│  │                  }                                               │    │
│  │              } catch (Exception e) {                             │    │
│  │                  nodeExec.setStatus("failed");                   │    │
│  │                  nodeExec.setErrorMessage(e.getMessage());      │    │
│  │                                                                   │    │
│  │                  if (node.isCritical()) {                        │    │
│  │                      throw new WorkflowExecuteException(          │    │
│  │                          context.getWorkflowId(), e.getMessage());│  │
│  │                  }                                               │    │
│  │              } finally {                                         │    │
│  │                  nodeExecutionRepository.save(nodeExec);        │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 检查节点是否可以执行（依赖节点是否已完成）                  │    │
│  │       */                                                         │    │
│  │      private boolean canExecute(Node node,                       │    │
│  │                                 ExecutionContext context,         │    │
│  │                                 Map<String, List<String>> adj) {  │    │
│  │          List<String> targets = adj.get(node.getId());          │    │
│  │          if (targets == null || targets.isEmpty()) {            │    │
│  │              return true;                                        │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 获取所有依赖当前节点的节点                            │    │
│  │          // 如果有任何一个依赖节点失败且是关键节点，返回 false   │    │
│  │          for (String source : targets) {                        │    │
│  │              String status = context.getNodeStatus(source);     │    │
│  │              if ("failed".equals(status)) {                     │    │
│  │                  Node n = context.getNode(source);             │    │
│  │                  if (n != null && n.isCritical()) {            │    │
│  │                      return false;                              │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │          return true;                                            │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 DAG 拓扑排序

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      DAG 拓扑排序（Kahn 算法）                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  DAGSorter 实现                                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class DAGSorter {                                        │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 拓扑排序                                                 │    │
│  │       * @param nodes 节点列表                                    │    │
│  │       * @param edges 边列表                                      │    │
│  │       * @return 拓扑排序后的节点列表                             │    │
│  │       */                                                         │    │
│  │      public List<Node> sort(List<Node> nodes, List<Edge> edges) {│    │
│  │          // 1. 构建入度表                                        │    │
│  │          Map<String, Integer> inDegree = new HashMap<>();        │    │
│  │          for (Node node : nodes) {                               │    │
│  │              inDegree.put(node.getId(), 0);                      │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 2. 构建邻接表                                        │    │
│  │          Map<String, List<String>> adjacency = new HashMap<>();  │    │
│  │          for (Node node : nodes) {                              │    │
│  │              adjacency.put(node.getId(), new ArrayList<>());     │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          for (Edge edge : edges) {                               │    │
│  │              // source -> target 表示 source 执行完后执行 target  │    │
│  │              adjacency.computeIfAbsent(edge.getSource(),         │    │
│  │                  k -> new ArrayList<>()).add(edge.getTarget()); │    │
│  │                                                                   │    │
│  │              // target 的入度加一                                 │    │
│  │              inDegree.merge(edge.getTarget(), 1, Integer::sum);  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 3. BFS 获取拓扑序                                    │    │
│  │          Queue<String> queue = new LinkedList<>();               │    │
│  │          for (Map.Entry<String, Integer> entry :                 │    │
│  │                   inDegree.entrySet()) {                        │    │
│  │              if (entry.getValue() == 0) {                       │    │
│  │                  queue.offer(entry.getKey());                    │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          List<Node> result = new ArrayList<>();                  │    │
│  │          Map<String, Node> nodeMap = nodes.stream()             │    │
│  │              .collect(Collectors.toMap(Node::getId, n -> n));   │    │
│  │                                                                   │    │
│  │          while (!queue.isEmpty()) {                              │    │
│  │              String nodeId = queue.poll();                        │    │
│  │              result.add(nodeMap.get(nodeId));                    │    │
│  │                                                                   │    │
│  │              List<String> neighbors = adjacency.get(nodeId);    │    │
│  │              if (neighbors != null) {                           │    │
│  │                  for (String neighbor : neighbors) {            │    │
│  │                      int newDegree = inDegree.get(neighbor) - 1;│    │
│  │                      inDegree.put(neighbor, newDegree);         │    │
│  │                      if (newDegree == 0) {                       │    │
│  │                          queue.offer(neighbor);                 │    │
│  │                      }                                           │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 4. 检测环                                            │    │
│  │          if (result.size() != nodes.size()) {                   │    │
│  │              throw new CycleDetectedException("工作流存在循环依赖");│  │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return result;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 并行节点检测（支持并行执行的节点）                        │    │
│  │       */                                                         │    │
│  │      public List<List<Node>> detectParallelGroups(               │    │
│  │          List<Node> nodes, List<Edge> edges) {                  │    │
│  │          // 检测可以并行执行的节点组                              │    │
│  │          // 返回分层的结果，每层包含可并行执行的节点              │    │
│  │          List<List<Node>> layers = new ArrayList<>();           │    │
│  │                                                                   │    │
│  │          Map<String, Integer> inDegree = new HashMap<>();        │    │
│  │          Map<String, Node> nodeMap = nodes.stream()             │    │
│  │              .collect(Collectors.toMap(Node::getId, n -> n));   │    │
│  │                                                                   │    │
│  │          for (Node node : nodes) {                               │    │
│  │              inDegree.put(node.getId(), 0);                      │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          for (Edge edge : edges) {                               │    │
│  │              inDegree.merge(edge.getTarget(), 1, Integer::sum);  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          Set<String> processed = new HashSet<>();               │    │
│  │                                                                   │    │
│  │          while (processed.size() < nodes.size()) {               │    │
│  │              List<Node> layer = new ArrayList<>();              │    │
│  │                                                                   │    │
│  │              for (Node node : nodes) {                           │    │
│  │                  if (processed.contains(node.getId())) {        │    │
│  │                      continue;                                   │    │
│  │                  }                                               │    │
│  │                                                                   │    │
│  │                  if (inDegree.get(node.getId()) == 0) {        │    │
│  │                      layer.add(node);                             │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │                                                                   │    │
│  │              if (layer.isEmpty()) {                              │    │
│  │                  throw new CycleDetectedException("检测到循环依赖");│  │
│  │              }                                                   │    │
│  │                                                                   │    │
│  │              layers.add(layer);                                  │    │
│  │                                                                   │    │
│  │              for (Node node : layer) {                          │    │
│  │                  processed.add(node.getId());                   │    │
│  │                                                                   │    │
│  │                  for (Edge edge : edges) {                      │    │
│  │                      if (edge.getSource().equals(node.getId())){│  │
│  │                          inDegree.merge(edge.getTarget(), -1,    │  │
│  │                              Integer::sum);                      │    │
│  │                      }                                           │    │
│  │                  }                                               │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return layers;                                          │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  执行示例                                                        │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  工作流：                                                        │    │
│  │                                                                   │    │
│  │       A                                                          │    │
│  │      / \                                                         │    │
│  │     B   C                                                        │    │
│  │      \ /                                                         │    │
│  │       D                                                          │    │
│  │      / \                                                         │    │
│  │     E   F                                                        │    │
│  │                                                                   │    │
│  │  拓扑排序结果：                                                  │    │
│  │  1. A                                                            │    │
│  │  2. B, C （可并行）                                              │    │
│  │  3. D                                                            │    │
│  │  4. E, F （可并行）                                              │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.3 执行上下文

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行上下文                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionContext 执行上下文                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  public class ExecutionContext {                                  │    │
│  │                                                                   │    │
│  │      private final String workflowId;                           │    │
│  │      private final String executionId;                          │    │
│  │      private final Map<String, Object> input;                   │    │
│  │      private final Map<String, Object> variables;               │    │
│  │      private final Map<String, String> nodeStatus;               │    │
│  │      private final Map<String, Node> nodes;                     │    │
│  │      private final String traceId;                               │    │
│  │      private final String userId;                                │    │
│  │      private final long startTime;                               │    │
│  │                                                                   │    │
│  │      // Builder 模式                                            │    │
│  │      public static Builder builder() {                           │    │
│  │          return new Builder();                                   │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public Object getVariable(String key) {                     │    │
│  │          return variables.get(key);                               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public void setVariable(String key, Object value) {         │    │
│  │          variables.put(key, value);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public Map<String, Object> getAllVariables() {              │    │
│  │          return Collections.unmodifiableMap(variables);          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public void setNodeStatus(String nodeId, String status) {  │    │
│  │          nodeStatus.put(nodeId, status);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public String getNodeStatus(String nodeId) {                 │    │
│  │          return nodeStatus.get(nodeId);                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public Map<String, Object> getOutput() {                    │    │
│  │          // 返回最终输出（以 output 结尾的变量）                   │    │
│  │          Map<String, Object> output = new HashMap<>();          │    │
│  │          for (Map.Entry<String, Object> entry : variables      │    │
│  │                   .entrySet()) {                                │    │
│  │              if (entry.getKey().endsWith(".output")) {          │    │
│  │                  output.put(entry.getKey(), entry.getValue());  │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │          return output;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static class Builder {                                │    │
│  │          private String workflowId;                             │    │
│  │          private String executionId;                            │    │
│  │          private Map<String, Object> input = new HashMap<>();  │    │
│  │          private Map<String, Object> variables = new HashMap<>();│  │
│  │          private Map<String, String> nodeStatus = new HashMap<>();│ │
│  │          private Map<String, Node> nodes = new HashMap<>();    │    │
│  │          private String traceId;                                │    │
│  │          private String userId;                                  │    │
│  │          private long startTime = System.currentTimeMillis();   │    │
│  │                                                                   │    │
│  │          public Builder workflowId(String workflowId) {          │    │
│  │              this.workflowId = workflowId;                       │    │
│  │              return this;                                         │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // ... 其他 setter 方法                                   │    │
│  │                                                                   │    │
│  │          public ExecutionContext build() {                       │    │
│  │              ExecutionContext ctx = new ExecutionContext();      │    │
│  │              ctx.workflowId = this.workflowId;                   │    │
│  │              ctx.executionId = this.executionId;               │    │
│  │              ctx.input = this.input;                            │    │
│  │              ctx.variables = this.variables;                    │    │
│  │              ctx.nodeStatus = this.nodeStatus;                  │    │
│  │              ctx.nodes = this.nodes;                            │    │
│  │              ctx.traceId = this.traceId;                        │    │
│  │              ctx.userId = this.userId;                          │    │
│  │              ctx.startTime = this.startTime;                    │    │
│  │              return ctx;                                         │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、执行结果

### 6.1 节点执行结果

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          节点执行结果                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  NodeResult 节点执行结果                                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class NodeResult implements Serializable {              │    │
│  │                                                                   │    │
│  │      private boolean success;                                    │    │
│  │      private String errorMessage;                                │    │
│  │      private String errorStack;                                  │    │
│  │      private Map<String, Object> output;                        │    │
│  │      private long duration;                                      │    │
│  │      private Map<String, Object> metadata;                       │    │
│  │                                                                   │    │
│  │      public static NodeResult success() {                        │    │
│  │          NodeResult result = new NodeResult();                   │    │
│  │          result.setSuccess(true);                                │    │
│  │          result.setOutput(Collections.emptyMap());              │    │
│  │          return result;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static NodeResult success(Map<String, Object> output) {│ │
│  │          NodeResult result = new NodeResult();                   │    │
│  │          result.setSuccess(true);                                │    │
│  │          result.setOutput(output);                                │    │
│  │          return result;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static NodeResult fail(String errorMessage) {        │    │
│  │          NodeResult result = new NodeResult();                   │    │
│  │          result.setSuccess(false);                               │    │
│  │          result.setErrorMessage(errorMessage);                   │    │
│  │          return result;                                           │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionResult 工作流执行结果                                   │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class ExecutionResult implements Serializable {          │    │
│  │                                                                   │    │
│  │      private boolean success;                                    │    │
│  │      private String executionId;                                │    │
│  │      private String status;          // success/failed/running  │    │
│  │      private Map<String, Object> output;                        │    │
│  │      private String errorMessage;                                │    │
│  │      private long duration;                                      │    │
│  │      private BigDecimal cost;         // 执行成本                │    │
│  │      private List<NodeExecutionDTO> nodeExecutions;             │    │
│  │      private Map<String, Object> metadata;                       │    │
│  │                                                                   │    │
│  │      public static ExecutionResult success(Map<String, Object> output) {│
│  │          ExecutionResult result = new ExecutionResult();        │    │
│  │          result.setSuccess(true);                               │    │
│  │          result.setStatus("success");                            │    │
│  │          result.setOutput(output);                               │    │
│  │          return result;                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      public static ExecutionResult fail(String errorMessage) {   │    │
│  │          ExecutionResult result = new ExecutionResult();        │    │
│  │          result.setSuccess(false);                               │    │
│  │          result.setStatus("failed");                             │    │
│  │          result.setErrorMessage(errorMessage);                   │    │
│  │          return result;                                           │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、工作流管理服务

### 7.1 工作流服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          工作流管理服务                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowService 工作流服务                                       │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  @Transactional                                                 │    │
│  │  public class WorkflowService {                                  │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private WorkflowRepository workflowRepository;               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private WorkflowVersionService versionService;             │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 创建工作流                                                │    │
│  │       */                                                         │    │
│  │      public Workflow createWorkflow(CreateWorkflowRequest request) {│ │
│  │          Workflow workflow = new Workflow();                    │    │
│  │          workflow.setName(request.getName());                   │    │
│  │          workflow.setDescription(request.getDescription());     │    │
│  │          workflow.setUserId(userContextHolder.getUserId());     │    │
│  │          workflow.setOrgId(userContextHolder.getOrgId());      │    │
│  │          workflow.setStatus("draft");                            │    │
│  │          workflow.setVersion(1);                                │    │
│  │                                                                   │    │
│  │          // 解析并保存工作流定义                                  │    │
│  │          WorkflowDefinition definition = parseDefinition(        │    │
│  │              request.getDefinition());                           │    │
│  │          workflow.setDefinition(definition);                    │    │
│  │                                                                   │    │
│  │          return workflowRepository.save(workflow);               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 更新工作流                                                │    │
│  │       */                                                         │    │
│  │      public Workflow updateWorkflow(String workflowId,           │    │
│  │                                     UpdateWorkflowRequest request) {│ │
│  │          Workflow workflow = workflowRepository.findById(workflowId);│ │
│  │          if (workflow == null) {                                 │    │
│  │              throw new WorkflowNotFoundException(workflowId);    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 检查权限                                              │    │
│  │          checkWorkflowOwner(workflow);                           │    │
│  │                                                                   │    │
│  │          // 只能更新草稿状态的工作流                               │    │
│  │          if (!"draft".equals(workflow.getStatus())) {            │    │
│  │              throw new BizException(ResultCode.WORKFLOW_NOT_DRAFT);│ │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 更新基本信息                                          │    │
│  │          workflow.setName(request.getName());                   │    │
│  │          workflow.setDescription(request.getDescription());     │    │
│  │          workflow.setCategory(request.getCategory());           │    │
│  │          workflow.setTags(request.getTags());                   │    │
│  │                                                                   │    │
│  │          // 更新定义                                              │    │
│  │          WorkflowDefinition definition = parseDefinition(       │    │
│  │              request.getDefinition());                           │    │
│  │          workflow.setDefinition(definition);                    │    │
│  │                                                                   │    │
│  │          return workflowRepository.save(workflow);               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 发布工作流                                                │    │
│  │       */                                                         │    │
│  │      public Workflow publishWorkflow(String workflowId) {         │    │
│  │          Workflow workflow = workflowRepository.findById(workflowId);│ │
│  │          if (workflow == null) {                                 │    │
│  │              throw new WorkflowNotFoundException(workflowId);    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 创建新版本                                            │    │
│  │          versionService.createVersion(workflow);                │    │
│  │                                                                   │    │
│  │          // 更新状态                                              │    │
│  │          workflow.setStatus("active");                           │    │
│  │          workflow.setPublishTime(new Date());                   │    │
│  │          workflow.setPublishUserId(userContextHolder.getUserId());│ │
│  │                                                                   │    │
│  │          return workflowRepository.save(workflow);               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 停用工作流                                                │    │
│  │       */                                                         │    │
│  │      public Workflow stopWorkflow(String workflowId) {            │    │
│  │          Workflow workflow = workflowRepository.findById(workflowId);│ │
│  │          if (workflow == null) {                                 │    │
│  │              throw new WorkflowNotFoundException(workflowId);    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          workflow.setStatus("stopped");                          │    │
│  │          return workflowRepository.save(workflow);               │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 复制工作流                                                │    │
│  │       */                                                         │    │
│  │      public Workflow copyWorkflow(String workflowId) {            │    │
│  │          Workflow source = workflowRepository.findById(workflowId);│ │
│  │          if (source == null) {                                   │    │
│  │              throw new WorkflowNotFoundException(workflowId);    │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          Workflow copy = new Workflow();                        │    │
│  │          copy.setName(source.getName() + " (副本)");            │    │
│  │          copy.setDescription(source.getDescription());           │    │
│  │          copy.setDefinition(source.getDefinition());            │    │
│  │          copy.setUserId(userContextHolder.getUserId());         │    │
│  │          copy.setOrgId(userContextHolder.getOrgId());           │    │
│  │          copy.setStatus("draft");                                 │    │
│  │          copy.setVersion(1);                                     │    │
│  │                                                                   │    │
│  │          return workflowRepository.save(copy);                   │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 分页查询工作流                                            │    │
│  │       */                                                         │    │
│  │      public Page<Workflow> listWorkflows(WorkflowQuery query) {   │    │
│  │          return workflowRepository.findByConditions(              │    │
│  │              userContextHolder.getUserId(),                      │    │
│  │              userContextHolder.getOrgId(),                       │    │
│  │              query                                             │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private WorkflowDefinition parseDefinition(Object definition) {│ │
│  │          if (definition instanceof WorkflowDefinition) {        │    │
│  │              return (WorkflowDefinition) definition;           │    │
│  │          }                                                       │    │
│  │          return objectMapper.convertValue(definition,           │    │
│  │              WorkflowDefinition.class);                         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 八、版本管理

### 8.1 版本服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          版本管理服务                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowVersionService 版本管理服务                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  @Transactional                                                 │    │
│  │  public class WorkflowVersionService {                           │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private WorkflowVersionRepository versionRepository;         │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 创建新版本                                                │    │
│  │       */                                                         │    │
│  │      public WorkflowVersion createVersion(Workflow workflow) {   │    │
│  │          // 获取当前版本号                                        │    │
│  │          int newVersion = workflow.getVersion() + 1;              │    │
│  │                                                                   │    │
│  │          // 保存当前定义为新版本                                  │    │
│  │          WorkflowVersion version = new WorkflowVersion();       │    │
│  │          version.setWorkflowId(workflow.getId());               │    │
│  │          version.setVersion(newVersion);                         │    │
│  │          version.setDefinition(workflow.getDefinition());        │    │
│  │          version.setChangeLog("版本 " + newVersion + " 发布");  │    │
│  │          version.setUserId(userContextHolder.getUserId());     │    │
│  │          version.setIsPublished(true);                          │    │
│  │                                                                   │    │
│  │          versionRepository.save(version);                         │    │
│  │                                                                   │    │
│  │          // 更新工作流版本号                                      │    │
│  │          workflow.setVersion(newVersion);                        │    │
│  │                                                                   │    │
│  │          return version;                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 获取版本列表                                              │    │
│  │       */                                                         │    │
│  │      public List<WorkflowVersion> getVersions(String workflowId) {│ │
│  │          return versionRepository.findByWorkflowIdOrderByVersionDesc(│ │
│  │              workflowId);                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                         │    │
│  │       * 回滚到指定版本                                            │    │
│  │       */                                                         │    │
│  │      public Workflow rollback(String workflowId, int version) {  │    │
│  │          WorkflowVersion targetVersion =                         │    │
│  │              versionRepository.findByWorkflowIdAndVersion(       │    │
│  │                  workflowId, version);                            │    │
│  │          if (targetVersion == null) {                            │    │
│  │              throw new VersionNotFoundException(workflowId, version);│ │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          Workflow workflow = workflowRepository.findById(workflowId);│ │
│  │          workflow.setDefinition(targetVersion.getDefinition());  │    │
│  │                                                                   │    │
│  │          // 创建新版本记录回滚操作                                │    │
│  │          createVersion(workflow);                                │    │
│  │                                                                   │    │
│  │          return workflowRepository.save(workflow);               │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 九、API 接口

### 9.1 工作流管理接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          工作流管理 API                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  WorkflowController 接口定义                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/workflows")                            │    │
│  │  @Api(tags = "工作流管理")                                        │    │
│  │  public class WorkflowController {                                │    │
│  │                                                                   │    │
│  │      @PostMapping                                                │    │
│  │      @ApiOperation("创建工作流")                                  │    │
│  │      public Result<WorkflowDTO> create(                         │    │
│  │          @RequestBody @Valid CreateWorkflowRequest request) {   │    │
│  │          Workflow workflow = workflowService.createWorkflow(request);│ │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/{id}")                                        │    │
│  │      @ApiOperation("更新工作流")                                  │    │
│  │      public Result<WorkflowDTO> update(                         │    │
│  │          @PathVariable String id,                               │    │
│  │          @RequestBody @Valid UpdateWorkflowRequest request) {   │    │
│  │          Workflow workflow = workflowService.updateWorkflow(id,  │    │
│  │              request);                                           │    │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @DeleteMapping("/{id}")                                     │    │
│  │      @ApiOperation("删除工作流")                                  │    │
│  │      public Result<Void> delete(@PathVariable String id) {       │    │
│  │          workflowService.deleteWorkflow(id);                     │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{id}")                                        │    │
│  │      @ApiOperation("获取工作流详情")                              │    │
│  │      public Result<WorkflowDTO> getById(@PathVariable String id) {│  │
│  │          Workflow workflow = workflowService.getWorkflow(id);    │    │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping                                                 │    │
│  │      @ApiOperation("分页查询工作流")                              │    │
│  │      public Result<Page<WorkflowDTO>> list(                     │    │
│  │          @ModelAttribute WorkflowQuery query) {                  │    │
│  │          Page<Workflow> page = workflowService.listWorkflows(query);│ │
│  │          return Result.success(page.map(this::toDTO));         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/publish")                               │    │
│  │      @ApiOperation("发布工作流")                                  │    │
│  │      public Result<WorkflowDTO> publish(@PathVariable String id) {│  │
│  │          Workflow workflow = workflowService.publishWorkflow(id);│  │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/stop")                                  │    │
│  │      @ApiOperation("停用工作流")                                  │    │
│  │      public Result<WorkflowDTO> stop(@PathVariable String id) {  │  │
│  │          Workflow workflow = workflowService.stopWorkflow(id);   │  │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/copy")                                  │    │
│  │      @ApiOperation("复制工作流")                                  │    │
│  │      public Result<WorkflowDTO> copy(@PathVariable String id) {  │  │
│  │          Workflow workflow = workflowService.copyWorkflow(id);   │  │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{id}/versions")                               │    │
│  │      @ApiOperation("获取版本历史")                                │    │
│  │      public Result<List<VersionDTO>> getVersions(                │    │
│  │          @PathVariable String id) {                              │    │
│  │          List<WorkflowVersion> versions =                        │    │
│  │              versionService.getVersions(id);                    │    │
│  │          return Result.success(versions.stream()                 │    │
│  │              .map(this::toVersionDTO).collect(Collectors.toList()));│ │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/rollback/{version}")                     │    │
│  │      @ApiOperation("回滚到指定版本")                              │    │
│  │      public Result<WorkflowDTO> rollback(                       │    │
│  │          @PathVariable String id,                                │    │
│  │          @PathVariable int version) {                            │    │
│  │          Workflow workflow = versionService.rollback(id, version);│ │
│  │          return Result.success(toDTO(workflow));                  │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 9.2 执行接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行 API                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionController 接口定义                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/executions")                          │    │
│  │  @Api(tags = "执行管理")                                          │    │
│  │  public class ExecutionController {                              │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private WorkflowEngine workflowEngine;                      │    │
│  │                                                                   │    │
│  │      @PostMapping("/{workflowId}/execute")                       │    │
│  │      @ApiOperation("同步执行工作流")                              │    │
│  │      public Result<ExecutionResultDTO> execute(                  │    │
│  │          @PathVariable String workflowId,                        │    │
│  │          @RequestBody(required = false)                         │    │
│  │              Map<String, Object> input) {                        │    │
│  │          ExecutionResult result = workflowEngine.execute(        │    │
│  │              workflowId, input != null ? input : new HashMap<>());│ │
│  │          return Result.success(toResultDTO(result));            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{workflowId}/execute-async")                 │    │
│  │      @ApiOperation("异步执行工作流")                              │    │
│  │      public Result<String> executeAsync(                         │    │
│  │          @PathVariable String workflowId,                       │    │
│  │          @RequestBody(required = false)                         │    │
│  │              Map<String, Object> input) {                        │    │
│  │          Execution execution = createExecution(workflowId, input);│ │
│  │          workflowEngine.executeAsync(workflowId, input);        │    │
│  │          return Result.success(execution.getId());              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{executionId}")                               │    │
│  │      @ApiOperation("获取执行详情")                                │    │
│  │      public Result<ExecutionDTO> getExecution(                    │    │
│  │          @PathVariable String executionId) {                     │    │
│  │          Execution execution = executionRepository               │    │
│  │              .findById(executionId);                            │    │
│  │          return Result.success(toExecutionDTO(execution));      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{executionId}/nodes")                         │    │
│  │      @ApiOperation("获取节点执行记录")                            │    │
│  │      public Result<List<NodeExecutionDTO>> getNodeExecutions(   │    │
│  │          @PathVariable String executionId) {                     │    │
│  │          List<NodeExecution> nodeExecs = nodeExecutionRepository │    │
│  │              .findByExecutionId(executionId);                   │    │
│  │          return Result.success(nodeExecs.stream()                │    │
│  │              .map(this::toNodeExecutionDTO)                     │    │
│  │              .collect(Collectors.toList()));                    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/workflow/{workflowId}")                       │    │
│  │      @ApiOperation("查询工作流的执行记录")                        │    │
│  │      public Result<Page<ExecutionDTO>> listByWorkflow(          │    │
│  │          @PathVariable String workflowId,                        │    │
│  │          @ModelAttribute ExecutionQuery query) {                  │    │
│  │          Page<Execution> page = executionRepository            │    │
│  │              .findByWorkflowId(workflowId, query);             │    │
│  │          return Result.success(page.map(this::toExecutionDTO)); │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{executionId}/cancel")                       │    │
│  │      @ApiOperation("取消执行")                                    │    │
│  │      public Result<Void> cancel(@PathVariable String executionId) {│ │
│  │          workflowEngine.cancelExecution(executionId);            │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{executionId}/logs")                         │    │
│  │      @ApiOperation("获取执行日志")                                │    │
│  │      public Result<Page<ExecutionLogDTO>> getLogs(               │    │
│  │          @PathVariable String executionId,                       │    │
│  │          @ModelAttribute LogQuery query) {                        │    │
│  │          Page<ExecutionLog> logs = logRepository                │    │
│  │              .findByExecutionId(executionId, query);            │    │
│  │          return Result.success(logs.map(this::toLogDTO));       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 十、配置说明

### 10.1 YAML 配置

```yaml
MeowFlow:
  workflow:
    engine:
      max-concurrent-executions: 100
      node-timeout-default: 60
      max-node-retries: 3
    version:
      max-versions-per-workflow: 50
      auto-clean-old-versions: true
      keep-recent-versions: 10
    trigger:
      webhook-secret-required: true
      schedule-enabled: true
```

---

## 十一、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-07-10 | 初始版本，参考 Ragent Pipeline 编排设计 |

---

**文档版本：v1.0**
**基于 Ragent Pipeline 编排设计理念**
**最后更新：2026-07-10**
