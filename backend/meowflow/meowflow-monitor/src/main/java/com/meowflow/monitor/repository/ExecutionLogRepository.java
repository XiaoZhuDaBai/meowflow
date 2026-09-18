package com.meowflow.monitor.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.monitor.entity.ExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExecutionLogRepository extends BaseMapper<ExecutionLog> {

    @Select("SELECT * FROM mf_wf_execution_log WHERE execution_id = #{executionId} ORDER BY id ASC")
    List<ExecutionLog> findByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT * FROM mf_wf_execution_log WHERE execution_id = #{executionId} AND node_id = #{nodeId} ORDER BY id ASC")
    List<ExecutionLog> findByExecutionIdAndNodeId(@Param("executionId") Long executionId, @Param("nodeId") String nodeId);

    @Select("SELECT * FROM mf_wf_execution_log WHERE execution_id = #{executionId} ORDER BY created_at DESC")
    IPage<ExecutionLog> findByExecutionIdPage(Page<ExecutionLog> page, @Param("executionId") Long executionId);

    @Select("SELECT * FROM mf_wf_execution_log WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    IPage<ExecutionLog> findByTimeRange(Page<ExecutionLog> page, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM mf_wf_execution_log WHERE level = 'error' ORDER BY created_at DESC")
    IPage<ExecutionLog> findErrorLogs(Page<ExecutionLog> page);

    @Select("SELECT * FROM mf_wf_execution_log WHERE id = #{id}")
    ExecutionLog findById(@Param("id") Long id);
}

