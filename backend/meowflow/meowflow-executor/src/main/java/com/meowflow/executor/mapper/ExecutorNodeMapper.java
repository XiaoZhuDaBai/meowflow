package com.meowflow.executor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.executor.entity.ExecutorNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExecutorNodeMapper extends BaseMapper<ExecutorNodeEntity> {

    @Select("SELECT * FROM mf_exe_executor_node WHERE node_id = #{nodeId}")
    ExecutorNodeEntity findByNodeId(@Param("nodeId") String nodeId);

    @Select("SELECT * FROM mf_exe_executor_node WHERE status = #{status} ORDER BY active_tasks ASC")
    List<ExecutorNodeEntity> findByStatus(@Param("status") String status);

    @Update("UPDATE mf_exe_executor_node SET status = #{status}, last_heartbeat = #{heartbeat}, " +
            "active_tasks = #{activeTasks}, completed_tasks = #{completedTasks}, failed_tasks = #{failedTasks}, " +
            "memory_used = #{memoryUsed}, update_time = #{updateTime} WHERE node_id = #{nodeId}")
    int updateHeartbeat(@Param("nodeId") String nodeId,
                        @Param("status") String status,
                        @Param("heartbeat") LocalDateTime heartbeat,
                        @Param("activeTasks") Integer activeTasks,
                        @Param("completedTasks") Long completedTasks,
                        @Param("failedTasks") Long failedTasks,
                        @Param("memoryUsed") Long memoryUsed,
                        @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE mf_exe_executor_node SET status = 'offline', update_time = #{now} " +
            "WHERE last_heartbeat < #{threshold}")
    int markOfflineNodes(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);
}
