package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import lombok.Getter;

@Getter
public class NodeException extends BizException {

    private static final long serialVersionUID = 1L;

    private final String nodeId;
    private final String nodeType;

    public NodeException(String nodeId, String nodeType, String message) {
        super(ResultCode.NODE_EXECUTE_ERROR, message);
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        addDetail("nodeId", nodeId);
        addDetail("nodeType", nodeType);
    }

    public NodeException(String nodeId, String nodeType, String message, Throwable cause) {
        super(ResultCode.NODE_EXECUTE_ERROR, message);
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        initCause(cause);
    }
}