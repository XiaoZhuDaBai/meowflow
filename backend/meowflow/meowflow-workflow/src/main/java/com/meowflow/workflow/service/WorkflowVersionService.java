package com.meowflow.workflow.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowVersionService {

    private final WorkflowVersionRepository versionRepository;

    public Optional<WorkflowVersion> findById(Long id) {
        return Optional.ofNullable(versionRepository.selectById(id));
    }

    public Optional<WorkflowVersion> findByWorkflowIdAndVersion(Long workflowId, String version) {
        return versionRepository.findByWorkflowIdAndVersion(workflowId, version);
    }

    public Optional<WorkflowVersion> findPublishedVersion(Long workflowId) {
        return versionRepository.findPublishedVersion(workflowId);
    }

    public Optional<WorkflowVersion> findDraftVersion(Long workflowId) {
        return versionRepository.findDraftVersion(workflowId);
    }

    public List<WorkflowVersion> findAllByWorkflowId(Long workflowId) {
        return versionRepository.findByWorkflowId(workflowId);
    }

    @Transactional
    public WorkflowVersion create(Long workflowId, String version, WorkflowDefinition definition, Long userId) {
        WorkflowVersion versionEntity = new WorkflowVersion();
        versionEntity.setWorkflowId(workflowId);
        versionEntity.setVersion(version);
        versionEntity.setDefinition(JsonUtils.toJson(definition));
        versionEntity.setInputSchema(definition.getInputSchema());
        versionEntity.setOutputSchema(definition.getOutputSchema());
        versionEntity.setPublishStatus("draft");
        versionEntity.setCreateBy(userId);
        versionRepository.insert(versionEntity);
        return versionEntity;
    }

    @Transactional
    public WorkflowVersion updateDefinition(Long versionId, WorkflowDefinition definition) {
        WorkflowVersion version = versionRepository.selectById(versionId);
        if (version == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "版本不存在");
        }
        version.setDefinition(JsonUtils.toJson(definition));
        version.setInputSchema(definition.getInputSchema());
        version.setOutputSchema(definition.getOutputSchema());
        versionRepository.updateById(version);
        return version;
    }

    @Transactional
    public WorkflowVersion publish(Long versionId, Long userId) {
        WorkflowVersion version = versionRepository.selectById(versionId);
        if (version == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "版本不存在");
        }
        version.setPublishStatus("published");
        version.setPublishedBy(userId);
        version.setPublishedAt(java.time.LocalDateTime.now());
        versionRepository.updateById(version);
        return version;
    }

    public int countByWorkflowId(Long workflowId) {
        return versionRepository.countByWorkflowId(workflowId);
    }

    public WorkflowDefinition parseDefinition(WorkflowVersion version) {
        if (version == null || version.getDefinition() == null) {
            return null;
        }
        return JsonUtils.fromJson(version.getDefinition(), WorkflowDefinition.class);
    }
}
