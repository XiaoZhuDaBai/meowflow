package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.Execution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface ExecutionRepository extends BaseMapper<Execution> {

    @Select("SELECT * FROM mf_wf_execution WHERE workflow_id = #{workflowId} ORDER BY create_time DESC")
    List<Execution> findByWorkflowId(@Param("workflowId") Long workflowId);

    @Select("SELECT * FROM mf_wf_execution WHERE workflow_id = #{workflowId} ORDER BY create_time DESC LIMIT #{limit}")
    List<Execution> findRecentByWorkflowId(@Param("workflowId") Long workflowId, @Param("limit") int limit);

    @Select("SELECT * FROM mf_wf_execution WHERE status = #{status} ORDER BY create_time ASC")
    List<Execution> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM mf_wf_execution WHERE trigger_user_id = #{userId} ORDER BY create_time DESC")
    List<Execution> findByTriggerUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM mf_wf_execution WHERE workflow_id = #{workflowId} AND create_time BETWEEN #{startTime} AND #{endTime}")
    List<Execution> findByWorkflowIdAndTimeRange(
            @Param("workflowId") Long workflowId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM mf_wf_execution WHERE workflow_id = #{workflowId} AND status = #{status}")
    long countByWorkflowIdAndStatus(@Param("workflowId") Long workflowId, @Param("status") String status);

    @Select("SELECT AVG(cost_ms) FROM mf_wf_execution WHERE workflow_id = #{workflowId} AND status = 'success' AND cost_ms IS NOT NULL")
    Double avgCostMsByWorkflowId(@Param("workflowId") Long workflowId);

    @Select("SELECT SUM(cost_token) FROM mf_wf_execution WHERE workflow_id = #{workflowId} AND cost_token IS NOT NULL")
    Long totalCostTokenByWorkflowId(@Param("workflowId") Long workflowId);

    // --- Stats aggregation queries (return Map for flexibility) ---

    @Select("SELECT status, COUNT(*) as cnt FROM mf_wf_execution WHERE create_time >= #{today} GROUP BY status")
    List<Map<String, Object>> countByStatusToday(@Param("today") LocalDateTime today);

    @Select("SELECT DATE(create_time) as exec_date, COUNT(*) as total, " +
            "SUM(CASE WHEN status='success' THEN 1 ELSE 0 END) as success_cnt, " +
            "AVG(cost_ms) as avg_ms " +
            "FROM mf_wf_execution " +
            "WHERE create_time BETWEEN #{start} AND #{end} " +
            "GROUP BY DATE(create_time) ORDER BY exec_date ASC")
    List<Map<String, Object>> dailyStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM mf_wf_execution WHERE create_time >= #{today}")
    long countTodayTotal(@Param("today") LocalDateTime today);

    @Select("SELECT AVG(cost_ms) FROM mf_wf_execution WHERE create_time >= #{today} AND status='success' AND cost_ms IS NOT NULL")
    Double avgCostMsToday(@Param("today") LocalDateTime today);

    @Select("SELECT SUM(cost_amount) FROM mf_wf_execution WHERE create_time >= #{today} AND cost_amount IS NOT NULL")
    Double totalCostAmountToday(@Param("today") LocalDateTime today);

    @Select("SELECT COUNT(*) FROM mf_wf_execution WHERE create_time >= #{today} AND status='success'")
    long countTodaySuccess(@Param("today") LocalDateTime today);

    @Select("SELECT workflow_id, COUNT(*) as exec_cnt FROM mf_wf_execution GROUP BY workflow_id ORDER BY exec_cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> topWorkflows(@Param("limit") int limit);

    @Select("SELECT SUM(cost_amount) FROM mf_wf_execution WHERE cost_amount IS NOT NULL")
    Double totalCostAmount();

    @Select("SELECT workflow_id, SUM(cost_amount) as total FROM mf_wf_execution WHERE cost_amount IS NOT NULL GROUP BY workflow_id ORDER BY total DESC")
    List<Map<String, Object>> costByWorkflow();

    // --- Per-workflow stats aggregation (returned as Map for flexibility) ---

    @Select("SELECT " +
            "COUNT(*) AS total_runs, " +
            "SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) AS fail_count, " +
            "COALESCE(AVG(cost_ms), 0) AS avg_duration_ms, " +
            "COALESCE(SUM(cost_amount), 0) AS total_cost, " +
            "SUM(CASE WHEN create_time >= #{today} THEN 1 ELSE 0 END) AS today_run_count, " +
            "COALESCE(SUM(CASE WHEN create_time >= #{today} THEN cost_amount ELSE 0 END), 0) AS today_cost " +
            "FROM mf_wf_execution WHERE workflow_id = #{workflowId}")
    Map<String, Object> aggregateStatsByWorkflow(
            @Param("workflowId") Long workflowId,
            @Param("today") LocalDateTime today);

    /**
     * Batch-aggregate stats for a list of workflows in a single query.
     * Returns one row per workflow_id.
     */
    @Select("<script>" +
            "SELECT " +
            "workflow_id, " +
            "COUNT(*) AS total_runs, " +
            "SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) AS fail_count, " +
            "COALESCE(AVG(cost_ms), 0) AS avg_duration_ms, " +
            "COALESCE(SUM(cost_amount), 0) AS total_cost, " +
            "SUM(CASE WHEN create_time >= #{today} THEN 1 ELSE 0 END) AS today_run_count, " +
            "COALESCE(SUM(CASE WHEN create_time >= #{today} THEN cost_amount ELSE 0 END), 0) AS today_cost " +
            "FROM mf_wf_execution " +
            "WHERE workflow_id IN " +
            "<foreach collection='workflowIds' item='wid' open='(' separator=',' close=')'>" +
            "#{wid}" +
            "</foreach> " +
            "GROUP BY workflow_id" +
            "</script>")
    List<Map<String, Object>> aggregateStatsByWorkflows(
            @Param("workflowIds") List<Long> workflowIds,
            @Param("today") LocalDateTime today);
}
