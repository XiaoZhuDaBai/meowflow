package com.meowflow.common.test;

import com.meowflow.common.context.TraceContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试数据构造工具 - Trace 相关。
 */
public final class TraceTestDataBuilder {

    private TraceTestDataBuilder() {
    }

    public static TraceContext createTraceContext() {
        return createTraceContext("trace-" + System.nanoTime());
    }

    public static TraceContext createTraceContext(String traceId) {
        return TraceContext.builder()
                .traceId(traceId)
                .spanId("span-001")
                .parentSpanId(null)
                .startTime(System.currentTimeMillis())
                .tags(new HashMap<>())
                .build();
    }

    public static TraceContext createTraceContextWithTag(String traceId, String key, String value) {
        TraceContext ctx = createTraceContext(traceId);
        Map<String, String> tags = new HashMap<>();
        tags.put(key, value);
        ctx.setTags(tags);
        return ctx;
    }
}
