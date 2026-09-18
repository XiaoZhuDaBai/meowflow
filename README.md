# 喵流 (MeowFlow)

> 让每一份工作，像猫一样优雅流动 🐱
> Powered by 汉堡（金渐层猫）

![Logo](docs/assets/logo-banner.svg)

## 产品特点

- **零门槛**：无需编程基础，通过可视化拖拽即可构建 AI 工作流
- **模板市场**：丰富的预置模板，拿来即用
- **智能向导**：问卷式创建，引导业务人员完成流程设计
- **AI 助手**：自然语言描述，AI 自动生成工作流
- **企业级**：支持团队协作、权限管理、成本控制
- **安全可控**：私有化部署，数据完全自主

## 快速开始

### 前置要求

- JDK 17+
- Node.js 22+（前端测试依赖 jsdom 30，需要较新的 V8；Node 20 会在 vitest 阶段报错）
- PostgreSQL 13+
- Redis 6+
- RabbitMQ 3.8+

### 安装部署

```bash
# 1. 克隆代码
git clone https://github.com/XiaoZhuDaBai/meowflow.git
cd meowflow

# 2. 构建后端（Maven 工程位于 backend/meowflow，不是仓库根目录）
cd backend/meowflow
mvn clean install -DskipTests
mvn -pl meowflow-workflow spring-boot:run   # 或按需启动其它模块

# 3. 启动前端
cd frontend/meowflow-ui
npm install
npm run dev
```

> **Nacos 开关**：`meowflow-workflow` 等服务默认 `NACOS_ENABLED=false`。
> 若通过网关（8080）访问，必须显式开启，否则网关拿不到实例会返回 503：
> `mvn -pl meowflow-workflow spring-boot:run -Dspring-boot.run.jvmArguments=-DNACOS_ENABLED=true`

### 数据库

表结构由 **Flyway 自动迁移**，无需手工建表。迁移脚本位于
`backend/meowflow/meowflow-common/src/main/resources/db/migration`，
配置在 `common-defaults.yml`。新建一个空库即可，启动时自动建出全部表。

```sql
CREATE DATABASE meowflow OWNER meowflow;
```

连接参数通过环境变量覆盖（默认值见 `deploy/docker/.env`）：

```
POSTGRES_HOST / POSTGRES_PORT / POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD
```

#### 存量数据库（已被手工建过表）

如果数据库里的表是用 `scripts/sql` 手工建的、**没有 `flyway_schema_history` 表**，
Flyway 会把它当作空库并从头执行 `V1__init_schema.sql`，从而报
`relation "xxx" already exists`。此时给该库补一条基线记录即可（版本取已应用的最高迁移号，
当前最高为 22）：

```sql
INSERT INTO flyway_schema_history
  (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
  (1, '22', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'meowflow', now(), 0, true);
```

之后启动会看到 `Schema "public" is up to date. No migration necessary.`。
**全新部署不需要这一步。**

### Docker 部署

```bash
docker compose -f backend/meowflow/deploy/docker/docker-compose.dev.yml up -d
```

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2
- Spring Cloud Alibaba
- MyBatis-Plus
- Flyway（数据库迁移）
- PostgreSQL
- Redis
- RabbitMQ

### 前端
- Vue 3 + TypeScript
- Element Plus
- AntV G6
- Vite
- Pinia

## 项目结构

```
喵流/
├── docs/                      # 项目文档
│   ├── product/               # 产品文档
│   ├── technical/             # 技术文档
│   └── prototype/             # 原型文件
│
├── backend/                   # 后端项目
│   └── meowflow/              # 主项目
│       ├── meowflow-common/   # 公共模块
│       ├── meowflow-executor/ # 执行器服务
│       ├── meowflow-workflow/ # 工作流服务
│       ├── meowflow-user/     # 用户服务
│       ├── meowflow-template/ # 模板服务
│       └── meowflow-monitor/  # 监控服务
│
├── frontend/                  # 前端项目
│   └── meowflow-ui/           # 用户界面
│
└── scripts/                   # 部署脚本
    ├── docker/                # Docker 相关
    └── sql/                   # 数据库脚本
```

## 核心功能

### 工作流编辑器
- 可视化拖拽编排
- 丰富的节点类型
- 实时预览与测试
- 版本管理与回滚

### 节点类型

| 类型 | 节点 | 说明 |
|------|------|------|
| 触发器 | Webhook、定时、表单、消息 | 工作流入口 |
| AI | LLM、分类、提取、总结 | AI 能力 |
| 流程控制 | 条件分支、循环、并行、等待 | 流程控制 |
| 数据处理 | 代码、变量、JSON 解析 | 数据处理 |
| 集成 | HTTP、数据库、Redis | 第三方集成 |
| 通知 | 钉钉、企业微信、飞书、邮件、短信 | 消息通知 |

### 模板市场
- 行业分类模板
- 场景化解决方案
- 一键使用
- 自定义发布

### 执行监控
- 实时执行状态
- 详细日志追踪
- 成本统计
- 失败告警

## 部署脚本

数据库迁移由 Flyway 自动完成，`scripts/sql` 中的脚本仅作参考 / 手工初始化用，
见 [scripts/sql/](./scripts/sql/README.md)。

```bash
cd scripts/sql
./apply.sh                       # 手工初始化(默认连 localhost postgres)
psql -d meowflow -f reset.sql    # 清空业务表(仅本地)
```

## 测试

```bash
# 后端：单元 + 集成测试（需要 PostgreSQL / Redis）
cd backend/meowflow
mvn test

# 覆盖率报告
mvn org.jacoco:jacoco-maven-plugin:0.8.11:report

# 覆盖率门禁（LINE 65% / BRANCH 50%）
mvn verify -Pjacoco-gate

# 前端：类型检查 + 单元测试 + 构建
cd frontend/meowflow-ui
npx vue-tsc --noEmit
npm run test
npm run build
```

CI 配置见 [.github/workflows/ci.yml](./.github/workflows/ci.yml)：
`backend`（PG + Redis service）、`frontend`、`package` 三个 job。

## 文档

- [产品需求文档 (PRD)](./docs/PRD.md)
- [技术架构文档](./docs/technical/architecture.md)
- [数据库设计](./docs/technical/database.md)
- [数据库脚本指南](./scripts/sql/README.md)
- [改名记录](./docs/RENAME_LOG.md)

## 路线图

- [x] MVP 版本 - 核心功能上线
- [ ] v1.1 - 企业功能增强
- [ ] v1.2 - AI 能力增强
- [ ] v2.0 - 开放平台

## License

MIT License

## 联系我们

- 官网：https://meowflow.com
- 邮箱：contact@meowflow.com
- 微信群：[二维码链接]

---

**🐾 让 AI 能力触手可及**
