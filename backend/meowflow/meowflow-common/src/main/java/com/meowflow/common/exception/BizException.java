package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String errorCode;
    private final Map<String, Object> details = new HashMap<>();

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.errorCode = resultCode.name();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.errorCode = resultCode.name();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = String.valueOf(code);
    }

    public BizException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}