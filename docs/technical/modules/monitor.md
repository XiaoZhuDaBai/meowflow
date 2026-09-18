# MeowFlow Monitor - 监控服务模块设计文档

> 本文档详细描述喵流平台的监控服务模块（MeowFlow-monitor）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          MeowFlow-monitor 模块定位                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-monitor 是监控服务模块，负责：                                   │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 执行监控                                                    │   │
│  │      • 执行日志收集                                              │   │
│  │      • 执行指标统计                                              │   │
│  │      • 执行趋势分析                                              │   │
│  │                                                                   │   │
│  │   2. 告警管理                                                    │   │
│  │      • 告警规则配置                                             │   │
│  │      • 告警触发与通知                                           │   │
│  │      • 告警历史记录                                              │   │
│  │                                                                   │   │
│  │   3. 系统监控                                                    │   │
│  │      • 系统指标采集                                              │   │
│  │      • 资源使用监控                                              │   │
│  │      • 服务健康检查                                              │   │
│  │                                                                   │   │
│  │   4. 成本分析                                                    │   │
│  │      • Token 消耗统计                                           │   │
│  │      • 成本分摊                                                  │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 全链路追踪                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 从请求到执行全链路追踪                                            │
│  • 节点级别耗时分析                                                  │
│  • 异常链路定位                                                      │
│                                                                          │
│  2. 可观测性                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 日志、指标、追踪三位一体                                          │
│  • 支持多维度查询                                                    │
│  • 可视化展示                                                        │
│                                                                          │
│  3. 告警及时性                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 多渠道告警（钉钉、邮件、短信）                                    │
│  • 告警收敛防止轰炸                                                │
│  • 告警升级机制                                                      │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
MeowFlow-monitor/
├── pom.xml
└── src/main/java/com/MeowFlow/monitor/
    │
    ├── controller/                      # 控制器层
    │   ├── ExecutionLogController.java # 执行日志
    │   ├── AlertController.java       # 告警管理
    │   └── MetricsController.java    # 指标管理
    │
    ├── service/                         # 业务逻辑层
    │   ├── ExecutionLogService.java  # 执行日志服务
    │   ├── MetricsService.java      # 指标服务
    │   ├── AlertService.java       # 告警服务
    │   └── AlertNotifyService.java # 告警通知服务
    │
    ├── repository/                     # 数据访问层
    │   ├── ExecutionLogRepository.java
    │   ├── AlertRuleRepository.java
    │   └── AlertRecordRepository.java
    │
    ├── entity/                        # 实体类
    │   ├── ExecutionLog.java
    │   ├── AlertRule.java
    │   ├── AlertRecord.java
    │   └── AlertChannel.java
    │
    ├── dto/                          # 数据传输对象
    │   ├── MetricsDTO.java
    │   └── AlertDTO.java
    │
    ├── metrics/                      # 指标采集
    │   ├── MetricsCollector.java   # 指标采集器
    │   ├── SystemMetrics.java     # 系统指标
    │   └── BusinessMetrics.java   # 业务指标
    │
    └── alert/                        # 告警
        ├── AlertRuleEngine.java   # 告警规则引擎
        └── AlertNotifier.java    # 告警通知器
```

---

## 三、执行日志

### 3.1 日志实体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行日志实体                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionLog 执行日志                                            │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("wf_execution_log")                                  │    │
│  │  public class ExecutionLog implements Serializable {               │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      private String executionId;                                  │    │
│  │      private String nodeId;                                      │    │
│  │      private String nodeName;                                     │    │
│  │                                                                   │    │
│  │      private String level;           // DEBUG/INFO/WARN/ERROR   │    │
│  │      private String type;            // START/NODE/COND/TOKEN/END │    │
│  │                                                                   │    │
│  │      @Column(columnDefinition = "text")                          │    │
│  │      private String message;                                     │    │
│  │                                                                   │    │
│  │      @Column(columnDefinition = "jsonb")                        │    │
│  │      private String detail;                                      │    │
│  │                                                                   │    │
│  │      private Integer sort;                                       │    │
│  │      private LocalDateTime createTime;                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 日志服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行日志服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionLogService 执行日志服务                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class ExecutionLogService {                                │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutionLogRepository logRepository;                  │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 记录日志                                                │    │
│  │       */                                                        │    │
│  │      public void log(String executionId, String nodeId,           │    │
│  │                      String level, String type, String message,    │    │
│  │                      Map<String, Object> detail) {                │    │
│  │          ExecutionLog log = new ExecutionLog();                    │    │
│  │          log.setExecutionId(executionId);                        │    │
│  │          log.setNodeId(nodeId);                                   │    │
│  │          log.setLevel(level);                                    │    │
│  │          log.setType(type);                                     │    │
│  │          log.setMessage(message);                                │    │
│  │          log.setDetail(toJson(detail));                          │    │
│  │          log.setCreateTime(LocalDateTime.now());                  │    │
│  │                                                                   │    │
│  │          logRepository.save(log);                                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 记录节点开始                                            │    │
│  │       */                                                        │    │
│  │      public void logNodeStart(String executionId, Node node) {   │    │
│  │          Map<String, Object> detail = new HashMap<>();           │    │
│  │          detail.put("nodeType", node.getType());                 │    │
│  │          detail.put("nodeName", node.getName());                 │    │
│  │          detail.put("timestamp", System.currentTimeMillis());    │    │
│  │                                                                   │    │
│  │          log(executionId, node.getId(), "INFO", "NODE",         │    │
│  │              "节点开始执行: " + node.getName(), detail);         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 记录节点结束                                            │    │
│  │       */                                                        │    │
│  │      public void logNodeEnd(String executionId, Node node,       │    │
│  │                            boolean success, long duration) {      │    │
│  │          String level = success ? "INFO" : "ERROR";             │    │
│  │          String message = success ?                               │    │
│  │              "节点执行成功: " + node.getName() +                  │    │
│  │              " (耗时: " + duration + "ms)" :                     │    │
│  │              "节点执行失败: " + node.getName();                  │    │
│  │                                                                   │    │
│  │          Map<String, Object> detail = new HashMap<>();           │    │
│  │          detail.put("nodeType", node.getType());                 │    │
│  │          detail.put("nodeName", node.getName());                 │    │
│  │          detail.put("success", success);                          │    │
│  │          detail.put("duration", duration);                       │    │
│  │          detail.put("timestamp", System.currentTimeMillis());    │    │
│  │                                                                   │    │
│  │          log(executionId, node.getId(), level, "NODE", message, detail);│   │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 查询执行日志                                            │    │
│  │       */                                                        │    │
│  │      public List<ExecutionLog> getLogs(String executionId,       │    │
│  │                                          String level) {         │    │
│  │          if (StringUtils.isBlank(level)) {                      │    │
│  │              return logRepository.findByExecutionIdOrderByCreateTimeAsc(executionId);│  │
│  │          } else {                                                │    │
│  │              return logRepository                                │    │
│  │                  .findByExecutionIdAndLevelOrderByCreateTimeAsc(  │    │
│  │                      executionId, level);                        │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 分页查询日志                                            │    │
│  │       */                                                        │    │
│  │      public Page<ExecutionLog> getLogsPage(String executionId,  │    │
│  │                                              Pageable pageable) {   │    │
│  │          return logRepository                                    │    │
│  │              .findByExecutionIdOrderByCreateTimeDesc(            │    │
│  │                  executionId, pageable);                         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、指标服务

### 4.1 业务指标

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          业务指标设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  BusinessMetrics 业务指标                                           │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public class BusinessMetrics implements Serializable {            │    │
│  │                                                                   │    │
│  │      /** 指标时间 */                                             │    │
│  │      private long timestamp;                                       │    │
│  │                                                                   │    │
│  │      // 执行指标                                                  │    │
│  │      private long totalExecutions;        // 总执行次数           │    │
│  │      private long successExecutions;      // 成功次数            │    │
│  │      private long failedExecutions;       // 失败次数            │    │
│  │      private double successRate;          // 成功率              │    │
│  │      private double avgDuration;          // 平均耗时（ms）       │    │
│  │      private double maxDuration;          // 最大耗时             │    │
│  │      private double minDuration;          // 最小耗时             │    │
│  │                                                                   │    │
│  │      // Token 消耗                                               │    │
│  │      private long totalInputTokens;      // 总输入 Token          │    │
│  │      private long totalOutputTokens;     // 总输出 Token          │    │
│  │      private double totalCost;           // 总成本（元）         │    │
│  │                                                                   │    │
│  │      // 节点指标                                                 │    │
│  │      private Map<String, NodeMetrics> nodeMetrics;                 │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  public static class NodeMetrics implements Serializable {          │    │
│  │      private String nodeType;                                    │    │
│  │      private long count;                                          │    │
│  │      private long successCount;                                   │    │
│  │      private double avgDuration;                                 │    │
│  │      private long inputTokens;                                    │    │
│  │      private long outputTokens;                                   │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 指标服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          指标服务                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  MetricsService 指标服务                                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class MetricsService {                                     │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutionRepository executionRepository;                 │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取执行统计                                              │    │
│  │       */                                                        │    │
│  │      public ExecutionStatistics getExecutionStatistics(            │    │
│  │          String workflowId, LocalDateTime startTime,              │    │
│  │          LocalDateTime endTime) {                                │    │
│  │                                                                   │    │
│  │          // 查询执行记录                                          │    │
│  │          List<Execution> executions = executionRepository         │    │
│  │              .findByWorkflowIdAndCreateTimeBetween(              │    │
│  │                  workflowId, startTime, endTime);                 │    │
│  │                                                                   │    │
│  │          // 计算统计                                              │    │
│  │          ExecutionStatistics stats = new ExecutionStatistics();    │    │
│  │          stats.setWorkflowId(workflowId);                         │    │
│  │          stats.setTotalCount((long) executions.size());          │    │
│  │          stats.setSuccessCount(executions.stream()             │    │
│  │              .filter(e -> "success".equals(e.getStatus()))       │    │
│  │              .count());                                          │    │
│  │          stats.setFailedCount(executions.stream()               │    │
│  │              .filter(e -> "failed".equals(e.getStatus()))       │    │
│  │              .count());                                         │    │
│  │          stats.setSuccessRate(stats.getTotalCount() > 0 ?        │    │
│  │              (double) stats.getSuccessCount() / stats.getTotalCount() * 100 : 0);│  │
│  │          stats.setAvgDuration(executions.stream()               │    │
│  │              .mapToLong(Execution::getDuration)                 │    │
│  │              .average().orElse(0));                             │    │
│  │                                                                   │    │
│  │          return stats;                                          │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取执行趋势                                            │    │
│  │       */                                                        │    │
│  │      public List<TrendData> getExecutionTrend(                   │    │
│  │          String workflowId, LocalDateTime startTime,              │    │
│  │          LocalDateTime endTime, String interval) {                │    │
│  │                                                                   │    │
│  │          // 按时间间隔聚合                                       │    │
│  │          List<Execution> executions = executionRepository        │    │
│  │              .findByWorkflowIdAndCreateTimeBetween(              │    │
│  │                  workflowId, startTime, endTime);                 │    │
│  │                                                                   │    │
│  │          // 根据 interval 分组                                    │    │
│  │          Map<String, List<Execution>> grouped = executions.stream()│   │
│  │              .collect(Collectors.groupingBy(e ->                  │    │
│  │                  getGroupKey(e.getCreateTime(), interval)));     │    │
│  │                                                                   │    │
│  │          // 计算每个时间点的数据                                  │    │
│  │          return grouped.entrySet().stream()                      │    │
│  │              .map(entry -> {                                    │    │
│  │                  TrendData data = new TrendData();              │    │
│  │                  data.setTime(entry.getKey());                  │    │
│  │                  data.setTotalCount((long) entry.getValue().size());│  │
│  │                  data.setSuccessCount(entry.getValue().stream() |│    │
│  │                      .filter(e -> "success".equals(e.getStatus()))│  │
│  │                      .count());                                 │    │
│  │                  return data;                                   │    │
│  │              }))                                                │    │
│  │              .sorted(Comparator.comparing(TrendData::getTime))   │    │
│  │              .collect(Collectors.toList());                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取工作流排名                                          │    │
│  │       */                                                        │    │
│  │      public List<WorkflowRanking> getWorkflowRanking(             │    │
│  │          LocalDateTime startTime, LocalDateTime endTime,         │    │
│  │          String orderBy, int limit) {                           │    │
│  │          return executionRepository                            │    │
│  │              .getWorkflowRanking(startTime, endTime, orderBy, limit);│  │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、告警管理

### 5.1 告警实体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          告警实体设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AlertRule 告警规则                                               │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("alert_rule")                                       │    │
│  │  public class AlertRule implements Serializable {                 │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      private String name;                                         │    │
│  │      private String description;                                  │    │
│  │                                                                   │    │
│  │      /** 告警类型：execution_success/execution_fail/cost/execution_time */ │    │
│  │      private String alertType;                                    │    │
│  │                                                                   │    │
│  │      /** 告警条件（JSON）*/                                       │    │
│  │      @Column(columnDefinition = "jsonb")                        │    │
│  │      private String condition;                                    │    │
│  │                                                                   │    │
│  │      /** 告警级别：low/medium/high/critical */                   │    │
│  │      private String level;                                        │    │
│  │                                                                   │    │
│  │      /** 告警渠道 */                                             │    │
│  │      @Column(columnDefinition = "jsonb")                        │    │
│  │      private String channels;                                     │    │
│  │                                                                   │    │
│  │      /** 是否启用 */                                             │    │
│  │      private Boolean enabled;                                     │    │
│  │                                                                   │    │
│  │      /** 沉默周期（分钟）*/                                     │    │
│  │      private Integer silencePeriod;                               │    │
│  │                                                                   │    │
│  │      private String createBy;                                     │    │
│  │      private LocalDateTime createTime;                            │    │
│  │      private String updateBy;                                     │    │
│  │      private LocalDateTime updateTime;                            │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐  │    │
│  │  │  AlertRecord 告警记录                                      │  │    │
│  │  │  ─────────────────────────────────────────────────────────│  │    │
│  │  │                                                             │  │    │
│  │  │  @Data                                                      │  │    │
│  │  │  @TableName("alert_record")                                 │  │    │
│  │  │  public class AlertRecord implements Serializable {         │  │    │
│  │  │                                                             │  │    │
│  │  │      @TableId                                                │  │    │
│  │  │      private String id;                                      │  │    │
│  │  │                                                             │  │    │
│  │  │      private String ruleId;                                  │  │    │
│  │  │      private String ruleName;                                │  │    │
│  │  │                                                             │  │    │
│  │  │      /** 告警级别 */                                         │  │    │
│  │  │      private String level;                                    │  │    │
│  │  │                                                             │  │    │
│  │  │      /** 告警内容 */                                         │  │    │
│  │  │      private String title;                                   │  │    │
│  │  │      @Column(columnDefinition = "text")                      │  │    │
│  │  │      private String content;                                  │  │    │
│  │  │                                                             │  │    │
│  │  │      /** 关联对象 */                                         │  │    │
│  │  │      private String workflowId;                               │  │    │
│  │  │      private String executionId;                              │  │    │
│  │  │                                                             │  │    │
│  │  │      /** 状态：triggered/resolved/silenced */                 │  │    │
│  │  │      private String status;                                   │  │    │
│  │  │                                                             │  │    │
│  │  │      private LocalDateTime triggerTime;                      │  │    │
│  │  │      private LocalDateTime resolveTime;                      │  │    │
│  │  │  }                                                           │  │    │
│  │  │                                                             │  │    │
│  │  └─────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 告警规则引擎

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          告警规则引擎                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AlertRuleEngine 告警规则引擎                                       │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class AlertRuleEngine {                                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private AlertRuleRepository ruleRepository;                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private AlertNotifyService notifyService;                     │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 评估执行结果                                              │    │
│  │       */                                                        │    │
│  │      public void evaluateExecution(Execution execution) {         │    │
│  │          // 获取所有启用的告警规则                                  │    │
│  │          List<AlertRule> rules = ruleRepository                  │    │
│  │              .findByEnabledAndAlertType(true, "execution_result");│    │
│  │                                                                   │    │
│  │          for (AlertRule rule : rules) {                          │    │
│  │              try {                                               │    │
│  │                  if (evaluateRule(rule, execution)) {            │    │
│  │                      triggerAlert(rule, execution);               │    │
│  │                  }                                               │    │
│  │              } catch (Exception e) {                              │    │
│  │                  log.error("评估告警规则失败: ruleId={}",          │    │
│  │                      rule.getId(), e);                          │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 评估规则                                                │    │
│  │       */                                                        │    │
│  │      private boolean evaluateRule(AlertRule rule,                    │    │
│  │                                 Execution execution) {            │    │
│  │          AlertCondition condition = parseCondition(rule.getCondition());│   │
│  │                                                                   │    │
│  │          // 检查沉默周期                                          │    │
│  │          if (isInSilencePeriod(rule, execution.getWorkflowId())) {│    │
│  │              return false;                                        │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 评估条件                                              │    │
│  │          switch (condition.getOperator()) {                       │    │
│  │              case "eq":                                          │    │
│  │                  return equals(execution, condition);             │    │
│  │              case "ne":                                          │    │
│  │                  return notEquals(execution, condition);         │    │
│  │              case "gt":                                          │    │
│  │                  return greaterThan(execution, condition);        │    │
│  │              case "lt":                                          │    │
│  │                  return lessThan(execution, condition);           │    │
│  │              case "gte":                                         │    │
│  │                  return greaterThanOrEqual(execution, condition);│    │
│  │              case "lte":                                         │    │
│  │                  return lessThanOrEqual(execution, condition);    │    │
│  │              default:                                            │    │
│  │                  return false;                                    │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean equals(Execution execution, AlertCondition condition) {│    │
│  │          Object value = getFieldValue(execution, condition.getField());│   │
│  │          return Objects.equals(value, condition.getValue());      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private boolean greaterThan(Execution execution, AlertCondition condition) {│  │
│  │          Object value = getFieldValue(execution, condition.getField());│   │
│  │          if (value instanceof Number) {                           │    │
│  │              double v = ((Number) value).doubleValue();            │    │
│  │              double threshold = ((Number) condition.getValue()).doubleValue();│    │
│  │              return v > threshold;                                │    │
│  │          }                                                       │    │
│  │          return false;                                            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 触发告警                                                │    │
│  │       */                                                        │    │
│  │      private void triggerAlert(AlertRule rule, Execution execution) {│   │
│  │          AlertRecord record = new AlertRecord();                  │    │
│  │          record.setRuleId(rule.getId());                          │    │
│  │          record.setRuleName(rule.getName());                      │    │
│  │          record.setLevel(rule.getLevel());                        │    │
│  │          record.setTitle("执行" + ("success".equals(execution.getStatus()) ? "成功" : "失败"));│   │
│  │          record.setContent(buildAlertContent(rule, execution));    │    │
│  │          record.setWorkflowId(execution.getWorkflowId());         │    │
│  │          record.setExecutionId(execution.getId());                │    │
│  │          record.setStatus("triggered");                          │    │
│  │          record.setTriggerTime(LocalDateTime.now());              │    │
│  │                                                                   │    │
│  │          alertRecordRepository.save(record);                      │    │
│  │                                                                   │    │
│  │          // 发送通知                                              │    │
│  │          notifyService.notify(record, rule.getChannels());        │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.3 告警通知服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          告警通知服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AlertNotifyService 告警通知服务                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class AlertNotifyService {                                │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private DingtalkNotifier dingtalkNotifier;                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private EmailNotifier emailNotifier;                         │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private SmsNotifier smsNotifier;                             │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 发送通知                                                │    │
│  │       */                                                        │    │
│  │      public void notify(AlertRecord record, String channels) {   │    │
│  │          List<String> channelList = parseChannels(channels);     │    │
│  │                                                                   │    │
│  │          for (String channel : channelList) {                   │    │
│  │              try {                                               │    │
│  │                  sendNotification(channel, record);               │    │
│  │              } catch (Exception e) {                             │    │
│  │                  log.error("发送告警通知失败: channel={}, alertId={}",│  │
│  │                      channel, record.getId(), e);                │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void sendNotification(String channel, AlertRecord record) {│  │
│  │          switch (channel) {                                       │    │
│  │              case "dingtalk":                                    │    │
│  │                  dingtalkNotifier.send(record);                   │    │
│  │                  break;                                          │    │
│  │              case "email":                                       │    │
│  │                  emailNotifier.send(record);                      │    │
│  │                  break;                                          │    │
│  │              case "sms":                                         │    │
│  │                  smsNotifier.send(record);                        │    │
│  │                  break;                                          │    │
│  │              default:                                            │    │
│  │                  log.warn("不支持的告警渠道: {}", channel);      │    │
│  │          }                                                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  │  ┌─────────────────────────────────────────────────────────────┐  │    │
│  │  │  DingtalkNotifier 钉钉通知器                                │  │    │
│  │  │  ─────────────────────────────────────────────────────────│  │    │
│  │  │                                                             │  │    │
│  │  │  @Component                                                 │  │    │
│  │  │  public class DingtalkNotifier implements AlertNotifier {  │  │    │
│  │  │                                                             │  │    │
│  │  │      @Value("${alert.dingtalk.webhook-url}")               │  │    │
│  │  │      private String webhookUrl;                             │  │    │
│  │  │                                                             │  │    │
│  │  │      @Override                                              │  │    │
│  │  │      public void send(AlertRecord record) {                 │  │    │
│  │  │          Map<String, Object> body = new HashMap<>();       │  │    │
│  │  │          body.put("msgtype", "markdown");                  │  │    │
│  │  │                                                             │  │    │
│  │  │          Map<String, Object> markdown = new HashMap<>();  │  │    │
│  │  │          markdown.put("title", record.getTitle());         │  │    │
│  │  │          markdown.put("text", buildMarkdownContent(record));│  │    │
│  │  │          body.put("markdown", markdown);                   │  │    │
│  │  │                                                             │  │    │
│  │  │          restTemplate.postForObject(webhookUrl, body, Map.class);│ │  │
│  │  │      }                                                       │  │    │
│  │  │  }                                                           │  │    │
│  │  │                                                             │  │    │
│  │  └─────────────────────────────────────────────────────────────┘  │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、API 接口

### 6.1 执行日志接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          执行日志 API                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  ExecutionLogController 执行日志接口                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/monitor/logs")                        │    │
│  │  @Api(tags = "执行日志")                                        │    │
│  │  public class ExecutionLogController {                           │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private ExecutionLogService logService;                      │    │
│  │                                                                   │    │
│  │      @GetMapping("/{executionId}")                               │    │
│  │      @ApiOperation("获取执行日志")                                │    │
│  │      public Result<List<ExecutionLogDTO>> getLogs(               │    │
│  │          @PathVariable String executionId,                        │    │
│  │          @RequestParam(required = false) String level) {         │    │
│  │          List<ExecutionLog> logs = logService.getLogs(executionId, level);│   │
│  │          return Result.success(logs.stream()                      │    │
│  │              .map(this::toDTO).collect(Collectors.toList()));    │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{executionId}/page")                          │    │
│  │      @ApiOperation("分页查询执行日志")                            │    │
│  │      public Result<Page<ExecutionLogDTO>> getLogsPage(          │    │
│  │          @PathVariable String executionId,                        │    │
│  │          @ModelAttribute PageRequest pageRequest) {               │    │
│  │          Page<ExecutionLog> page = logService.getLogsPage(      │    │
│  │              executionId, pageRequest);                           │    │
│  │          return Result.success(page.map(this::toDTO));           │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 告警接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          告警 API                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AlertController 告警接口                                         │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/monitor/alerts")                      │    │
│  │  @Api(tags = "告警管理")                                        │    │
│  │  public class AlertController {                                  │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private AlertService alertService;                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/rules")                                      │    │
│  │      @ApiOperation("获取告警规则列表")                            │    │
│  │      public Result<List<AlertRuleDTO>> getRules() {              │    │
│  │          List<AlertRule> rules = alertService.getAllRules();    │    │
│  │          return Result.success(rules.stream()                   │    │
│  │              .map(this::toRuleDTO).collect(Collectors.toList()));│  │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/rules")                                      │    │
│  │      @ApiOperation("创建告警规则")                                │    │
│  │      public Result<AlertRuleDTO> createRule(                     │    │
│  │          @RequestBody @Valid AlertRuleCreateRequest request) {    │    │
│  │          AlertRule rule = alertService.createRule(request);      │    │
│  │          return Result.success(toRuleDTO(rule));                 │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/records")                                     │    │
│  │      @ApiOperation("获取告警记录")                                │    │
│  │      public Result<Page<AlertRecordDTO>> getRecords(              │    │
│  │          @ModelAttribute AlertRecordQuery query,                  │    │
│  │          @ModelAttribute PageRequest pageRequest) {               │    │
│  │          Page<AlertRecord> page = alertService.getRecords(query, pageRequest);│  │
│  │          return Result.success(page.map(this::toRecordDTO));     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/records/{id}/resolve")                        │    │
│  │      @ApiOperation("标记告警为已解决")                            │    │
│  │      public Result<Void> resolve(@PathVariable String id) {       │    │
│  │          alertService.resolveAlert(id);                           │    │
│  │          return Result.success();                                 │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、配置说明

### 7.1 YAML 配置

```yaml
MeowFlow:
  monitor:
    alert:
      dingtalk:
        webhook-url: ${DINGTALK_WEBHOOK_URL:}
      email:
        host: ${EMAIL_HOST:smtp.example.com}
        port: ${EMAIL_PORT:587}
        username: ${EMAIL_USERNAME:}
        password: ${EMAIL_PASSWORD:}
    metrics:
      aggregation-interval: 60000  # 指标聚合间隔（毫秒）
      retention-days: 30  # 指标保留天数
```

---

## 八、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-07-10 | 初始版本 |

---

**文档版本：v1.0**
**最后更新：2026-07-10**
