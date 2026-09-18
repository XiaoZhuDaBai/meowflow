package com.meowflow.common.web;

import com.meowflow.common.context.TraceContext;
import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.trace.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-User-Name";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SPAN_ID = "X-Span-Id";
    public static final String HEADER_ROLES = "X-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(HEADER_TRACE_ID);
            String spanId = request.getHeader(HEADER_SPAN_ID);
            if (traceId == null || traceId.isEmpty()) {
                traceId = TraceIdGenerator.generate();
            }
            if (spanId == null || spanId.isEmpty()) {
                spanId = TraceIdGenerator.generateSpanId();
            }
            TraceContext traceCtx = TraceContext.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .startTime(System.currentTimeMillis())
                    .build();
            TraceContextHolder.set(traceCtx);

            String userIdStr = request.getHeader(HEADER_USER_ID);
            if (userIdStr != null && !userIdStr.isEmpty()) {
                Long userId = parseLongOrNull(userIdStr);
                UserContext userCtx = UserContext.builder()
                        .userId(userId)
                        .username(request.getHeader(HEADER_USERNAME))
                        .orgId(parseLongOrNull(request.getHeader(HEADER_ORG_ID)))
                        .build();
                UserContextHolder.set(userCtx);
            }

            response.setHeader(HEADER_TRACE_ID, traceId);
            response.setHeader(HEADER_SPAN_ID, spanId);

            chain.doFilter(request, response);
        } finally {
            UserContextHolder.remove();
            TraceContextHolder.remove();
        }
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}