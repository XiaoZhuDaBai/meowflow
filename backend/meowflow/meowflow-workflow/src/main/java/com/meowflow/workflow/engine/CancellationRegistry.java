package com.meowflow.workflow.engine;

import com.meowflow.common.context.CancellationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行取消令牌的注册中心。
 *
 * 每个运行中的执行（executionId）在启动时注册一个 {@link CancellationToken}；
 * cancel 请求通过 executionId 找到对应令牌并设置取消信号。
 *
 * 注意：此组件仅负责内存中的令牌管理，不持有执行状态。
 * 执行最终状态的持久化仍由 {@link com.meowflow.workflow.service.ExecutionService} 负责。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationRegistry {

    /**
     * executionId -> CancellationToken
     */
    private final Map<Long, CancellationToken> tokens = new ConcurrentHashMap<>();

    /**
     * 为指定执行注册一个取消令牌。
     *
     * @param executionId 执行 ID
     * @return 新创建的令牌
     */
    public CancellationToken register(Long executionId) {
        CancellationToken token = new CancellationToken(executionId);
        CancellationToken existing = tokens.put(executionId, token);
        if (existing != null) {
            log.warn("CancellationToken already existed for executionId={}, replacing", executionId);
        }
        log.debug("Registered CancellationToken for executionId={}", executionId);
        return token;
    }

    /**
     * 获取指定执行的取消令牌。
     *
     * @param executionId 执行 ID
     * @return 已注册的令牌；若未注册返回 null
     */
    public CancellationToken get(Long executionId) {
        return tokens.get(executionId);
    }

    /**
     * 请求取消指定执行。
     *
     * @param executionId 执行 ID
     * @param reason     取消原因（可选，可为 null）
     * @return true 表示成功发出取消信号；false 表示该执行未注册或已被取消
     */
    public boolean cancel(Long executionId, String reason) {
        CancellationToken token = tokens.get(executionId);
        if (token == null) {
            log.warn("No CancellationToken found for executionId={}, cannot cancel", executionId);
            return false;
        }
        boolean cancelled = token.cancel(reason);
        if (cancelled) {
            log.info("Cancellation requested for executionId={}, reason={}", executionId, reason);
        } else {
            log.debug("executionId={} already cancelled", executionId);
        }
        return cancelled;
    }

    /**
     * 注销令牌（执行正常结束时调用）。
     *
     * @param executionId 执行 ID
     */
    public void unregister(Long executionId) {
        CancellationToken removed = tokens.remove(executionId);
        if (removed != null) {
            log.debug("Unregistered CancellationToken for executionId={}", executionId);
        }
    }

    /**
     * 检查指定执行是否已请求取消。
     */
    public boolean isCancelled(Long executionId) {
        CancellationToken token = tokens.get(executionId);
        return token != null && token.isCancelled();
    }
}
