package com.meowflow.common.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.common.result.ResultCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, msg={}, path={}, traceId={}",
                e.getCode(), e.getMessage(), request.getRequestURI(), TraceContextHolder.getTraceId());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NodeException.class)
    public Result<Void> handleNodeException(NodeException e, HttpServletRequest request) {
        log.warn("节点执行异常: nodeId={}, nodeType={}, msg={}, path={}",
                e.getNodeId(), e.getNodeType(), e.getMessage(), request.getRequestURI());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(WorkflowException.class)
    public Result<Void> handleWorkflowException(WorkflowException e, HttpServletRequest request) {
        log.warn("工作流异常: workflowId={}, executionId={}, msg={}, path={}",
                e.getWorkflowId(), e.getExecutionId(), e.getMessage(), request.getRequestURI());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(ResultCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage()));
    }

    /**
     * Sentinel 限流/熔断触发统一处理
     */
    @ExceptionHandler(BlockException.class)
    public ResponseEntity<Result<Void>> handleSentinelBlock(BlockException e) {
        log.warn("Sentinel 触发: rule={} resource={}", e.getRule(), e.getRule() != null ? e.getRule().getResource() : "");
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ResultCode code = ResultCode.RATE_LIMITED;
        if (e.getClass().getSimpleName().contains("Degrade")) {
            code = ResultCode.CIRCUIT_BREAKER_OPEN;
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        return ResponseEntity.status(status).body(Result.error(code));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: path={}", request.getRequestURI(), e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), "服务器内部错误: " + e.getMessage());
    }
}