package com.meowflow.workflow.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.entity.WorkflowGroup;
import com.meowflow.workflow.repository.WorkflowGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGroupService {

    private final WorkflowGroupRepository groupRepository;

    @Transactional
    public GroupResponse create(GroupCreateRequest request, Long userId) {
        WorkflowGroup group = new WorkflowGroup();
        group.setUserId(userId);
        group.setName(request.getName());
        group.setSort(request.getSort() != null ? request.getSort() : 0);

        groupRepository.insert(group);
        return toResponse(group);
    }

    @Transactional
    public GroupResponse update(Long id, GroupUpdateRequest request, Long userId) {
        WorkflowGroup group = groupRepository.selectById(id);
        if (group == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分组不存在");
        }

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getSort() != null) {
            group.setSort(request.getSort());
        }

        groupRepository.updateById(group);
        return toResponse(group);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        WorkflowGroup group = groupRepository.selectById(id);
        if (group == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分组不存在");
        }
        if (!group.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限删除该分组");
        }

        groupRepository.deleteById(id);
    }

    public List<GroupResponse> listByUser(Long userId) {
        return groupRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getById(Long id) {
        WorkflowGroup group = groupRepository.selectById(id);
        if (group == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分组不存在");
        }
        return toResponse(group);
    }

    private GroupResponse toResponse(WorkflowGroup group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setUserId(group.getUserId());
        response.setName(group.getName());
        response.setSort(group.getSort());
        response.setCreateTime(group.getCreateTime());
        return response;
    }

    @lombok.Data
    public static class GroupResponse {
        private Long id;
        private Long userId;
        private String name;
        private Integer sort;
        private java.time.LocalDateTime createTime;
    }
}
