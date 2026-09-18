package com.meowflow.monitor.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.entity.MetricDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MetricRepository extends BaseMapper<MetricDaily> {

    @Select("SELECT * FROM mf_mon_metric_daily ORDER BY metric_date DESC")
    IPage<MetricDaily> findAll(Page<MetricDaily> page);

    @Select("SELECT * FROM mf_mon_metric_daily WHERE metric_date BETWEEN #{startDate} AND #{endDate} ORDER BY metric_date DESC")
    IPage<MetricDaily> findByDateRange(Page<MetricDaily> page, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM mf_mon_metric_daily ORDER BY metric_date DESC LIMIT #{limit}")
    List<MetricDaily> findLatestMetrics(@Param("limit") int limit);

    @Select("SELECT * FROM mf_mon_metric_daily WHERE id = #{id}")
    MetricDaily findById(@Param("id") Long id);
}
