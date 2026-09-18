package com.meowflow.template.service;

import cn.hutool.core.bean.BeanUtil;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.template.dto.CategoryCreateRequest;
import com.meowflow.template.dto.TemplateCategoryDTO;
import com.meowflow.template.dto.TagCreateRequest;
import com.meowflow.template.dto.TemplateTagDTO;
import com.meowflow.template.entity.TemplateCategory;
import com.meowflow.template.entity.TemplateTag;
import com.meowflow.template.repository.TemplateCategoryRepository;
import com.meowflow.template.repository.TemplateTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateCategoryService {

    private final TemplateCategoryRepository categoryRepository;
    private final TemplateTagRepository tagRepository;
    private final IdGeneratorFactory idGenerator;

    @Transactional(rollbackFor = Exception.class)
    public TemplateCategoryDTO createCategory(CategoryCreateRequest request) {
        TemplateCategory category = new TemplateCategory();
        category.setId(idGenerator.nextId());
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setCode(request.getCode());
        category.setIcon(request.getIcon());
        category.setDescription(request.getDescription());
        category.setSort(request.getSort() != null ? request.getSort() : 0);
        category.setStatus("active");
        category.setCreateBy(getCurrentUserId());
        category.setRemark(request.getRemark());

        if (request.getParentId() != null) {
            TemplateCategory parent = categoryRepository.selectById(request.getParentId());
            if (parent != null) {
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setLevel(1);
            }
        } else {
            category.setLevel(1);
        }

        categoryRepository.insert(category);
        log.info("Created category: {} by user: {}", category.getId(), getCurrentUserId());

        return convertToDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateCategoryDTO updateCategory(Long id, CategoryCreateRequest request) {
        TemplateCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }
        if (request.getCode() != null) {
            category.setCode(request.getCode());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        if (request.getRemark() != null) {
            category.setRemark(request.getRemark());
        }

        category.setUpdateBy(getCurrentUserId());
        categoryRepository.updateById(category);
        log.info("Updated category: {}", id);

        return convertToDTO(category);
    }

    public void deleteCategory(Long id) {
        TemplateCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        List<TemplateCategory> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "请先删除子分类");
        }

        category.setStatus("deleted");
        category.setUpdateBy(getCurrentUserId());
        categoryRepository.updateById(category);
        log.info("Deleted category: {}", id);
    }

    public TemplateCategoryDTO getCategory(Long id) {
        TemplateCategory category = categoryRepository.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        return convertToDTO(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateTagDTO createTag(TagCreateRequest request) {
        TemplateTag tag = new TemplateTag();
        tag.setId(idGenerator.nextId());
        tag.setName(request.getName());
        tag.setColor(request.getColor());
        tag.setSort(request.getSort() != null ? request.getSort() : 0);
        tag.setUsageCount(0L);
        tag.setCreateBy(getCurrentUserId());
        tag.setRemark(request.getRemark());

        tagRepository.insert(tag);
        log.info("Created tag: {} by user: {}", tag.getId(), getCurrentUserId());

        return convertToDTO(tag);
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateTagDTO updateTag(Long id, TagCreateRequest request) {
        TemplateTag tag = tagRepository.selectById(id);
        if (tag == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }

        if (request.getName() != null) {
            tag.setName(request.getName());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        if (request.getSort() != null) {
            tag.setSort(request.getSort());
        }
        if (request.getRemark() != null) {
            tag.setRemark(request.getRemark());
        }

        tag.setUpdateBy(getCurrentUserId());
        tagRepository.updateById(tag);
        log.info("Updated tag: {}", id);

        return convertToDTO(tag);
    }

    public void deleteTag(Long id) {
        TemplateTag tag = tagRepository.selectById(id);
        if (tag == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }

        tagRepository.deleteById(id);
        log.info("Deleted tag: {}", id);
    }

    private TemplateCategoryDTO convertToDTO(TemplateCategory category) {
        return BeanUtil.copyProperties(category, TemplateCategoryDTO.class);
    }

    private TemplateTagDTO convertToDTO(TemplateTag tag) {
        return BeanUtil.copyProperties(tag, TemplateTagDTO.class);
    }

    private Long getCurrentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.getUserId() : 0L;
    }
}









