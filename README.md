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

### 脚本约定

仓库里的 `.ps1` 一律保存为 **UTF-8 with BOM**。Windows PowerShell 5.1 读取无 BOM 的
`.ps1` 时会按系统 ANSI（中文环境为 GBK）解码，脚本里的中文注释会变成乱码并可能连带
吞掉引号/换行，导致整段解析失败（报 `Missing closing '}'` / `The string is missing the
terminator`）。这类问题只在 Windows PowerShell 5.1 上出现，`pwsh` 7 无此限制。

新增或修改 `.ps1` 后建议自查一次：

```powershell
[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path .\x.ps1), [ref]$null, [ref]$e)
$e.Count   # 应为 0
```

另外，Maven 构建（`mvn clean`）无法删除正在被运行中服务占用的 `*-exec.jar`，
重新构建前请先停止对应服务（`stop-all.ps1`）。

### 资源要求

7 个微服务 + PostgreSQL / Redis / Nacos / RabbitMQ / MinIO 同时运行，建议至少
**8 GB 可用内存**。`start-all.ps1` 已为每个 Java 进程加上 `-Xms128m -Xmx512m`：
不加约束时 JVM 会按宿主机总内存推算堆上限（24 GB 机器上单进程可预留数 GB），
并发启动时容易把内存吃光，最后启动的服务会因分配不到内存而起不来，日志里表现为
`Failed to start bean 'webServerStartStop'`（真实原因常被日志框架的报错盖住）。

若某个服务启动失败，`start-all.ps1` 会自动重试一次；仍失败时请检查：

```powershell
# 看是否有残留进程占着端口
Get-NetTCPConnection -LocalPort 8080 -State Listen

# 看该服务的真实启动错误
Get-Content backend/meowflow/logs/gateway-error.log -Tail 40
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
# 后端：单元 + 集成测试（需要 PostgreSQL / Redis，空库即可，Flyway 会自动建表）
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

### 端到端校验

服务全部起来后（`backend/meowflow/start-all.ps1` + 前端 `npm run dev`），可跑一次真实链路校验：

```powershell
# 仓库根目录：基础设施 -> 服务健康 -> Nacos 注册 -> 前端代理 -> 业务链路
./verify-e2e.ps1

# 只跑业务链路（登录 -> 创建工作流 -> 保存版本 -> 发布 -> 执行 -> 用模板创建）
cd frontend/meowflow-ui
npm run test:e2e
```

`verify-e2e.ps1` 需要 Docker、Node.js，以及在运行的 7 个后端服务；
它会自动创建并清理测试工作流，可安全重复执行。

CI 配置见 [.github/workflows/ci.yml](./.github/workflows/ci.yml)：
`backend`（PG + Redis service）、`frontend`、`package` 三个 job。
端到端校验需要完整服务栈，未纳入 CI，请按上面步骤在本地执行。

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
