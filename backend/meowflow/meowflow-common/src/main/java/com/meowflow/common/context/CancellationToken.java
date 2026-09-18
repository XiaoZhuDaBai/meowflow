package com.meowflow.common.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 执行取消令牌。
 *
 * 与 CancellationRegistry 配合使用：
 * - Engine 启动执行时创建令牌并注册到 Registry
 * - 节点执行器在外向调用（HTTP/LLM）前检查 isCancelled()
 * - 外部 cancel 请求通过 Registry 找到令牌并调用 cancel()
 */
@Getter
@RequiredArgsConstructor
public class CancellationToken {

    private final Long executionId;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private volatile String cancelReason;

    public boolean cancel(String reason) {
        if (cancelled.compareAndSet(false, true)) {
            this.cancelReason = reason;
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
