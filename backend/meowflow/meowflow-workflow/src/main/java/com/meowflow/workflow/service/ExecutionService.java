package com.meowflow.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.DebugManager;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.engine.CancellationRegistry;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.ExecutionResult;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.engine.WorkflowEngine;
import com.meowflow.workflow.entity.Execution;
import com.meowflow.workflow.entity.NodeExecution;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.NodeExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionRepository executionRepository;
    private final NodeExecutionRepository nodeExecutionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowEngine workflowEngine;
    private final CancellationRegistry cancellationRegistry;
    private final ApplicationContext applicationContext;
    private final HumanTaskService humanTaskService;

    /**
     * 执行工作流。
     *
     * <p>这里**故意不加 {@code @Transactional}**：工作流执行可能持续数分钟，
     * 若把整段执行包在一个事务里，会长时间占用一个数据库连接（默认池只有 10），
     * 且执行期间写入的节点行与 running 状态对轮询接口不可见。
     * 现在拆成「准备（独立短事务）→ 执行（无事务）→ 收尾（独立短事务）」三段。</p>
     */
    public ExecutionResponse execute(ExecutionRequest request, Long userId) {
        ExecutionService self = applicationContext.getBean(ExecutionService.class);

        // 1) 准备：校验 + 落库 pending 执行记录（独立短事务）
        PreparedExecution prepared = self.prepareExecution(request, userId);
        Execution execution = prepared.execution();
        Workflow workflow = prepared.workflow();
        WorkflowVersion workflowVersion = prepared.version();

        // 2) 调试会话必须在异步任务调度前挂接，否则执行可能先跑过断点节点。
        boolean debugMode = Boolean.TRUE.equals(request.getDebug())
                || request.getBreakpointNodeIds() != null;
        if (debugMode) {
            DebugManager debugManager = DebugManager.attach(execution.getId());
            if (request.getBreakpointNodeIds() != null) {
                debugManager.clearBreakpoints();
                request.getBreakpointNodeIds().stream()
                        .filter(nodeId -> nodeId != null && !nodeId.isBlank())
                        .forEach(debugManager::addBreakpoint);
            }
        }

        // 调试会在断点处阻塞执行线程；必须异步调度，HTTP 请求才能及时返回 executionId。
        boolean async = debugMode || (request.getAsync() != null && request.getAsync());

        if (async) {
            // 准备事务已提交，这里可以直接派发异步执行。
            self.executeAsync(execution.getId(), workflow, workflowVersion);
            return toResponse(execution);
        }

        // 3) 同步执行：不持有事务与连接
        ExecutionResult result = executeSync(execution, workflow, workflowVersion);

        // 注意：不要再在这里 updateById(workflow)。updateExecutionFromResult 内部
        // 会重新查询一条最新的 Workflow 并自增 statTotalRun，而这里持有的 workflow
        // 是上面加载的陈旧对象（statTotalRun 非 null），再写一次会把自增结果
        // 覆盖回去，导致同步执行的运行次数统计从第 2 次起不再增长。
        self.updateExecutionFromResult(execution, result);

        return toResponse(execution);
    }

    /** 一次执行的准备结果（工作流 + 版本 + 已落库的执行记录）。 */
    public record PreparedExecution(Workflow workflow, WorkflowVersion version, Execution execution) {
    }

    /**
     * 准备阶段：校验工作流状态与版本，并插入 pending 执行记录。
     *
     * <p>必须经代理调用（{@code applicationContext.getBean}）才能让
     * {@code @Transactional} 生效。</p>
     */
    @Transactional
    public PreparedExecution prepareExecution(ExecutionRequest request, Long userId) {
        Long workflowId = request.getWorkflowId();
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        if (!"running".equals(workflow.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "工作流未发布，无法执行");
        }

        String version = request.getVersion() != null ? request.getVersion() : workflow.getCurrentVersion();
        WorkflowVersion workflowVersion = versionRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "版本不存在"));

        // 显式指定的版本必须已发布。否则未通过编译校验的草稿会被直接执行，
        // 运行到一半才抛 "Unsupported node type"，或带着环/孤立节点跑出错误结果。
        // 调试模式（画布试运行）允许跑草稿，这是调试功能的必要行为。
        boolean debugRequested = Boolean.TRUE.equals(request.getDebug())
                || request.getBreakpointNodeIds() != null;
        if (request.getVersion() != null && !debugRequested
                && !"published".equals(workflowVersion.getPublishStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "版本 " + version + " 尚未发布，无法执行（请先发布，或使用调试模式）");
        }

        Execution execution = new Execution();
        execution.setWorkflowId(workflowId);
        execution.setVersion(version);
        execution.setTriggerType(request.getTriggerType() != null ? request.getTriggerType() : "manual");
        execution.setTriggerUserId(userId);
        execution.setStatus("pending");
        execution.setInput(request.getInput());
        execution.setStartedAt(LocalDateTime.now());

        executionRepository.insert(execution);

        return new PreparedExecution(workflow, workflowVersion, execution);
    }

    @Async("workflowExecutorPool")
    public void executeAsync(Long executionId, Workflow workflow, WorkflowVersion version) {
        Execution execution = executionRepository.selectById(executionId);
        try {
            ExecutionResult result = executeSync(execution, workflow, version);
            updateExecutionFromResult(execution, result);
        } catch (Exception e) {
            log.error("Async execution failed: executionId={}", executionId, e);
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setFinishedAt(LocalDateTime.now());
            executionRepository.updateById(execution);
        }
    }

    private ExecutionResult executeSync(Execution execution, Workflow workflow, WorkflowVersion version) {
        execution.setStatus("running");
        executionRepository.updateById(execution);

        WorkflowDefinition definition = parseDefinition(version.getDefinition());
        if (definition == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "工作流定义为空或解析失败");
        }

        ExecutionContext context = ExecutionContext.builder()
                .executionId(execution.getId())
                .workflowId(workflow.getId())
                .version(version.getVersion())
                .definition(definition)
                .input(execution.getInput())
                .variables(new HashMap<>())
                .build();

        return workflowEngine.execute(context);
    }

    @Transactional
    public void updateExecutionFromResult(Execution execution, ExecutionResult result) {
        // 必须以数据库中的最新状态判断终态。传入的 execution 是运行线程持有的
        // 内存快照，用户并发调用 cancel() 时改的是另一个实例 —— 用内存快照判断
        // 会把刚写入的 cancelled 覆盖成 failed/success。
        Execution current = executionRepository.selectById(execution.getId());
        if (current == null) {
            log.warn("Execution {} disappeared, skipping result update", execution.getId());
            return;
        }
        String currentStatus = current.getStatus();
        if ("cancelled".equals(currentStatus) || "success".equals(currentStatus) || "failed".equals(currentStatus)) {
            log.warn("Execution {} already in final state: {}, skipping update from result: {}",
                    execution.getId(), currentStatus, result.getStatus());
            return;
        }

        humanTaskService.cancelExecution(execution.getId());
        current.setStatus(result.getStatus());
        current.setOutput(result.getOutput());
        current.setErrorMessage(result.getErrorMessage());
        current.setCostMs(result.getCostMs());
        current.setCostToken(result.getCostToken());
        current.setCostAmount(result.getCostAmount());
        current.setFinishedAt(LocalDateTime.now());
        executionRepository.updateById(current);

        // 让调用方拿到最新状态
        execution.setStatus(current.getStatus());
        execution.setOutput(current.getOutput());
        execution.setErrorMessage(current.getErrorMessage());
        execution.setCostMs(current.getCostMs());
        execution.setFinishedAt(current.getFinishedAt());

        Workflow workflow = workflowRepository.selectById(execution.getWorkflowId());
        if (workflow != null) {
            workflow.setStatTotalRun(workflow.getStatTotalRun() != null ? workflow.getStatTotalRun() + 1 : 1L);
            workflow.setStatLastRunAt(LocalDateTime.now());
            workflowRepository.updateById(workflow);
        }
    }

    @Transactional
    public void cancel(Long executionId) {
        Execution execution = executionRepository.selectById(executionId);
        if (execution == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "执行记录不存在");
        }

        if ("running".equals(execution.getStatus()) || "pending".equals(execution.getStatus())) {
            humanTaskService.cancelExecution(executionId);
            cancellationRegistry.cancel(executionId, "User requested cancellation");
            execution.setStatus("cancelled");
            execution.setFinishedAt(LocalDateTime.now());
            executionRepository.updateById(execution);
        } else {
            throw new BizException(ResultCode.BAD_REQUEST, "当前状态无法取消");
        }
    }

    public ExecutionResponse getById(Long executionId) {
        Execution execution = executionRepository.selectById(executionId);
        if (execution == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "执行记录不存在");
        }
        return toResponse(execution);
    }

    public PageResponse<ExecutionResponse> page(ExecutionQueryRequest request) {
        LambdaQueryWrapper<Execution> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Execution::getCreateTime);

        if (request.getWorkflowId() != null) {
            wrapper.eq(Execution::getWorkflowId, request.getWorkflowId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Execution::getStatus, request.getStatus());
        }
        if (request.getTriggerType() != null) {
            wrapper.eq(Execution::getTriggerType, request.getTriggerType());
        }
        if (request.getTriggerUserId() != null) {
            wrapper.eq(Execution::getTriggerUserId, request.getTriggerUserId());
        }

        int current = request.getCurrent() != null ? request.getCurrent() : 1;
        int size = request.getSize() != null ? request.getSize() : 10;
        IPage<Execution> page = executionRepository.selectPage(new Page<>(current, size), wrapper);

        List<ExecutionResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(records, page.getTotal(), current, size);
    }

    public List<NodeExecutionDto> getNodeExecutions(Long executionId) {
        return nodeExecutionRepository.findByExecutionId(executionId).stream()
                .map(this::toNodeExecutionDto)
                .collect(Collectors.toList());
    }

    private com.meowflow.workflow.definition.WorkflowDefinition parseDefinition(String definitionJson) {
        if (definitionJson == null || definitionJson.isEmpty()) {
            return null;
        }
        return com.meowflow.common.util.JsonUtils.fromJson(definitionJson,
                com.meowflow.workflow.definition.WorkflowDefinition.class);
    }

    private ExecutionResponse toResponse(Execution execution) {
        ExecutionResponse response = new ExecutionResponse();
        response.setExecutionId(execution.getId());
        response.setWorkflowId(execution.getWorkflowId());
        response.setVersion(execution.getVersion());
        response.setTriggerType(execution.getTriggerType());
        response.setStatus(execution.getStatus());
        response.setInput(execution.getInput());
        response.setOutput(execution.getOutput());
        response.setErrorMessage(execution.getErrorMessage());
        response.setCostMs(execution.getCostMs());
        response.setCostToken(execution.getCostToken());
        response.setCostAmount(execution.getCostAmount());
        response.setStartedAt(execution.getStartedAt() != null ? execution.getStartedAt().toString() : null);
        response.setFinishedAt(execution.getFinishedAt() != null ? execution.getFinishedAt().toString() : null);
        return response;
    }

    private NodeExecutionDto toNodeExecutionDto(NodeExecution nodeExecution) {
        return NodeExecutionDto.builder()
                .id(nodeExecution.getId())
                .nodeId(nodeExecution.getNodeId())
                .nodeType(nodeExecution.getNodeType())
                .nodeName(nodeExecution.getNodeName())
                .status(nodeExecution.getStatus())
                .input(nodeExecution.getInput())
                .output(nodeExecution.getOutput())
                .errorMessage(nodeExecution.getErrorMessage())
                .retryCount(nodeExecution.getRetryCount())
                .costMs(nodeExecution.getCostMs())
                .costToken(nodeExecution.getCostToken())
                .startedAt(nodeExecution.getStartedAt() != null ? nodeExecution.getStartedAt().toString() : null)
                .finishedAt(nodeExecution.getFinishedAt() != null ? nodeExecution.getFinishedAt().toString() : null)
                .createTime(nodeExecution.getCreateTime() != null ? nodeExecution.getCreateTime().toString() : null)
                .build();
    }

    @Data
    public static class ExecutionQueryRequest extends PageRequest {
        private Long workflowId;
        private String status;
        private String triggerType;
        private Long triggerUserId;
    }
}



