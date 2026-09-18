-- 告警记录前端展示所需的字段补齐。
-- 历史表仅有 triggered_at/resolved_at，导致规则、通知和解决说明无法完整回显。
ALTER TABLE mf_mon_alert_rule
    ADD COLUMN IF NOT EXISTS severity VARCHAR(16) DEFAULT 'WARNING';

UPDATE mf_mon_alert_rule
SET severity = 'WARNING'
WHERE severity IS NULL OR severity = '';

ALTER TABLE mf_mon_alert
    ADD COLUMN IF NOT EXISTS resolved_by VARCHAR(64);

ALTER TABLE mf_mon_alert
    ADD COLUMN IF NOT EXISTS resolution_note VARCHAR(512);

COMMENT ON COLUMN mf_mon_alert_rule.severity IS '告警严重程度：INFO/WARNING/ERROR/CRITICAL';
COMMENT ON COLUMN mf_mon_alert.resolved_by IS '解决人';
COMMENT ON COLUMN mf_mon_alert.resolution_note IS '解决说明';