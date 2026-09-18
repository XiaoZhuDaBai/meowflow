package com.meowflow.common.trace;

import com.meowflow.common.context.TraceContext;
import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Aspect
@Component
public class TraceAspect {

    @Around("@annotation(traceNode)")
    public Object around(ProceedingJoinPoint joinPoint, TraceNode traceNode) throws Throwable {
        long start = System.currentTimeMillis();
        TraceContext ctx = TraceContextHolder.get();
        String traceId = ctx != null ? ctx.getTraceId() : TraceIdGenerator.generate();
        String parentSpan = ctx != null ? ctx.getSpanId() : null;
        String spanId = TraceIdGenerator.generateSpanId();

        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        String nodeName = traceNode.value().isEmpty() ? method.getName() : traceNode.value();

        Throwable error = null;
        Object result = null;
        try {
            if (traceNode.recordInput()) {
                Object[] args = joinPoint.getArgs();
                String masked = maskFields(args, traceNode.inputMaskFields());
                log.debug("[TraceNode start] traceId={} spanId={} node={} category={} input={}",
                        traceId, spanId, nodeName, traceNode.category(), masked);
            }
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (error != null) {
                log.warn("[TraceNode failed] traceId={} spanId={} parentSpan={} node={} category={} duration={}ms error={}",
                        traceId, spanId, parentSpan, nodeName, traceNode.category(), duration, error.getMessage());
                asyncSaveTraceLog(buildTraceLog(traceId, spanId, parentSpan, nodeName, traceNode.category(),
                        start, duration, "FAILED", null, null, error.getMessage(), getStackTrace(error)));
            } else {
                String output = traceNode.recordOutput() && result != null
                        ? maskSingle(result, traceNode.outputMaskFields())
                        : "";
                log.debug("[TraceNode success] traceId={} spanId={} parentSpan={} node={} category={} duration={}ms output={}",
                        traceId, spanId, parentSpan, nodeName, traceNode.category(), duration, output);
                asyncSaveTraceLog(buildTraceLog(traceId, spanId, parentSpan, nodeName, traceNode.category(),
                        start, duration, "SUCCESS", null, output, null, null));
            }
        }
    }

    /**
     * 构建 TraceLog 对象
     */
    private TraceLog buildTraceLog(String traceId, String spanId, String parentSpanId,
                                   String nodeName, String category, long startTime,
                                   long duration, String status, String inputData,
                                   String outputData, String errorMsg, String errorStack) {
        return TraceLog.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .nodeName(nodeName)
                .category(category)
                .startTime(startTime)
                .endTime(startTime + duration)
                .duration(duration)
                .status(status)
                .inputData(inputData)
                .outputData(outputData)
                .errorMsg(errorMsg)
                .errorStack(errorStack)
                .userId(UserContextHolder.getUserId())
                .build();
    }

    /**
     * 异步保存 TraceLog
     */
    private void asyncSaveTraceLog(TraceLog traceLog) {
        CompletableFuture.runAsync(() -> {
            try {
                // 输出到日志，格式化为 JSON 便于收集和分析
                log.info("[TRACE_LOG] {}", traceLog.toJson());
            } catch (Exception e) {
                log.error("异步保存TraceLog失败: traceId={}", traceLog.getTraceId(), e);
            }
        });
    }

    private String maskFields(Object[] args, String[] maskFields) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            return JsonUtils.toJson(args);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private String maskSingle(Object obj, String[] maskFields) {
        if (obj == null) {
            return "";
        }
        try {
            return JsonUtils.toJson(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String getStackTrace(Throwable t) {
        if (t == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("... (truncated)");
                break;
            }
        }
        return sb.toString();
    }
}