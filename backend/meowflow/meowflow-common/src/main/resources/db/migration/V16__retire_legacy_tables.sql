-- =============================================================
-- V16__retire_legacy_tables.sql
-- 退役重复的旧表，先迁移数据再删除。
-- =============================================================

-- 旧知识库表 -> knowledge_base/document/document_chunk
INSERT INTO knowledge_base
    (id, name, description, vector_store_type, dimension, status, config,
     create_time, update_time, deleted)
SELECT id,
       name,
       description,
       'pgvector',
       NULL,
       COALESCE(status, 'active'),
       jsonb_build_object(
           'legacy_embedding_id', embedding_id,
           'chunk_size', chunk_size,
           'overlap', overlap,
           'owner_id', owner_id,
           'doc_count', doc_count
       )::text,
       create_time,
       update_time,
       FALSE
FROM mf_ai_knowledge
ON CONFLICT (id) DO NOTHING;

INSERT INTO document
    (id, title, content, content_type, file_size, file_path, knowledge_base_id,
     chunk_count, status, error_message, metadata, create_time, update_time, deleted)
SELECT id,
       name,
       NULL,
       file_type,
       file_size,
       url,
       kb_id,
       COALESCE(chunk_count, 0),
       COALESCE(status, 'pending'),
       error_message,
       COALESCE(parse_config::text, '{}'),
       create_time,
       update_time,
       FALSE
FROM mf_ai_document
ON CONFLICT (id) DO NOTHING;

INSERT INTO document_chunk
    (id, document_id, content, chunk_index, token_count, vector_key, metadata,
     create_time, update_time, deleted)
SELECT id,
       document_id,
       content,
       chunk_index,
       token_count,
       NULL,
       COALESCE(metadata::text, '{}'),
       create_time,
       create_time,
       FALSE
FROM mf_ai_chunk
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('knowledge_base','id'), GREATEST((SELECT MAX(id) FROM knowledge_base), 1));
SELECT setval(pg_get_serial_sequence('document','id'), GREATEST((SELECT MAX(id) FROM document), 1));
SELECT setval(pg_get_serial_sequence('document_chunk','id'), GREATEST((SELECT MAX(id) FROM document_chunk), 1));

DROP TABLE IF EXISTS mf_ai_chunk;
DROP TABLE IF EXISTS mf_ai_document;
DROP TABLE IF EXISTS mf_ai_knowledge;

-- 旧集成配置表 -> mf_integration_config
INSERT INTO mf_integration_config
    (type, name, webhook_url, secret, access_key_id, access_key_secret,
     custom_config, enabled, retry_times, timeout_seconds,
     create_time, update_time, deleted)
SELECT c.type,
       c.name,
       c.config ->> 'webhook',
       c.config ->> 'secret',
       c.config ->> 'accessKey',
       c.config ->> 'accessKeySecret',
       c.config::text,
       COALESCE(c.enabled, TRUE),
       3,
       30,
       c.create_time,
       c.update_time,
       0
FROM mf_int_config c
WHERE NOT EXISTS (
    SELECT 1
    FROM mf_integration_config n
    WHERE n.type = c.type
      AND n.name = c.name
      AND n.deleted = 0
);

DROP TABLE IF EXISTS mf_int_send_log;
DROP TABLE IF EXISTS mf_int_mcp_tool;
DROP TABLE IF EXISTS mf_int_config;
