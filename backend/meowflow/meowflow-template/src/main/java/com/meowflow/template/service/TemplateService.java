package com.meowflow.template.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.template.cache.TemplateCacheSupport;
import com.meowflow.template.catalog.BuiltinTemplateCatalog;
import com.meowflow.template.counter.UseCountCounter;
import com.meowflow.template.dto.TemplateCreateRequest;
import com.meowflow.template.dto.TemplateDTO;
import com.meowflow.template.dto.TemplateUpdateRequest;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateCategory;
import com.meowflow.template.entity.TemplateTag;
import com.meowflow.template.repository.TemplateCategoryRepository;
import com.meowflow.template.repository.TemplateRepository;
import com.meowflow.template.repository.TemplateTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final Duration DETAIL_TTL = Duration.ofHours(1);

    private final TemplateRepository templateRepository;
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateTagRepository tagRepository;
    private final BuiltinTemplateCatalog builtinCatalog;
    private final TemplateCacheSupport cacheSupport;
    private final UseCountCounter useCountCounter;

    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO createTemplate(TemplateCreateRequest request) {
        String definition = StrUtil.blankToDefault(
                StrUtil.blankToDefault(request.getDefinition(), request.getWorkflowJson()), "{}");
        validateWorkflowJson(definition);

        Template template = new Template();
        // 主键由数据库自增 (BIGSERIAL)，此处不预填
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDefinition(definition);
        template.setWorkflowJson(request.getWorkflowJson() != null ? request.getWorkflowJson() : definition);
        template.setWorkflowGraph(request.getWorkflowGraph());
        template.setCategoryId(request.getCategoryId());
        template.setIcon(request.getCoverIcon());
        template.setCoverImage(request.getCoverImage());
        template.setCoverIcon(request.getCoverIcon());
        template.setPreviewImages(request.getPreviewImages());
        template.setIndustry(request.getIndustry());
        template.setScene(request.getScene());
        template.setAuthor(request.getAuthor());
        template.setPrice(request.getPrice() != null ? request.getPrice() : 0.0);
        template.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : "N");
        template.setStatus("active");
        template.setReviewStatus("pending");
        template.setUseCount(0L);
        template.setScore(0.0);
        template.setReviewCount(0L);
        template.setVersion("v1");
        template.setTemplateVersion(1);
        template.setCreateBy(getCurrentUserId());
        template.setRemark(request.getRemark());

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            String csv = request.getTagIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            template.setTags(csv);
            request.getTagIds().forEach(id -> incrementTagUsageSafely(id));
        }

        templateRepository.insert(template);
        log.info("Created template id={} by user={}", template.getId(), getCurrentUserId());

        // 新增模板：list / categories / tags 都可能受影响，全失效
        cacheSupport.evictAllLists();

        return convertToDTO(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO updateTemplate(Long id, TemplateUpdateRequest request) {
        Template template = templateRepository.findById(id);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        checkTemplateOwner(template);

        if (request.getDefinition() != null) {
            validateWorkflowJson(request.getDefinition());
            template.setDefinition(request.getDefinition());
        }
        if (request.getWorkflowJson() != null) template.setWorkflowJson(request.getWorkflowJson());
        if (request.getWorkflowGraph() != null) template.setWorkflowGraph(request.getWorkflowGraph());

        if (StrUtil.isNotBlank(request.getName())) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getCategoryId() != null) template.setCategoryId(request.getCategoryId());
        if (request.getCoverImage() != null) template.setCoverImage(request.getCoverImage());
        if (request.getCoverIcon() != null) {
            template.setCoverIcon(request.getCoverIcon());
            template.setIcon(request.getCoverIcon());
        }
        if (request.getPreviewImages() != null) template.setPreviewImages(request.getPreviewImages());
        if (request.getIndustry() != null) template.setIndustry(request.getIndustry());
        if (request.getScene() != null) template.setScene(request.getScene());
        if (request.getAuthor() != null) template.setAuthor(request.getAuthor());
        if (request.getPrice() != null) template.setPrice(request.getPrice());
        if (request.getIsPublic() != null) template.setIsPublic(request.getIsPublic());
        if (request.getIsFeatured() != null) template.setIsFeatured(request.getIsFeatured());
        if (request.getRemark() != null) template.setRemark(request.getRemark());
        if (request.getTagIds() != null) {
            String csv = request.getTagIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            template.setTags(csv);
        }

        template.setUpdateBy(getCurrentUserId());
        Integer next = template.getTemplateVersion() == null ? 1 : template.getTemplateVersion() + 1;
        template.setTemplateVersion(next);

        templateRepository.updateById(template);
        log.info("Updated template id={}", id);

        // 更新模板：详情 + 搜索列表失效
        cacheSupport.evictDetail(String.valueOf(id));
        cacheSupport.evictSearch();

        return convertToDTO(template);
    }

    public void deleteTemplate(Long id) {
        Template template = templateRepository.findById(id);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        checkTemplateOwner(template);

        template.setStatus("deleted");
        template.setUpdateBy(getCurrentUserId());
        templateRepository.updateById(template);
        log.info("Soft-deleted template id={}", id);

        // 删除模板：详情 + 列表 + categories 都失效
        cacheSupport.evictDetail(String.valueOf(id));
        cacheSupport.evictAllLists();
    }

    public TemplateDTO getTemplate(String id) {
        // 1) 内置模板（避免数据库未启动时失败）—— 不进缓存（数据来自内存 catalog）
        var builtin = builtinCatalog.findById(id);
        if (builtin != null) {
            return builtin.toDTO();
        }

        // 2) 数据库 —— 走 Redis 缓存
        return cacheSupport.loadDetail(
                id,
                TemplateDTO.class,
                () -> {
                    Long dbId = parseLongSafe(id);
                    if (dbId == null) {
                        throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
                    }
                    Template template = templateRepository.findById(dbId);
                    if (template == null || "deleted".equals(template.getStatus())) {
                        throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
                    }
                    return convertToDTO(template);
                },
                DETAIL_TTL);
    }

    public IPage<TemplateDTO> getMyTemplates(Integer pageNum, Integer pageSize) {
        Page<Template> page = new Page<>(pageNum, pageSize);
        IPage<Template> templatePage = templateRepository.findByUserId(page, getCurrentUserId());
        return templatePage.convert(this::convertToDTO);
    }

    public void useTemplate(String id) {
        Long dbId = parseLongSafe(id);
        if (dbId == null) {
            // 内置模板直接放行（不增 use_count，从目录元数据读取）
            if (builtinCatalog.findById(id) != null) {
                log.info("Used builtin template {} by user {}", id, getCurrentUserId());
                return;
            }
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        Template template = templateRepository.findById(dbId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        if (!"approved".equals(template.getReviewStatus()) || !"active".equals(template.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "模板未通过审核或已下架，无法使用");
        }
        // Redis 计数器 +1，DB 写入由 UseCountFlushTask 每 30s 批量刷
        useCountCounter.increment(dbId);
        log.info("Template used id={} by user {}", dbId, getCurrentUserId());
    }

    public TemplateDTO copyTemplate(String id) {
        var builtin = builtinCatalog.findById(id);
        if (builtin != null) {
            TemplateDTO dto = builtin.toDTO();
            dto.setName(dto.getName() + " (副本)");
            dto.setReviewStatus("draft");
            dto.setIsPublic("N");
            return dto;
        }

        Long dbId = parseLongSafe(id);
        if (dbId == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        Template original = templateRepository.findById(dbId);
        if (original == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        Template copy = new Template();
        copy.setName(original.getName() + " (副本)");
        copy.setDescription(original.getDescription());
        copy.setDefinition(original.getDefinition());
        copy.setWorkflowJson(original.getWorkflowJson());
        copy.setWorkflowGraph(original.getWorkflowGraph());
        copy.setCategoryId(original.getCategoryId());
        copy.setIcon(original.getIcon());
        copy.setCoverImage(original.getCoverImage());
        copy.setCoverIcon(original.getCoverIcon());
        copy.setPreviewImages(original.getPreviewImages());
        copy.setTags(original.getTags());
        copy.setIndustry(original.getIndustry());
        copy.setScene(original.getScene());
        copy.setIsPublic("N");
        copy.setStatus("active");
        copy.setReviewStatus("pending");
        copy.setUseCount(0L);
        copy.setScore(0.0);
        copy.setReviewCount(0L);
        copy.setVersion("v1");
        copy.setTemplateVersion(1);
        copy.setCreateBy(getCurrentUserId());
        copy.setRemark("从模板 " + id + " 复制");

        templateRepository.insert(copy);

        // 复制创建新模板：list / categories / tags 同样受影响
        cacheSupport.evictAllLists();

        return convertToDTO(copy);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TemplateDTO convertToDTO(Template template) {
        TemplateDTO dto = BeanUtil.copyProperties(template, TemplateDTO.class);

        builtinCatalog.enrichDefinitionIfMissing(dto);

        if (template.getCategoryId() != null) {
            TemplateCategory category = categoryRepository.selectById(template.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }

        if (StrUtil.isNotBlank(template.getTags())) {
            try {
                List<Long> ids = Arrays.stream(template.getTags().split(","))
                        .map(String::trim)
                        .map(this::parseLongSafe)
                        .filter(Objects::nonNull)
                        .toList();
                if (!ids.isEmpty()) {
                    List<TemplateTag> tags = tagRepository.selectBatchIds(ids);
                    dto.setTagIds(new HashSet<>(ids));
                    dto.setTagNames(tags.stream().map(TemplateTag::getName).collect(Collectors.toSet()));
                }
            } catch (Exception ignored) {
                // tags 列兼容 JSONB / 文本
                Set<String> names = Arrays.stream(template.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                dto.setTagNames(names);
            }
        }

        return dto;
    }

    private void incrementTagUsageSafely(Long id) {
        try {
            tagRepository.incrementUsageCount(id);
        } catch (Exception e) {
            log.warn("Failed to bump tag usage_count for tagId={}", id, e);
        }
    }

    private void validateWorkflowJson(String definition) {
        try {
            com.meowflow.common.workflow.WorkflowFullDefinition def =
                    com.meowflow.common.workflow.WorkflowJsonConverter.normalize(definition);
            if (def == null) {
                throw new BizException(ResultCode.BIZ_ERROR, "工作流定义不能为空对象");
            }
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BIZ_ERROR, "工作流定义 JSON 非法: " + e.getMessage());
        }
    }

    private Long parseLongSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException ignored) { return null; }
    }

    private Long getCurrentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null && ctx.getUserId() != null ? ctx.getUserId() : 0L;
    }

    private void checkTemplateOwner(Template template) {
        Long currentUserId = getCurrentUserId();
        Long ownerId = template.getCreateBy();
        if (ownerId == null || (!ownerId.equals(currentUserId) && !UserContextHolder.isAdmin())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作此模板");
        }
    }
}







