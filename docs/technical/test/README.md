# 喵流 (MeowFlow) - 测试总览文档

> 本文档定义喵流项目的测试策略、规范、最佳实践与 CI 集成
> 适用范围：后端多模块 Maven 项目（Spring Boot 3.2 + Java 17）
>
> 📌 **数据一致性测试用例**：详见 [`DATA_CONSISTENCY_TEST.md`](DATA_CONSISTENCY_TEST.md)（P0 ~ P4 共 34 个测试用例）
> 📌 **交叉引用**：测试用例 ↔ 技术栈 ↔ 模块 三向映射，详见 [`CROSS_REFERENCE.md`](CROSS_REFERENCE.md)

---

## 一、测试金字塔与层次结构

### 1.1 测试层次模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          测试金字塔                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                              /\                                          │
│                             /  \         E2E 测试 (Playwright/Postman) │
│                            / E2E\         数量：少（5-10 个关键场景）    │
│                           /______\        速度：慢（分钟级）             │
│                          /        \       维护：高                       │
│                         /  集成测试 \     ────────────────────────────   │
│                        / (Testcont.)\    集成测试 (Testcontainers)      │
│                       /______________\   数量：中（30-50 个）            │
│                      /                \  速度：中（秒级 ~ 分钟级）       │
│                     /    单元测试      \ ────────────────────────────    │
│                    /   (JUnit+Mockito) \ 单元测试 (JUnit 5 + Mockito)  │
│                   /______________________\ 数量：多（数百个）              │
│                                         速度：快（毫秒级）              │
│                                         维护：低                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 各层次测试的目标与比例

| 层次 | 目标 | 工具栈 | 数量比例 | 执行频率 | 覆盖率目标 |
|------|------|--------|---------|---------|-----------|
| **单元测试** | 验证单个类/方法的逻辑正确性 | JUnit 5 + Mockito + AssertJ | ~70% | 每次提交 | 行 ≥ 80% / 分支 ≥ 70% |
| **集成测试** | 验证模块间协作（DB/Redis/MQ） | Spring Boot Test + Testcontainers | ~25% | 每次 PR | 关键路径 100% |
| **契约测试** | 验证服务间 API 兼容性 | OpenAPI Schema Validator | ~3% | 每次发版 | API 100% |
| **E2E 测试** | 验证完整业务流程 | Playwright / Postman + Newman | ~2% | 每日/发版前 | 关键场景 100% |
| **性能测试** | 验证 SLA 与性能基线 | JMeter / Gatling | 关键场景 | 每周/发版前 | 满足 P99 指标 |

### 1.3 当前项目测试现状

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     当前测试覆盖情况（基于代码扫描）                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  已有测试的模块：                                                        │
│  ✓ meowflow-workflow    ：BaseUnitTest / BaseServiceTest /              │
│                           WorkflowServiceTest / DAGSorterTest /          │
│                           TestDataBuilder / TestRedisConfiguration       │
│  ✓ meowflow-executor    ：BaseExecutorUnitTest / BaseExecutorServiceTest│
│                           ExecutionMessageTest                           │
│  ✓ meowflow-infra       ：ModelRouterTest / AIProviderTest /             │
│                           OpenAIChatClientTest / ChatClientTest          │
│  ✓ meowflow-template    ：TemplateRatingServiceTest                      │
│                                                                          │
│  测试覆盖缺失的模块：                                                    │
│  ✗ meowflow-common      ：无单元测试（IdGenerator / TraceAspect 等）     │
│  ✗ meowflow-user        ：无单元测试（UserService / PermissionService）  │
│  ✗ meowflow-monitor     ：无单元测试（AlertService / LogQueryService）   │
│                                                                          │
│  Controller 层测试：缺失（24 个 Controller 仅有 0 个 @WebMvcTest）        │
│  契约测试：缺失                                                           │
│  E2E 测试：缺失                                                           │
│  性能测试：缺失                                                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.4 模块测试类清单

| 模块 | 已有测试类 | 数量 | 覆盖范围 |
|------|-----------|------|---------|
| **meowflow-workflow** | `WorkflowServiceTest`, `DAGSorterTest`, `BaseUnitTest`, `BaseServiceTest`, `TestDataBuilder`, `TestRedisConfiguration` | 4 测试类 + 2 基类 | Service 层 + DAG 算法 |
| **meowflow-executor** | `ExecutionMessageTest`, `BaseExecutorUnitTest`, `BaseExecutorServiceTest` | 1 测试类 + 2 基类 | 消息模型 |
| **meowflow-infra** | `ModelRouterTest`, `AIProviderTest`, `OpenAIChatClientTest`, `ChatClientTest` | 4 测试类 | LLM 路由 / Provider / Client |
| **meowflow-template** | `TemplateRatingServiceTest` | 1 测试类 | 评分 Service |
| **meowflow-common** | 无 | 0 | **需补齐** |
| **meowflow-user** | 无 | 0 | **需补齐** |
| **meowflow-monitor** | 无 | 0 | **需补齐** |

---

## 二、测试环境与基础设施

### 2.1 测试依赖（已配置）

**父 POM** `backend/meowflow/pom.xml` 中已统一管理：

| 依赖 | 版本 | 用途 | Scope |
|------|------|------|-------|
| `spring-boot-starter-test` | 3.2.0 | JUnit 5 / Mockito / AssertJ / JsonPath | test |
| `mockito-junit-jupiter` | 5.8.0 | Mockito 扩展 | test |
| `testcontainers-bom` | 1.19.3 | Testcontainers 依赖管理 | import |
| `testcontainers:junit-jupiter` | 1.19.3 | Testcontainers JUnit 5 集成 | test |
| `testcontainers:postgresql` | 1.19.3 | PostgreSQL Testcontainer | test |
| `h2database` | 2.2.224 | 内存数据库（快速单元测试） | test |
| `jacoco-maven-plugin` | 0.8.11 | 代码覆盖率报告 | - |
| `maven-surefire-plugin` | 3.2.2 | Maven 测试运行器 | - |

### 2.2 测试基础设施

#### 2.2.1 基类体系（已实现）

项目已实现两套基类，位于 `meowflow-workflow` 模块：

| 基类 | 路径 | 用途 |
|------|------|------|
| `BaseUnitTest<S, R>` | `meowflow-workflow/src/test/java/com/meowflow/workflow/BaseUnitTest.java` | 纯 Mockito 单元测试，自动注入 Service 和 Repository Mock |
| `BaseServiceTest` | `meowflow-workflow/src/test/java/com/meowflow/workflow/BaseServiceTest.java` | Testcontainers PostgreSQL 集成测试基类 |
| `BaseExecutorUnitTest` | `meowflow-executor/src/test/java/com/meowflow/executor/BaseExecutorUnitTest.java` | Executor 模块的单元测试基类（同 workflow 模式） |
| `BaseExecutorServiceTest` | `meowflow-executor/src/test/java/com/meowflow/executor/BaseExecutorServiceTest.java` | Executor 模块的集成测试基类 |

**注**：`meowflow-template`、`meowflow-infra` 模块当前未提供统一基类，可复用 workflow 的两个基类（提取到 `meowflow-common` 模块更合理）。

#### 2.2.2 测试配置 `application-test.yml`

各模块 `src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false

# Mock 外部服务
meowflow:
  ai:
    mock: true
  notification:
    mock: true
  mcp:
    mock: true
```

**说明**：当前使用 H2 内存数据库（PostgreSQL 兼容模式）+ JPA 自动建表，适合快速集成测试。对于真实 PostgreSQL 行为测试（JSONB、tsvector、pg_trgm），仍需 Testcontainers。

#### 2.2.3 Testcontainers 配置示例

`BaseServiceTest` 提供的 Testcontainer 配置：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("meowflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }
}
```

---

## 三、CI 集成（GitHub Actions）

### 3.1 工作流概述

CI 配置位于 `.github/workflows/test.yml`，包含两个 Job：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CI Pipeline 流程                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   push / pull_request to main, develop                                  │
│              │                                                           │
│              ▼                                                           │
│   ┌────────────────────┐                                                │
│   │  test Job          │                                                │
│   │  (Unit Tests)      │                                                │
│   │  ubuntu-latest     │                                                │
│   │  + PostgreSQL 15   │                                                │
│   │  Service Container │                                                │
│   └──────────┬─────────┘                                                │
│              │                                                           │
│              ├─► Checkout                                                │
│              ├─► Set up JDK 17 (Temurin + Maven Cache)                  │
│              ├─► mvn clean install -DskipTests                          │
│              ├─► mvn test -Dspring.profiles.active=test                 │
│              ├─► mvn jacoco:report                                      │
│              ├─► Upload coverage to Codecov (flags=unittests)           │
│              ├─► Check coverage gate (≥ 60%)                            │
│              └─► Upload test-results (surefire-reports/*.xml)          │
│                                                                          │
│              ▼                                                           │
│   ┌────────────────────┐                                                │
│   │  build Job         │                                                │
│   │  (Build Check)     │  needs: test                                   │
│   │  ubuntu-latest     │                                                │
│   └──────────┬─────────┘                                                │
│              │                                                           │
│              ├─► mvn clean package -DskipTests                          │
│              └─► Upload JAR artifacts (retention 7d)                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 关键配置

#### 3.2.1 PostgreSQL Service Container

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
      POSTGRES_DB: meowflow_test
    ports:
      - 5432:5432
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

#### 3.2.2 覆盖率门槛

**双层校验**：
- **JaCoCo Maven Plugin**：`mvn verify` 时自动执行 `jacoco:check`，行覆盖率门槛 `0.60`（60%）
- **CI Shell 脚本**：解析 `jacoco.xml`，若低于 60% 直接 `exit 1`

```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.60</minimum>
</limit>
```

**当前门槛偏低**：项目文档目标行覆盖率应提升至 **≥ 80%**，分支覆盖率 **≥ 70%**（详见第八章）。

#### 3.2.3 Codecov 集成

```yaml
- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ${{ github.workspace }}/backend/meowflow/target/site/jacoco/jacoco.xml
    flags: unittests
    name: codecov-umbrella
  continue-on-error: true
```

**说明**：`continue-on-error: true` 意味着 Codecov 上传失败不影响 CI 通过，但会丢失历史趋势数据。建议移除该选项以强制执行。

#### 3.2.4 测试结果报告

测试完成后上传 Surefire 报告：

```yaml
- name: Upload test results
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: test-results
    path: ${{ github.workspace }}/backend/meowflow/**/target/surefire-reports/*.xml
```

### 3.3 CI 缺失项

| 缺失项 | 建议 |
|--------|------|
| 集成测试 Job | 当前 `mvn test` 同时跑了单元+集成测试，但启动慢，应拆分 Job 串行 |
| Testcontainers Docker 缓存 | 添加 `cache-from: type=gha` 加速镜像拉取 |
| 覆盖率历史趋势 | 移除 `continue-on-error: true`，让 Codecov 失败成为警告 |
| 静态代码检查 | 添加 SonarQube / SpotBugs Job |
| 安全扫描 | 添加 OWASP Dependency-Check |

---

## 四、文档导航

| 文档 | 说明 |
|------|------|
| [`DATA_CONSISTENCY_TEST.md`](DATA_CONSISTENCY_TEST.md) | 34 个数据一致性 / 架构异常测试用例（P0 ~ P4） |
| [`CROSS_REFERENCE.md`](CROSS_REFERENCE.md) | 测试用例 ↔ 技术栈 ↔ 模块三向交叉引用 |
| [`UNIT_TEST_GUIDE.md`](UNIT_TEST_GUIDE.md) | 单元测试规范与最佳实践 |
| [`INTEGRATION_TEST_GUIDE.md`](INTEGRATION_TEST_GUIDE.md) | 集成测试规范与 Testcontainers |
| [`CONTRACT_TEST_GUIDE.md`](CONTRACT_TEST_GUIDE.md) | API 契约测试规范 |
| [`PERFORMANCE_TEST_GUIDE.md`](PERFORMANCE_TEST_GUIDE.md) | 性能测试规范 |
| [`COVERAGE_GUIDE.md`](COVERAGE_GUIDE.md) | 测试覆盖率目标与 JaCoCo 配置 |
| [`BEST_PRACTICES.md`](BEST_PRACTICES.md) | 测试编写最佳实践 |
| [`FAQ.md`](FAQ.md) | 常见问题 |

---

## 五、版本

| 文档 | 路径 | 版本 | 最后更新 |
|------|------|------|---------|
| 测试总览（本文档） | `docs/technical/test/README.md` | v1.0 | 2026-07-13 |
| 数据一致性测试 | `docs/technical/test/DATA_CONSISTENCY_TEST.md` | v1.1 | 2026-07-11 |
| 交叉引用 | `docs/technical/test/CROSS_REFERENCE.md` | v1.2 | 2026-07-11 |
