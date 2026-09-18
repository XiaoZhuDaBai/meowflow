# 喵流 (MeowFlow) - API 契约测试规范

> 本文档定义服务间 API 契约测试的策略、工具与最佳实践

---

## 一、什么是 API 契约测试

> **API 契约测试**（Contract Testing）：验证服务提供者（Provider）发布的 API 是否符合消费者（Consumer）期望的接口契约。

### 1.1 为什么需要契约测试

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       传统集成测试 vs 契约测试                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  传统集成测试（端到端）：                                                │
│  Consumer ──HTTP──► Provider                                            │
│  • 需要所有服务都启动                                                    │
│  • 一个服务挂掉，所有相关测试都失败                                     │
│  • 反馈周期长（数分钟）                                                 │
│  • 测试环境维护成本高                                                    │
│                                                                          │
│  契约测试：                                                              │
│  Consumer 验证 ◄── 契约文件 ──► Provider 验证                          │
│  • 服务可独立测试                                                        │
│  • 快速反馈（秒级）                                                     │
│  • 提前发现接口不兼容问题                                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 适用场景

喵流项目中的适用点：

| 场景 | 是否适用 | 说明 |
|------|---------|------|
| **meowflow-workflow ↔ meowflow-executor** | ✅ 是 | 通过 RabbitMQ 消息交互，需要契约保证 |
| **meowflow-user 暴露 API** | ✅ 是 | 供其他模块鉴权调用 |
| **前端 meowflow-ui ↔ 后端** | ✅ 是 | OpenAPI Schema 可被前端消费 |
| **LLM 模型对接**（OpenAI/Anthropic） | ⚠️ 部分 | 第三方接口，可使用 Schema Validator 验证响应 |

---

## 二、契约测试方案

### 2.1 方案对比

| 方案 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| **Spring Cloud Contract** | 与 Spring 生态深度集成 | 仅限 Spring Boot | ⭐⭐⭐⭐ |
| **Pact (Pact-JVM)** | 语言无关、生态成熟 | 学习曲线较陡 | ⭐⭐⭐⭐ |
| **OpenAPI Schema Validator** | 直接基于 OpenAPI 规范 | 仅验证请求/响应结构 | ⭐⭐⭐⭐⭐ |
| **JSON Schema 校验** | 简单直接 | 需手动维护 Schema | ⭐⭐⭐ |

### 2.2 推荐方案：OpenAPI Schema Validator（最简单）

项目已使用 Knife4j 生成 OpenAPI 文档，可直接复用：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>
</dependency>
```

#### 2.2.1 编写契约测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private RequestSpecification spec;

    @BeforeEach
    void setUp() {
        spec = new RequestSpecification();
        spec.setBaseUri("http://localhost:" + port);
    }

    @Test
    @DisplayName("POST /api/workflows - 响应符合 OpenAPI Schema")
    void testCreateWorkflowResponseSchema() throws Exception {
        // Given - OpenAPI Schema
        JsonSchema workflowSchema = loadSchema("/schemas/workflow-response.json");

        // When
        Map<String, Object> request = Map.of(
            "name", "Test Workflow",
            "code", "test-workflow-001"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/workflows", request, String.class);

        // Then - 验证响应符合 Schema
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        Set<ValidationMessage> errors = workflowSchema.validate(responseBody);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("GET /api/workflows/{id} - 404 响应符合 Schema")
    void testNotFoundResponseSchema() throws Exception {
        JsonSchema errorSchema = loadSchema("/schemas/error-response.json");

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/workflows/99999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        Set<ValidationMessage> errors = errorSchema.validate(responseBody);

        assertThat(errors).isEmpty();
    }

    private JsonSchema loadSchema(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        JsonNode schemaNode = new ObjectMapper().readTree(is);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaNode);
    }
}
```

#### 2.2.2 Schema 文件示例

`src/test/resources/schemas/workflow-response.json`：

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "code": { "type": "integer", "minimum": 0 },
    "message": { "type": "string" },
    "data": {
      "type": "object",
      "properties": {
        "id": { "type": "integer" },
        "name": { "type": "string", "minLength": 1, "maxLength": 100 },
        "code": { "type": "string", "pattern": "^[a-z0-9-]+$" },
        "status": { "enum": ["draft", "published", "archived"] },
        "ownerId": { "type": "integer" },
        "createTime": { "type": "string", "format": "date-time" }
      },
      "required": ["id", "name", "code", "status", "ownerId"]
    }
  },
  "required": ["code", "message", "data"]
}
```

### 2.3 方案对比：Spring Cloud Contract（推荐用于服务间）

如果需要更严格的契约验证（不仅是 Schema，还包括消息格式）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-verifier</artifactId>
    <scope>test</scope>
</dependency>
```

```groovy
// src/test/resources/contracts/workflow/shouldCreateWorkflow.groovy
Contract.make {
    description "should create a new workflow"
    request {
        method 'POST'
        url '/api/workflows'
        body([
            name: 'Test Workflow',
            code: 'test-001'
        ])
        headers {
            contentType('application/json')
        }
    }
    response {
        status 200
        body([
            code: 200,
            message: 'success',
            data: [
                id: $(anyPositiveInt()),
                name: 'Test Workflow',
                code: 'test-001',
                status: 'draft'
            ]
        ])
    }
}
```

---

## 三、契约测试组织

### 3.1 目录结构

```
src/test/
├── java/com/meowflow/<module>/
│   └── contract/
│       ├── WorkflowApiContractTest.java
│       └── UserApiContractTest.java
└── resources/
    ├── contracts/                    # Spring Cloud Contract
    │   └── workflow/
    │       └── shouldCreateWorkflow.groovy
    └── schemas/                       # JSON Schema
        ├── workflow-response.json
        ├── error-response.json
        └── page-response.json
```

### 3.2 CI 集成

```yaml
- name: Run contract tests
  run: mvn test -Dgroups=contract
```

---

## 四、契约变更管理

### 4.1 向后兼容原则

> ⚠️ **重要**：API 契约变更（破坏性）必须先于消费者升级，避免运行时错误。

### 4.2 破坏性变更检测

```java
@Test
@DisplayName("向后兼容性 - 现有字段未删除")
void testBackwardCompatibility() {
    JsonSchema currentSchema = loadSchema("/schemas/v2/workflow-response.json");
    JsonSchema previousSchema = loadSchema("/schemas/v1/workflow-response.json");

    Set<String> removedFields = findRemovedFields(currentSchema, previousSchema);
    assertThat(removedFields).as("已删除字段").isEmpty();
}
```

### 4.3 版本控制

| 路径 | 含义 |
|------|------|
| `/api/v1/workflows` | 旧版 API（保留中） |
| `/api/v2/workflows` | 新版 API（推荐） |

保留至少一个旧版本（deprecation period），给消费者升级时间。

---

## 五、API 兼容性 Checklist

每次 API 变更前确认：

- [ ] 新增字段：在响应中**追加**（不破坏旧消费者）
- [ ] 删除字段：标记为 deprecated，至少保留 2 个版本
- [ ] 修改字段类型：新增字段替代旧字段
- [ ] 修改 URL：保留旧路径（重定向到新路径）
- [ ] 错误码变更：在响应 body 中增加新字段说明
- [ ] 更新 OpenAPI 文档（Knife4j）
- [ ] 通知所有 API 消费者

---

## 六、参考资源

- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
- [Pact 官方文档](https://docs.pact.io/)
- [OpenAPI 规范](https://swagger.io/specification/)
- [JSON Schema 规范](https://json-schema.org/)