package com.meowflow.executor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.executor.entity.ExecutorTaskEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExecutorTaskMapper extends BaseMapper<ExecutorTaskEntity> {

    @Select("SELECT * FROM mf_exe_task WHERE execution_id = #{executionId} ORDER BY id ASC")
    List<ExecutorTaskEntity> findByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT * FROM mf_exe_task WHERE node_id = #{nodeId} ORDER BY priority DESC, id ASC")
    List<ExecutorTaskEntity> findByNodeId(@Param("nodeId") String nodeId);

    @Select("SELECT * FROM mf_exe_task WHERE status = #{status} ORDER BY priority DESC, id ASC LIMIT #{limit}")
    List<ExecutorTaskEntity> findPendingTasks(@Param("status") String status, @Param("limit") int limit);

    @Update("UPDATE mf_exe_task SET status = #{status}, end_time = NOW(), cost_ms = #{costMs}, " +
            "output = #{output}::jsonb WHERE id = #{id}")
    int finishTask(@Param("id") Long id,
                   @Param("status") String status,
                   @Param("costMs") Long costMs,
                   @Param("output") String output);

    @Update("UPDATE mf_exe_task SET status = #{status}, end_time = NOW(), " +
            "error_message = #{errorMessage} WHERE id = #{id}")
    int failTask(@Param("id") Long id,
                 @Param("status") String status,
                 @Param("errorMessage") String errorMessage);
}
