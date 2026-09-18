-- =============================================================
-- V14__schema_reconciliation.sql
-- 以 Java 实体为准，统一历史库、脚本库和空库之间的结构漂移。
-- 本迁移必须可重复执行。
-- =============================================================

-- 通用审计函数：仅更新目标表中真实存在的列。
CREATE OR REPLACE FUNCTION mf_set_audit_columns()
RETURNS TRIGGER AS $$
BEGIN
    IF to_jsonb(NEW) ? 'update_time' THEN
        NEW := jsonb_populate_record(NEW, jsonb_build_object('update_time', CURRENT_TIMESTAMP));
    END IF;
    IF TG_OP = 'INSERT' AND to_jsonb(NEW) ? 'create_time' THEN
        IF (to_jsonb(NEW) ->> 'create_time') IS NULL THEN
            NEW := jsonb_populate_record(NEW, jsonb_build_object('create_time', CURRENT_TIMESTAMP));
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================
-- 1. 缺失表
-- =============================================================

CREATE TABLE IF NOT EXISTS mf_log_event (
    id            BIGINT PRIMARY KEY,
    trace_id      VARCHAR(64),
    module        VARCHAR(64),
    level         VARCHAR(16),
    type          VARCHAR(64),
    message       TEXT,
    payload       TEXT,
    user_id       BIGINT,
    user_name     VARCHAR(128),
    ip            VARCHAR(64),
    duration      BIGINT,
    status        INTEGER,
    error_message TEXT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS mf_log_event_id_seq;
CREATE INDEX IF NOT EXISTS idx_mf_log_event_trace_id ON mf_log_event(trace_id);
CREATE INDEX IF NOT EXISTS idx_mf_log_event_module ON mf_log_event(module);
CREATE INDEX IF NOT EXISTS idx_mf_log_event_level ON mf_log_event(level);
CREATE INDEX IF NOT EXISTS idx_mf_log_event_type ON mf_log_event(type);
CREATE INDEX IF NOT EXISTS idx_mf_log_event_user_id ON mf_log_event(user_id);
CREATE INDEX IF NOT EXISTS idx_mf_log_event_create_time ON mf_log_event(create_time);

CREATE TABLE IF NOT EXISTS knowledge_base (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(128) NOT NULL,
    description       TEXT,
    vector_store_type VARCHAR(64),
    dimension         INTEGER,
    status            VARCHAR(16) DEFAULT 'active',
    config            TEXT,
    create_by         VARCHAR(64),
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS document (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    content           TEXT,
    content_type      VARCHAR(64),
    file_size         BIGINT,
    file_path         VARCHAR(512),
    knowledge_base_id BIGINT NOT NULL,
    chunk_count       INTEGER DEFAULT 0,
    status            VARCHAR(16) DEFAULT 'pending',
    error_message     TEXT,
    metadata          TEXT,
    create_by         VARCHAR(64),
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content     TEXT,
    chunk_index INTEGER,
    token_count INTEGER,
    vector_key  VARCHAR(128),
    metadata    TEXT,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_document_kb ON document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON document(status);
CREATE INDEX IF NOT EXISTS idx_document_chunk_doc ON document_chunk(document_id);

CREATE TABLE IF NOT EXISTS mf_mon_alert_silence (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(128) NOT NULL,
    match_type     VARCHAR(32) NOT NULL,
    alert_rule_id  BIGINT,
    alert_id       BIGINT,
    target_pattern VARCHAR(256),
    start_time     TIMESTAMP NOT NULL,
    end_time       TIMESTAMP NOT NULL,
    reason         VARCHAR(512),
    create_by      VARCHAR(64),
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by      VARCHAR(64),
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mf_mon_alert_silence_match_type ON mf_mon_alert_silence(match_type);
CREATE INDEX IF NOT EXISTS idx_mf_mon_alert_silence_rule_id ON mf_mon_alert_silence(alert_rule_id);
CREATE INDEX IF NOT EXISTS idx_mf_mon_alert_silence_alert_id ON mf_mon_alert_silence(alert_id);
CREATE INDEX IF NOT EXISTS idx_mf_mon_alert_silence_time_range ON mf_mon_alert_silence(start_time, end_time);

CREATE TABLE IF NOT EXISTS mf_tpl_tag (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL UNIQUE,
    color       VARCHAR(16),
    sort        INTEGER DEFAULT 0,
    usage_count BIGINT DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS mf_tpl_template_tag (
    template_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (template_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_template_tag_tag ON mf_tpl_template_tag(tag_id);

CREATE TABLE IF NOT EXISTS mf_tpl_review_record (
    id            BIGSERIAL PRIMARY KEY,
    template_id   BIGINT,
    reviewer_id   BIGINT,
    reviewer_name VARCHAR(64),
    action        VARCHAR(32),
    comment       TEXT,
    review_time   TIMESTAMP,
    create_by     BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_review_record_template ON mf_tpl_review_record(template_id);
CREATE INDEX IF NOT EXISTS idx_mf_tpl_review_record_reviewer ON mf_tpl_review_record(reviewer_id);

CREATE TABLE IF NOT EXISTS mf_wf_execution_snapshot (
    id               BIGSERIAL PRIMARY KEY,
    execution_id     BIGINT NOT NULL,
    workflow_id      BIGINT,
    version          VARCHAR(32),
    status           VARCHAR(32),
    completed_nodes  TEXT,
    variables        TEXT,
    loop_cursors     TEXT,
    waiting_reason   VARCHAR(64),
    pending_edge_id  VARCHAR(64),
    last_event_id    VARCHAR(64),
    snapshot_version INTEGER DEFAULT 1,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mf_wf_execution_snapshot_execution ON mf_wf_execution_snapshot(execution_id);

-- =============================================================
-- 2. 已有表补列
-- =============================================================

ALTER TABLE mf_sys_permission
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE mf_sys_post
    ADD COLUMN IF NOT EXISTS post_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS post_name VARCHAR(64),
    ADD COLUMN IF NOT EXISTS post_sort INTEGER DEFAULT 0;

ALTER TABLE mf_sys_role_permission
    ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE mf_sys_audit_log
    ADD COLUMN IF NOT EXISTS request_method VARCHAR(16),
    ADD COLUMN IF NOT EXISTS request_params TEXT,
    ADD COLUMN IF NOT EXISTS response_result TEXT,
    ADD COLUMN IF NOT EXISTS cost_time BIGINT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(16),
    ADD COLUMN IF NOT EXISTS error_msg TEXT,
    ADD COLUMN IF NOT EXISTS operate_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE mf_wf_category
    ADD COLUMN IF NOT EXISTS level INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS remark VARCHAR(255);

ALTER TABLE mf_tpl_review
    ADD COLUMN IF NOT EXISTS reviewer_id BIGINT,
    ADD COLUMN IF NOT EXISTS action VARCHAR(32),
    ADD COLUMN IF NOT EXISTS comment TEXT,
    ADD COLUMN IF NOT EXISTS result VARCHAR(32),
    ADD COLUMN IF NOT EXISTS create_by BIGINT,
    ADD COLUMN IF NOT EXISTS update_by BIGINT,
    ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

ALTER TABLE mf_ai_invoke_log
    ADD COLUMN IF NOT EXISTS workflow_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS model VARCHAR(128),
    ADD COLUMN IF NOT EXISTS provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE mf_ai_model
    ADD COLUMN IF NOT EXISTS capabilities VARCHAR(500);

-- 历史重复数据去重后建立业务唯一索引
DELETE FROM mf_ai_model a
USING mf_ai_model b
WHERE a.id > b.id
  AND a.provider = b.provider
  AND a.model_type = b.model_type;

CREATE UNIQUE INDEX IF NOT EXISTS uk_mf_ai_model_provider_model
    ON mf_ai_model(provider, model_type);
-- 兼容旧字段回填
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_post' AND column_name='code') THEN
        EXECUTE 'UPDATE mf_sys_post SET post_code = COALESCE(post_code, code)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_post' AND column_name='name') THEN
        EXECUTE 'UPDATE mf_sys_post SET post_name = COALESCE(post_name, name)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_post' AND column_name='sort') THEN
        EXECUTE 'UPDATE mf_sys_post SET post_sort = COALESCE(post_sort, sort)';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_audit_log' AND column_name='method') THEN
        EXECUTE 'UPDATE mf_sys_audit_log SET request_method = COALESCE(request_method, method)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_audit_log' AND column_name='params') THEN
        EXECUTE 'UPDATE mf_sys_audit_log SET request_params = COALESCE(request_params, params::text)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_audit_log' AND column_name='result') THEN
        EXECUTE 'UPDATE mf_sys_audit_log SET response_result = COALESCE(response_result, result)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_audit_log' AND column_name='cost_ms') THEN
        EXECUTE 'UPDATE mf_sys_audit_log SET cost_time = COALESCE(cost_time, cost_ms)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_sys_audit_log' AND column_name='created_at') THEN
        EXECUTE 'UPDATE mf_sys_audit_log SET operate_time = COALESCE(operate_time, created_at)';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_ai_invoke_log' AND column_name='created_at') THEN
        EXECUTE 'UPDATE mf_ai_invoke_log SET create_time = COALESCE(create_time, created_at)';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_ai_invoke_log' AND column_name='model_id') THEN
        EXECUTE 'UPDATE mf_ai_invoke_log SET model = COALESCE(model, model_id::text)';
    END IF;
END $$;

-- 修正明显类型漂移
ALTER TABLE mf_mon_alert_rule
    ALTER COLUMN enabled TYPE BOOLEAN USING
        CASE WHEN enabled IS NULL THEN TRUE
             WHEN enabled::text IN ('1','true','TRUE') THEN TRUE
             ELSE FALSE END;

ALTER TABLE mf_mon_alert_rule
    ALTER COLUMN enabled SET DEFAULT TRUE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='mf_mon_alert_rule' AND column_name='auto_resolve') THEN
        EXECUTE 'ALTER TABLE mf_mon_alert_rule ALTER COLUMN auto_resolve TYPE BOOLEAN USING CASE WHEN auto_resolve IS NULL THEN FALSE WHEN auto_resolve::text IN (''1'',''true'',''TRUE'') THEN TRUE ELSE FALSE END';
        EXECUTE 'ALTER TABLE mf_mon_alert_rule ALTER COLUMN auto_resolve SET DEFAULT FALSE';
    END IF;
END $$;

-- 新增表的审计触发器
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema='public'
          AND t.table_name IN ('mf_log_event','knowledge_base','document','document_chunk',
                               'mf_mon_alert_silence','mf_tpl_tag','mf_tpl_review_record',
                               'mf_wf_execution_snapshot')
    LOOP
        EXECUTE format('CREATE OR REPLACE TRIGGER trg_%s_audit BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION mf_set_audit_columns()', r.table_name, r.table_name);
    END LOOP;
END $$;


