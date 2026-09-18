package com.meowflow.workflow.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.engine.ExecutionResult;
import com.meowflow.workflow.engine.CancellationRegistry;
import com.meowflow.workflow.engine.WorkflowEngine;
import com.meowflow.workflow.entity.Execution;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.NodeExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExecutionService Unit Tests")
class ExecutionServiceUnitTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private NodeExecutionRepository nodeExecutionRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowVersionRepository versionRepository;

    @Mock
    private WorkflowEngine workflowEngine;

    @Mock
    private CancellationRegistry cancellationRegistry;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HumanTaskService humanTaskService;

    @InjectMocks
    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        // 默认设置：versionRepository 返回存在
        WorkflowVersion v1 = new WorkflowVersion();
        v1.setId(1L);
        v1.setWorkflowId(1L);
        v1.setVersion("v1");
        v1.setDefinition("{}");
        v1.setPublishStatus("published");
        when(versionRepository.findByWorkflowIdAndVersion(any(), any()))
                .thenReturn(Optional.of(v1));

        when(applicationContext.getBean(ExecutionService.class)).thenReturn(executionService);

        // 让 insert 返回 ID=1L
        doAnswer(invocation -> {
            Execution execution = invocation.getArgument(0);
            execution.setId(1L);
            return 1;
        }).when(executionRepository).insert(any(Execution.class));
    }

    @Test
    @DisplayName("execute - 工作流不存在抛异常")
    void execute_workflowNotFound_throws() {
        when(workflowRepository.selectById(99L)).thenReturn(null);

        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(99L);

        assertThatThrownBy(() -> executionService.execute(req, 1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("execute - 工作流非 running 状态抛异常")
    void execute_workflowNotRunning_throws() {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setStatus("draft");
        wf.setCurrentVersion("v1");
        when(workflowRepository.selectById(1L)).thenReturn(wf);

        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(1L);

        assertThatThrownBy(() -> executionService.execute(req, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未发布");
    }

    @Test
    @DisplayName("execute - 同步执行成功")
    void execute_sync_succeeds() {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setStatus("running");
        wf.setCurrentVersion("v1");
        when(workflowRepository.selectById(1L)).thenReturn(wf);

        ExecutionResult result = ExecutionResult.success(1L, 1L, "v1", Map.of(), Map.of(), 100L);
        when(workflowEngine.execute(any())).thenReturn(result);

        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(1L);
        req.setInput(Map.of("k", "v"));

        ExecutionResponse response = executionService.execute(req, 1L);

        assertThat(response.getExecutionId()).isNotNull();
        assertThat(response.getWorkflowId()).isEqualTo(1L);
        verify(executionRepository).insert(any(Execution.class));
        verify(workflowEngine).execute(any());
    }

    @Test
    @DisplayName("execute - 指定 version")
    void execute_specificVersion_usesProvided() {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setStatus("running");
        wf.setCurrentVersion("v1");
        when(workflowRepository.selectById(1L)).thenReturn(wf);

        ExecutionResult result = ExecutionResult.success(1L, 1L, "v2", Map.of(), Map.of(), 50L);
        when(workflowEngine.execute(any())).thenReturn(result);

        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(1L);
        req.setVersion("v2");

        ExecutionResponse response = executionService.execute(req, 1L);

        assertThat(response.getVersion()).isEqualTo("v2");
        verify(versionRepository).findByWorkflowIdAndVersion(1L, "v2");
    }

    @Test
    @DisplayName("execute - 异步执行路径")
    void execute_async_invokesAsyncMethod() {
        Workflow wf = new Workflow();
        wf.setId(1L);
        wf.setStatus("running");
        wf.setCurrentVersion("v1");
        when(workflowRepository.selectById(1L)).thenReturn(wf);

        Execution execution = new Execution();
        execution.setId(1L);
        execution.setWorkflowId(1L);
        execution.setStatus("pending");
        when(executionRepository.selectById(1L)).thenReturn(execution);

        when(workflowEngine.execute(any())).thenReturn(
                ExecutionResult.success(1L, 1L, "v1", Map.of(), Map.of(), 100L));

        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(1L);
        req.setAsync(true);

        ExecutionResponse response = executionService.execute(req, 1L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("cancel - 取消正在运行的执行")
    void cancel_running_marksCancelled() {
        Execution execution = new Execution();
        execution.setId(1L);
        execution.setStatus("running");
        when(executionRepository.selectById(1L)).thenReturn(execution);

        executionService.cancel(1L);

        assertThat(execution.getStatus()).isEqualTo("cancelled");
        verify(executionRepository).updateById(execution);
    }

    @Test
    @DisplayName("cancel - 已完成的不能取消")
    void cancel_finished_throwsException() {
        Execution execution = new Execution();
        execution.setId(1L);
        execution.setStatus("success");
        when(executionRepository.selectById(1L)).thenReturn(execution);

        assertThatThrownBy(() -> executionService.cancel(1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("cancel - 不存在的执行抛异常")
    void cancel_notFound_throws() {
        when(executionRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> executionService.cancel(99L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("getById - 存在的执行")
    void getById_existing_returnsExecution() {
        Execution execution = new Execution();
        execution.setId(1L);
        execution.setWorkflowId(1L);
        execution.setVersion("v1");
        execution.setStatus("success");
        when(executionRepository.selectById(1L)).thenReturn(execution);

        ExecutionResponse response = executionService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getExecutionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById - 不存在抛异常")
    void getById_notFound_throws() {
        when(executionRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> executionService.getById(99L))
                .isInstanceOf(BizException.class);
    }
}


