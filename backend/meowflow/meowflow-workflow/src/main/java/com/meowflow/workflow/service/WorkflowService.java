package com.meowflow.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.compiler.WorkflowCompiler;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.ExecutionRepository;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowCompiler workflowCompiler;
    private final ExecutionRepository executionRepository;

    @Transactional
    public WorkflowResponse create(WorkflowCreateRequest request, Long userId) {
        if (request.getCode() != null) {
            LambdaQueryWrapper<Workflow> codeCheck = new LambdaQueryWrapper<>();
            codeCheck.eq(Workflow::getCode, request.getCode());
            if (workflowRepository.selectCount(codeCheck) > 0) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "工作流编码已存在");
            }
        }

        Workflow workflow = new Workflow();
        workflow.setName(request.getName());
        workflow.setCode(request.getCode());
        workflow.setDescription(request.getDescription());
        workflow.setIcon(request.getIcon());
        workflow.setCategoryId(request.getCategoryId());
        workflow.setGroupId(request.getGroupId());
        workflow.setOwnerId(userId);
        workflow.setStatus("draft");
        workflow.setCurrentVersion("v1");
        workflow.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        workflow.setTags(request.getTags());
        workflow.setStatTotalRun(0L);

        workflowRepository.insert(workflow);

        if (request.getDefinition() != null) {
            WorkflowDefinitionRequest defReq = request.getDefinition();
            String versionStr = defReq.getVersion() != null ? defReq.getVersion() : "v1";
            String definitionJson = buildDefinitionJson(defReq);

            WorkflowVersion version = new WorkflowVersion();
            version.setWorkflowId(workflow.getId());
            version.setVersion(versionStr);
            version.setDefinition(definitionJson);
            version.setInputSchema(defReq.getInputSchema());
            version.setOutputSchema(defReq.getOutputSchema());
            version.setChangelog(defReq.getChangelog());
            version.setPublishStatus("draft");
            version.setCreateBy(userId);
            versionRepository.insert(version);

            workflow.setCurrentVersion(versionStr);
            workflowRepository.updateById(workflow);
        }

        return toResponse(workflow);
    }    @Transactional
    public WorkflowResponse update(Long id, WorkflowUpdateRequest request, Long userId) {
        Workflow workflow = workflowRepository.selectById(id);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getCategoryId() != null) {
            workflow.setCategoryId(request.getCategoryId());
        }
        if (request.getGroupId() != null) {
            workflow.setGroupId(request.getGroupId());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            workflow.setIcon(request.getIcon());
        }
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }
        if (request.getTags() != null) {
            workflow.setTags(request.getTags());
        }
        if (request.getIsPublic() != null) {
            workflow.setIsPublic(request.getIsPublic());
        }
        workflow.setUpdateBy(userId);

        workflowRepository.updateById(workflow);
        return toResponse(workflow);
    }

    @Transactional
    public void delete(Long id) {
        Workflow workflow = workflowRepository.selectById(id);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        workflow.setDeleted(true);
        workflowRepository.updateById(workflow);
    }

    public WorkflowResponse getById(Long id) {
        Workflow workflow = workflowRepository.selectById(id);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        WorkflowResponse response = toResponse(workflow);
        applyStats(response, id);

        // 加载定义：优先「当前版本」（= 已发布版本，也正是执行时会跑的版本），
        // 其次才是草稿版本，最后退回最新的任一版本。
        //
        // 只查草稿版本会导致已发布工作流拿到空定义（发布后该版本不再是 draft），
        // 编辑器画布为空；而优先草稿又会让编辑器展示一个「执行时并不会跑」的版本。
        WorkflowVersion definitionVersion = resolveDefinitionVersion(id, workflow).orElse(null);
        if (definitionVersion != null) {
            try {
                WorkflowDefinitionRequest def = JsonUtils.fromJson(
                    definitionVersion.getDefinition(),
                    WorkflowDefinitionRequest.class
                );
                response.setDefinition(def);
            } catch (Exception e) {
                log.warn("解析工作流定义失败 workflowId={}", id, e);
            }
        }

        return response;
    }

    /** 解析用于回显的工作流定义版本：当前版本 → 草稿 → 最新版本。 */
    private java.util.Optional<WorkflowVersion> resolveDefinitionVersion(Long workflowId, Workflow workflow) {
        String current = workflow.getCurrentVersion();
        if (current != null) {
            java.util.Optional<WorkflowVersion> byCurrent =
                    versionRepository.findByWorkflowIdAndVersion(workflowId, current);
            if (byCurrent.isPresent()) {
                return byCurrent;
            }
        }
        java.util.Optional<WorkflowVersion> draft = versionRepository.findDraftVersion(workflowId);
        if (draft.isPresent()) {
            return draft;
        }
        List<WorkflowVersion> all = versionRepository.findByWorkflowId(workflowId);
        return all.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(all.get(0));
    }

    public PageResponse<WorkflowResponse> page(WorkflowQueryRequest request) {
        LambdaQueryWrapper<Workflow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Workflow::getDeleted, false);

        if (request.getCategoryId() != null) {
            wrapper.eq(Workflow::getCategoryId, request.getCategoryId());
        }
        if (request.getGroupId() != null) {
            wrapper.eq(Workflow::getGroupId, request.getGroupId());
        }
        if (request.getOwnerId() != null) {
            wrapper.eq(Workflow::getOwnerId, request.getOwnerId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Workflow::getStatus, request.getStatus());
        }
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Workflow::getName, request.getKeyword())
                    .or().like(Workflow::getCode, request.getKeyword())
                    .or().like(Workflow::getDescription, request.getKeyword()));
        }
        if (request.getIsPublic() != null) {
            wrapper.eq(Workflow::getIsPublic, request.getIsPublic());
        }

        wrapper.orderByDesc(Workflow::getUpdateTime);

        int current = request.getCurrent() != null ? request.getCurrent() : 1;
        int size = request.getSize() != null ? request.getSize() : 10;
        IPage<Workflow> page = workflowRepository.selectPage(new Page<>(current, size), wrapper);

        List<WorkflowResponse> records = page.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 批量聚合统计：一次 SQL 取所有工作流的成功/失败/平均耗时/今日数据
        if (!records.isEmpty()) {
            List<Long> ids = records.stream().map(WorkflowResponse::getId).collect(Collectors.toList());
            try {
                LocalDateTime today = LocalDate.now().atStartOfDay();
                List<Map<String, Object>> rows = executionRepository.aggregateStatsByWorkflows(ids, today);
                Map<Long, Map<String, Object>> byId = new HashMap<>();
                for (Map<String, Object> row : rows) {
                    Object widObj = row.get("workflow_id");
                    if (widObj != null) {
                        byId.put(toLong(widObj), row);
                    }
                }
                for (WorkflowResponse r : records) {
                    Map<String, Object> row = byId.get(r.getId());
                    if (row != null) {
                        applyStatsFromMap(r, row);
                    }
                }
            } catch (Exception e) {
                log.warn("聚合工作流统计失败（忽略）: {}", e.getMessage());
            }
        }

        return PageResponse.of(records, page.getTotal(), current, size);
    }

    @Transactional
    public WorkflowVersionResponse saveVersion(Long workflowId, WorkflowDefinitionRequest request, Long userId) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        String versionStr = request.getVersion() != null ? request.getVersion() : generateNextVersion(workflow);

        // 版本号唯一：同一工作流下 (workflow_id, version) 有唯一约束。
        // 直接 insert 会撞约束抛 500，改为先给出明确错误。
        // 前端保存草稿时把 workflow.version 原样回传，若上一次保存没有推进
        // 「已发布版本」就很容易重复，这里必须拦成 400 而不是 500。
        if (versionRepository.findByWorkflowIdAndVersion(workflowId, versionStr).isPresent()) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                    "版本 " + versionStr + " 已存在，请使用新的版本号");
        }

        String definitionJson = buildDefinitionJson(request);

        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflowId);
        version.setVersion(versionStr);
        version.setDefinition(definitionJson);
        version.setInputSchema(request.getInputSchema());
        version.setOutputSchema(request.getOutputSchema());
        version.setChangelog(request.getChangelog());
        version.setPublishStatus("draft");
        version.setCreateBy(userId);

        versionRepository.insert(version);

        // 保存草稿时不切换 currentVersion，避免污染执行版本
        // workflow.setCurrentVersion(versionStr);
        // workflowRepository.updateById(workflow);

        return toVersionResponse(version);
    }

    @Transactional
    public WorkflowVersionResponse publishVersion(Long workflowId, PublishVersionRequest request, Long userId) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        WorkflowVersion version = versionRepository.findByWorkflowIdAndVersion(workflowId, request.getVersion())
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "版本不存在"));

        // 发布前编译校验：环检测、孤立节点检测、executor 注册检查
        com.meowflow.workflow.definition.WorkflowDefinition definition =
                com.meowflow.common.util.JsonUtils.fromJson(
                        version.getDefinition(),
                        com.meowflow.workflow.definition.WorkflowDefinition.class);
        workflowCompiler.compile(definition, workflowId);

        version.setPublishStatus("published");
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(userId);
        if (request.getChangelog() != null) {
            version.setChangelog(request.getChangelog());
        }

        versionRepository.updateById(version);

        workflow.setCurrentVersion(request.getVersion());
        workflow.setStatus("running");
        workflowRepository.updateById(workflow);

        return toVersionResponse(version);
    }

    public List<WorkflowVersionResponse> listVersions(Long workflowId) {
        return versionRepository.findByWorkflowId(workflowId).stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    public WorkflowVersion getVersion(Long workflowId, String version) {
        return versionRepository.findByWorkflowIdAndVersion(workflowId, version)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "版本不存在"));
    }

    @Transactional
    public void updateStatus(Long workflowId, String status) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        workflow.setStatus(status);
        workflowRepository.updateById(workflow);
    }

    @Transactional
    public void updateCurrentVersion(Long workflowId, String version) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        workflow.setCurrentVersion(version);
        workflowRepository.updateById(workflow);
    }

    private String buildDefinitionJson(WorkflowDefinitionRequest request) {
        return JsonUtils.toJson(request);
    }

    /**
     * 生成下一个可用版本号。
     *
     * <p>不能只看 currentVersion：草稿版本不会推进 currentVersion，
     * 因此 currentVersion=v1 时可能已经存在草稿 v2、v3，
     * 单纯 +1 会撞唯一约束。这里取「已存在的最大 vN」+1。</p>
     */
    private String generateNextVersion(Workflow workflow) {
        int max = 0;
        boolean matchedAny = false;
        for (WorkflowVersion existing : versionRepository.findByWorkflowId(workflow.getId())) {
            String label = existing.getVersion();
            if (label != null && label.matches("v\\d+")) {
                matchedAny = true;
                max = Math.max(max, Integer.parseInt(label.substring(1)));
            }
        }
        String currentVersion = workflow.getCurrentVersion();
        if (currentVersion != null && currentVersion.matches("v\\d+")) {
            matchedAny = true;
            max = Math.max(max, Integer.parseInt(currentVersion.substring(1)));
        }
        if (!matchedAny) {
            return "v" + System.currentTimeMillis();
        }
        return "v" + (max + 1);
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(workflow.getId());
        response.setCategoryId(workflow.getCategoryId());
        response.setGroupId(workflow.getGroupId());
        response.setName(workflow.getName());
        response.setCode(workflow.getCode());
        response.setDescription(workflow.getDescription());
        response.setIcon(workflow.getIcon());
        response.setStatus(workflow.getStatus());
        response.setCurrentVersion(workflow.getCurrentVersion());
        response.setOwnerId(workflow.getOwnerId());
        response.setOrgId(workflow.getOrgId());
        response.setIsPublic(workflow.getIsPublic());
        response.setTags(workflow.getTags());
        response.setStatTotalRun(workflow.getStatTotalRun());
        response.setStatLastRunAt(workflow.getStatLastRunAt());
        response.setCreateTime(workflow.getCreateTime());
        response.setUpdateTime(workflow.getUpdateTime());
        return response;
    }

    /**
     * 实时聚合指定工作流的执行统计并写入响应。
     * 失败时不影响其它字段。
     */
    private void applyStats(WorkflowResponse response, Long workflowId) {
        try {
            LocalDateTime today = LocalDate.now().atStartOfDay();
            Map<String, Object> row = executionRepository.aggregateStatsByWorkflow(workflowId, today);
            applyStatsFromMap(response, row);
        } catch (Exception e) {
            log.warn("加载工作流统计失败 workflowId={}: {}", workflowId, e.getMessage());
        }
    }

    private void applyStatsFromMap(WorkflowResponse response, Map<String, Object> row) {
        if (row == null || row.isEmpty()) return;
        long totalRuns = toLong(row.get("total_runs"));
        long successCount = toLong(row.get("success_count"));
        long failCount = toLong(row.get("fail_count"));
        double avgDuration = toDouble(row.get("avg_duration_ms"));
        double totalCost = toDouble(row.get("total_cost"));
        long todayRuns = toLong(row.get("today_run_count"));
        double todayCost = toDouble(row.get("today_cost"));

        response.setStatSuccessCount(successCount);
        response.setStatFailCount(failCount);
        response.setStatAvgDurationMs(avgDuration);
        response.setStatTotalCost(totalCost);
        response.setStatTodayRunCount(todayRuns);
        response.setStatTodayCost(todayCost);
        // Use aggregated total_runs as source of truth when statTotalRun is null
        if (response.getStatTotalRun() == null || response.getStatTotalRun() == 0L) {
            response.setStatTotalRun(totalRuns);
        }
    }

    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (Exception e) { return 0L; }
    }

    private static double toDouble(Object v) {
        if (v == null) return 0d;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); }
        catch (Exception e) { return 0d; }
    }

    private WorkflowVersionResponse toVersionResponse(WorkflowVersion version) {
        WorkflowVersionResponse response = new WorkflowVersionResponse();
        response.setId(version.getId());
        response.setWorkflowId(version.getWorkflowId());
        response.setVersion(version.getVersion());
        response.setPublishStatus(version.getPublishStatus());
        response.setChangelog(version.getChangelog());
        response.setPublishedAt(version.getPublishedAt() != null ? version.getPublishedAt().toString() : null);
        response.setPublishedBy(version.getPublishedBy());
        response.setCreateTime(version.getCreateTime() != null ? version.getCreateTime().toString() : null);
        return response;
    }

    @Data
    public static class WorkflowQueryRequest extends PageRequest {
        private Long categoryId;
        private Long groupId;
        private Long ownerId;
        private String status;
        private String keyword;
        private Boolean isPublic;
    }
}
