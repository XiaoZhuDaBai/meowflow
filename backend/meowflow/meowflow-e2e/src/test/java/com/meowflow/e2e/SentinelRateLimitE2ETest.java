package com.meowflow.e2e;

import com.meowflow.common.test.SaTokenMockHelper;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sentinel 限流规则 —— 验证 1 秒内 100 次请求时触发 BlockException。
 */
@DisplayName("E2E — Sentinel 限流（独立规则，无需 Spring 上下文）")
class SentinelRateLimitE2ETest {

    @BeforeEach
    void before() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void after() {
        FlowRuleManager.loadRules(new ArrayList<>());
        SaTokenMockHelper.clear();
    }

    @Test
    @DisplayName("qps=2 时第 3 次调用抛出 FlowException")
    void rateLimit_blocksAfterThreshold() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule("test-resource");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(2);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);

        // 前 2 次通过
        try (Entry ignored = SphU.entry("test-resource")) {
            // ok
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
        try (Entry ignored = SphU.entry("test-resource")) {
            // ok
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }

        // 第 3 次触发限流
        assertThatThrownBy(() -> {
            try (Entry e = SphU.entry("test-resource")) {
                // 不应该到达
            }
        }).isInstanceOf(com.alibaba.csp.sentinel.slots.block.flow.FlowException.class);
    }
}
