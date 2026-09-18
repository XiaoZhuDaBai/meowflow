-- Agent 会话表
CREATE TABLE IF NOT EXISTS mf_agent_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT,
    agent_node_id VARCHAR(128),
    status VARCHAR(32) DEFAULT 'active',
    last_active_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_session_id ON mf_agent_session(session_id);
CREATE INDEX idx_user_id ON mf_agent_session(user_id);
CREATE INDEX idx_status_expire ON mf_agent_session(status, expire_time);

COMMENT ON TABLE mf_agent_session IS 'Agent 会话表，存储会话元信息';
COMMENT ON COLUMN mf_agent_session.session_id IS '会话 ID（业务主键）';
COMMENT ON COLUMN mf_agent_session.status IS '会话状态：active/expired/archived';
COMMENT ON COLUMN mf_agent_session.expire_time IS '过期时间（24小时无活动自动过期）';

-- Agent 消息表
CREATE TABLE IF NOT EXISTS mf_agent_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    tool_name VARCHAR(128),
    tool_result JSONB,
    sequence_number INT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_message_session ON mf_agent_message(session_id, sequence_number DESC);
CREATE INDEX idx_message_session_seq ON mf_agent_message(session_id, sequence_number);

COMMENT ON TABLE mf_agent_message IS 'Agent 消息表，存储单条对话消息';
COMMENT ON COLUMN mf_agent_message.role IS '消息角色：user/assistant/system/tool';
COMMENT ON COLUMN mf_agent_message.sequence_number IS '消息序号（同一会话内递增，用于排序和淘汰）';
COMMENT ON COLUMN mf_agent_message.tool_result IS '工具调用结果（仅 role=tool 时有值）';
