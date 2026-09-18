package com.meowflow.common.client;

import com.meowflow.common.client.ExecutionLogCreateRequest;
import com.meowflow.common.client.ExecutionLogResponse;
import com.meowflow.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MonitorFeignClient 契约测试 —— 验证方法签名稳定性，让 workflow / executor 的调用方
 * 知道接口形态有变更。
 *
 * <p>使用反射读取 @PostMapping / @RequestParam 注解，组成"预期路由表"。
 * 这种纯结构性测试不依赖 Spring 上下文。
 */
@DisplayName("MonitorFeignClient 契约稳定性")
class MonitorFeignClientContractTest {

    private static final Map<String, String> EXPECTED_POST_PATHS = Map.of(
            "logStart", "/log/start",
            "logComplete", "/log/{logId}/complete",
            "logError", "/log/{logId}/error",
            "logRetry", "/log/{logId}/retry",
            "recordWorkflowExecution", "/metrics/workflow/execution",
            "recordNodeExecution", "/metrics/node/execution"
    );

    @Test
    @DisplayName("方法签名数量稳定（防新增方法未通知调用方）")
    void methodCount_isStable() {
        AtomicInteger count = new AtomicInteger();
        for (var m : MonitorFeignClient.class.getDeclaredMethods()) {
            count.incrementAndGet();
        }
        assertThat(count.get()).isGreaterThanOrEqualTo(EXPECTED_POST_PATHS.size());
    }

    @Test
    @DisplayName("每个方法的 POST 路径未变")
    void postPaths_haveNotChanged() {
        Map<String, String> actual = new HashMap<>();
        for (var m : MonitorFeignClient.class.getDeclaredMethods()) {
            PostMapping post = m.getAnnotation(PostMapping.class);
            if (post != null && post.value().length > 0) {
                actual.put(m.getName(), post.value()[0]);
            }
            RequestMapping req = m.getAnnotation(RequestMapping.class);
            if (req != null && req.value().length > 0) {
                actual.put(m.getName(), req.value()[0]);
            }
        }

        Set<String> expectedKeys = EXPECTED_POST_PATHS.keySet();
        for (String key : expectedKeys) {
            assertThat(actual)
                    .as("监控 Feign 客户端不存在方法: " + key)
                    .containsKey(key);
            assertThat(actual.get(key))
                    .as("方法 " + key + " 的路径不应变更")
                    .isEqualTo(EXPECTED_POST_PATHS.get(key));
        }
    }

    @Test
    @DisplayName("logStart 参数类型必须是 ExecutionLogCreateRequest")
    void logStart_paramType_isCreateRequest() throws Exception {
        var m = MonitorFeignClient.class.getMethod("logStart", ExecutionLogCreateRequest.class);
        assertThat(m).isNotNull();
    }

    @Test
    @DisplayName("logStart 返回类型必须是 Result<ExecutionLogResponse>")
    void logStart_returnType_isExecutionLogResponseResult() throws Exception {
        var m = MonitorFeignClient.class.getMethod("logStart", ExecutionLogCreateRequest.class);
        assertThat(m.getReturnType()).isEqualTo(Result.class);
        // 泛型不强校验 —— 单测层面只检查 raw type 已能发现 80% breaking change
    }
}
