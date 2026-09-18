-- 集成配置表：对齐 IntegrationConfigEntity / IntegrationConfigService
CREATE TABLE IF NOT EXISTS mf_integration_config (
    id                 BIGSERIAL PRIMARY KEY,
    type               VARCHAR(32) NOT NULL,
    name               VARCHAR(128) NOT NULL,
    webhook_url        VARCHAR(1000),
    secret             TEXT,
    access_key_id      TEXT,
    access_key_secret  TEXT,
    custom_config      TEXT,
    enabled            BOOLEAN DEFAULT TRUE,
    retry_times        INTEGER DEFAULT 3,
    timeout_seconds    INTEGER DEFAULT 30,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by          VARCHAR(64),
    update_by          VARCHAR(64),
    deleted            SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_mf_integration_config_type ON mf_integration_config(type);
CREATE INDEX IF NOT EXISTS idx_mf_integration_config_enabled ON mf_integration_config(enabled);
CREATE INDEX IF NOT EXISTS idx_mf_integration_config_deleted ON mf_integration_config(deleted);
COMMENT ON TABLE mf_integration_config IS '第三方集成配置表';
