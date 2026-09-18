package com.meowflow.common.trace;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class TraceIdGenerator {

    private static final AtomicLong SPAN_COUNTER = new AtomicLong(0);
    private static final String PID;

    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        PID = name.contains("@") ? name.split("@")[0] : "0";
    }

    private TraceIdGenerator() {
    }

    public static String generate() {
        long time = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(10000);
        return PID + time + String.format("%04d", rand);
    }

    public static String generateSpanId() {
        return System.currentTimeMillis() + "." + SPAN_COUNTER.incrementAndGet();
    }
}