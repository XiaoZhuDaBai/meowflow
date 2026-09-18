package com.meowflow.template.entity;

/**
 * 模板 use_count 增量 DTO，用于批量刷 Redis 计数器到 DB。
 *
 * @param id     模板主键
 * @param delta  增量值（>= 1）
 */
public record TemplateUseCountDelta(Long id, Long delta) {

    public boolean isValid() {
        return id != null && delta != null && delta > 0;
    }
}
