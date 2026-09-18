# MeowFlow 数据库初始化

## 单一来源

数据库结构按版本维护在：

`backend/meowflow/meowflow-common/src/main/resources/db/migration/V*.sql`

完整初始化脚本：

`backend/meowflow/scripts/sql/init.sql`

`init.sql` 由 V1–V20 迁移按顺序生成，不要手工修改。修改 migration 后运行：`pwsh scripts/sql/generate-init.ps1`。

## Docker 初始化

`docker-compose.dev.yml` 和 `docker-compose.full.yml` 会将 `init.sql` 挂载为：

`/docker-entrypoint-initdb.d/zz-meowflow-init.sql`

PostgreSQL 数据卷首次创建时会自动执行。已存在的数据卷不会自动重建，需执行迁移或清库重建。

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


