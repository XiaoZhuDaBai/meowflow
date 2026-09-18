package com.meowflow.template.cache;

import cn.hutool.core.util.StrUtil;
import com.meowflow.template.dto.TemplateSearchRequest;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 模板模块缓存 Key 集中管理。
 *
 * <p>所有业务 key 都在 RedisCacheManager 内自动追加前缀 {@code meowflow:cache:}，
 * 此处只关心业务层命名。命名空间统一 {@code tpl:*:v1:*}，未来升级时改为 v2 即可。
 *
 * <p>覆盖前端真实调用的 5 个接口：
 * <ul>
 *   <li>{@code GET /api/template/{id}}                       -> tpl:detail:v2:{id}</li>
 *   <li>{@code POST /api/template/search}                    -> tpl:search:v2:{hash}</li>
 *   <li>{@code GET /api/template/search/categories}          -> tpl:categories:v2</li>
 *   <li>{@code GET /api/template/search/tags}                -> tpl:tags:all:v2</li>
 *   <li>{@code POST /api/template/{id}/use}                  -> tpl:usecnt:v2:{id}（Redis 计数器）</li>
 * </ul>
 *
 * <p>刻意不缓存的接口（前端无调用）：
 * featured / popular / latest / hot tags / like / favorite / favorites。
 */
public final class TemplateCacheKeys {

    private TemplateCacheKeys() {}

    public static final String DETAIL_PREFIX = "tpl:detail:v2:";
    public static final String SEARCH_PREFIX = "tpl:search:v2:";
    public static final String CATEGORIES_KEY = "tpl:categories:v2";
    public static final String TAGS_ALL_KEY = "tpl:tags:all:v2";
    public static final String USE_COUNT_PREFIX = "tpl:usecnt:v2:";

    public static final String SEARCH_PATTERN = "tpl:search:v2:*";
    public static final String DETAIL_PATTERN = "tpl:detail:v2:*";
    public static final String USE_COUNT_PATTERN = "tpl:usecnt:v2:*";

    /**
     * 详情 key：builtin:xxx 也走同一前缀，但内置模板在 service 层短路，不会真正写缓存。
     */
    public static String detailKey(String id) {
        return DETAIL_PREFIX + id;
    }

    /**
     * 搜索 key：对入参做稳定哈希，保证相同语义查询命中同一 key。
     * 仅纳入影响查询结果或排序的字段，tagIds 排序后再哈希避免顺序抖动。
     */
    public static String searchKey(TemplateSearchRequest request) {
        String tagPart = "";
        List<String> tagIds = request.getTagIds();
        if (tagIds != null && !tagIds.isEmpty()) {
            tagPart = new TreeSet<>(tagIds).stream().collect(Collectors.joining(","));
        }
        String raw = StrUtil.builder()
                .append("k=").append(nullToEmpty(request.getKeyword())).append('|')
                .append("c=").append(nullToEmpty(request.getCategoryId())).append('|')
                .append("t=").append(tagPart).append('|')
                .append("s=").append(nullToEmpty(request.getSortBy())).append('|')
                .append("o=").append(nullToEmpty(request.getSortOrder())).append('|')
                .append("r=").append(nullToEmpty(request.getReviewStatus())).append('|')
                .append("p=").append(nullToEmpty(request.getIsPublic())).append('|')
                .append("pn=").append(request.getPageNum()).append('|')
                .append("ps=").append(request.getPageSize())
                .toString();
        return SEARCH_PREFIX + md5(raw);
    }

    public static String useCountKey(Long templateId) {
        return USE_COUNT_PREFIX + templateId;
    }

    private static String nullToEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
