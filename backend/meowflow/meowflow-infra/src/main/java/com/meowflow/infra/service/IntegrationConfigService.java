package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.dto.IntegrationConfigDTO;
import com.meowflow.infra.entity.IntegrationConfigEntity;
import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.persistence.mapper.IntegrationConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 集成配置 Service (持久化版本)
 *
 * <p>替换原 {@code IntegrationService} 中的内存 Map 实现,
 * 提供基于数据库的 CRUD/启停/测试连接操作,
 * 内部通过整合旧的发送器实现来执行 {@code testConnection}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConfigService {

    private final IntegrationConfigMapper mapper;
    /** 引用旧实现以提供 send/测试能力 */
    private final IntegrationService integrationService;

    public List<IntegrationConfigEntity> listAll() {
        return mapper.selectList(
                new LambdaQueryWrapper<IntegrationConfigEntity>()
                        .orderByDesc(IntegrationConfigEntity::getCreateTime));
    }

    public List<IntegrationConfigEntity> listEnabled() {
        return mapper.selectList(
                new LambdaQueryWrapper<IntegrationConfigEntity>()
                        .eq(IntegrationConfigEntity::getEnabled, true)
                        .orderByDesc(IntegrationConfigEntity::getCreateTime));
    }

    public List<IntegrationConfigEntity> listByType(String type) {
        return mapper.selectList(
                new LambdaQueryWrapper<IntegrationConfigEntity>()
                        .eq(IntegrationConfigEntity::getType, type)
                        .orderByDesc(IntegrationConfigEntity::getCreateTime));
    }

    public IntegrationConfigEntity getById(Long id) {
        return id == null ? null : mapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationConfigEntity create(IntegrationConfigDTO dto) {
        IntegrationConfigEntity entity = new IntegrationConfigEntity();
        copyPropertiesIgnoreNull(dto, entity);
        if (entity.getEnabled() == null) entity.setEnabled(true);
        if (entity.getRetryTimes() == null) entity.setRetryTimes(3);
        if (entity.getTimeoutSeconds() == null) entity.setTimeoutSeconds(30);
        mapper.insert(entity);
        refreshSender(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationConfigEntity update(Long id, IntegrationConfigDTO dto) {
        IntegrationConfigEntity exist = mapper.selectById(id);
        if (exist == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "集成配置不存在: " + id);
        copyPropertiesIgnoreNull(dto, exist);
        mapper.updateById(exist);
        refreshSender(exist);
        return exist;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IntegrationConfigEntity entity = mapper.selectById(id);
        if (entity != null) {
            mapper.deleteById(id);
            // 同步移除原 IntegrationService 中缓存的 sender
            integrationService.deleteConfig(entity.getType());
        }
    }

    /**
     * 测试连接:通过旧 IntegrationService.send 发送测试消息。
     */
    public Map<String, Object> testConnection(Long id) {
        IntegrationConfigEntity entity = getById(id);
        if (entity == null) {
            return Map.of("success", false, "message", "集成配置不存在");
        }
        try {
            refreshSender(entity);
            IntegrationSender.SendResult result = integrationService.send(
                    entity.getType(), "[喵流测试] 集成测试消息: " + java.time.LocalDateTime.now());
            return Map.of(
                    "success", result.success(),
                    "message", result.success() ? "测试成功" : ("测试失败: " + result.errorMessage()));
        } catch (Exception e) {
            log.error("Integration test failed", e);
            return Map.of("success", false, "message", "异常: " + e.getMessage());
        }
    }

    /**
     * 把数据库中的集成同步到原 IntegrationService 的内存 Map 中(供发送时使用)。
     */
    private void refreshSender(IntegrationConfigEntity entity) {
        integrationService.saveConfig(entity.toIntegrationConfig());
    }

    private void copyPropertiesIgnoreNull(IntegrationConfigDTO dto, IntegrationConfigEntity entity) {
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getWebhookUrl() != null) entity.setWebhookUrl(dto.getWebhookUrl());
        if (dto.getSecret() != null && !dto.getSecret().isBlank()) entity.setSecret(dto.getSecret());
        if (dto.getAccessKeyId() != null && !dto.getAccessKeyId().isBlank()) entity.setAccessKeyId(dto.getAccessKeyId());
        if (dto.getAccessKeySecret() != null && !dto.getAccessKeySecret().isBlank()) entity.setAccessKeySecret(dto.getAccessKeySecret());
        if (dto.getCustomConfig() != null) entity.setCustomConfig(dto.getCustomConfig());
        if (dto.getEnabled() != null) entity.setEnabled(dto.getEnabled());
        if (dto.getRetryTimes() != null) entity.setRetryTimes(dto.getRetryTimes());
        if (dto.getTimeoutSeconds() != null) entity.setTimeoutSeconds(dto.getTimeoutSeconds());
    }
}
