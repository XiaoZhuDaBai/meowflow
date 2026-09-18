package com.meowflow.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.meowflow.common.trace.TraceIdGenerator;

public final class TraceContextHolder {

    private static final TransmittableThreadLocal<TraceContext> CONTEXT = new TransmittableThreadLocal<>();

    private TraceContextHolder() {
    }

    public static void set(TraceContext context) {
        CONTEXT.set(context);
    }

    public static TraceContext get() {
        return CONTEXT.get();
    }

    public static void remove() {
        CONTEXT.remove();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static String getTraceId() {
        TraceContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getTraceId();
    }

    public static void setTraceId(String traceId) {
        TraceContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = TraceContext.builder().traceId(traceId).build();
            CONTEXT.set(ctx);
        } else {
            ctx.setTraceId(traceId);
        }
    }

    public static String getSpanId() {
        TraceContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getSpanId();
    }

    public static TraceContext getCurrentOrNew() {
        TraceContext ctx = CONTEXT.get();
        if (ctx == null) {
            return newTrace();
        }
        return ctx;
    }

    public static TraceContext newTrace() {
        TraceContext ctx = TraceContext.builder()
                .traceId(TraceIdGenerator.generate())
                .spanId(TraceIdGenerator.generateSpanId())
                .startTime(System.currentTimeMillis())
                .build();
        CONTEXT.set(ctx);
        return ctx;
    }

    public static TraceContext newChildSpan(String parentSpanId) {
        TraceContext parent = CONTEXT.get();
        TraceContext ctx = TraceContext.builder()
                .traceId(parent != null ? parent.getTraceId() : TraceIdGenerator.generate())
                .parentSpanId(parentSpanId != null ? parentSpanId : (parent != null ? parent.getSpanId() : null))
                .spanId(TraceIdGenerator.generateSpanId())
                .startTime(System.currentTimeMillis())
                .build();
        CONTEXT.set(ctx);
        return ctx;
    }

    public static String generateTraceId() {
        return TraceIdGenerator.generate();
    }
}