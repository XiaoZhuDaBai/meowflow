-- =============================================================
-- V1__init_schema.sql
-- 喵流 MeowFlow 初始 Schema
-- 前缀: mf_<module>_<entity>
-- 创建日期: 2026-07-13
-- =============================================================

-- ============================================================
-- M00 系统模块 (mf_sys_*)
--   组织 / 用户 / 角色 / 权限 / 登录日志 / 审计日志
-- ============================================================

-- 组织表
CREATE TABLE mf_sys_org (
    id              BIGSERIAL PRIMARY KEY,
    parent_id       BIGINT       DEFAULT 0,
    name            VARCHAR(64)  NOT NULL,
    code            VARCHAR(64)  NOT NULL UNIQUE,
    sort            INT          DEFAULT 0,
    leader_user_id  BIGINT,
    phone           VARCHAR(32),
    email           VARCHAR(128),
    status          VARCHAR(16)  DEFAULT 'active',
    remark          VARCHAR(255),
    deleted         BOOLEAN      DEFAULT FALSE,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  mf_sys_org IS '组织表';
COMMENT ON COLUMN mf_sys_org.parent_id IS '父组织 id,0 表示顶级';

-- 岗位表
CREATE TABLE mf_sys_post (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    sort        INT          DEFAULT 0,
    status      VARCHAR(16)  DEFAULT 'active',
    remark      VARCHAR(255),
    deleted     BOOLEAN      DEFAULT FALSE,
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_sys_post IS '岗位表';

-- 用户表
CREATE TABLE mf_sys_user (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    nickname       VARCHAR(64),
    email          VARCHAR(128),
    phone          VARCHAR(32),
    avatar         VARCHAR(255),
    real_name      VARCHAR(64),
    id_card        VARCHAR(32),
    status         VARCHAR(16)   DEFAULT 'active',
    last_login_ip  VARCHAR(64),
    last_login_at  TIMESTAMP,
    org_id         BIGINT,
    deleted        BOOLEAN       DEFAULT FALSE,
    create_by      BIGINT,
    create_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT,
    update_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  mf_sys_user IS '用户表';
COMMENT ON COLUMN mf_sys_user.password IS 'BCrypt 加密后的密码';

-- 角色表
CREATE TABLE mf_sys_role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(64)  NOT NULL,
    data_scope  VARCHAR(16)  DEFAULT 'all',
    sort        INT          DEFAULT 0,
    status      VARCHAR(16)  DEFAULT 'active',
    remark      VARCHAR(255),
    deleted     BOOLEAN      DEFAULT FALSE,
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_sys_role IS '角色表';

-- 用户-角色关联
CREATE TABLE mf_sys_user_role (
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    org_id      BIGINT DEFAULT 0 NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id, org_id)
);
COMMENT ON TABLE mf_sys_user_role IS '用户-角色关联表';

-- 菜单/权限表
CREATE TABLE mf_sys_permission (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       DEFAULT 0,
    type        VARCHAR(16)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    code        VARCHAR(128),
    path        VARCHAR(255),
    icon        VARCHAR(64),
    sort        INT          DEFAULT 0,
    status      VARCHAR(16)  DEFAULT 'active',
    remark      VARCHAR(255),
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_sys_permission IS '权限(菜单/按钮/接口)表';

-- 角色-权限关联
CREATE TABLE mf_sys_role_permission (
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
COMMENT ON TABLE mf_sys_role_permission IS '角色-权限关联表';

-- 登录日志
CREATE TABLE mf_sys_login_log (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    username     VARCHAR(64),
    ip           VARCHAR(64),
    user_agent   VARCHAR(512),
    region       VARCHAR(64),
    status       VARCHAR(16),
    message      VARCHAR(255),
    login_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_sys_login_log IS '登录日志表';

-- 操作审计
CREATE TABLE mf_sys_audit_log (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    username     VARCHAR(64),
    module       VARCHAR(64),
    action       VARCHAR(64),
    request_url  VARCHAR(512),
    method       VARCHAR(8),
    params       JSONB,
    result       TEXT,
    cost_ms      INT,
    ip           VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  mf_sys_audit_log IS '操作审计日志';
COMMENT ON COLUMN mf_sys_audit_log.params IS '请求参数 JSONB';


-- ============================================================
-- M01 工作流模块 (mf_wf_*)
-- ============================================================

-- 工作流分类
CREATE TABLE mf_wf_category (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    code        VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    icon        VARCHAR(255),
    sort        INT DEFAULT 0,
    status      VARCHAR(16) DEFAULT 'active',
    create_by   BIGINT,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_category IS '工作流分类表';

-- 工作流分组
CREATE TABLE mf_wf_group (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(64) NOT NULL,
    sort        INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_group IS '工作流分组表(用户私有)';

-- 工作流主表
CREATE TABLE mf_wf_workflow (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT,
    group_id        BIGINT,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(128) UNIQUE,
    description     TEXT,
    icon            VARCHAR(255),
    status          VARCHAR(16)  DEFAULT 'draft',
    current_version VARCHAR(16)  DEFAULT 'v1',
    owner_id        BIGINT       NOT NULL,
    org_id          BIGINT,
    is_public       BOOLEAN      DEFAULT FALSE,
    tags            JSONB,
    stat_total_run  BIGINT       DEFAULT 0,
    stat_last_run_at TIMESTAMP,
    deleted         BOOLEAN      DEFAULT FALSE,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  mf_wf_workflow IS '工作流主表';
COMMENT ON COLUMN mf_wf_workflow.tags IS '标签数组';

-- 工作流版本
CREATE TABLE mf_wf_workflow_version (
    id            BIGSERIAL PRIMARY KEY,
    workflow_id   BIGINT  NOT NULL,
    version       VARCHAR(16) NOT NULL,
    definition    JSONB   NOT NULL,
    input_schema  JSONB,
    output_schema JSONB,
    dsl_text      TEXT,
    changelog     TEXT,
    publish_status VARCHAR(16) DEFAULT 'draft',
    published_at  TIMESTAMP,
    published_by  BIGINT,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(workflow_id, version)
);
COMMENT ON TABLE  mf_wf_workflow_version IS '工作流版本表';
COMMENT ON COLUMN mf_wf_workflow_version.definition IS '节点(nodes) + 连线(edges) JSON';

-- 执行记录
CREATE TABLE mf_wf_execution (
    id            BIGSERIAL PRIMARY KEY,
    workflow_id   BIGINT  NOT NULL,
    version       VARCHAR(16),
    trigger_type  VARCHAR(32),
    trigger_user_id BIGINT,
    status        VARCHAR(16) DEFAULT 'pending',
    input         JSONB,
    output        JSONB,
    error_message TEXT,
    cost_ms       BIGINT,
    cost_token    INT,
    cost_amount   DECIMAL(10,4),
    started_at    TIMESTAMP,
    finished_at   TIMESTAMP,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_execution IS '工作流执行记录表';

-- 节点执行记录
CREATE TABLE mf_wf_node_execution (
    id            BIGSERIAL PRIMARY KEY,
    execution_id  BIGINT  NOT NULL,
    node_id       VARCHAR(64),
    node_type     VARCHAR(64),
    node_name     VARCHAR(128),
    status        VARCHAR(16) DEFAULT 'pending',
    input         JSONB,
    output        JSONB,
    error_message TEXT,
    retry_count   INT DEFAULT 0,
    started_at    TIMESTAMP,
    finished_at   TIMESTAMP,
    cost_ms       BIGINT,
    cost_token    INT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_node_execution IS '节点级执行明细';

-- 执行日志(流式追加)
CREATE TABLE mf_wf_execution_log (
    id           BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    node_id      VARCHAR(64),
    level        VARCHAR(16) DEFAULT 'info',
    message      TEXT,
    payload      JSONB,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_execution_log IS '执行日志(可分区)';


-- ============================================================
-- M04 模板模块 (mf_tpl_*)
-- ============================================================

-- 模板主表
CREATE TABLE mf_tpl_template (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    icon         VARCHAR(255),
    industry     VARCHAR(64),
    scene        VARCHAR(64),
    definition   JSONB NOT NULL,
    version      VARCHAR(16) DEFAULT 'v1',
    author       VARCHAR(64),
    author_id    BIGINT,
    price        DECIMAL(10,2) DEFAULT 0,
    tags         JSONB,
    use_count    BIGINT DEFAULT 0,
    score        DECIMAL(3,2) DEFAULT 5.00,
    review_status VARCHAR(16) DEFAULT 'pending',
    reviewed_by  BIGINT,
    reviewed_at  TIMESTAMP,
    review_remark VARCHAR(255),
    status       VARCHAR(16) DEFAULT 'active',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_tpl_template IS '模板市场主表';

-- 模板评价
CREATE TABLE mf_tpl_review (
    id           BIGSERIAL PRIMARY KEY,
    template_id  BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    score        DECIMAL(3,2),
    content      TEXT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_tpl_review IS '模板评价表';


-- ============================================================
-- M07 监控模块 (mf_mon_*)
-- ============================================================

-- 告警规则
CREATE TABLE mf_mon_alert_rule (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    metric       VARCHAR(64)  NOT NULL,
    condition    VARCHAR(16)  NOT NULL,
    threshold    DECIMAL(15,4) NOT NULL,
    duration_s   INT DEFAULT 60,
    channels     JSONB,
    webhook      VARCHAR(512),
    enabled      BOOLEAN DEFAULT TRUE,
    owner_id     BIGINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_mon_alert_rule IS '告警规则表';

-- 告警记录
CREATE TABLE mf_mon_alert (
    id           BIGSERIAL PRIMARY KEY,
    rule_id      BIGINT,
    metric       VARCHAR(64),
    value        DECIMAL(15,4),
    status       VARCHAR(16) DEFAULT 'firing',
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP,
    notify_log   JSONB,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_mon_alert IS '告警事件表';

-- 统计指标(每天一行)
CREATE TABLE mf_mon_metric_daily (
    id              BIGSERIAL PRIMARY KEY,
    metric_date     DATE NOT NULL UNIQUE,
    wf_total        BIGINT DEFAULT 0,
    wf_active       BIGINT DEFAULT 0,
    exec_total      BIGINT DEFAULT 0,
    exec_success    BIGINT DEFAULT 0,
    exec_failed     BIGINT DEFAULT 0,
    exec_avg_ms     INT DEFAULT 0,
    token_total     BIGINT DEFAULT 0,
    cost_amount     DECIMAL(12,4) DEFAULT 0,
    user_total      BIGINT DEFAULT 0,
    user_active     BIGINT DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_mon_metric_daily IS '每日聚合指标';


-- ============================================================
-- M03 AI 模块 (mf_ai_*)
-- ============================================================

-- 模型配置
CREATE TABLE mf_ai_model (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    provider      VARCHAR(64) NOT NULL,
    model_type    VARCHAR(32) DEFAULT 'chat',
    api_base      VARCHAR(255),
    api_key       VARCHAR(255),
    enabled       BOOLEAN DEFAULT TRUE,
    is_default    BOOLEAN DEFAULT FALSE,
    priority      INT DEFAULT 0,
    max_tokens    INT,
    temperature   DECIMAL(3,2) DEFAULT 0.7,
    timeout_s     INT DEFAULT 60,
    config        JSONB,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_ai_model IS 'AI 模型配置表';

-- 知识库
CREATE TABLE mf_ai_knowledge (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    description   TEXT,
    embedding_id  BIGINT,
    chunk_size    INT DEFAULT 500,
    overlap       INT DEFAULT 50,
    owner_id      BIGINT,
    status        VARCHAR(16) DEFAULT 'active',
    doc_count     BIGINT DEFAULT 0,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_ai_knowledge IS '知识库表';

-- 知识库文档
CREATE TABLE mf_ai_document (
    id            BIGSERIAL PRIMARY KEY,
    kb_id         BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    url           VARCHAR(512),
    file_type     VARCHAR(32),
    file_size     BIGINT,
    status        VARCHAR(16) DEFAULT 'pending',
    chunk_count   INT DEFAULT 0,
    error_message TEXT,
    parse_config  JSONB,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_ai_document IS '知识库文档表';

-- 知识库文档分块 (降级为 BYTEA 以兼容无 pgvector 的环境)
CREATE TABLE mf_ai_chunk (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT NOT NULL,
    kb_id         BIGINT NOT NULL,
    chunk_index   INT,
    content       TEXT,
    embedding     BYTEA,
    token_count   INT,
    metadata      JSONB,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_ai_chunk IS '知识库文档分块(embedding 字段为 BYTEA,需 pgvector 时改为 vector)';

-- AI 调用记录
CREATE TABLE mf_ai_invoke_log (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT,
    execution_id  BIGINT,
    node_id       VARCHAR(64),
    model_id      BIGINT,
    prompt        TEXT,
    completion    TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens  INT,
    cost_ms       BIGINT,
    cost_amount   DECIMAL(10,4),
    status        VARCHAR(16),
    error_message TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_ai_invoke_log IS 'AI 调用日志';


-- ============================================================
-- M10 集成模块 (mf_int_*)
-- ============================================================

-- 集成配置
CREATE TABLE mf_int_config (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(32) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    config      JSONB NOT NULL,
    description VARCHAR(255),
    owner_id    BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_int_config IS '第三方集成配置表';

-- 集成发送日志
CREATE TABLE mf_int_send_log (
    id            BIGSERIAL PRIMARY KEY,
    config_id     BIGINT,
    type          VARCHAR(32),
    channel       VARCHAR(32),
    receiver      VARCHAR(255),
    subject       VARCHAR(255),
    content       TEXT,
    status        VARCHAR(16),
    error_message TEXT,
    cost_ms       BIGINT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_int_send_log IS '集成消息发送日志';

-- MCP 工具注册
CREATE TABLE mf_int_mcp_tool (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(128),
    description TEXT,
    endpoint    VARCHAR(512),
    transport   VARCHAR(16) DEFAULT 'stdio',
    config      JSONB,
    input_schema JSONB,
    enabled     BOOLEAN DEFAULT TRUE,
    version     VARCHAR(16) DEFAULT 'v1',
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_int_mcp_tool IS 'MCP 工具注册表';


-- ============================================================
-- 执行器模块 (mf_exe_*)
-- ============================================================

-- 执行器节点状态
CREATE TABLE mf_exe_executor_node (
    id            BIGSERIAL PRIMARY KEY,
    node_id       VARCHAR(64) NOT NULL UNIQUE,
    name          VARCHAR(128),
    host          VARCHAR(255),
    port          INT DEFAULT 8080,
    status        VARCHAR(16) DEFAULT 'offline',
    last_heartbeat TIMESTAMP,
    cpu_count     INT,
    memory_total  BIGINT,
    memory_used   BIGINT,
    active_tasks  INT DEFAULT 0,
    completed_tasks BIGINT DEFAULT 0,
    failed_tasks   BIGINT DEFAULT 0,
    tags          JSONB,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_exe_executor_node IS '执行器节点表';

-- 执行任务
CREATE TABLE mf_exe_task (
    id            BIGSERIAL PRIMARY KEY,
    execution_id  BIGINT,
    node_id       VARCHAR(64),
    status        VARCHAR(16) DEFAULT 'pending',
    priority      INT DEFAULT 0,
    input         JSONB,
    output        JSONB,
    error_message TEXT,
    start_time    TIMESTAMP,
    end_time      TIMESTAMP,
    cost_ms       BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_exe_task IS '执行任务表';


-- ============================================================
-- 触发器与调度模块 (mf_wf_trigger, mf_wf_schedule)
-- ============================================================

-- 工作流触发器
CREATE TABLE mf_wf_trigger (
    id            BIGSERIAL PRIMARY KEY,
    workflow_id   BIGINT NOT NULL,
    type          VARCHAR(32) NOT NULL,
    name          VARCHAR(128),
    config        JSONB,
    enabled       BOOLEAN DEFAULT TRUE,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_trigger IS '工作流触发器表';

-- 工作流调度
CREATE TABLE mf_wf_schedule (
    id            BIGSERIAL PRIMARY KEY,
    workflow_id   BIGINT NOT NULL,
    cron          VARCHAR(64),
    timezone      VARCHAR(32) DEFAULT 'Asia/Shanghai',
    enabled       BOOLEAN DEFAULT TRUE,
    next_run_at   TIMESTAMP,
    last_run_at   TIMESTAMP,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE mf_wf_schedule IS '工作流定时调度表';
