package com.meowflow.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceContextHolderTest {

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    @Test
    void setAndGet_shouldWorkCorrectly() {
        TraceContext context = TraceContext.builder()
            .traceId("trace-123")
            .spanId("span-456")
            .startTime(System.currentTimeMillis())
            .build();

        TraceContextHolder.set(context);
        TraceContext retrieved = TraceContextHolder.get();

        assertNotNull(retrieved);
        assertEquals("trace-123", retrieved.getTraceId());
        assertEquals("span-456", retrieved.getSpanId());
    }

    @Test
    void get_shouldReturnNullWhenNotSet() {
        assertNull(TraceContextHolder.get());
    }

    @Test
    void getTraceId_shouldReturnNullWhenNotSet() {
        assertNull(TraceContextHolder.getTraceId());
    }

    @Test
    void setTraceId_shouldCreateContextIfNotExists() {
        TraceContextHolder.setTraceId("new-trace-id");
        assertEquals("new-trace-id", TraceContextHolder.getTraceId());
    }

    @Test
    void setTraceId_shouldUpdateExistingContext() {
        TraceContext context = TraceContext.builder()
            .traceId("old-trace-id")
            .build();
        TraceContextHolder.set(context);

        TraceContextHolder.setTraceId("new-trace-id");
        assertEquals("new-trace-id", TraceContextHolder.getTraceId());
    }

    @Test
    void getSpanId_shouldReturnNullWhenNotSet() {
        assertNull(TraceContextHolder.getSpanId());
    }

    @Test
    void remove_shouldClearContext() {
        TraceContext context = TraceContext.builder()
            .traceId("trace-123")
            .build();
        TraceContextHolder.set(context);
        assertNotNull(TraceContextHolder.get());

        TraceContextHolder.remove();
        assertNull(TraceContextHolder.get());
    }

    @Test
    void clear_shouldClearContext() {
        TraceContext context = TraceContext.builder()
            .traceId("trace-123")
            .build();
        TraceContextHolder.set(context);
        assertNotNull(TraceContextHolder.get());

        TraceContextHolder.clear();
        assertNull(TraceContextHolder.get());
    }

    @Test
    void getCurrentOrNew_shouldReturnExistingContext() {
        TraceContext context = TraceContext.builder()
            .traceId("existing-trace")
            .build();
        TraceContextHolder.set(context);

        TraceContext result = TraceContextHolder.getCurrentOrNew();
        assertEquals("existing-trace", result.getTraceId());
    }

    @Test
    void getCurrentOrNew_shouldCreateNewContextWhenNotExists() {
        TraceContext result = TraceContextHolder.getCurrentOrNew();
        assertNotNull(result.getTraceId());
    }

    @Test
    void newTrace_shouldCreateContextWithGeneratedIds() {
        TraceContext context = TraceContextHolder.newTrace();
        assertNotNull(context);
        assertNotNull(context.getTraceId());
        assertNotNull(context.getSpanId());
        assertTrue(context.getStartTime() > 0);
    }

    @Test
    void newChildSpan_shouldCreateChildContext() {
        TraceContext parent = TraceContext.builder()
            .traceId("parent-trace")
            .spanId("parent-span")
            .build();
        TraceContextHolder.set(parent);

        TraceContext child = TraceContextHolder.newChildSpan("parent-span");
        assertNotNull(child);
        assertEquals("parent-trace", child.getTraceId());
        assertEquals("parent-span", child.getParentSpanId());
        assertNotNull(child.getSpanId());
    }

    @Test
    void newChildSpan_withNullParent_shouldCreateNewTrace() {
        TraceContext child = TraceContextHolder.newChildSpan(null);
        assertNotNull(child);
        assertNotNull(child.getTraceId());
    }

    @Test
    void generateTraceId_shouldReturnNonNullId() {
        String traceId = TraceContextHolder.generateTraceId();
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    @Test
    void context_shouldSupportTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("env", "test");
        tags.put("version", "1.0");

        TraceContext context = TraceContext.builder()
            .traceId("trace-123")
            .tags(tags)
            .build();

        TraceContextHolder.set(context);
        TraceContext retrieved = TraceContextHolder.get();

        assertNotNull(retrieved.getTags());
        assertEquals("test", retrieved.getTags().get("env"));
        assertEquals("1.0", retrieved.getTags().get("version"));
    }
}
