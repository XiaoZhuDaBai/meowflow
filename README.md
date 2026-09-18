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
- Node.js 18+
- PostgreSQL 13+
- Redis 6+
- RabbitMQ 3.8+

### 安装部署

```bash
# 1. 克隆代码
git clone https://github.com/your-org/meowflow.git
cd meowflow

# 2. 启动后端
cd backend/meowflow
./mvnw spring-boot:run

# 3. 启动前端
cd frontend/meowflow-ui
npm install
npm run dev
```

### Docker 部署

```bash
# 使用 Docker Compose 一键启动
docker-compose -f docker/docker-compose.yml up -d
```

## 技术栈

### 后端
- Java 18+
- Spring Boot 3.x
- Spring Cloud Alibaba
- MyBatis-Plus
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

数据库初始化脚本见 [scripts/sql/](./scripts/sql/README.md)。

```bash
cd scripts/sql
./apply.sh            # 一键初始化(默认连 localhost postgres)
psql -d meowflow -f reset.sql   # 清空业务表(仅本地)
```

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
