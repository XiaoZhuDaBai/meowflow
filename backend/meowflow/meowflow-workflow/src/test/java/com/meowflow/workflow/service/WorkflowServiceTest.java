package com.meowflow.workflow.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.dto.WorkflowUpdateRequest;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import com.meowflow.workflow.compiler.WorkflowCompiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowVersionRepository versionRepository;

    @Mock
    private WorkflowCompiler workflowCompiler;

    @Mock
    private ExecutionRepository executionRepository;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(workflowRepository, versionRepository,
                workflowCompiler, executionRepository);
    }

    @Test
    void create_shouldCreateWorkflowSuccessfully() {
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("Test Workflow");
        request.setCode("test-wf");
        request.setDescription("Test description");
        request.setCategoryId(1L);
        request.setGroupId(1L);

        Workflow insertedWorkflow = createTestWorkflow(1L, "Test Workflow");
        when(workflowRepository.selectCount(any())).thenReturn(0L);
        when(workflowRepository.insert(any(Workflow.class))).thenReturn(1);
        when(workflowRepository.selectById(1L)).thenReturn(insertedWorkflow);

        WorkflowResponse response = workflowService.create(request, 1L);

        assertNotNull(response);
        assertEquals("Test Workflow", response.getName());
        verify(workflowRepository).insert(any(Workflow.class));
    }

    @Test
    void create_shouldThrowExceptionWhenCodeExists() {
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("Test Workflow");
        request.setCode("existing-code");

        when(workflowRepository.selectCount(any())).thenReturn(1L);

        BizException exception = assertThrows(BizException.class, () -> {
            workflowService.create(request, 1L);
        });

        assertEquals(ResultCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void getById_shouldReturnWorkflow() {
        Workflow workflow = createTestWorkflow(1L, "Test Workflow");
        when(workflowRepository.selectById(1L)).thenReturn(workflow);

        WorkflowResponse response = workflowService.getById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Workflow", response.getName());
    }

    @Test
    void getById_shouldThrowExceptionWhenNotFound() {
        when(workflowRepository.selectById(999L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> {
            workflowService.getById(999L);
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void update_shouldUpdateWorkflowFields() {
        Workflow existingWorkflow = createTestWorkflow(1L, "Old Name");
        WorkflowUpdateRequest request = new WorkflowUpdateRequest();
        request.setName("New Name");
        request.setDescription("New description");

        when(workflowRepository.selectById(1L)).thenReturn(existingWorkflow);
        when(workflowRepository.updateById(any(Workflow.class))).thenReturn(1);

        WorkflowResponse response = workflowService.update(1L, request, 1L);

        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals("New description", response.getDescription());
    }

    @Test
    void update_shouldThrowExceptionWhenNotFound() {
        WorkflowUpdateRequest request = new WorkflowUpdateRequest();
        request.setName("New Name");

        when(workflowRepository.selectById(999L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> {
            workflowService.update(999L, request, 1L);
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void delete_shouldSoftDeleteWorkflow() {
        Workflow workflow = createTestWorkflow(1L, "Test Workflow");
        when(workflowRepository.selectById(1L)).thenReturn(workflow);
        when(workflowRepository.updateById(any(Workflow.class))).thenReturn(1);

        workflowService.delete(1L);

        assertTrue(workflow.getDeleted());
        verify(workflowRepository).updateById(workflow);
    }

    @Test
    void delete_shouldThrowExceptionWhenNotFound() {
        when(workflowRepository.selectById(999L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> {
            workflowService.delete(999L);
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void updateStatus_shouldUpdateWorkflowStatus() {
        Workflow workflow = createTestWorkflow(1L, "Test Workflow");
        when(workflowRepository.selectById(1L)).thenReturn(workflow);
        when(workflowRepository.updateById(any(Workflow.class))).thenReturn(1);

        workflowService.updateStatus(1L, "running");

        assertEquals("running", workflow.getStatus());
        verify(workflowRepository).updateById(workflow);
    }

    @Test
    void updateCurrentVersion_shouldUpdateVersion() {
        Workflow workflow = createTestWorkflow(1L, "Test Workflow");
        when(workflowRepository.selectById(1L)).thenReturn(workflow);
        when(workflowRepository.updateById(any(Workflow.class))).thenReturn(1);

        workflowService.updateCurrentVersion(1L, "v2");

        assertEquals("v2", workflow.getCurrentVersion());
        verify(workflowRepository).updateById(workflow);
    }

    private Workflow createTestWorkflow(Long id, String name) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setName(name);
        workflow.setStatus("draft");
        workflow.setCurrentVersion("v1");
        workflow.setOwnerId(1L);
        workflow.setIsPublic(false);
        workflow.setStatTotalRun(0L);
        workflow.setDeleted(false);
        workflow.setCreateTime(LocalDateTime.now());
        workflow.setUpdateTime(LocalDateTime.now());
        return workflow;
    }
}
