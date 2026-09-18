-- ============================================================
-- 恢复默认告警规则，并兼容曾被测试数据清理掉的开发库
-- ============================================================
ALTER TABLE mf_mon_alert_rule
    ADD COLUMN IF NOT EXISTS description VARCHAR(512);

INSERT INTO mf_mon_alert_rule (name, metric, condition, threshold, duration_s, channels, enabled, owner_id)
SELECT '执行失败告警', 'execution_failed', 'gt', 5, 60, '["email"]'::jsonb, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM mf_mon_alert_rule WHERE name = '执行失败告警');

INSERT INTO mf_mon_alert_rule (name, metric, condition, threshold, duration_s, channels, enabled, owner_id)
SELECT '执行超时告警', 'execution_timeout', 'gt', 3, 60, '["dingtalk"]'::jsonb, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM mf_mon_alert_rule WHERE name = '执行超时告警');

INSERT INTO mf_mon_alert_rule (name, metric, condition, threshold, duration_s, channels, enabled, owner_id)
SELECT '成本超限告警', 'cost_exceeded', 'gt', 100, 3600, '["email"]'::jsonb, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM mf_mon_alert_rule WHERE name = '成本超限告警');
