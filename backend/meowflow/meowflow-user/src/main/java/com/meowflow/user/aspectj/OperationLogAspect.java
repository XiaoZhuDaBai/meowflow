package com.meowflow.user.aspectj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.user.entity.AuditLog;
import com.meowflow.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final IdGeneratorFactory idGenerator;

    @Around("@annotation(com.meowflow.user.aspectj.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(UserContextHolder.getUsername());
        auditLog.setModule(operationLog.module());
        auditLog.setMethod(operationLog.method());
        auditLog.setRequestMethod(getRequestMethod());

        HttpServletRequest request = getRequest();
        if (request != null) {
            auditLog.setRequestUrl(request.getRequestURI());
            auditLog.setIp(getClientIp(request));
        }

        if (operationLog.saveParams()) {
            auditLog.setRequestParams(getParams(joinPoint));
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            auditLog.setStatus("success");

            if (operationLog.saveResult() && result != null) {
                auditLog.setResponseResult(objectMapper.writeValueAsString(result));
            }

            return result;
        } catch (Exception e) {
            auditLog.setStatus("fail");
            auditLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            auditLog.setCostTime(endTime - startTime);
            auditLog.setOperateTime(LocalDateTime.now());

            try {
                auditLogRepository.insert(auditLog);
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getRequestMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : "UNKNOWN";
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip.split(",")[0].trim();
    }

    private String getParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "Failed to serialize params";
        }
    }
}
