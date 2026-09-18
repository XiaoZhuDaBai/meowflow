package com.meowflow.workflow.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.workflow.dto.*;
import com.meowflow.workflow.entity.WorkflowCategory;
import com.meowflow.workflow.repository.WorkflowCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowCategoryService {

    private final WorkflowCategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.findByCode(request.getCode()).isPresent()) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "分类编码已存在");
        }

        WorkflowCategory category = new WorkflowCategory();
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setSort(request.getSort() != null ? request.getSort() : 0);
        category.setStatus("active");

        categoryRepository.insert(category);
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        WorkflowCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        categoryRepository.updateById(category);
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        WorkflowCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        categoryRepository.deleteById(id);
    }

    public CategoryResponse getById(Long id) {
        WorkflowCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        return toResponse(category);
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findActiveCategories().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> tree() {
        List<WorkflowCategory> all = categoryRepository.findActiveCategories();
        return buildTree(all, 0L);
    }

    private List<CategoryResponse> buildTree(List<WorkflowCategory> all, Long parentId) {
        return all.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .map(c -> {
                    CategoryResponse response = toResponse(c);
                    response.setChildren(buildTree(all, c.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private CategoryResponse toResponse(WorkflowCategory category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setParentId(category.getParentId());
        response.setCode(category.getCode());
        response.setName(category.getName());
        response.setIcon(category.getIcon());
        response.setSort(category.getSort());
        response.setStatus(category.getStatus());
        response.setCreateTime(category.getCreateTime());
        response.setUpdateTime(category.getUpdateTime());
        return response;
    }

    @lombok.Data
    public static class CategoryResponse {
        private Long id;
        private Long parentId;
        private String code;
        private String name;
        private String icon;
        private Integer sort;
        private String status;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
        private List<CategoryResponse> children;
    }
}
