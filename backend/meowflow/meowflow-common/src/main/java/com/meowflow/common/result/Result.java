package com.meowflow.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.meowflow.common.context.TraceContext;
import com.meowflow.common.context.TraceContextHolder;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String traceId;

    public Result() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = getCurrentTraceId();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(ResultCode rc) {
        Result<T> r = new Result<>();
        r.setCode(rc.getCode());
        r.setMessage(rc.getMessage());
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> error(ResultCode rc, String message) {
        Result<T> r = new Result<>();
        r.setCode(rc.getCode());
        r.setMessage(message);
        return r;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    private static String getCurrentTraceId() {
        try {
            TraceContext ctx = TraceContextHolder.get();
            return ctx != null ? ctx.getTraceId() : null;
        } catch (Exception ignore) {
            return null;
        }
    }
}