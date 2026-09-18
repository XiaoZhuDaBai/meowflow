# MeowFlow Executor - 测试指南

## 快速开始

### 运行所有测试

```bash
cd backend/meowflow/meowflow-executor
mvn test
```

### 运行指定测试类

```bash
# 运行 TaskService 测试
mvn test -Dtest=TaskServiceTest

# 运行 ExecutorNodeRegistry 测试
mvn test -Dtest=ExecutorNodeRegistryTest

# 运行所有 Service 层测试
mvn test -Dtest=**/service/*Test
```

### 运行指定测试方法

```bash
mvn test -Dtest=TaskServiceTest#createTask_shouldCreateTaskWithDefaultValues
```

## 测试覆盖率报告

### 生成 JaCoCo 覆盖率报告

```bash
mvn clean test jacoco:report
```

### 查看报告

```bash
# Windows
start target/site/jacoco/index.html

# macOS
open target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html
```

## 测试结构

```
src/test/java/com/meowflow/executor/
├── service/                    # 服务层测试
│   ├── TaskServiceTest.java
│   ├── ExecutorNodeRegistryTest.java
│   ├── MonitorServiceTest.java
│   └── TaskDispatchServiceTest.java
├── controller/                 # 控制器测试
│   ├── ExecutorControllerTest.java
│   └── ExecutorControllerMockMvcTest.java
├── model/                      # 模型测试
│   ├── TaskTest.java
│   ├── TaskQueueTest.java
│   ├── TaskContextTest.java
│   ├── TaskResultTest.java
│   ├── TaskStatusTest.java
│   └── TaskTypeTest.java
├── registry/                   # 注册管理测试
│   ├── ExecutorNodeTest.java
│   ├── HealthCheckerTest.java
│   └── TimeoutScannerTest.java
├── client/                     # 客户端测试
│   ├── ExecutorClientTest.java
│   └── ExecutorNodeConfigTest.java
├── loadbalancer/              # 负载均衡测试
│   └── LoadBalancerTest.java
├── mq/                        # 消息队列测试
│   ├── TaskProducerTest.java
│   ├── TaskConsumerTest.java
│   ├── TaskMessageTest.java
│   └── TaskRabbitMQIntegrationTest.java
├── entity/                    # 实体测试
│   ├── ExecutorNodeEntityTest.java
│   └── ExecutorTaskEntityTest.java
├── mapper/                    # 数据访问测试
│   ├── ExecutorNodeMapperTest.java
│   └── ExecutorTaskMapperTest.java
└── config/                    # 配置测试
    └── ExecutorConfigTest.java
```

## 测试分类

### 单元测试 (快速执行)

```bash
mvn test -Dgroups=unit
```

### 集成测试 (需要外部依赖)

```bash
# 需要 RabbitMQ
mvn test -Dtest=TaskRabbitMQIntegrationTest

# 需要数据库
mvn test -Dtest=ExecutorNodeMapperTest,ExecutorTaskMapperTest
```

## 持续集成

### 在 CI/CD 中运行

```yaml
# GitHub Actions 示例
- name: Run Tests
  run: mvn test -B
  
- name: Generate Coverage Report
  run: mvn jacoco:report
  
- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

## 测试依赖

所有测试依赖已在 `pom.xml` 中配置：

- JUnit 5 - 测试框架
- Mockito - Mock 框架
- AssertJ - 断言库
- Spring Boot Test - Spring 测试支持
- TestContainers - 容器化集成测试

## 调试测试

### IntelliJ IDEA

1. 右键点击测试类/方法
2. 选择 "Debug 'TestName'"

### 命令行调试

```bash
mvn test -Dtest=TaskServiceTest -Dmaven.surefire.debug
```

然后在 IDE 中连接到端口 5005。

## 常见问题

### Q: 测试执行很慢

A: 跳过集成测试：
```bash
mvn test -DskipITs
```

### Q: 某些测试失败

A: 检查依赖服务是否启动（RabbitMQ、PostgreSQL）

### Q: 如何只运行失败的测试

```bash
mvn test -Dsurefire.rerunFailingTestsCount=2
```

## 更多信息

- 📊 [测试覆盖报告](./TEST_COVERAGE.md)
- 📝 [测试完成报告](./TEST_COMPLETION_REPORT.md)
