# 喵流 (MeowFlow) - 数据库设计文档

> 本文档详细描述喵流平台的数据库表结构设计

---

## 一、数据库概览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          数据库信息                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  数据库类型：PostgreSQL 15+                                             │
│  数据库名称：MeowFlow                                                    │
│  字符集：UTF8                                                          │
│  排序规则：zh_CN.UTF-8                                                  │
│                                                                          │
│  命名规范：                                                            │
│  ├─ 表名：小写下划线，模块前缀，如 wf_workflow, sys_user               │
│  ├─ 字段名：小写下划线，如 user_id, create_time                       │
│  ├─ 主键：id，UUID 或自增 bigint                                       │
│  └─ 索引：idx_字段名，uk_字段名（唯一索引）                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、ER 图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          数据库 ER 图                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│                        ┌─────────────────┐                              │
│                        │   sys_user      │                              │
│                        │     用户表       │                              │
│                        └────────┬────────┘                              │
│                                 │                                       │
│                                 │ 1:N                                   │
│                                 ▼                                       │
│     ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐      │
│     │  sys_user_role   │ │    sys_org     │ │  sys_user_org  │      │
│     │   用户角色关联   │ │    组织表      │ │   用户组织关联  │      │
│     └────────┬─────────┘ └────────┬────────┘ └────────┬────────┘      │
│              │                     │                    │               │
│              │ 1:N                │ 1:N                │ 1:N          │
│              ▼                    ▼                    ▼               │
│     ┌─────────────────┐ ┌─────────────────┐                       │
│     │    sys_role     │ │  sys_user_post  │                       │
│     │     角色表      │ │    用户岗位表    │                       │
│     └────────┬─────────┘ └─────────────────┘                       │
│              │                                                       │
│              │ 1:N                                                 │
│              ▼                                                       │
│     ┌─────────────────┐                                             │
│     │sys_role_permission│                                           │
│     │   角色权限关联  │                                             │
│     └────────┬─────────┘                                             │
│              │                                                       │
│              │ N:1                                                   │
│              ▼                                                       │
│     ┌─────────────────┐                                             │
│     │sys_permission   │                                             │
│     │    权限表      │                                             │
│     └─────────────────┘                                             │
│                                                                          │
│                                                                          │
│     ┌─────────────────┐    ┌─────────────────┐                        │
│     │ wf_category     │    │wf_workflow_group│                        │
│     │  工作流分类表   │    │   工作流分组表   │                        │
│     └────────┬────────┘    └────────┬────────┘                        │
│              │                      │                                  │
│              │ 1:N                  │ 1:N                             │
│              ▼                      ▼                                  │
│     ┌─────────────────────────────────────────┐                       │
│     │            wf_workflow                  │                       │
│     │              工作流表                    │                       │
│     └────────────────────┬───────────────────┘                       │
│                          │                                           │
│                          │ 1:N                                       │
│                          ▼                                           │
│     ┌─────────────────────────────────────────┐                       │
│     │      wf_workflow_version                │                       │
│     │           工作流版本表                   │                       │
│     └────────────────────┬───────────────────┘                       │
│                          │                                           │
│                          │ 1:N                                       │
│                          ▼                                           │
│     ┌─────────────────────────────────────────┐                       │
│     │          wf_execution                  │                       │
│     │            执行记录表                    │                       │
│     └────────────────────┬───────────────────┘                       │
│                          │                                           │
│                          │ 1:N                                       │
│                          ▼                                           │
│     ┌─────────────────────────────────────────┐ ┌─────────────────┐  │
│     │       wf_node_execution                 │ │ wf_execution_log│  │
│     │         节点执行记录表                  │ │    执行日志表   │  │
│     └─────────────────────────────────────────┘ └─────────────────┘  │
│                                                                          │
│                                                                          │
│     ┌─────────────────┐                                             │
│     │  wf_template    │                                             │
│     │     模板表      │                                             │
│     └────────┬────────┘                                             │
│              │                                                       │
│              │ 1:N                                                   │
│              ▼                                                       │
│     ┌─────────────────┐                                             │
│     │wf_template_review│                                             │
│     │     模板评价表   │                                             │
│     └─────────────────┘                                             │
│                                                                          │
│                                                                          │
│     ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐│
│     │ ai_model_config │    │integration_config│    │  alert_rule     ││
│     │   AI模型配置表   │    │  第三方集成配置表 │    │   告警规则表    ││
│     └─────────────────┘    └─────────────────┘    └────────┬────────┘│
│                                                          │            │
│                                                          │ 1:N        │
│                                                          ▼            │
│                            ┌─────────────────┐ ┌─────────────────┐  │
│                            │ alert_record    │ │ alert_channel   │  │
│                            │   告警记录表    │ │   告警渠道表    │  │
│                            └─────────────────┘ └─────────────────┘  │
│                                                                          │
│                                                                          │
│     ┌─────────────────┐                                             │
│     │  sys_dict_type  │                                             │
│     │    字典类型表   │                                             │
│     └────────┬────────┘                                             │
│              │                                                       │
│              │ 1:N                                                   │
│              ▼                                                       │
│     ┌─────────────────┐                                             │
│     │  sys_dict_data  │                                             │
│     │    字典数据表   │                                             │
│     └─────────────────┘                                             │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 三、表结构详细设计

### 3.1 用户与权限模块

#### sys_user 用户表

```sql
-- 用户表
CREATE TABLE sys_user (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    nick_name       VARCHAR(100),
    email           VARCHAR(100) UNIQUE,
    phone           VARCHAR(20) UNIQUE,
    avatar          VARCHAR(500),
    sex             VARCHAR(10) DEFAULT 'unknown',  -- unknown/male/female
    status          VARCHAR(10) DEFAULT '1',        -- 1:启用 0:禁用
    login_ip        VARCHAR(50),
    login_date      TIMESTAMP,
    login_count     INTEGER DEFAULT 0,
    create_dept_id  VARCHAR(32),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500),
    
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_phone UNIQUE (phone)
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户账号';
COMMENT ON COLUMN sys_user.password IS '密码';
COMMENT ON COLUMN sys_user.nick_name IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.avatar IS '头像';
COMMENT ON COLUMN sys_user.sex IS '性别';
COMMENT ON COLUMN sys_user.status IS '状态（1正常 0停用）';
COMMENT ON COLUMN sys_user.login_ip IS '最后登录IP';
COMMENT ON COLUMN sys_user.login_date IS '最后登录时间';
COMMENT ON COLUMN sys_user.login_count IS '登录次数';

-- 索引
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_user_create_time ON sys_user(create_time);
```

#### sys_org 组织表

```sql
-- 组织表
CREATE TABLE sys_org (
    id          VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    parent_id   VARCHAR(32) DEFAULT '0',
    ancestors    VARCHAR(500) DEFAULT '',
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(100),
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    sort        INTEGER DEFAULT 0,
    status      VARCHAR(10) DEFAULT '1',
    create_dept VARCHAR(32),
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(32),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(500)
);

COMMENT ON TABLE sys_org IS '组织表';
COMMENT ON COLUMN sys_org.id IS '组织id';
COMMENT ON COLUMN sys_org.parent_id IS '父组织id';
COMMENT ON COLUMN sys_org.ancestors IS '祖级列表';
COMMENT ON COLUMN sys_org.name IS '组织名称';
COMMENT ON COLUMN sys_org.code IS '组织编码';
COMMENT ON COLUMN sys_org.leader IS '负责人';
COMMENT ON COLUMN sys_org.sort IS '显示顺序';

-- 索引
CREATE INDEX idx_org_parent ON sys_org(parent_id);
CREATE INDEX idx_org_status ON sys_org(status);
```

#### sys_role 角色表

```sql
-- 角色表
CREATE TABLE sys_role (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(100) NOT NULL UNIQUE,
    sort            INTEGER DEFAULT 0,
    data_scope      VARCHAR(10) DEFAULT '1',  -- 1:全部 2:本部门 3:本部门及以下 4:仅本人 5:自定义
    menu_check_strictly BOOLEAN DEFAULT TRUE,
    dept_check_strictly BOOLEAN DEFAULT TRUE,
    status          VARCHAR(10) DEFAULT '1',
    create_dept     VARCHAR(32),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围';

-- 索引
CREATE UNIQUE INDEX uk_role_code ON sys_role(code);
```

#### sys_permission 权限表

```sql
-- 菜单权限表
CREATE TABLE sys_permission (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(50) NOT NULL,
    pid             VARCHAR(32) DEFAULT '0',
    path            VARCHAR(200),
    component       VARCHAR(255),
    component_name  VARCHAR(100),
    redirect        VARCHAR(255),
    menu_type       VARCHAR(10) NOT NULL,  -- M:目录 C:菜单 F:按钮
    visible         VARCHAR(10) DEFAULT '1',  -- 1:显示 0:隐藏
    status          VARCHAR(10) DEFAULT '1',
    perms           VARCHAR(100),
    perms_type      VARCHAR(10) DEFAULT '1',  -- 1:可见 2:停用
    icon            VARCHAR(100),
    sort             INTEGER DEFAULT 0,
    is_route        INTEGER DEFAULT 1,
    keep_alive      INTEGER DEFAULT 0,
    always_show     INTEGER DEFAULT 1,
    internal_or_external INTEGER DEFAULT 0,
    create_dept     VARCHAR(32),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE sys_permission IS '菜单权限表';
COMMENT ON COLUMN sys_permission.menu_type IS '菜单类型（M目录 C菜单 F按钮）';
COMMENT ON COLUMN sys_permission.perms IS '权限标识';

-- 索引
CREATE INDEX idx_permission_pid ON sys_permission(pid);
CREATE INDEX idx_permission_menu_type ON sys_permission(menu_type);
```

#### sys_user_role 用户角色关联表

```sql
-- 用户和角色关联表
CREATE TABLE sys_user_role (
    user_id     VARCHAR(32) NOT NULL,
    role_id     VARCHAR(32) NOT NULL,
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_role IS '用户和角色关联表';
```

#### sys_role_permission 角色权限关联表

```sql
-- 角色和菜单权限关联表
CREATE TABLE sys_role_permission (
    role_id     VARCHAR(32) NOT NULL,
    permission_id VARCHAR(32) NOT NULL,
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE sys_role_permission IS '角色和菜单权限关联表';
```

#### sys_user_org 用户组织关联表

```sql
-- 用户组织关联表
CREATE TABLE sys_user_org (
    user_id     VARCHAR(32) NOT NULL,
    org_id      VARCHAR(32) NOT NULL,
    is_default  INTEGER DEFAULT 0,  -- 是否默认组织
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, org_id)
);

COMMENT ON TABLE sys_user_org IS '用户组织关联表';
```

---

### 3.2 工作流模块

#### wf_workflow 工作流表

```sql
-- 工作流表
CREATE TABLE wf_workflow (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    definition       JSONB NOT NULL DEFAULT '{}',  -- 工作流定义JSON
    version         INTEGER DEFAULT 1,
    status          VARCHAR(20) DEFAULT 'draft',  -- draft/active/stopped
    category        VARCHAR(50),                    -- 分类
    tags            JSONB DEFAULT '[]',           -- 标签数组
    org_id          VARCHAR(32),                   -- 所属组织
    user_id         VARCHAR(32),                   -- 创建人
    trigger_type    VARCHAR(30) DEFAULT 'manual', -- webhook/schedule/manual/form
    trigger_config  JSONB DEFAULT '{}',            -- 触发器配置
    webhook_url     VARCHAR(500),                  -- Webhook URL
    webhook_secret  VARCHAR(100),                  -- Webhook 签名密钥
    execute_count   INTEGER DEFAULT 0,              -- 执行次数
    success_count   INTEGER DEFAULT 0,              -- 成功次数
    fail_count      INTEGER DEFAULT 0,              -- 失败次数
    success_rate    DECIMAL(5,2) DEFAULT 0,        -- 成功率
    total_cost      DECIMAL(20,8) DEFAULT 0,       -- 总成本
    last_execute_time TIMESTAMP,                    -- 最后执行时间
    is_template     INTEGER DEFAULT 0,             -- 是否为模板
    is_public       INTEGER DEFAULT 0,             -- 是否公开
    sort            INTEGER DEFAULT 0,
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    remark          VARCHAR(1000)
);

COMMENT ON TABLE wf_workflow IS '工作流表';
COMMENT ON COLUMN wf_workflow.definition IS '工作流定义JSON（nodes + edges）';
COMMENT ON COLUMN wf_workflow.trigger_type IS '触发类型（webhook/schedule/manual/form）';
COMMENT ON COLUMN wf_workflow.trigger_config IS '触发器配置JSON';

-- 索引
CREATE INDEX idx_workflow_org ON wf_workflow(org_id);
CREATE INDEX idx_workflow_user ON wf_workflow(user_id);
CREATE INDEX idx_workflow_status ON wf_workflow(status);
CREATE INDEX idx_workflow_category ON wf_workflow(category);
CREATE INDEX idx_workflow_create_time ON wf_workflow(create_time);
CREATE INDEX idx_workflow_deleted ON wf_workflow(deleted);
CREATE INDEX idx_workflow_definition ON wf_workflow USING GIN(definition);
```

#### wf_workflow_version 工作流版本表

```sql
-- 工作流版本表
CREATE TABLE wf_workflow_version (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    workflow_id     VARCHAR(32) NOT NULL,
    version         INTEGER NOT NULL,
    definition       JSONB NOT NULL DEFAULT '{}',
    change_log      VARCHAR(1000),
    user_id         VARCHAR(32),
    is_published    INTEGER DEFAULT 0,  -- 是否已发布
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_workflow_version UNIQUE (workflow_id, version)
);

COMMENT ON TABLE wf_workflow_version IS '工作流版本表';

-- 索引
CREATE INDEX idx_version_workflow ON wf_workflow_version(workflow_id);
```

#### wf_workflow_group 工作流分组表

```sql
-- 工作流分组表
CREATE TABLE wf_workflow_group (
    id          VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name        VARCHAR(100) NOT NULL,
    parent_id   VARCHAR(32) DEFAULT '0',
    ancestors   VARCHAR(500) DEFAULT '',
    icon        VARCHAR(100),
    sort        INTEGER DEFAULT 0,
    org_id      VARCHAR(32),
    user_id     VARCHAR(32),
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(32),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(500)
);

COMMENT ON TABLE wf_workflow_group IS '工作流分组表';

-- 索引
CREATE INDEX idx_group_org ON wf_workflow_group(org_id);
CREATE INDEX idx_group_parent ON wf_workflow_group(parent_id);
```

#### wf_category 工作流分类表

```sql
-- 工作流分类表
CREATE TABLE wf_category (
    id          VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(100) UNIQUE,
    icon        VARCHAR(100),
    color       VARCHAR(20),
    sort        INTEGER DEFAULT 0,
    status      VARCHAR(10) DEFAULT '1',
    create_by   VARCHAR(32),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(32),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(500)
);

COMMENT ON TABLE wf_category IS '工作流分类表';

-- 索引
CREATE UNIQUE INDEX uk_category_code ON wf_category(code);
```

---

### 3.3 执行记录模块

#### wf_execution 执行记录表

```sql
-- 执行记录表
CREATE TABLE wf_execution (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    workflow_id     VARCHAR(32) NOT NULL,
    workflow_name   VARCHAR(200),
    workflow_version INTEGER DEFAULT 1,
    trigger_type    VARCHAR(30),                  -- webhook/schedule/manual
    trigger_source  VARCHAR(200),                  -- 触发来源
    trigger_data    JSONB DEFAULT '{}',            -- 触发数据（输入）
    status          VARCHAR(20) DEFAULT 'waiting',  -- waiting/running/success/failed/cancelled
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration        BIGINT DEFAULT 0,               -- 耗时（毫秒）
    cost            DECIMAL(20,8) DEFAULT 0,        -- 成本
    input_data      JSONB DEFAULT '{}',             -- 输入数据
    output_data     JSONB DEFAULT '{}',             -- 输出数据
    error_message   TEXT,
    user_id         VARCHAR(32),
    org_id          VARCHAR(32),
    trace_id        VARCHAR(64),                    -- 链路追踪ID
    parent_id       VARCHAR(32),                    -- 父执行ID（子流程）
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE wf_execution IS '执行记录表';
COMMENT ON COLUMN wf_execution.status IS '状态（waiting/running/success/failed/cancelled）';
COMMENT ON COLUMN wf_execution.duration IS '执行耗时（毫秒）';

-- 索引
CREATE INDEX idx_execution_workflow ON wf_execution(workflow_id);
CREATE INDEX idx_execution_status ON wf_execution(status);
CREATE INDEX idx_execution_user ON wf_execution(user_id);
CREATE INDEX idx_execution_create_time ON wf_execution(create_time);
CREATE INDEX idx_execution_trigger_type ON wf_execution(trigger_type);
CREATE INDEX idx_execution_trace ON wf_execution(trace_id);
```

#### wf_node_execution 节点执行记录表

```sql
-- 节点执行记录表
CREATE TABLE wf_node_execution (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    execution_id    VARCHAR(32) NOT NULL,
    node_id         VARCHAR(100) NOT NULL,         -- 节点ID
    node_name       VARCHAR(200),                   -- 节点名称
    node_type       VARCHAR(50),                    -- 节点类型
    status          VARCHAR(20) DEFAULT 'waiting', -- waiting/running/success/failed/skipped
    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    duration        BIGINT DEFAULT 0,
    input_data      JSONB DEFAULT '{}',             -- 输入数据
    output_data     JSONB DEFAULT '{}',             -- 输出数据
    error_message   TEXT,
    error_stack     TEXT,
    retry_count     INTEGER DEFAULT 0,              -- 重试次数
    cost            DECIMAL(20,8) DEFAULT 0,        -- 成本
    token_input     INTEGER DEFAULT 0,              -- 输入Token
    token_output    INTEGER DEFAULT 0,              -- 输出Token
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_execution FOREIGN KEY (execution_id) 
        REFERENCES wf_execution(id) ON DELETE CASCADE
);

COMMENT ON TABLE wf_node_execution IS '节点执行记录表';

-- 索引
CREATE INDEX idx_node_execution ON wf_node_execution(execution_id);
CREATE INDEX idx_node_status ON wf_node_execution(status);
CREATE INDEX idx_node_create_time ON wf_node_execution(create_time);
```

#### wf_execution_log 执行日志表

```sql
-- 执行日志表
CREATE TABLE wf_execution_log (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    execution_id    VARCHAR(32) NOT NULL,
    node_id         VARCHAR(100),
    node_name       VARCHAR(200),
    level           VARCHAR(10) DEFAULT 'INFO',   -- DEBUG/INFO/WARN/ERROR
    type            VARCHAR(30),                   -- START/NODE/COND/TOKEN/END
    message         TEXT,
    detail          JSONB,
    sort            INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_log_execution FOREIGN KEY (execution_id) 
        REFERENCES wf_execution(id) ON DELETE CASCADE
);

COMMENT ON TABLE wf_execution_log IS '执行日志表';

-- 索引
CREATE INDEX idx_log_execution ON wf_execution_log(execution_id);
CREATE INDEX idx_log_node ON wf_execution_log(node_id);
CREATE INDEX idx_log_level ON wf_execution_log(level);
CREATE INDEX idx_log_create_time ON wf_execution_log(create_time);
```

---

### 3.4 模板模块

#### wf_template 模板表

```sql
-- 模板表
CREATE TABLE wf_template (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    definition       JSONB NOT NULL DEFAULT '{}',
    cover_image     VARCHAR(500),                  -- 封面图
    category        VARCHAR(50),
    tags            JSONB DEFAULT '[]',
    status          VARCHAR(20) DEFAULT 'pending', -- pending/approved/rejected/offline
    use_count       INTEGER DEFAULT 0,             -- 使用次数
    view_count      INTEGER DEFAULT 0,             -- 浏览次数
    rating          DECIMAL(3,2) DEFAULT 0,       -- 平均评分
    review_count    INTEGER DEFAULT 0,             -- 评价数量
    official        INTEGER DEFAULT 0,              -- 是否官方模板
    public_template INTEGER DEFAULT 1,              -- 是否公开模板
    user_id         VARCHAR(32),
    org_id          VARCHAR(32),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    remark          VARCHAR(1000)
);

COMMENT ON TABLE wf_template IS '模板表';
COMMENT ON COLUMN wf_template.status IS '状态（pending/approved/rejected/offline）';
COMMENT ON COLUMN wf_template.official IS '是否官方模板（0否 1是）';
COMMENT ON COLUMN wf_template.public_template IS '是否公开模板（0否 1是）';

-- 索引
CREATE INDEX idx_template_category ON wf_template(category);
CREATE INDEX idx_template_status ON wf_template(status);
CREATE INDEX idx_template_user ON wf_template(user_id);
CREATE INDEX idx_template_deleted ON wf_template(deleted);
CREATE INDEX idx_template_official ON wf_template(official);
```

#### wf_template_review 模板评价表

```sql
-- 模板评价表
CREATE TABLE wf_template_review (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    template_id     VARCHAR(32) NOT NULL,
    user_id         VARCHAR(32),
    rating          INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    content         VARCHAR(1000),
    is_anonymous    INTEGER DEFAULT 0,
    status          VARCHAR(10) DEFAULT '1',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_template_review FOREIGN KEY (template_id) 
        REFERENCES wf_template(id) ON DELETE CASCADE
);

COMMENT ON TABLE wf_template_review IS '模板评价表';

-- 索引
CREATE INDEX idx_review_template ON wf_template_review(template_id);
CREATE INDEX idx_review_user ON wf_template_review(user_id);
```

---

### 3.5 集成配置模块

#### ai_model_config AI模型配置表

```sql
-- AI模型配置表
CREATE TABLE ai_model_config (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    provider        VARCHAR(50) NOT NULL,         -- openai/anthropic/ali/baidu/zhipu
    model           VARCHAR(100) NOT NULL,
    api_key         VARCHAR(500),                  -- 加密存储
    base_url        VARCHAR(500),
    config          JSONB DEFAULT '{}',            -- 模型参数配置
    price_input     DECIMAL(10,6) DEFAULT 0,      -- 输入价格（元/千token）
    price_output    DECIMAL(10,6) DEFAULT 0,      -- 输出价格（元/千token）
    is_default      INTEGER DEFAULT 0,
    status          VARCHAR(10) DEFAULT '1',
    org_id          VARCHAR(32),
    user_id         VARCHAR(32),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE ai_model_config IS 'AI模型配置表';
COMMENT ON COLUMN ai_model_config.provider IS '模型提供商';
COMMENT ON COLUMN ai_model_config.config IS '模型配置（温度、最大token等）';

-- 索引
CREATE INDEX idx_model_org ON ai_model_config(org_id);
CREATE INDEX idx_model_status ON ai_model_config(status);
CREATE INDEX idx_model_provider ON ai_model_config(provider);
```

#### integration_config 第三方集成配置表

```sql
-- 第三方集成配置表
CREATE TABLE integration_config (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(50) NOT NULL,         -- dingtalk/wxwork/feishu/mail/sms
    config          JSONB NOT NULL DEFAULT '{}',  -- 配置JSON
    status          VARCHAR(10) DEFAULT '1',
    org_id          VARCHAR(32),
    user_id         VARCHAR(32),
    last_test_time  TIMESTAMP,
    last_test_status VARCHAR(10),
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE integration_config IS '第三方集成配置表';
COMMENT ON COLUMN integration_config.type IS '集成类型';

-- 索引
CREATE INDEX idx_integration_org ON integration_config(org_id);
CREATE INDEX idx_integration_type ON integration_config(type);
CREATE INDEX idx_integration_status ON integration_config(status);
```

---

### 3.6 系统配置模块

#### sys_dict_type 字典类型表

```sql
-- 字典类型表
CREATE TABLE sys_dict_type (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(100) UNIQUE,
    description     VARCHAR(500),
    sort            INTEGER DEFAULT 0,
    status          VARCHAR(10) DEFAULT '1',
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE sys_dict_type IS '字典类型表';

-- 索引
CREATE UNIQUE INDEX uk_dict_code ON sys_dict_type(code);
```

#### sys_dict_data 字典数据表

```sql
-- 字典数据表
CREATE TABLE sys_dict_data (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    dict_sort       INTEGER DEFAULT 0,
    dict_label      VARCHAR(100) NOT NULL,
    dict_value      VARCHAR(100) NOT NULL,
    dict_type       VARCHAR(100) NOT NULL,
    css_class       VARCHAR(100),
    list_class      VARCHAR(100),
    is_default      VARCHAR(10) DEFAULT 'N',
    status          VARCHAR(10) DEFAULT '1',
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE sys_dict_data IS '字典数据表';

-- 索引
CREATE INDEX idx_dict_type ON sys_dict_data(dict_type);
CREATE INDEX idx_dict_status ON sys_dict_data(status);
```

---

### 3.7 操作日志模块

#### sys_oper_log 操作日志表

```sql
-- 操作日志表
CREATE TABLE sys_oper_log (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) DEFAULT '',
    business_type    VARCHAR(20),
    method          VARCHAR(200),
    request_method  VARCHAR(10),
    operator_type   VARCHAR(10),
    request_url     VARCHAR(500),
    remote_ip       VARCHAR(50),
    user_agent      VARCHAR(500),
    request_param   TEXT,
    return_param    TEXT,
    status          INTEGER DEFAULT 0,            -- 0:正常 1:异常
    error_msg       TEXT,
    duration         BIGINT,                        -- 耗时（毫秒）
    user_id         VARCHAR(32),
    username        VARCHAR(100),
    org_id          VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_oper_log IS '操作日志表';

-- 索引
CREATE INDEX idx_oper_log_business ON sys_oper_log(business_type);
CREATE INDEX idx_oper_log_user ON sys_oper_log(user_id);
CREATE INDEX idx_oper_log_status ON sys_oper_log(status);
CREATE INDEX idx_oper_log_create_time ON sys_oper_log(create_time);
```

#### sys_login_log 登录日志表

```sql
-- 登录日志表
CREATE TABLE sys_login_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(32),
    username        VARCHAR(100),
    ip              VARCHAR(50),
    login_location  VARCHAR(200),
    browser         VARCHAR(100),
    os              VARCHAR(100),
    status          VARCHAR(10),                    -- success/fail
    msg             VARCHAR(200),
    login_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_login_log IS '登录日志表';

-- 索引
CREATE INDEX idx_login_user ON sys_login_log(user_id);
CREATE INDEX idx_login_status ON sys_login_log(status);
CREATE INDEX idx_login_time ON sys_login_log(login_time);
```

---

### 3.8 告警模块

#### alert_rule 告警规则表

```sql
-- 告警规则表
CREATE TABLE alert_rule (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    alert_type      VARCHAR(50) NOT NULL,         -- execution_success/execution_fail/cost/execution_time
    condition       JSONB NOT NULL DEFAULT '{}',   -- 告警条件JSON
    level           VARCHAR(20) DEFAULT 'medium',  -- low/medium/high/critical
    channels        JSONB DEFAULT '[]',           -- 告警渠道数组
    enabled         INTEGER DEFAULT 1,             -- 是否启用
    silence_period  INTEGER DEFAULT 60,            -- 沉默周期（分钟）
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE alert_rule IS '告警规则表';
COMMENT ON COLUMN alert_rule.alert_type IS '告警类型';
COMMENT ON COLUMN alert_rule.condition IS '告警条件JSON';
COMMENT ON COLUMN alert_rule.level IS '告警级别（low/medium/high/critical）';
COMMENT ON COLUMN alert_rule.channels IS '告警渠道';
COMMENT ON COLUMN alert_rule.silence_period IS '沉默周期（分钟）';

-- 索引
CREATE INDEX idx_alert_rule_type ON alert_rule(alert_type);
CREATE INDEX idx_alert_rule_level ON alert_rule(level);
CREATE INDEX idx_alert_rule_enabled ON alert_rule(enabled);
```

#### alert_record 告警记录表

```sql
-- 告警记录表
CREATE TABLE alert_record (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    rule_id         VARCHAR(32) NOT NULL,
    rule_name       VARCHAR(100),
    level           VARCHAR(20),                  -- low/medium/high/critical
    title           VARCHAR(200),
    content         TEXT,
    workflow_id     VARCHAR(32),
    execution_id    VARCHAR(32),
    status          VARCHAR(20) DEFAULT 'triggered', -- triggered/resolved/silenced
    trigger_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolve_time    TIMESTAMP,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE alert_record IS '告警记录表';
COMMENT ON COLUMN alert_record.level IS '告警级别';
COMMENT ON COLUMN alert_record.status IS '状态（triggered/resolved/silenced）';

-- 索引
CREATE INDEX idx_alert_record_rule ON alert_record(rule_id);
CREATE INDEX idx_alert_record_workflow ON alert_record(workflow_id);
CREATE INDEX idx_alert_record_execution ON alert_record(execution_id);
CREATE INDEX idx_alert_record_status ON alert_record(status);
CREATE INDEX idx_alert_record_trigger_time ON alert_record(trigger_time);
```

#### alert_channel 告警渠道表

```sql
-- 告警渠道表
CREATE TABLE alert_channel (
    id              VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(50) NOT NULL,         -- dingtalk/wxwork/feishu/email/sms
    config          JSONB NOT NULL DEFAULT '{}',  -- 渠道配置JSON
    enabled         INTEGER DEFAULT 1,
    create_by       VARCHAR(32),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(32),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(500)
);

COMMENT ON TABLE alert_channel IS '告警渠道表';
COMMENT ON COLUMN alert_channel.type IS '渠道类型';

-- 索引
CREATE INDEX idx_alert_channel_type ON alert_channel(type);
CREATE INDEX idx_alert_channel_enabled ON alert_channel(enabled);
```

---

## 四、字段类型说明

### 4.1 常用字段类型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          字段类型说明                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ID 字段                                                                │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  用户表/角色表等主表：                                                  │
│  VARCHAR(32) - 使用 UUID，兼容分布式                                   │
│                                                                          │
│  日志表等大数据量表：                                                   │
│  BIGSERIAL - 自增主键，性能更好                                        │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  状态字段                                                                │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  VARCHAR(10) - 字符类型，更灵活                                        │
│  常用值：'1'/'0' 或 'active'/'inactive'                               │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  JSON 字段（PostgreSQL 特有）                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  JSONB - 二进制 JSON，可建索引                                          │
│                                                                          │
│  应用场景：                                                             │
│  • 工作流定义（nodes + edges）                                         │
│  • 触发器配置                                                          │
│  • AI 模型参数                                                          │
│  • 执行输入/输出                                                       │
│                                                                          │
│  示例：                                                                 │
│  CREATE INDEX idx_definition ON wf_workflow USING GIN(definition);       │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  时间字段                                                                │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  TIMESTAMP - 带时区的时间戳                                             │
│  DEFAULT CURRENT_TIMESTAMP - 自动设置创建时间                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、索引规划

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          索引规划                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  高频查询字段：                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  工作流表：                                                             │
│  ├─ idx_workflow_org       → 按组织查询                                │
│  ├─ idx_workflow_user      → 按用户查询                                │
│  ├─ idx_workflow_status    → 按状态筛选                                │
│  ├─ idx_workflow_category  → 按分类筛选                                │
│  └─ idx_workflow_deleted   → 软删除过滤                                │
│                                                                          │
│  执行记录表：                                                           │
│  ├─ idx_execution_workflow → 查看工作流的执行记录                      │
│  ├─ idx_execution_status  → 按状态筛选                                │
│  ├─ idx_execution_user    → 按用户查询                                │
│  └─ idx_execution_create_time → 按时间排序                            │
│                                                                          │
│  模板表：                                                             │
│  ├─ idx_template_category  → 按分类筛选                                │
│  ├─ idx_template_status   → 按状态筛选                                │
│  ├─ idx_template_user     → 按用户查询                                │
│  ├─ idx_template_deleted  → 软删除过滤                                │
│  └─ idx_template_official → 官方模板筛选                               │
│                                                                          │
│  告警规则表：                                                         │
│  ├─ idx_alert_rule_type     → 按告警类型筛选                         │
│  ├─ idx_alert_rule_level    → 按告警级别筛选                         │
│  └─ idx_alert_rule_enabled  → 启用状态筛选                           │
│                                                                          │
│  告警记录表：                                                         │
│  ├─ idx_alert_record_rule       → 按规则查询                         │
│  ├─ idx_alert_record_workflow   → 按工作流查询                       │
│  ├─ idx_alert_record_status     → 按状态筛选                         │
│  └─ idx_alert_record_trigger_time → 按触发时间排序                   │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  JSON 字段索引：                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  工作流定义全文索引：                                                   │
│  CREATE INDEX idx_workflow_definition ON wf_workflow USING GIN(definition); │
│                                                                          │
│  触发器配置索引（可选）：                                               │
│  CREATE INDEX idx_trigger_type ON wf_workflow(trigger_type);            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、分表策略

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          分表策略                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  当前设计：单表                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  说明：MVP阶段数据量有限，暂不进行分表                                   │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  预估数据量：                                                            │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  场景：1000个企业，每个企业10个工作流，每个工作流每天100次执行           │
│                                                                          │
│  表名              │ 单表预估行数   │ 预估大小                           │
│  ───────────────────────────────────────────────────────────────────────│
│  sys_user         │ 10,000        │ < 10 MB                          │
│  wf_workflow      │ 10,000        │ < 100 MB                         │
│  wf_execution     │ 365,000,000  │ ~ 500 GB (按年计算)              │
│  wf_node_execution│ 1,825,000,000│ ~ 2 TB (每执行平均5节点)         │
│  wf_execution_log │ 3,650,000,000│ ~ 3 TB                           │
│                                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  分表建议（v2.0版本）：                                                 │
│  ───────────────────────────────────────────────────────────────────────│
│                                                                          │
│  执行记录表：按月分表                                                    │
│  ├─ wf_execution_202601                                                  │
│  ├─ wf_execution_202602                                                  │
│  └─ ...                                                                 │
│                                                                          │
│  节点执行记录表：按月分表                                                │
│  ├─ wf_node_execution_202601                                             │
│  └─ ...                                                                 │
│                                                                          │
│  日志表：按月分表 + 定期归档                                             │
│  ├─ 6个月内数据保留在分表                                              │
│  └─ 超过6个月归档到冷存储                                              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、初始化数据

### 7.1 字典数据

```sql
-- 工作流状态
INSERT INTO sys_dict_type (name, code, description) VALUES
('工作流状态', 'wf_status', '工作流的运行状态');

INSERT INTO sys_dict_data (dict_label, dict_value, dict_type, dict_sort) VALUES
('草稿', 'draft', 'wf_status', 1),
('运行中', 'active', 'wf_status', 2),
('已停止', 'stopped', 'wf_status', 3);

-- 执行状态
INSERT INTO sys_dict_type (name, code, description) VALUES
('执行状态', 'execution_status', '工作流执行的状态');

INSERT INTO sys_dict_data (dict_label, dict_value, dict_type, dict_sort) VALUES
('等待中', 'waiting', 'execution_status', 1),
('运行中', 'running', 'execution_status', 2),
('成功', 'success', 'execution_status', 3),
('失败', 'failed', 'execution_status', 4),
('已取消', 'cancelled', 'execution_status', 5);

-- 触发类型
INSERT INTO sys_dict_type (name, code, description) VALUES
('触发类型', 'trigger_type', '工作流触发方式');

INSERT INTO sys_dict_data (dict_label, dict_value, dict_type, dict_sort) VALUES
('手动触发', 'manual', 'trigger_type', 1),
('Webhook', 'webhook', 'trigger_type', 2),
('定时执行', 'schedule', 'trigger_type', 3),
('表单提交', 'form', 'trigger_type', 4),
('消息触发', 'message', 'trigger_type', 5);

-- 节点类型
INSERT INTO sys_dict_type (name, code, description) VALUES
('节点类型', 'node_type', '工作流节点类型');

INSERT INTO sys_dict_data (dict_label, dict_value, dict_type, dict_sort) VALUES
('开始节点', 'start', 'node_type', 1),
('结束节点', 'end', 'node_type', 2),
('LLM节点', 'llm', 'node_type', 3),
('条件分支', 'condition', 'node_type', 4),
('循环节点', 'loop', 'node_type', 5),
('HTTP请求', 'http', 'node_type', 6),
('数据库', 'database', 'node_type', 7),
('通知节点', 'notify', 'node_type', 8),
('Webhook', 'webhook', 'node_type', 9),
('代码执行', 'code', 'node_type', 10),
('变量赋值', 'variable', 'node_type', 11),
('模板渲染', 'template', 'node_type', 12);
```

### 7.2 初始管理员

```sql
-- 创建管理员用户
INSERT INTO sys_user (id, username, password, nick_name, email, status) VALUES
('admin', 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE/TU', '管理员', 'admin@MeowFlow.com', '1');

-- 创建管理员角色
INSERT INTO sys_role (id, name, code, role_sort, data_scope) VALUES
('admin_role', '超级管理员', 'admin', 1, '1');

-- 关联管理员和角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
('admin', 'admin_role');
```

---

**文档版本：v1.0**
**最后更新：2026-07-10**
