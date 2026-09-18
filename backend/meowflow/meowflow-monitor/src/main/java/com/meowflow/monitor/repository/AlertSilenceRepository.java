package com.meowflow.monitor.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.entity.AlertSilence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AlertSilenceRepository extends BaseMapper<AlertSilence> {

    @Select("SELECT * FROM mf_mon_alert_silence WHERE start_time <= #{currentTime} AND end_time >= #{currentTime}")
    List<AlertSilence> findActiveSilences(@Param("currentTime") LocalDateTime currentTime);

    @Select("SELECT * FROM mf_mon_alert_silence WHERE alert_rule_id = #{ruleId}")
    List<AlertSilence> findActiveByRuleId(@Param("ruleId") Long ruleId);

    @Select("SELECT * FROM mf_mon_alert_silence ORDER BY id DESC")
    IPage<AlertSilence> findAll(Page<AlertSilence> page);

    @Select("SELECT * FROM mf_mon_alert_silence WHERE id = #{id}")
    AlertSilence findById(@Param("id") Long id);
}
