package com.meowflow.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.workflow.entity.NodeExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface NodeExecutionRepository extends BaseMapper<NodeExecution> {

    @Select("SELECT * FROM mf_wf_node_execution WHERE execution_id = #{executionId} ORDER BY started_at ASC")
    List<NodeExecution> findByExecutionId(@Param("executionId") Long executionId);

    /**
     * 查询某执行下某节点的所有记录，最新的在前。
     *
     * <p>注意：同一 (execution_id, node_id) 可能存在多行 —— 循环节点每轮迭代都会
     * 新插一行（见 LoopSubgraphDriver）。因此这里必须返回列表，
     * 不能声明成 {@code Optional<NodeExecution>}：那样第二次迭代会抛
     * {@code TooManyResultsException}，且被 recorder 静默吞掉，
     * 导致节点永远停在 running、输出丢失。</p>
     */
    @Select("SELECT * FROM mf_wf_node_execution WHERE execution_id = #{executionId} AND node_id = #{nodeId} "
            + "ORDER BY id DESC")
    List<NodeExecution> findAllByExecutionIdAndNodeId(@Param("executionId") Long executionId,
                                                     @Param("nodeId") String nodeId);

    @Select("SELECT * FROM mf_wf_node_execution WHERE execution_id = #{executionId} AND status = #{status}")
    List<NodeExecution> findByExecutionIdAndStatus(@Param("executionId") Long executionId, @Param("status") String status);

    @Select("SELECT * FROM mf_wf_node_execution WHERE execution_id = #{executionId} ORDER BY started_at DESC LIMIT 1")
    Optional<NodeExecution> findLastByExecutionId(@Param("executionId") Long executionId);

    @Select("SELECT COUNT(*) FROM mf_wf_node_execution WHERE execution_id = #{executionId} AND status = #{status}")
    long countByExecutionIdAndStatus(@Param("executionId") Long executionId, @Param("status") String status);
}
