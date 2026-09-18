package com.meowflow.gateway.util;

import org.slf4j.MDC;
import reactor.util.context.Context;

import java.util.function.Consumer;

/**
 * 响应式场景下的 MDC 工具类
 *
 * <p>Spring Cloud Gateway 基于 WebFlux（Reactor），MDC 需要通过 Context 传递。</p>
 * <p>本工具类提供了将 MDC 写入 Context 并在订阅时恢复的能力。</p>
 */
public class ReactiveMdcUtils {

    private ReactiveMdcUtils() {}

    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 将 traceId 写入 MDC 并放入 Context
     */
    public static Context putTraceId(Context ctx, String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return ctx.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 清理 MDC（结束时执行）
     */
    public static Runnable mdcCleaner() {
        return () -> MDC.remove(TRACE_ID_KEY);
    }
}