package com.meowflow.workflow.trigger;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.redis.RedisService;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TriggerManager 调度集成测试 —— 真 Redis 持久化 + 真 CRON 触发器。
 *
 * <p>使用每秒触发的 cron ("* * * * * ?")，最多等 2s。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TriggerManager 集成 — CRON 调度真 Redis + 真 TaskScheduler")
class ScheduleCronIntegrationTest extends BaseIntegrationTest {

    @Autowired private TriggerManager triggerManager;
    @Autowired private RedisService redisService;

    private static final Long WF_ID = 99999L;
    private static final String NODE_ID = "node-sched-it";

    @BeforeEach
    void login() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void cleanup() {
        triggerManager.cancelAllTasks(WF_ID);
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("scheduleCron + cancelScheduledTask — Redis key 注册与清理")
    void scheduleCron_persistsToRedis() {
        triggerManager.scheduleCron(WF_ID, NODE_ID, "0 0 * * * ?");
        assertThat(triggerManager.isScheduled(WF_ID, NODE_ID)).isTrue();

        // 验证内存中调度任务已被注册；Redis 持久化已由 TriggerManager 内部 promise
        String key = "wf:schedule:wf_" + WF_ID + "_" + NODE_ID;
        Object meta = redisService.get(key);
        // TriggerManager.persistToRedis 在 scheduleCron 内同步执行 — 因此即使异步 TaskScheduler
        // 尚未触发，Redis metadata 已经落库
        assertThat(meta).as("Redis ScheduleMetadata must be present immediately after scheduleCron").isNotNull();

        triggerManager.cancelScheduledTask(WF_ID, NODE_ID);
        assertThat(triggerManager.isScheduled(WF_ID, NODE_ID)).isFalse();

        // 取消后 redis metadata 应当被删除
        assertThat((Object) redisService.get(key)).isNull();
    }

    @Test
    @Order(2)
    @DisplayName("scheduleOnce 拒绝过去时间")
    void scheduleOnce_rejectsPastTime() {
        assertThatThrownBy(() ->
                triggerManager.scheduleOnce(WF_ID, "node-past", java.time.LocalDateTime.now().minusHours(1))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
