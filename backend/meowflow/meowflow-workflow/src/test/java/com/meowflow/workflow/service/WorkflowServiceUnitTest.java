package com.meowflow.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.exception.BizException;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.dto.WorkflowUpdateRequest;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.compiler.WorkflowCompiler;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WorkflowService 单元测试，使用 Mockito 模拟 Repository。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowService Unit Tests")
class WorkflowServiceUnitTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowVersionRepository versionRepository;

    @Mock
    private WorkflowCompiler workflowCompiler;

    @Mock
    private ExecutionRepository executionRepository;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        // 默认 mock：版本查询返回空
        when(versionRepository.findByWorkflowIdAndVersion(any(), any()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    @DisplayName("create - 成功创建工作流")
    void create_validRequest_persistsWorkflow() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("Test");
        req.setCode("test-code");

        when(workflowRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        WorkflowResponse response = workflowService.create(req, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test");
        assertThat(response.getCode()).isEqualTo("test-code");
        assertThat(response.getStatus()).isEqualTo("draft");
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getCurrentVersion()).isEqualTo("v1");

        verify(workflowRepository).insert(any(Workflow.class));
    }

    @Test
    @DisplayName("create - 重复 code 抛出异常")
    void create_duplicateCode_throwsBizException() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("dup");
        req.setCode("dup-code");

        when(workflowRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> workflowService.create(req, 1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("create - 不带 code 也能创建")
    void create_noCode_succeeds() {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("No Code");

        WorkflowResponse response = workflowService.create(req, 1L);

        assertThat(response.getName()).isEqualTo("No Code");
        verify(workflowRepository, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("update - 成功更新工作流")
    void update_existingWorkflow_updatesFields() {
        Workflow existing = createWorkflow(1L);
        when(workflowRepository.selectById(1L)).thenReturn(existing);

        WorkflowUpdateRequest req = new WorkflowUpdateRequest();
        req.setName("Updated");
        req.setDescription("New desc");
        req.setStatus("published");

        WorkflowResponse response = workflowService.update(1L, req, 2L);

        assertThat(response.getName()).isEqualTo("Updated");
        assertThat(response.getDescription()).isEqualTo("New desc");
        assertThat(response.getStatus()).isEqualTo("published");
        assertThat(existing.getUpdateBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("update - 工作流不存在抛异常")
    void update_nonExistent_throwsBizException() {
        when(workflowRepository.selectById(999L)).thenReturn(null);

        WorkflowUpdateRequest req = new WorkflowUpdateRequest();
        req.setName("X");

        assertThatThrownBy(() -> workflowService.update(999L, req, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工作流不存在");
    }

    @Test
    @DisplayName("delete - 软删除工作流")
    void delete_existing_marksAsDeleted() {
        Workflow existing = createWorkflow(1L);
        when(workflowRepository.selectById(1L)).thenReturn(existing);
        when(workflowRepository.deleteById(1L)).thenReturn(1);

        workflowService.delete(1L);

        // 必须走 deleteById 才会真正写入逻辑删除标记；
        // updateById 会被全局 logic-delete-field 排除掉 deleted 列，等于没删。
        verify(workflowRepository).deleteById(1L);
        verify(workflowRepository, never()).updateById(any(Workflow.class));
    }

    @Test
    @DisplayName("delete - 工作流不存在抛异常")
    void delete_nonExistent_throwsBizException() {
        when(workflowRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> workflowService.delete(99L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("getById - 返回存在的 workflow")
    void getById_existing_returnsWorkflow() {
        when(workflowRepository.selectById(1L)).thenReturn(createWorkflow(1L));

        WorkflowResponse response = workflowService.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById - 不存在抛异常")
    void getById_notFound_throws() {
        when(workflowRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> workflowService.getById(99L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("page - 分页查询工作流")
    void page_returnsPagedResults() {
        Workflow wf = createWorkflow(1L);
        Page<Workflow> page = new Page<>(1, 10);
        page.setRecords(List.of(wf));
        page.setTotal(1L);

        when(workflowRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        WorkflowService.WorkflowQueryRequest req = new WorkflowService.WorkflowQueryRequest();
        req.setCurrent(1);
        req.setSize(10);

        var result = workflowService.page(req);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("page - 默认分页参数")
    void page_defaultParams_appliesDefaults() {
        Page<Workflow> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0L);
        when(workflowRepository.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = workflowService.page(new WorkflowService.WorkflowQueryRequest());

        assertThat(result.getCurrent()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("listVersions - 返回版本列表")
    void listVersions_returnsList() {
        WorkflowVersion v1 = new WorkflowVersion();
        v1.setId(1L);
        v1.setWorkflowId(1L);
        v1.setVersion("v1");
        v1.setPublishStatus("draft");

        when(versionRepository.findByWorkflowId(1L)).thenReturn(List.of(v1));

        var versions = workflowService.listVersions(1L);

        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).getVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("getVersion - 存在返回")
    void getVersion_existing_returnsVersion() {
        WorkflowVersion v1 = new WorkflowVersion();
        v1.setId(1L);
        v1.setWorkflowId(1L);
        v1.setVersion("v1");

        when(versionRepository.findByWorkflowIdAndVersion(1L, "v1")).thenReturn(java.util.Optional.of(v1));

        WorkflowVersion result = workflowService.getVersion(1L, "v1");

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("getVersion - 不存在抛异常")
    void getVersion_notFound_throws() {
        when(versionRepository.findByWorkflowIdAndVersion(99L, "v99"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> workflowService.getVersion(99L, "v99"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("saveVersion - 写入新版本")
    void saveVersion_validInput_createsVersion() {
        Workflow existing = createWorkflow(1L);
        existing.setCurrentVersion("v1");
        when(workflowRepository.selectById(1L)).thenReturn(existing);

        com.meowflow.workflow.dto.WorkflowDefinitionRequest defReq =
                new com.meowflow.workflow.dto.WorkflowDefinitionRequest();
        defReq.setChangelog("Test changes");

        var response = workflowService.saveVersion(1L, defReq, 1L);

        assertThat(response).isNotNull();
        ArgumentCaptor<WorkflowVersion> captor = ArgumentCaptor.forClass(WorkflowVersion.class);
        verify(versionRepository).insert(captor.capture());
        assertThat(captor.getValue().getPublishStatus()).isEqualTo("draft");
    }

    @Test
    @DisplayName("saveVersion - 工作流不存在抛异常")
    void saveVersion_workflowNotFound_throws() {
        when(workflowRepository.selectById(99L)).thenReturn(null);

        com.meowflow.workflow.dto.WorkflowDefinitionRequest defReq =
                new com.meowflow.workflow.dto.WorkflowDefinitionRequest();

        assertThatThrownBy(() -> workflowService.saveVersion(99L, defReq, 1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("publishVersion - 标记版本已发布")
    void publishVersion_validInput_publishes() {
        Workflow existing = createWorkflow(1L);
        when(workflowRepository.selectById(1L)).thenReturn(existing);

        WorkflowVersion v1 = new WorkflowVersion();
        v1.setId(1L);
        v1.setWorkflowId(1L);
        v1.setVersion("v1");
        v1.setPublishStatus("draft");
        when(versionRepository.findByWorkflowIdAndVersion(eq(1L), eq("v1")))
                .thenReturn(java.util.Optional.of(v1));

        com.meowflow.workflow.dto.PublishVersionRequest req =
                new com.meowflow.workflow.dto.PublishVersionRequest();
        req.setVersion("v1");
        req.setChangelog("Initial release");

        var response = workflowService.publishVersion(1L, req, 1L);

        assertThat(response.getPublishStatus()).isEqualTo("published");
        assertThat(existing.getStatus()).isEqualTo("running");
        assertThat(existing.getCurrentVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("publishVersion - 版本不存在抛异常")
    void publishVersion_versionNotFound_throws() {
        Workflow existing = createWorkflow(1L);
        when(workflowRepository.selectById(1L)).thenReturn(existing);
        when(versionRepository.findByWorkflowIdAndVersion(eq(1L), eq("v99")))
                .thenReturn(java.util.Optional.empty());

        com.meowflow.workflow.dto.PublishVersionRequest req =
                new com.meowflow.workflow.dto.PublishVersionRequest();
        req.setVersion("v99");

        assertThatThrownBy(() -> workflowService.publishVersion(1L, req, 1L))
                .isInstanceOf(BizException.class);
    }

    private Workflow createWorkflow(Long id) {
        Workflow wf = new Workflow();
        wf.setId(id);
        wf.setName("Test " + id);
        wf.setCode("code-" + id);
        wf.setStatus("draft");
        wf.setCurrentVersion("v1");
        wf.setDeleted(false);
        return wf;
    }
}

