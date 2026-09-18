package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.dto.AIModelDTO;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.persistence.mapper.AIModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * AI 模型配置 Service
 *
 * <p>提供 CRUD/启停/设默认/测试连通性等操作。
 * API Key 通过 {@code @Encrypted} 自动加解密,列表返回时打码显示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIModelService {

    private final AIModelMapper mapper;

    /**
     * 列出所有模型(管理用),按 sortOrder 升序。
     */
    public List<AIModelEntity> listAll() {
        return mapper.selectList(
                new LambdaQueryWrapper<AIModelEntity>()
                        .orderByAsc(AIModelEntity::getSortOrder)
                        .orderByAsc(AIModelEntity::getId));
    }

    /**
     * 列出所有启用的模型(节点选择/工作流引擎使用)。
     */
    public List<AIModelEntity> listEnabled() {
        return mapper.selectList(
                new LambdaQueryWrapper<AIModelEntity>()
                        .eq(AIModelEntity::getEnabled, true)
                        .orderByAsc(AIModelEntity::getSortOrder)
                        .orderByAsc(AIModelEntity::getId));
    }

    /**
     * 解析前端/工作流传入的模型引用。支持数据库模型 ID、模型 key 和 default。
     */
    public AIModelEntity resolveForCall(String modelRef) {
        if (modelRef == null || modelRef.isBlank() || "default".equalsIgnoreCase(modelRef)) {
            return resolveDefault();
        }
        String value = modelRef.trim();
        if (value.matches("\\d+")) {
            AIModelEntity byId = getById(Long.valueOf(value));
            return byId != null && Boolean.TRUE.equals(byId.getEnabled()) ? byId : null;
        }
        return mapper.selectOne(
                new LambdaQueryWrapper<AIModelEntity>()
                        .eq(AIModelEntity::getModelKey, value)
                        .eq(AIModelEntity::getEnabled, true)
                        .last("LIMIT 1"));
    }

    /**
     * 获取默认的 Embedding 模型。
     */
    public AIModelEntity resolveEmbedding() {
        List<AIModelEntity> candidates = listEnabled().stream()
                .filter(item -> item.getCapabilities() != null
                        && item.getCapabilities().toLowerCase().contains("embedding"))
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                .findFirst()
                .orElse(candidates.get(0));
    }

    public AIModelEntity getById(Long id) {
        if (id == null) return null;
        return mapper.selectById(id);
    }

    /**
     * 获取默认模型(供无指定模型的节点使用)。
     */
    public AIModelEntity getDefault() {
        return mapper.selectOne(
                new LambdaQueryWrapper<AIModelEntity>()
                        .eq(AIModelEntity::getIsDefault, true)
                        .eq(AIModelEntity::getEnabled, true)
                        .last("LIMIT 1"));
    }

    @Transactional(rollbackFor = Exception.class)
    public AIModelEntity create(AIModelDTO dto) {
        AIModelEntity entity = new AIModelEntity();
        copyPropertiesIgnoreNull(dto, entity);
        // 默认值
        if (entity.getEnabled() == null) entity.setEnabled(true);
        if (entity.getIsDefault() == null) entity.setIsDefault(false);
        if (entity.getSortOrder() == null) entity.setSortOrder(0);
        // apiKey 为空时不覆盖
        if (dto.getApiKey() == null || dto.getApiKey().isBlank()) {
            entity.setApiKey(null);
        }
        mapper.insert(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public AIModelEntity update(Long id, AIModelDTO dto) {
        AIModelEntity exist = mapper.selectById(id);
        if (exist == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "模型不存在: " + id);
        copyPropertiesIgnoreNull(dto, exist);
        // apiKey 为空时不更新
        if (dto.getApiKey() == null || dto.getApiKey().isBlank()) {
            exist.setApiKey(null);
        }
        mapper.updateById(exist);
        return exist;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        // 先重置所有默认标记
        mapper.update(null, new UpdateWrapper<AIModelEntity>().set("is_default", false));
        AIModelEntity entity = mapper.selectById(id);
        if (entity == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "模型不存在: " + id);
        entity.setIsDefault(true);
        entity.setEnabled(true);
        mapper.updateById(entity);
    }

    /**
     * 测试连接。
     * 默认返回 true(由 ChatClientFactory 注入后真正调用 LLM);此处仅校验配置完整性。
     */
    public Map<String, Object> testConnection(Long id) {
        AIModelEntity entity = getById(id);
        if (entity == null) {
            return Map.of("success", false, "message", "模型不存在");
        }
        if (entity.getApiKey() == null || entity.getApiKey().isBlank()) {
            return Map.of("success", false, "message", "未配置 API Key");
        }
        // 实际测试由 ChatClient 完成;此处仅为占位
        return Map.of("success", true, "message", "配置检查通过,真实调用由 ChatClientFactory 完成");
    }

    /**
     * 分页查询(管理后台使用)。
     */
    public Page<AIModelEntity> page(long current, long size, String keyword) {
        LambdaQueryWrapper<AIModelEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AIModelEntity::getName, keyword)
                    .or().like(AIModelEntity::getProvider, keyword)
                    .or().like(AIModelEntity::getModelKey, keyword));
        }
        wrapper.orderByDesc(AIModelEntity::getCreateTime);
        return mapper.selectPage(Page.of(current, size), wrapper);
    }

    private void copyPropertiesIgnoreNull(AIModelDTO dto, AIModelEntity entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getProvider() != null) entity.setProvider(dto.getProvider());
        if (dto.getModelKey() != null) entity.setModelKey(dto.getModelKey());
        if (dto.getBaseUrl() != null) entity.setBaseUrl(dto.getBaseUrl());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) entity.setApiKey(dto.getApiKey());
        if (dto.getApiSecret() != null) entity.setApiSecret(dto.getApiSecret());
        if (dto.getMaxContextTokens() != null) entity.setMaxContextTokens(dto.getMaxContextTokens());
        if (dto.getMaxOutputTokens() != null) entity.setMaxOutputTokens(dto.getMaxOutputTokens());
        if (dto.getInputPrice() != null) entity.setInputPrice(dto.getInputPrice());
        if (dto.getOutputPrice() != null) entity.setOutputPrice(dto.getOutputPrice());
        if (dto.getCapabilities() != null) entity.setCapabilities(dto.getCapabilities());
        if (dto.getVersion() != null) entity.setVersion(dto.getVersion());
        if (dto.getEnabled() != null) entity.setEnabled(dto.getEnabled());
        if (dto.getIsDefault() != null) entity.setIsDefault(dto.getIsDefault());
        if (dto.getSortOrder() != null) entity.setSortOrder(dto.getSortOrder());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());
    }

    /**
     * 找到第一个启用的模型(顺序:默认 → 第一个启用)
     */
    public AIModelEntity resolveDefault() {
        AIModelEntity def = getDefault();
        if (def != null) return def;
        List<AIModelEntity> enabled = listEnabled();
        if (enabled.isEmpty()) return null;
        return enabled.stream().min(Comparator.comparingInt(AIModelEntity::getSortOrder)).orElse(null);
    }
}
