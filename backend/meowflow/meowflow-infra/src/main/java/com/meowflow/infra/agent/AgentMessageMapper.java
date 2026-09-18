package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent 消息 Mapper
 */
@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {

    /**
     * 获取指定会话的最近 N 条消息（按序号降序）
     */
    @Select("SELECT * FROM mf_agent_message WHERE session_id = #{sessionId} " +
            "ORDER BY sequence_number DESC LIMIT #{limit}")
    List<AgentMessage> selectRecentMessages(@Param("sessionId") String sessionId, 
                                             @Param("limit") int limit);

    /**
     * 获取指定会话的当前最大序号
     */
    @Select("SELECT COALESCE(MAX(sequence_number), 0) FROM mf_agent_message WHERE session_id = #{sessionId}")
    int selectMaxSequence(@Param("sessionId") String sessionId);

    /**
     * 删除会话的所有消息
     */
    @Delete("DELETE FROM mf_agent_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 淘汰旧消息，保留最近 N 条
     */
    @Delete("DELETE FROM mf_agent_message WHERE session_id = #{sessionId} " +
            "AND sequence_number <= (SELECT COALESCE(MAX(sequence_number), 0) - #{keepCount} " +
            "FROM mf_agent_message WHERE session_id = #{sessionId})")
    int evictOldMessages(@Param("sessionId") String sessionId, @Param("keepCount") int keepCount);
}
