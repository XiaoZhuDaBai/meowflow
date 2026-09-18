# 喵流 MeowFlow - 数据库脚本

> 本目录是 PostgreSQL 数据库初始化与维护脚本。
> 仅供项目使用;数据无价,生产环境请先备份再跑。

## 目录结构

```
scripts/sql/
├── README.md                # 本文件
├── 00-init.sql              # 数据库创建 + 公共函数
├── 01-schema.sql            # 全部业务表 DDL
├── 02-index.sql             # 索引 + 扩展
├── 03-data.sql              # 初始化数据(管理员、菜单、模型)
├── reset.sql                # ⚠️ 清空全部表(仅本地开发)
└── apply.sh                 # 一键初始化脚本
```

## 命名规范

**品牌前缀 + 模块前缀 + 实体名**：

```
mf_<module>_<entity>
│   │       │
│   │       └─ 业务实体名(下划线分隔)
│   └─ 模块缩写
└─ MeowFlow 品牌前缀
```

| 模块 | 缩写 | 范围 |
|------|------|------|
| 系统（用户/权限/组织） | `sys` | mf_sys_* |
| 工作流 | `wf`  | mf_wf_* |
| 模板市场 | `tpl` | mf_tpl_* |
| AI / 知识库 | `ai` | mf_ai_* |
| 集成（通知/MCP） | `int` | mf_int_* |
| 监控（告警/指标） | `mon` | mf_mon_* |

## 执行顺序

```bash
# 1. 首次初始化
psql -U postgres -d postgres  -f 00-init.sql
psql -U postgres -d meowflow -f 01-schema.sql
psql -U postgres -d meowflow -f 02-index.sql
psql -U postgres -d meowflow -f 03-data.sql

# 或者一行搞定
./apply.sh
```

## 重置（仅 dev）

```bash
psql -U postgres -d meowflow -f reset.sql
```

## 表清单（2026-07-12 现行版）

| 模块 | 表 | 说明 |
|------|----|------|
| sys | mf_sys_org | 组织 |
| sys | mf_sys_post | 岗位 |
| sys | mf_sys_user | 用户 |
| sys | mf_sys_role | 角色 |
| sys | mf_sys_user_role | 用户角色关联 |
| sys | mf_sys_permission | 权限（菜单/按钮/接口） |
| sys | mf_sys_role_permission | 角色权限关联 |
| sys | mf_sys_login_log | 登录日志 |
| sys | mf_sys_audit_log | 操作审计 |
| wf | mf_wf_category | 工作流分类 |
| wf | mf_wf_group | 工作流分组 |
| wf | mf_wf_workflow | 工作流主表 |
| wf | mf_wf_workflow_version | 工作流版本 |
| wf | mf_wf_execution | 执行记录 |
| wf | mf_wf_node_execution | 节点执行明细 |
| wf | mf_wf_execution_log | 执行日志 |
| tpl | mf_tpl_template | 模板主表 |
| tpl | mf_tpl_review | 模板评价 |
| mon | mf_mon_alert_rule | 告警规则 |
| mon | mf_mon_alert | 告警事件 |
| mon | mf_mon_metric_daily | 每日指标 |
| ai | mf_ai_model | AI 模型配置 |
| ai | mf_ai_knowledge | 知识库 |
| ai | mf_ai_document | 知识库文档 |
| ai | mf_ai_chunk | 文档分块 |
| ai | mf_ai_invoke_log | AI 调用日志 |
| int | mf_int_config | 集成配置 |
| int | mf_int_send_log | 发送日志 |
| int | mf_int_mcp_tool | MCP 工具注册 |

**共 29 张表**

## 命名迁移说明

- **旧名（已废弃）**：曾计划 `pig_xxx` / `pigflow.xxx`，但实际未落地任何脚本，所以这次是**首版正式命名**，不需要写迁移脚本。
- 早期文档（如 `docs/technical/database.md`）中出现的 `sys_*` / `wf_*` 是**模块前缀**（不含品牌前缀），本文档正式引入 `mf_` 品牌前缀做唯一标识。
- 历史 PRD/架构文档中的命名会逐步对齐到本规范。

## 默认账号

| 字段 | 值 |
|------|-----|
| 用户名 | `admin` |
| 初始密码 | `meow@2026` |
| 角色 | `super_admin` |

> ⚠️ **生产环境部署后请立即修改密码**。
> 03-data.sql 中插入的密码 hash 是 BCrypt 示例值,如果 `sa-token` 或 Spring Security 拒绝登录，请应用层重置。

## 扩展依赖

| 扩展 | 必需？ | 用途 |
|------|--------|------|
| `uuid-ossp` | 是 | UUID 生成 |
| `pgcrypto` | 是 | 加密 |
| `btree_gist` | 否 | GIST 增强 |
| `pg_trgm` | 否（推荐） | 工作流名称模糊检索 |
| `pgvector` | 否（推荐） | 知识库向量索引 |

如果未安装 `pgvector`，`mf_ai_chunk.embedding` 字段需要手动改为 `BYTEA`。
