package com.meowflow.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.definition.Edge;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流导入导出服务。
 * <p>
 * 支持将工作流导出为 JSON 文件，以及从 JSON 文件导入工作流。
 * 导出的 JSON 包含工作流基本信息、最新版本定义和版本历史。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowImportExportService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;

    private static final String EXPORT_VERSION = "1.0";

    /**
     * 导出工作流（包含基本信息 + 最新版本定义）。
     *
     * @param workflowId 工作流 ID
     * @return 导出的 JSON 字符串
     */
    public String export(Long workflowId) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null || Boolean.TRUE.equals(workflow.getDeleted())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        ExportPayload payload = new ExportPayload();
        payload.version = EXPORT_VERSION;
        payload.exportedAt = LocalDateTime.now().toString();
        payload.workflow = buildWorkflowExport(workflow);

        String latestVersionStr = workflow.getCurrentVersion();
        if (latestVersionStr != null) {
            var latestVersion = versionRepository.findByWorkflowIdAndVersion(workflowId, latestVersionStr);
            latestVersion.ifPresent(v -> payload.latestDefinition = parseDefinitionJson(v.getDefinition()));
        }

        try {
            return new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(payload);
        } catch (IOException e) {
            log.error("导出工作流失败: workflowId={}", workflowId, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 导出工作流（含版本历史）。
     *
     * @param workflowId 工作流 ID
     * @param includeHistory 是否包含版本历史
     * @return 导出的 JSON 字符串
     */
    public String export(Long workflowId, boolean includeHistory) {
        Workflow workflow = workflowRepository.selectById(workflowId);
        if (workflow == null || Boolean.TRUE.equals(workflow.getDeleted())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }

        ExportPayload payload = new ExportPayload();
        payload.version = EXPORT_VERSION;
        payload.exportedAt = LocalDateTime.now().toString();
        payload.workflow = buildWorkflowExport(workflow);

        String latestVersionStr = workflow.getCurrentVersion();
        if (latestVersionStr != null) {
            var latestVersion = versionRepository.findByWorkflowIdAndVersion(workflowId, latestVersionStr);
            latestVersion.ifPresent(v -> payload.latestDefinition = parseDefinitionJson(v.getDefinition()));
        }

        if (includeHistory) {
            List<WorkflowVersion> allVersions = versionRepository.findByWorkflowId(workflowId);
            payload.versions = allVersions.stream()
                    .map(v -> {
                        VersionInfo info = new VersionInfo();
                        info.version = v.getVersion();
                        info.publishStatus = v.getPublishStatus();
                        info.changelog = v.getChangelog();
                        info.publishedAt = v.getPublishedAt() != null ? v.getPublishedAt().toString() : null;
                        info.publishedBy = v.getPublishedBy();
                        info.definition = parseDefinitionJson(v.getDefinition());
                        return info;
                    })
                    .toList();
        }

        try {
            return new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(payload);
        } catch (IOException e) {
            log.error("导出工作流（含历史）失败: workflowId={}", workflowId, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "导出失败: " + e.getMessage());
        }
    }

    /**
     * 导入工作流。
     * <p>
     * 支持两种模式：
     * <ul>
     *   <li>newWorkflow=true：创建新工作流（name + code 去重检测）</li>
     *   <li>newWorkflow=false：在现有工作流上创建新版本</li>
     * </ul>
     *
     * @param importJson   导入的 JSON 字符串
     * @param newWorkflow 是否创建新工作流
     * @param userId       用户 ID
     * @return 新创建的工作流版本
     */
    @Transactional
    public WorkflowVersion importFromJson(String importJson, boolean newWorkflow, Long userId) {
        DifyWorkflowImporter.ImportedDify dify = DifyWorkflowImporter.tryImport(importJson);
        if (dify != null) {
            return importDifyWorkflow(dify, newWorkflow, userId);
        }

        ExportPayload payload;
        try {
            payload = new ObjectMapper().readValue(importJson, ExportPayload.class);
        } catch (Exception e) {
            log.error("解析导入 JSON 失败: {}", e.getMessage());
            throw new BizException(ResultCode.PARAM_ERROR, "导入 JSON 格式错误: " + e.getMessage());
        }

        if (payload.workflow == null || payload.workflow.name == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "导入 JSON 缺少工作流基本信息");
        }

        Workflow workflow;
        String versionLabel;

        if (newWorkflow) {
            // 检查 code 重复
            if (payload.workflow.code != null) {
                var existing = workflowRepository.selectByCode(payload.workflow.code);
                if (existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
                    throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "工作流编码已存在: " + payload.workflow.code);
                }
            }

            workflow = new Workflow();
            workflow.setName(payload.workflow.name);
            workflow.setCode(payload.workflow.code);
            workflow.setDescription(payload.workflow.description);
            workflow.setIcon(payload.workflow.icon);
            workflow.setOwnerId(userId);
            workflow.setStatus("draft");
            workflow.setCurrentVersion("v1");
            workflow.setIsPublic(payload.workflow.isPublic != null ? payload.workflow.isPublic : false);
            workflow.setTags(payload.workflow.tags);
            workflow.setStatTotalRun(0L);
            workflowRepository.insert(workflow);

            versionLabel = "v1";
            log.info("导入创建新工作流: id={}, name={}", workflow.getId(), workflow.getName());
        } else {
            // 追加版本到现有工作流
            if (payload.workflow.id == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "非新建模式需要提供 workflow.id");
            }
            workflow = workflowRepository.selectById(payload.workflow.id);
            if (workflow == null || Boolean.TRUE.equals(workflow.getDeleted())) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "目标工作流不存在");
            }

            // 生成新版本号
            String current = workflow.getCurrentVersion();
            int next = 1;
            if (current != null && current.matches("v\\d+")) {
                next = Integer.parseInt(current.substring(1)) + 1;
            }
            versionLabel = "v" + next;
            log.info("导入追加版本: workflowId={}, newVersion={}", workflow.getId(), versionLabel);
        }

        // 找最新版本定义
        WorkflowDefinition definition = payload.latestDefinition;
        if (definition == null && payload.versions != null && !payload.versions.isEmpty()) {
            // 取最后一个（最新）
            var latest = payload.versions.get(payload.versions.size() - 1);
            definition = latest.definition;
        }

        if (definition == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "导入 JSON 缺少版本定义");
        }

        // 重新生成节点 ID，避免与现有工作流冲突
        if (newWorkflow) {
            definition = regenerateNodeIds(definition);
        }

        // 创建版本记录
        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflow.getId());
        version.setVersion(versionLabel);
        version.setDefinition(JsonUtils.toJson(definition));
        version.setInputSchema(definition.getInputSchema());
        version.setOutputSchema(definition.getOutputSchema());
        version.setChangelog("从 JSON 导入");
        version.setPublishStatus("draft");
        version.setCreateBy(userId);
        versionRepository.insert(version);

        // 更新工作流当前版本
        workflow.setCurrentVersion(versionLabel);
        workflowRepository.updateById(workflow);

        log.info("导入成功: workflowId={}, version={}", workflow.getId(), versionLabel);
        return version;
    }

    private WorkflowVersion importDifyWorkflow(DifyWorkflowImporter.ImportedDify dify,
                                               boolean newWorkflow,
                                               Long userId) {
        if (!newWorkflow) {
            throw new BizException(ResultCode.PARAM_ERROR, "Dify DSL 仅支持创建新工作流");
        }

        Workflow workflow = new Workflow();
        workflow.setName(dify.name());
        workflow.setDescription(dify.description());
        workflow.setOwnerId(userId);
        workflow.setStatus("draft");
        workflow.setCurrentVersion("v1");
        workflow.setIsPublic(false);
        workflow.setStatTotalRun(0L);
        workflowRepository.insert(workflow);

        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflow.getId());
        version.setVersion("v1");
        version.setDefinition(JsonUtils.toJson(dify.definition()));
        version.setInputSchema(dify.definition().getInputSchema());
        version.setOutputSchema(dify.definition().getOutputSchema());
        version.setChangelog(dify.definition().getChangelog());
        version.setPublishStatus("draft");
        version.setCreateBy(userId);
        versionRepository.insert(version);

        log.info("导入 Dify DSL: workflowId={}, name={}", workflow.getId(), dify.name());
        return version;
    }

    // ==================== 内部方法 ====================

    private WorkflowInfo buildWorkflowExport(Workflow w) {
        WorkflowInfo info = new WorkflowInfo();
        info.id = w.getId();
        info.name = w.getName();
        info.code = w.getCode();
        info.description = w.getDescription();
        info.icon = w.getIcon();
        info.status = w.getStatus();
        info.currentVersion = w.getCurrentVersion();
        info.ownerId = w.getOwnerId();
        info.orgId = w.getOrgId();
        info.isPublic = w.getIsPublic();
        info.tags = w.getTags();
        info.statTotalRun = w.getStatTotalRun();
        return info;
    }

    private WorkflowDefinition parseDefinitionJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return JsonUtils.fromJson(json, WorkflowDefinition.class);
        } catch (Exception e) {
            log.warn("解析版本定义 JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    private WorkflowDefinition regenerateNodeIds(WorkflowDefinition def) {
        if (def == null) return null;
        WorkflowDefinition copy = JsonUtils.fromJson(JsonUtils.toJson(def), WorkflowDefinition.class);
        Map<String, String> idMap = new java.util.LinkedHashMap<>();
        if (copy.getNodes() != null) {
            for (NodeDefinition node : copy.getNodes()) {
                String oldId = node.getId();
                String newId = generateNodeId();
                node.setId(newId);
                if (oldId != null) {
                    idMap.put(oldId, newId);
                }
            }
        }
        if (copy.getEdges() != null) {
            for (Edge edge : copy.getEdges()) {
                edge.setSource(idMap.getOrDefault(edge.getSource(), edge.getSource()));
                edge.setTarget(idMap.getOrDefault(edge.getTarget(), edge.getTarget()));
            }
        }
        return copy;
    }

    private String generateNodeId() {
        return "node_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    // ==================== 内部 DTO ====================

    public static class ExportPayload {
        public String version;
        public String exportedAt;
        public WorkflowInfo workflow;
        public WorkflowDefinition latestDefinition;
        public List<VersionInfo> versions;
    }

    public static class WorkflowInfo {
        public Long id;
        public String name;
        public String code;
        public String description;
        public String icon;
        public String status;
        public String currentVersion;
        public Long ownerId;
        public Long orgId;
        public Boolean isPublic;
        public List<String> tags;
        public Long statTotalRun;
    }

    public static class VersionInfo {
        public String version;
        public String publishStatus;
        public String changelog;
        public String publishedAt;
        public Long publishedBy;
        public WorkflowDefinition definition;
    }
}
