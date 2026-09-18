package com.meowflow.executor.service;

import cn.hutool.json.JSONUtil;
import com.meowflow.common.client.ExecutionLogCreateRequest;
import com.meowflow.common.client.ExecutionLogResponse;
import com.meowflow.common.client.MonitorFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 监控服务封装
 * 封装对 Monitor 服务的 Feign 调用，提供优雅降级
 */
@Slf4j
@Service
public class MonitorService {

    @Autowired(required = false)
    private MonitorFeignClient monitorClient;

    @Value("${meowflow.monitor.enabled:true}")
    private boolean monitorEnabled;

    /**
     * 记录任务开始执行
     */
    public ExecutionLogResponse logStart(String logId, ExecutionLogCreateRequest request) {
        if (!monitorEnabled || monitorClient == null) {
            log.debug("Monitor disabled or client not available, skip logging start");
            return null;
        }

        try {
            return monitorClient.logStart(request).getData();
        } catch (Exception e) {
            log.warn("Failed to log task start: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 记录任务完成
     */
    public ExecutionLogResponse logComplete(String logId, String outputData) {
        if (!monitorEnabled || monitorClient == null || logId == null) {
            return null;
        }

        try {
            return monitorClient.logComplete(logId, outputData).getData();
        } catch (Exception e) {
            log.warn("Failed to log task complete: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 记录任务错误
     */
    public ExecutionLogResponse logError(String logId, String errorMessage) {
        if (!monitorEnabled || monitorClient == null || logId == null) {
            return null;
        }

        try {
            return monitorClient.logError(logId, errorMessage).getData();
        } catch (Exception e) {
            log.warn("Failed to log task error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 记录工作流执行指标
     */
    public void recordWorkflowExecution(String workflowId, String workflowName,
                                        Integer version, String triggerType,
                                        boolean success, long durationMs) {
        if (!monitorEnabled || monitorClient == null) {
            return;
        }

        try {
            monitorClient.recordWorkflowExecution(workflowId, workflowName, version,
                    triggerType, success, durationMs);
        } catch (Exception e) {
            log.warn("Failed to record workflow execution: {}", e.getMessage());
        }
    }

    /**
     * 记录节点执行指标
     */
    public void recordNodeExecution(String workflowId, String nodeId,
                                    String nodeType, boolean success, long durationMs) {
        if (!monitorEnabled || monitorClient == null) {
            return;
        }

        try {
            monitorClient.recordNodeExecution(workflowId, nodeId, nodeType, success, durationMs);
        } catch (Exception e) {
            log.warn("Failed to record node execution: {}", e.getMessage());
        }
    }

    /**
     * 构建执行日志创建请求
     */
    public ExecutionLogCreateRequest buildStartRequest(String workflowId, String workflowName,
                                                       String nodeId, String nodeName,
                                                       String nodeType, String inputData) {
        ExecutionLogCreateRequest request = new ExecutionLogCreateRequest();
        request.setWorkflowId(workflowId);
        request.setWorkflowName(workflowName);
        request.setNodeId(nodeId);
        request.setNodeName(nodeName);
        request.setNodeType(nodeType);
        request.setInputData(inputData);
        request.setStartTime(LocalDateTime.now());
        return request;
    }
}
