# MeowFlow 数据库初始化

## 单一来源

数据库结构按版本维护在：

`backend/meowflow/meowflow-common/src/main/resources/db/migration/V*.sql`

完整初始化脚本：

`backend/meowflow/scripts/sql/init.sql`

`init.sql` 由 `db/migration` 下的 `V*.sql` 按版本顺序生成（当前至 V22），不要手工修改。修改 migration 后在 `backend/meowflow` 目录运行：`pwsh scripts/sql/generate-init.ps1`。

## Docker 初始化

**schema 由 Flyway 在服务启动时创建**，这是唯一来源。

`docker-compose.dev.yml` / `docker-compose.full.yml` 早期版本会把 `init.sql` 挂载为
`/docker-entrypoint-initdb.d/zz-meowflow-init.sql`，让 PostgreSQL 首次建卷时执行。这样做会和
Flyway 争抢同一套表结构，报 `relation "mf_sys_org" already exists` 导致服务起不来，
该挂载已移除，`init.sql` 现在只作为阅读与手工排查用途。

因此：**新建空库直接启动服务即可**，不需要先手工跑 `init.sql`。

## 本地重置

```powershell
docker exec -i meowflow-postgres psql -U meowflow -d meowflow -f - < scripts/sql/drop.sql
Get-Content -Raw scripts/sql/init.sql |
  docker exec -i meowflow-postgres psql -U meowflow -d meowflow -v ON_ERROR_STOP=1 -f -
```

## 规范

- Java 实体使用 `@TableName` / `@TableField` 作为字段契约。
- 新增实体字段时必须同时新增 migration。
- migration 必须可在空库按版本顺序执行，并尽量保持幂等。
- 不使用固定负数 ID、手工改主键或每个环境单独补字段。
- 审计字段由 Java MetaObjectHandler 或数据库触发器维护，二者不能互相冲突。
- 测试库与开发库都必须通过同一套 migration 创建。

## 当前模块表

- 系统：`mf_sys_*`
- 工作流：`mf_wf_*`
- 模板：`mf_tpl_*`
- 监控：`mf_mon_*`
- AI / 集成 / 执行器：`mf_ai_*`、`mf_integration_config`、`mf_exe_*`
- 知识库：`knowledge_base`、`document`、`document_chunk`

## 开发账号

- 用户名：`admin`
- 密码：`meow@2026`
- 仅限开发环境，部署生产前必须修改并轮换密钥。


