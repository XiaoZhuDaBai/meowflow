package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 会话 Mapper
 */
@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {

    /**
     * 批量更新过期会话状态
     */
    @Update("UPDATE mf_agent_session SET status = 'expired' WHERE status = 'active' AND expire_time < #{now}")
    int expireOldSessions(@Param("now") LocalDateTime now);

    /**
     * 查询需要清理的过期会话（超过 7 天未活跃）
     */
    @Select("SELECT session_id FROM mf_agent_session WHERE status = 'expired' AND last_active_time < #{threshold}")
    List<String> findExpiredSessionIds(@Param("threshold") LocalDateTime threshold);
}
