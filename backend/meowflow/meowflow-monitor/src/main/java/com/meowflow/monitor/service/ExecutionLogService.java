package com.meowflow.monitor.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.monitor.dto.ExecutionLogCreateRequest;
import com.meowflow.monitor.dto.ExecutionLogDTO;
import com.meowflow.monitor.dto.ExecutionLogQuery;
import com.meowflow.monitor.entity.ExecutionLog;
import com.meowflow.monitor.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionLogService {

    private final ExecutionLogRepository logRepository;

    @Async
    public void logStartAsync(ExecutionLogCreateRequest request) {
        try {
            ExecutionLog executionLog = new ExecutionLog();
            executionLog.setExecutionId(request.getExecutionId() != null ? Long.parseLong(request.getExecutionId()) : null);
            executionLog.setNodeId(request.getNodeId());
            executionLog.setLevel("info");
            executionLog.setMessage("Execution started");
            executionLog.setCreatedAt(LocalDateTime.now());

            logRepository.insert(executionLog);
            log.debug("Execution log started: executionId={}, nodeId={}", request.getExecutionId(), request.getNodeId());
        } catch (Exception e) {
            log.error("Failed to log execution start", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ExecutionLog logStart(ExecutionLogCreateRequest request) {
        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setExecutionId(request.getExecutionId() != null ? Long.parseLong(request.getExecutionId()) : null);
        executionLog.setNodeId(request.getNodeId());
        executionLog.setLevel("info");
        executionLog.setMessage("Execution started");
        executionLog.setCreatedAt(LocalDateTime.now());

        logRepository.insert(executionLog);
        log.info("Execution log created: {}", executionLog.getId());

        return executionLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExecutionLog logComplete(Long logId, String outputData) {
        ExecutionLog executionLog = logRepository.findById(logId);
        if (executionLog == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "执行日志不存在");
        }

        executionLog.setLevel("info");
        executionLog.setMessage("Execution completed");
        executionLog.setPayload(outputData);

        logRepository.updateById(executionLog);
        log.info("Execution completed: logId={}", logId);

        return executionLog;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExecutionLog logError(Long logId, String errorMessage) {
        ExecutionLog executionLog = logRepository.findById(logId);
        if (executionLog == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "执行日志不存在");
        }

        executionLog.setLevel("error");
        executionLog.setMessage(errorMessage);

        logRepository.updateById(executionLog);
        log.error("Execution failed: logId={}, error={}", logId, errorMessage);

        return executionLog;
    }

    public IPage<ExecutionLogDTO> queryLogs(ExecutionLogQuery query) {
        Page<ExecutionLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<ExecutionLog> result;

        if (StrUtil.isNotBlank(query.getExecutionId())) {
            List<ExecutionLog> logs = logRepository.findByExecutionId(Long.parseLong(query.getExecutionId()));
            result = new Page<>(1, logs.size());
            result.setRecords(logs);
            result.setTotal(logs.size());
        } else if (query.getStartTime() != null && query.getEndTime() != null) {
            result = logRepository.findByTimeRange(page, query.getStartTime(), query.getEndTime());
        } else {
            result = new Page<>(query.getPageNum(), query.getPageSize());
            result.setRecords(List.of());
            result.setTotal(0);
        }

        return result.convert(this::convertToDTO);
    }

    public List<ExecutionLogDTO> getExecutionLogs(String executionId) {
        List<ExecutionLog> logs = logRepository.findByExecutionId(Long.parseLong(executionId));
        return logs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ExecutionLogDTO> getExecutionLogs(Long executionId) {
        List<ExecutionLog> logs = logRepository.findByExecutionId(executionId);
        return logs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ExecutionLogDTO> getNodeLogs(Long executionId, String nodeId) {
        List<ExecutionLog> logs = logRepository.findByExecutionIdAndNodeId(executionId, nodeId);
        return logs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public IPage<ExecutionLogDTO> getErrorLogs(Integer pageNum, Integer pageSize) {
        Page<ExecutionLog> page = new Page<>(pageNum, pageSize);
        IPage<ExecutionLog> result = logRepository.findErrorLogs(page);
        return result.convert(this::convertToDTO);
    }

    private ExecutionLogDTO convertToDTO(ExecutionLog log) {
        ExecutionLogDTO dto = new ExecutionLogDTO();
        dto.setId(log.getId());
        dto.setExecutionId(log.getExecutionId());
        dto.setNodeId(log.getNodeId());
        dto.setLevel(log.getLevel());
        dto.setMessage(log.getMessage());
        dto.setPayload(log.getPayload());
        dto.setErrorMessage("error".equalsIgnoreCase(log.getLevel()) ? log.getMessage() : null);
        dto.setStatus(log.getStatus());
        dto.setOutputData(log.getOutputData());
        dto.setCreateTime(log.getCreatedAt());
        return dto;
    }

    private String getCurrentUserId() {
        return Optional.ofNullable(UserContextHolder.get())
                .map(ctx -> String.valueOf(ctx.getUserId()))
                .orElse("system");
    }
}

