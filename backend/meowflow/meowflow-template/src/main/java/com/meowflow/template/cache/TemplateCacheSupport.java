package com.meowflow.template.cache;

import com.meowflow.infra.cache.RedisCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 模板模块缓存支持类，集中封装缓存读写与失效。
 *
 * <p>设计原则：
 * <ul>
 *   <li>所有失效调用包 try-catch，Redis 异常时降级为日志，不影响主流程。</li>
 *   <li>对外只暴露业务语义方法（evictDetail / evictSearch / evictAllLists 等），不暴露 key 拼装细节。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateCacheSupport {

    private final RedisCacheManager cacheManager;

    /**
     * 读取 / 写入详情缓存。
     */
    public <T> T loadDetail(String id, java.util.function.Supplier<T> loader, Duration ttl) {
        return cacheManager.getOrLoad(TemplateCacheKeys.detailKey(id), loader, ttl);
    }

    /**
     * 读取 / 写入详情缓存。使用具体 Class 反序列化，避免泛型 TypeReference 擦除后
     * 缓存命中变成 LinkedHashMap，进而触发 TemplateDTO 强转失败。
     */
    public <T> T loadDetail(String id, Class<T> type, java.util.function.Supplier<T> loader, Duration ttl) {
        String key = TemplateCacheKeys.detailKey(id);
        java.util.Optional<T> cached = cacheManager.get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T value = loader.get();
        if (value != null) {
            cacheManager.put(key, value, ttl);
        }
        return value;
    }

    /**
     * 读取 / 写入搜索结果缓存。
     * <p>注意：传入的 key 必须是完整的业务 key（含前缀），由 TemplateCacheKeys.searchKey() 生成。
     * 这里不再二次拼接，避免生成 tpl:search:v1:tpl:search:v1:xxx 的错误 key。
     */
    public <T> T loadSearch(String fullKey, java.util.function.Supplier<T> loader, Duration ttl) {
        return cacheManager.getOrLoad(fullKey, loader, ttl);
    }

    /**
     * 读取 / 写入分类树缓存。
     */
    public <T> T loadCategories(java.util.function.Supplier<T> loader, Duration ttl) {
        return cacheManager.getOrLoad(TemplateCacheKeys.CATEGORIES_KEY, loader, ttl);
    }

    /**
     * 读取 / 写入标签列表缓存。
     */
    public <T> T loadTags(java.util.function.Supplier<T> loader, Duration ttl) {
        return cacheManager.getOrLoad(TemplateCacheKeys.TAGS_ALL_KEY, loader, ttl);
    }

    public void evictDetail(String id) {
        try {
            cacheManager.delete(TemplateCacheKeys.detailKey(id));
        } catch (Exception e) {
            log.warn("Failed to evict template detail cache id={}", id, e);
        }
    }

    public void evictDetail(Long id) {
        if (id != null) evictDetail(String.valueOf(id));
    }

    public void evictSearch() {
        try {
            cacheManager.deleteByPattern(TemplateCacheKeys.SEARCH_PATTERN);
        } catch (Exception e) {
            log.warn("Failed to evict template search cache", e);
        }
    }

    public void evictCategories() {
        try {
            cacheManager.delete(TemplateCacheKeys.CATEGORIES_KEY);
        } catch (Exception e) {
            log.warn("Failed to evict categories cache", e);
        }
    }

    public void evictTags() {
        try {
            cacheManager.delete(TemplateCacheKeys.TAGS_ALL_KEY);
        } catch (Exception e) {
            log.warn("Failed to evict tags cache", e);
        }
    }

    /**
     * 创建 / 删除模板时：search + categories + tags 全失效（写后立刻全部过期即可）。
     */
    public void evictAllLists() {
        evictSearch();
        evictCategories();
        evictTags();
    }

    /**
     * use_count flush 后：search + 各模板详情 失效（因为列表/详情里的 useCount 字段变了）。
     */
    public void evictAfterUseCountFlush(Iterable<Long> affectedIds) {
        evictSearch();
        if (affectedIds != null) {
            for (Long id : affectedIds) evictDetail(id);
        }
    }
}
