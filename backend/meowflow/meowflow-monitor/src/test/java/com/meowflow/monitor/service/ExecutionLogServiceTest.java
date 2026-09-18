package com.meowflow.monitor.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.monitor.dto.ExecutionLogCreateRequest;
import com.meowflow.monitor.dto.ExecutionLogQuery;
import com.meowflow.monitor.entity.ExecutionLog;
import com.meowflow.monitor.repository.ExecutionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ExecutionLogService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("执行日志服务测试")
class ExecutionLogServiceTest {

    @Mock
    private ExecutionLogRepository logRepository;

    @InjectMocks
    private ExecutionLogService executionLogService;

    private ExecutionLogCreateRequest createRequest;
    private ExecutionLog executionLog;

    @BeforeEach
    void setUp() {
        createRequest = new ExecutionLogCreateRequest();
        createRequest.setExecutionId("123");
        createRequest.setWorkflowId("456");
        createRequest.setNodeId("node-789");
        createRequest.setNodeName("LLM Node");
        createRequest.setNodeType("LLM");
        createRequest.setInputData("{\"prompt\": \"Hello\"}");

        executionLog = new ExecutionLog();
        executionLog.setId(1L);
        executionLog.setExecutionId(123L);
        executionLog.setNodeId("node-789");
        executionLog.setNodeName("LLM Node");
        executionLog.setNodeType("LLM");
        executionLog.setLevel("info");
        executionLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("测试记录执行开始")
    void testLogStart() {
        when(logRepository.insert(any(ExecutionLog.class))).thenAnswer(invocation -> {
            ExecutionLog inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        });

        ExecutionLog result = executionLogService.logStart(createRequest);

        assertNotNull(result);
        assertEquals("info", result.getLevel());
        assertEquals("Execution started", result.getMessage());

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository, times(1)).insert(captor.capture());
        ExecutionLog captured = captor.getValue();
        assertEquals(123L, captured.getExecutionId());
        assertEquals("node-789", captured.getNodeId());
        assertNotNull(captured.getCreatedAt());
    }

    @Test
    @DisplayName("测试记录执行完成")
    void testLogComplete() {
        Long logId = 1L;
        String outputData = "{\"result\": \"Hello, world!\"}";

        when(logRepository.findById(logId)).thenReturn(executionLog);
        when(logRepository.updateById(any(ExecutionLog.class))).thenReturn(1);

        ExecutionLog result = executionLogService.logComplete(logId, outputData);

        assertNotNull(result);

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).updateById(captor.capture());
    }

    @Test
    @DisplayName("测试记录执行完成 - 日志不存在")
    void testLogComplete_LogNotFound() {
        Long logId = 999L;

        when(logRepository.findById(logId)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> {
            executionLogService.logComplete(logId, "output");
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
        verify(logRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("测试记录执行错误")
    void testLogError() {
        Long logId = 1L;
        String errorMessage = "Connection timeout";

        when(logRepository.findById(logId)).thenReturn(executionLog);
        when(logRepository.updateById(any(ExecutionLog.class))).thenReturn(1);

        ExecutionLog result = executionLogService.logError(logId, errorMessage);

        assertNotNull(result);

        ArgumentCaptor<ExecutionLog> captor = ArgumentCaptor.forClass(ExecutionLog.class);
        verify(logRepository).updateById(captor.capture());
        assertEquals("error", captor.getValue().getLevel());
    }

    @Test
    @DisplayName("测试记录执行错误 - 日志不存在")
    void testLogError_LogNotFound() {
        Long logId = 999L;

        when(logRepository.findById(logId)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> {
            executionLogService.logError(logId, "error");
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
        verify(logRepository, never()).updateById(any());
    }

    @Test
    @DisplayName("测试获取执行日志列表")
    void testGetExecutionLogs() {
        String executionId = "123";

        ExecutionLog log1 = new ExecutionLog();
        log1.setId(1L);
        log1.setExecutionId(123L);
        log1.setNodeId("node-1");
        log1.setNodeName("Node 1");

        ExecutionLog log2 = new ExecutionLog();
        log2.setId(2L);
        log2.setExecutionId(123L);
        log2.setNodeId("node-2");
        log2.setNodeName("Node 2");

        List<ExecutionLog> logs = Arrays.asList(log1, log2);
        when(logRepository.findByExecutionId(123L)).thenReturn(logs);

        var result = executionLogService.getExecutionLogs(executionId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("node-1", result.get(0).getNodeId());
        assertEquals("node-2", result.get(1).getNodeId());

        verify(logRepository, times(1)).findByExecutionId(123L);
    }

    @Test
    @DisplayName("测试查询条件 - 按执行ID查询")
    void testQueryLogs_ByExecutionId() {
        ExecutionLogQuery query = new ExecutionLogQuery();
        query.setExecutionId("123");
        query.setPageNum(1);
        query.setPageSize(20);

        ExecutionLog log1 = new ExecutionLog();
        log1.setId(1L);
        log1.setExecutionId(123L);

        List<ExecutionLog> logs = Arrays.asList(log1);
        when(logRepository.findByExecutionId(123L)).thenReturn(logs);

        var result = executionLogService.queryLogs(query);

        assertNotNull(result);
        verify(logRepository).findByExecutionId(123L);
    }

    @Test
    @DisplayName("测试查询条件 - 无条件查询")
    void testQueryLogs_NoCondition() {
        ExecutionLogQuery query = new ExecutionLogQuery();
        query.setPageNum(1);
        query.setPageSize(20);

        var result = executionLogService.queryLogs(query);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }
}
