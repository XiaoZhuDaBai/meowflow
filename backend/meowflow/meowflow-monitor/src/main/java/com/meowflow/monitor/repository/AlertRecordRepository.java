package com.meowflow.monitor.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertRecordRepository extends BaseMapper<AlertRecord> {

    String RECORD_SELECT = "SELECT a.id, a.rule_id AS alertRuleId, "
            + "COALESCE(r.name, '未知规则') AS ruleName, "
            + "'METRIC' AS alertType, a.metric, a.value, a.status, "
            + "a.triggered_at AS firedAt, a.resolved_at AS resolvedAt, "
            + "a.resolved_by AS resolvedBy, a.resolution_note AS resolutionNote, "
            + "a.notify_log::text AS notifyLog, a.create_time AS createTime, "
            + "COALESCE(r.name, '未知规则') AS title, "
            + "'规则 [' || COALESCE(r.name, '未知规则') || '] 已触发' AS message, "
            + "COALESCE(r.severity, 'WARNING') AS severity, "
            + "a.value::text AS triggerValue, r.threshold AS thresholdValue, "
            + "r.condition AS operator, COALESCE(r.channels::text, '[]') AS alertChannel "
            + "FROM mf_mon_alert a LEFT JOIN mf_mon_alert_rule r ON r.id = a.rule_id ";

    @Select(RECORD_SELECT + "WHERE a.status = 'FIRING' ORDER BY a.id DESC")
    List<AlertRecord> findFiringAlerts();

    @Select(RECORD_SELECT + "WHERE a.status = #{status} ORDER BY a.id DESC")
    IPage<AlertRecord> findByStatus(Page<AlertRecord> page, @Param("status") String status);

    @Select(RECORD_SELECT + "WHERE a.rule_id = #{ruleId} ORDER BY a.id DESC")
    List<AlertRecord> findByRuleId(@Param("ruleId") Long ruleId);

    @Select(RECORD_SELECT + "ORDER BY a.id DESC")
    IPage<AlertRecord> findAll(Page<AlertRecord> page);

    @Select("SELECT COUNT(*) FROM mf_mon_alert WHERE status = 'FIRING'")
    int countFiringAlerts();

    @Select(RECORD_SELECT + "WHERE a.id = #{id}")
    AlertRecord findById(@Param("id") Long id);
}
