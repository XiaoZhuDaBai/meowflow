package com.meowflow.template.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.template.cache.TemplateCacheSupport;
import com.meowflow.template.catalog.BuiltinTemplateCatalog;
import com.meowflow.template.catalog.BuiltinTemplateEntry;
import com.meowflow.template.dto.TemplateDTO;
import com.meowflow.template.dto.TemplateSearchRequest;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateCategory;
import com.meowflow.template.entity.TemplateTag;
import com.meowflow.template.repository.TemplateCategoryRepository;
import com.meowflow.template.repository.TemplateRepository;
import com.meowflow.template.repository.TemplateTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateSearchService {

    private static final Duration SEARCH_TTL = Duration.ofMinutes(10);
    private static final Duration CATEGORIES_TTL = Duration.ofHours(1);
    private static final Duration TAGS_TTL = Duration.ofHours(1);

    private final TemplateRepository templateRepository;
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateTagRepository tagRepository;
    private final BuiltinTemplateCatalog builtinCatalog;
    private final TemplateCacheSupport cacheSupport;

    public IPage<TemplateDTO> search(TemplateSearchRequest request) {
        // IPage 经 Redis JSON 反序列化后会变成 Map，无法安全还原为 Page<TemplateDTO>。
        // 搜索结果直接查询数据库，避免运行时 ClassCastException。
        return doSearch(request);
    }

    private IPage<TemplateDTO> doSearch(TemplateSearchRequest request) {
        Page<Template> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<Template> result;

        String keyword = request.getKeyword();
        Long categoryId = parseLong(request.getCategoryId());
        String reviewStatus = request.getReviewStatus();
        List<Long> tagIds = parseLongList(request.getTagIds());

        if (StrUtil.isNotBlank(keyword) && (categoryId != null || (tagIds != null && !tagIds.isEmpty()))) {
            result = templateRepository.advancedSearch(page, keyword, categoryId, tagIds);
        } else if (StrUtil.isNotBlank(keyword)) {
            try {
                result = templateRepository.searchByFullText(page, keyword);
                if (result.getRecords() == null || result.getRecords().isEmpty()) {
                    result = templateRepository.searchByKeyword(page, keyword);
                }
            } catch (Exception e) {
                log.debug("PG fulltext search unavailable, fallback to LIKE search: {}", e.getMessage());
                result = templateRepository.searchByKeyword(page, keyword);
            }
        } else if (categoryId != null) {
            result = templateRepository.findByCategoryId(page, categoryId);
        } else if (StrUtil.isNotBlank(reviewStatus)) {
            result = templateRepository.findByReviewStatus(page, reviewStatus);
        } else {
            result = templateRepository.findActiveTemplates(page);
        }

        // 后端排序先用数据库；内置目录与 DB 结果合并后整体再排
        applySortForTemplate(result.getRecords(), request);

        IPage<TemplateDTO> dbPage = result.convert(this::convertToDTO);
        return mergeWithBuiltin(dbPage, page, request);
    }

    private void applySortForTemplate(List<Template> records, TemplateSearchRequest request) {
        if (records == null) return;
        if ("useCount".equalsIgnoreCase(request.getSortBy())) {
            records.sort(compareLong(Template::getUseCount, "asc".equalsIgnoreCase(request.getSortOrder())));
        } else if ("rating".equalsIgnoreCase(request.getSortBy()) || "score".equalsIgnoreCase(request.getSortBy())) {
            records.sort(compareDouble(Template::getScore, "asc".equalsIgnoreCase(request.getSortOrder())));
        }
    }

    private <T> Comparator<T> compareLong(java.util.function.Function<T, Long> fn, boolean asc) {
        Comparator<T> cmp = Comparator.comparing(fn, Comparator.nullsLast(Comparator.naturalOrder()));
        return asc ? cmp : cmp.reversed();
    }

    private <T> Comparator<T> compareDouble(java.util.function.Function<T, Double> fn, boolean asc) {
        Comparator<T> cmp = Comparator.comparing(fn, Comparator.nullsLast(Comparator.naturalOrder()));
        return asc ? cmp : cmp.reversed();
    }

    private IPage<TemplateDTO> mergeWithBuiltin(IPage<TemplateDTO> dbPage, Page<Template> page, TemplateSearchRequest request) {
        String keyword = request.getKeyword();
        Long categoryId = parseLong(request.getCategoryId());
        List<Long> tagIds = parseLongList(request.getTagIds());

        List<TemplateDTO> builtin = builtinCatalog.listAll().stream()
                .map(BuiltinTemplateEntry::toDTO)
                .filter(t -> matchesBuiltin(t, keyword, categoryId, tagIds))
                .collect(Collectors.toList());

        if (dbPage.getRecords() == null || dbPage.getRecords().isEmpty()) {
            return paginateBuiltin(builtin, page, request);
        }

        List<TemplateDTO> all = new ArrayList<>(dbPage.getRecords());
        Set<String> existingIds = all.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.toSet());
        Set<String> existingNames = all.stream().map(TemplateDTO::getName).filter(Objects::nonNull).collect(Collectors.toSet());
        for (TemplateDTO t : builtin) {
            String id = String.valueOf(t.getId());
            String name = t.getName();
            if (!existingIds.contains(id) && (name == null || !existingNames.contains(name))) {
                all.add(t);
                existingIds.add(id);
                if (name != null) existingNames.add(name);
            }
        }

        applySortForDto(all, request);

        long builtinAdded = all.size() - dbPage.getRecords().size();
        long total = dbPage.getTotal() + builtinAdded;
        return paginate(all, total, page);
    }

    private boolean matchesBuiltin(TemplateDTO t, String keyword, Long categoryId, List<Long> tagIds) {
        if (categoryId != null && t.getCategoryId() != null && !categoryId.equals(t.getCategoryId())) {
            return false;
        }
        if (StrUtil.isNotBlank(keyword)) {
            String lower = keyword.toLowerCase();
            return (t.getName() != null && t.getName().toLowerCase().contains(lower))
                    || (t.getDescription() != null && t.getDescription().toLowerCase().contains(lower));
        }
        return true;
    }

    private void applySortForDto(List<TemplateDTO> all, TemplateSearchRequest request) {
        String sortBy = request.getSortBy();
        boolean asc = "asc".equalsIgnoreCase(request.getSortOrder());
        if (StrUtil.isBlank(sortBy)) {
            sortBy = "useCount";
            asc = false;
        }
        Comparator<TemplateDTO> cmp = null;
        switch (sortBy.toLowerCase()) {
            case "usecount":
                cmp = compareLong(TemplateDTO::getUseCount, asc);
                break;
            case "rating":
            case "score":
                cmp = compareDouble(TemplateDTO::getScore, asc);
                break;
            case "createtime":
                cmp = Comparator.comparing(TemplateDTO::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                if (!asc) cmp = cmp.reversed();
                break;
            default:
                cmp = Comparator.comparing(t -> String.valueOf(t.getId()));
                if (!asc) cmp = cmp.reversed();
        }
        all.sort(cmp);
    }

    private IPage<TemplateDTO> paginateBuiltin(List<TemplateDTO> builtin, Page<Template> page, TemplateSearchRequest request) {
        TemplateSearchRequest tmp = new TemplateSearchRequest();
        tmp.setSortBy(request.getSortBy());
        tmp.setSortOrder(request.getSortOrder());
        applySortForDto(builtin, tmp);
        long from = (page.getCurrent() - 1) * page.getSize();
        long size = Math.max(0, page.getSize());
        List<TemplateDTO> slice = paginateSlice(builtin, from, size);
        IPage<TemplateDTO> result = new Page<>(page.getCurrent(), page.getSize(), builtin.size());
        result.setRecords(slice);
        return result;
    }

    private IPage<TemplateDTO> paginate(List<TemplateDTO> all, long total, Page<Template> page) {
        long from = (page.getCurrent() - 1) * page.getSize();
        long size = Math.max(0, page.getSize());
        List<TemplateDTO> slice = paginateSlice(all, from, size);
        IPage<TemplateDTO> result = new Page<>(page.getCurrent(), page.getSize(), total);
        result.setRecords(slice);
        return result;
    }

    private <T> List<T> paginateSlice(List<T> all, long from, long size) {
        if (all.isEmpty()) return Collections.emptyList();
        long start = Math.max(0, from);
        long end = Math.min(all.size(), start + size);
        if (start >= end) return Collections.emptyList();
        return new ArrayList<>(all.subList((int) start, (int) end));
    }

    // =========================================================================
    // 推荐/热门/最新（合并内置目录）
    // =========================================================================

    public List<TemplateDTO> getFeaturedTemplates() {
        List<TemplateDTO> fromDb = templateRepository.findFeaturedTemplates().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        List<TemplateDTO> fromBuiltin = builtinCatalog.listAll().stream()
                .map(BuiltinTemplateEntry::toDTO)
                .collect(Collectors.toList());
        Map<String, TemplateDTO> merged = new LinkedHashMap<>();
        fromDb.forEach(t -> merged.putIfAbsent(String.valueOf(t.getId()), t));
        fromBuiltin.forEach(t -> merged.putIfAbsent(String.valueOf(t.getId()), t));
        return new ArrayList<>(merged.values());
    }

    public List<TemplateDTO> getPopularTemplates(int limit) {
        List<TemplateDTO> fromDb = templateRepository.findPopularTemplates(limit).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        List<TemplateDTO> fromBuiltin = builtinCatalog.listAll().stream()
                .map(BuiltinTemplateEntry::toDTO)
                .sorted((a, b) -> Long.compare(b.getUseCount() == null ? 0L : b.getUseCount(),
                        a.getUseCount() == null ? 0L : a.getUseCount()))
                .limit(limit)
                .collect(Collectors.toList());
        Map<String, TemplateDTO> merged = new LinkedHashMap<>();
        fromDb.forEach(t -> merged.putIfAbsent(String.valueOf(t.getId()), t));
        fromBuiltin.forEach(t -> merged.putIfAbsent(String.valueOf(t.getId()), t));
        return merged.values().stream().limit(limit).collect(Collectors.toList());
    }

    public List<TemplateDTO> getLatestTemplates(int limit) {
        return templateRepository.findLatestTemplates(limit).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TemplateCategory> getAllCategories() {
        return cacheSupport.loadCategories(
                () -> {
                    List<TemplateCategory> rootCategories = categoryRepository.findRootCategories();
                    buildCategoryTree(rootCategories);
                    return rootCategories;
                },
                CATEGORIES_TTL);
    }

    public List<TemplateTag> getAllTags() {
        return cacheSupport.loadTags(
                () -> tagRepository.findAllOrderByUsageCount(),
                TAGS_TTL);
    }

    public List<TemplateTag> getHotTags(int limit) {
        List<TemplateTag> hotTags = tagRepository.findHotTags(Math.max(limit, 10));
        if (hotTags.size() > limit) {
            return hotTags.subList(0, limit);
        }
        return hotTags;
    }

    private void buildCategoryTree(List<TemplateCategory> categories) {
        for (TemplateCategory category : categories) {
            List<TemplateCategory> children = categoryRepository.findByParentId(category.getId());
            if (!children.isEmpty()) {
                category.setChildren(new HashSet<>(children));
                buildCategoryTree(children);
            }
            int count = templateRepository.countByCategoryId(category.getId());
            category.setTemplateCount(count);
        }
    }

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
            String[] tagIdStrs = template.getTags().split(",");
            try {
                List<Long> ids = Arrays.stream(tagIdStrs)
                        .map(String::trim)
                        .map(this::parseLongSafe)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    List<TemplateTag> tags = tagRepository.selectBatchIds(ids);
                    dto.setTagIds(new HashSet<>(ids));
                    dto.setTagNames(tags.stream().map(TemplateTag::getName).collect(Collectors.toSet()));
                }
            } catch (Exception ignored) {
                // tags 列兼容 JSONB / 文本，回退到名称列表
                dto.setTagNames(new HashSet<>(Arrays.asList(tagIdStrs)));
            }
        }

        return dto;
    }

    private Long parseLongSafe(String s) {
        try { return Long.parseLong(s); } catch (Exception ignored) { return null; }
    }

    private static Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (Exception ignored) { return null; }
    }

    private static List<Long> parseLongList(List<String> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<Long> out = new ArrayList<>();
        for (String s : list) {
            Long v = parseLong(s);
            if (v != null) out.add(v);
        }
        return out;
    }
}
