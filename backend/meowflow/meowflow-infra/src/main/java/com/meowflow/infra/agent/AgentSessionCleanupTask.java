package com.meowflow.infra.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agent 会话清理定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSessionCleanupTask {

    private final AgentSessionService sessionService;

    /**
     * 每小时标记过期会话
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireOldSessions() {
        try {
            int count = sessionService.expireOldSessions();
            if (count > 0) {
                log.info("Expired {} agent sessions", count);
            }
        } catch (Exception e) {
            log.error("Failed to expire old sessions", e);
        }
    }

    /**
     * 每天凌晨 3 点清理过期会话（超过 7 天未活跃）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredSessions() {
        try {
            int count = sessionService.cleanupExpiredSessions();
            if (count > 0) {
                log.info("Cleaned up {} expired agent sessions", count);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
        }
    }
}
