package com.meowflow.infra.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 会话服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentSessionServiceTest {

    @Autowired
    private AgentSessionService sessionService;

    @Autowired
    private AgentSessionMapper sessionMapper;

    @Autowired
    private AgentMessageMapper messageMapper;

    private Long userId = 1001L;
    private String agentNodeId = "agent-001";

    @BeforeEach
    void setUp() {
        // 使用特定条件清理测试数据，避免全表删除
        // 由于测试使用了 @Transactional，测试结束后会自动回滚
        // 这里只清理当前测试用户的数据
        LambdaQueryWrapper<AgentSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(AgentSession::getUserId, userId);

        List<String> sessionIds = sessionMapper.selectList(sessionWrapper)
                .stream()
                .map(AgentSession::getSessionId)
                .toList();
        if (!sessionIds.isEmpty()) {
            messageMapper.delete(new LambdaQueryWrapper<AgentMessage>()
                    .in(AgentMessage::getSessionId, sessionIds));
            sessionMapper.delete(sessionWrapper);
        }
    }

    @Test
    @DisplayName("创建会话")
    void testCreateSession() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
        
        AgentSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        
        assertNotNull(session);
        assertEquals(userId, session.getUserId());
        assertEquals(agentNodeId, session.getAgentNodeId());
        assertEquals(AgentSession.STATUS_ACTIVE, session.getStatus());
    }

    @Test
    @DisplayName("追加消息并自动淘汰")
    void testAppendMessagesWithEviction() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        // 追加 25 条消息（超过 30 条限制的阈值）
        for (int i = 1; i <= 25; i++) {
            sessionService.appendUserMessage(sessionId, "User message " + i);
            sessionService.appendAssistantMessage(sessionId, "Assistant response " + i);
        }
        
        // 查询消息总数（应该被限制在 30 条以内）
        long count = messageMapper.selectCount(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
        );
        
        assertTrue(count <= 30, "Should keep at most 30 messages (10 rounds * 3)");
    }

    @Test
    @DisplayName("获取会话历史")
    void testGetSessionHistory() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        sessionService.appendUserMessage(sessionId, "Hello");
        sessionService.appendAssistantMessage(sessionId, "Hi there");
        sessionService.appendUserMessage(sessionId, "How are you?");
        sessionService.appendAssistantMessage(sessionId, "I'm fine");
        
        var history = sessionService.getSessionHistory(sessionId, 10);
        
        assertEquals(4, history.size());
        assertEquals("Hello", history.get(0).getContent());
        assertEquals("Hi there", history.get(1).getContent());
    }

    @Test
    @DisplayName("清空会话")
    void testClearSession() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        sessionService.appendUserMessage(sessionId, "Test");
        sessionService.appendAssistantMessage(sessionId, "Response");
        
        sessionService.clearSession(sessionId);
        
        long count = messageMapper.selectCount(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
        );
        
        assertEquals(0, count);
    }

    @Test
    @DisplayName("会话过期")
    void testExpireOldSessions() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        // 手动设置为已过期
        AgentSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        session.setExpireTime(LocalDateTime.now().minusHours(1));
        sessionMapper.updateById(session);
        
        int expiredCount = sessionService.expireOldSessions();
        
        assertTrue(expiredCount > 0);
        
        AgentSession updated = sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        
        assertEquals(AgentSession.STATUS_EXPIRED, updated.getStatus());
    }

    @Test
    @DisplayName("清理过期会话")
    void testCleanupExpiredSessions() {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        
        // 设置为 8 天前过期
        AgentSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        session.setStatus(AgentSession.STATUS_EXPIRED);
        session.setLastActiveTime(LocalDateTime.now().minusDays(8));
        sessionMapper.updateById(session);
        
        sessionService.appendUserMessage(sessionId, "Old message");
        
        int cleanedCount = sessionService.cleanupExpiredSessions();
        
        assertTrue(cleanedCount > 0);
        
        // 会话应该被删除
        AgentSession deleted = sessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getSessionId, sessionId)
        );
        
        assertNull(deleted);
        
        // 消息也应该被删除
        long messageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getSessionId, sessionId)
        );
        
        assertEquals(0, messageCount);
    }
}


