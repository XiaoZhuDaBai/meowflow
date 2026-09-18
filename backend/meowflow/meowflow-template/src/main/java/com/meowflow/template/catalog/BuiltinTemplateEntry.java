package com.meowflow.template.catalog;

import com.meowflow.template.dto.TemplateDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 内置（官方）模板目录条目。
 * <p>所有字段与 {@link TemplateDTO} 对齐，运行时由 {@link BuiltinTemplateCatalog}
 * 提供给搜索/分类/推荐服务使用，不会持久化到数据库。
 * <p>业务键（id）以 "builtin:" 前缀标识，与数据库自增 ID 不冲突。
 */
@Data
@Builder
public class BuiltinTemplateEntry {

    /** 业务键，如 "builtin:customer-service-auto-reply" */
    private String id;

    /** 供前端稳定访问的负数内置 ID，不占用数据库正数自增 ID。 */
    private Long numericId;

    private String name;

    private String description;

    /** 工作流定义 JSON（兼容多 schema 解析） */
    private String definition;

    /** 编辑器友好的 workflowJson */
    private String workflowJson;

    /** 流程图预览 JSON（cx/cy/w/h 归一化） */
    private String workflowGraph;

    private Long categoryId;
    private String categoryName;

    /** 主图标（emoji / FontAwesome） */
    private String icon;
    private String coverImage;
    private String coverIcon;
    private String previewImages;

    private String industry;
    private String scene;

    /** 标签名集合 */
    private Set<String> tagNames;

    private Long useCount;
    private Double score;
    private Integer reviewCount;

    private String reviewStatus;
    private String author;
    private String authorName;
    private Long authorId;
    private Double price;

    private String createTime;
    private String isPublic;
    private String isFeatured;
    private String version;
    private String remark;

    /** 转换为 TemplateDTO（供搜索服务统一返回） */
    public TemplateDTO toDTO() {
        return TemplateDTO.builder()
                .id(numericId)
                .name(name)
                .description(description)
                .definition(definition)
                .workflowJson(workflowJson)
                .workflowGraph(workflowGraph)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .icon(icon)
                .coverImage(coverImage)
                .coverIcon(coverIcon)
                .previewImages(previewImages)
                .industry(industry)
                .scene(scene)
                .tagNames(tagNames)
                .tagIds(null)
                .useCount(useCount)
                .score(score)
                .reviewCount(reviewCount)
                .status("active")
                .reviewStatus(reviewStatus)
                .reviewRemark(null)
                .reviewBy(null)
                .reviewTime(null)
                .author(author)
                .authorId(authorId)
                .price(price)
                .version(version)
                .templateVersion(null)
                .createBy("builtin")
                .createByName(authorName)
                .createTime(createTime != null ? LocalDateTime.parse(createTime) : null)
                .updateTime(null)
                .isPublic(isPublic)
                .isFeatured(isFeatured)
                .remark(remark)
                .build();
    }

    private static Long parseLongSafe(String s) {
        if (s == null) return null;
        if (s.startsWith("builtin:")) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
