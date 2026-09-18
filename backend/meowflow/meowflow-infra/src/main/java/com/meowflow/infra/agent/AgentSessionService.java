package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meowflow.infra.chat.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agent 会话管理服务
 * 
 * <p>负责会话生命周期管理和消息持久化，支持 10 轮滚动窗口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSessionService {

    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;

    /** 默认保留最近 10 轮对话（1 轮 = 1 user + 1 assistant） */
    private static final int DEFAULT_MAX_ROUNDS = 10;
    /** 会话过期时间（24 小时无活动） */
    private static final int SESSION_EXPIRE_HOURS = 24;

    /**
     * 创建新会话
     */
    @Transactional(rollbackFor = Exception.class)
    public String createSession(Long userId, String agentNodeId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setAgentNodeId(agentNodeId);
        session.setStatus(AgentSession.STATUS_ACTIVE);
        session.setLastActiveTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusHours(SESSION_EXPIRE_HOURS));
        
        sessionMapper.insert(session);
        log.info("Created agent session: {}", sessionId);
        return sessionId;
    }

    /**
     * 获取会话（不存在则创建）
     */
    public String getOrCreateSession(Long userId, String agentNodeId, String existingSessionId) {
        if (existingSessionId != null) {
            AgentSession session = sessionMapper.selectOne(
                    new LambdaQueryWrapper<AgentSession>()
                            .eq(AgentSession::getSessionId, existingSessionId)
                            .eq(AgentSession::getStatus, AgentSession.STATUS_ACTIVE)
            );
            if (session != null) {
                // 刷新过期时间
                session.setLastActiveTime(LocalDateTime.now());
                session.setExpireTime(LocalDateTime.now().plusHours(SESSION_EXPIRE_HOURS));
                sessionMapper.updateById(session);
                return existingSessionId;
            }
        }
        return createSession(userId, agentNodeId);
    }

    /**
     * 追加用户消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendUserMessage(String sessionId, String content) {
        int nextSeq = messageMapper.selectMaxSequence(sessionId) + 1;
        
        AgentMessage message = AgentMessage.builder()
                .sessionId(sessionId)
                .role(AgentMessage.ROLE_USER)
                .content(content)
                .sequenceNumber(nextSeq)
                .build();
        
        messageMapper.insert(message);
        evictIfNeeded(sessionId);
    }

    /**
     * 追加 Assistant 消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendAssistantMessage(String sessionId, String content) {
        int nextSeq = messageMapper.selectMaxSequence(sessionId) + 1;
        
        AgentMessage message = AgentMessage.builder()
                .sessionId(sessionId)
                .role(AgentMessage.ROLE_ASSISTANT)
                .content(content)
                .sequenceNumber(nextSeq)
                .build();
        
        messageMapper.insert(message);
        evictIfNeeded(sessionId);
    }

    /**
     * 追加工具调用消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendToolMessage(String sessionId, String toolName, Object toolResult) {
        int nextSeq = messageMapper.selectMaxSequence(sessionId) + 1;
        
        AgentMessage message = AgentMessage.builder()
                .sessionId(sessionId)
                .role(AgentMessage.ROLE_TOOL)
                .content("Tool: " + toolName)
                .toolName(toolName)
                .toolResult(toolResult)
                .sequenceNumber(nextSeq)
                .build();
        
        messageMapper.insert(message);
        evictIfNeeded(sessionId);
    }

    /**
     * 获取会话的历史消息（转换为 Message 列表）
     */
    public List<Message> getSessionHistory(String sessionId, int maxRounds) {
        // 计算需要保留的消息数（每轮可能包含 user + assistant + 多个 tool）
        // 简化处理：直接取最近 maxRounds * 3 条消息（预留 tool 空间）
        int limit = maxRounds * 3;
        
        List<AgentMessage> messages = messageMapper.selectRecentMessages(sessionId, limit);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按序号升序排列
        Collections.reverse(messages);
        
        List<Message> history = new ArrayList<>();
        for (AgentMessage msg : messages) {
            switch (msg.getRole()) {
                case AgentMessage.ROLE_USER:
                    history.add(Message.user(msg.getContent()));
                    break;
                case AgentMessage.ROLE_ASSISTANT:
                    history.add(Message.assistant(msg.getContent()));
                    break;
                case AgentMessage.ROLE_SYSTEM:
                    history.add(Message.system(msg.getContent()));
                    break;
                case AgentMessage.ROLE_TOOL:
                    // 工具消息转换为 user 角色（部分 LLM 不支持 tool 角色）
                    String toolContent = String.format("Tool %s result: %s", 
                            msg.getToolName(), msg.getToolResult());
                    history.add(Message.user(toolContent));
                    break;
            }
        }
        
        return history;
    }

    /**
     * 清空会话历史
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearSession(String sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        log.info("Cleared session history: {}", sessionId);
    }

    /**
     * 删除会话
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.delete(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * 淘汰旧消息（保留最近 N 轮）
     */
    private void evictIfNeeded(String sessionId) {
        // 保留最近 10 轮 * 3 条消息 = 30 条
        int keepCount = DEFAULT_MAX_ROUNDS * 3;
        int deleted = messageMapper.evictOldMessages(sessionId, keepCount);
        if (deleted > 0) {
            log.debug("Evicted {} old messages from session {}", deleted, sessionId);
        }
    }

    /**
     * 定时任务：标记过期会话
     */
    public int expireOldSessions() {
        int count = sessionMapper.expireOldSessions(LocalDateTime.now());
        if (count > 0) {
            log.info("Marked {} sessions as expired", count);
        }
        return count;
    }

    /**
     * 定时任务：清理过期会话（超过 7 天未活跃）
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<String> expiredIds = sessionMapper.findExpiredSessionIds(threshold);
        
        for (String sessionId : expiredIds) {
            deleteSession(sessionId);
        }
        
        if (!expiredIds.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expiredIds.size());
        }
        return expiredIds.size();
    }
}
