package com.meowflow.monitor.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertRuleRepository extends BaseMapper<AlertRule> {

    String RULE_COLUMNS = "id, name, description, metric AS metricName, condition AS conditionType, "
            + "threshold AS thresholdValue, duration_s AS timeWindowSeconds, channels AS notificationChannels, "
            + "webhook, enabled, severity, owner_id AS ownerId, create_time AS createTime, update_time AS updateTime";

    @Select("SELECT " + RULE_COLUMNS + " FROM mf_mon_alert_rule WHERE enabled = true ORDER BY id")
    List<AlertRule> findActiveRules();

    @Select("SELECT " + RULE_COLUMNS + " FROM mf_mon_alert_rule WHERE metric = #{metricName} AND enabled = TRUE")
    List<AlertRule> findByMetricName(@Param("metricName") String metricName);

    @Select("SELECT " + RULE_COLUMNS + " FROM mf_mon_alert_rule WHERE id = #{id}")
    AlertRule findById(@Param("id") Long id);

    @Select("SELECT " + RULE_COLUMNS + " FROM mf_mon_alert_rule ORDER BY id DESC")
    IPage<AlertRule> findAll(Page<AlertRule> page);

    @Select("SELECT " + RULE_COLUMNS + " FROM mf_mon_alert_rule WHERE enabled = #{enabled} ORDER BY id DESC")
    IPage<AlertRule> findByEnabled(Page<AlertRule> page, @Param("enabled") Boolean enabled);
}
