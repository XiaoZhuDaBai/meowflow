package com.meowflow.gateway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveMdcUtilsTest {

    @Test
    void putTraceId_shouldWorkWithoutException() {
        reactor.util.context.Context context = reactor.util.context.Context.empty();
        reactor.util.context.Context result = ReactiveMdcUtils.putTraceId(context, "test-trace-id");
        assertNotNull(result);
    }

    @Test
    void mdcCleaner_shouldReturnRunnable() {
        Runnable cleaner = ReactiveMdcUtils.mdcCleaner();
        assertNotNull(cleaner);
        assertDoesNotThrow(() -> cleaner.run());
    }

    @Test
    void putTraceId_shouldHandleEmptyContext() {
        reactor.util.context.Context empty = reactor.util.context.Context.empty();
        reactor.util.context.Context result = ReactiveMdcUtils.putTraceId(empty, "trace-123");
        assertNotNull(result);
    }

    @Test
    void mdcCleaner_shouldBeIdempotent() {
        Runnable cleaner = ReactiveMdcUtils.mdcCleaner();
        assertDoesNotThrow(() -> {
            cleaner.run();
            cleaner.run();
        });
    }
}
