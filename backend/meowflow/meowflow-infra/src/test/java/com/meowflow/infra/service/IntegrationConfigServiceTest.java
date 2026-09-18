package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meowflow.common.exception.BizException;
import com.meowflow.infra.dto.IntegrationConfigDTO;
import com.meowflow.infra.entity.IntegrationConfigEntity;
import com.meowflow.infra.integration.IntegrationConfig;
import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.persistence.mapper.IntegrationConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationConfigServiceTest {

    @Mock
    private IntegrationConfigMapper mapper;

    @Mock
    private IntegrationService integrationService;

    @InjectMocks
    private IntegrationConfigService service;

    private IntegrationConfigEntity sample;

    @BeforeEach
    void setUp() {
        sample = new IntegrationConfigEntity();
        sample.setId(1L);
        sample.setType("dingtalk");
        sample.setName("默认钉钉");
        sample.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        sample.setEnabled(true);
        sample.setRetryTimes(3);
        sample.setTimeoutSeconds(30);
    }

    @Test
    @DisplayName("listAll - 返回所有集成")
    void listAll_returnsAll() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sample));
        assertThat(service.listAll()).hasSize(1);
    }

    @Test
    @DisplayName("listEnabled - 仅返回启用项")
    void listEnabled_filtersDisabled() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sample));
        assertThat(service.listEnabled()).hasSize(1);
    }

    @Test
    @DisplayName("listByType - 按类型过滤")
    void listByType_filters() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sample));
        assertThat(service.listByType("dingtalk")).hasSize(1);
    }

    @Test
    @DisplayName("getById - null 时返回 null")
    void getById_null() {
        assertThat(service.getById(null)).isNull();
    }

    @Test
    @DisplayName("create - 默认值补全")
    void create_fillsDefaults() {
        IntegrationConfigDTO dto = new IntegrationConfigDTO();
        dto.setType("dingtalk");
        dto.setName("New");
        dto.setWebhookUrl("https://example.com/webhook");

        when(mapper.insert(any(IntegrationConfigEntity.class))).thenReturn(1);
        IntegrationConfigEntity created = service.create(dto);

        assertThat(created.getEnabled()).isTrue();
        assertThat(created.getRetryTimes()).isEqualTo(3);
        assertThat(created.getTimeoutSeconds()).isEqualTo(30);
        verify(integrationService).saveConfig(any(IntegrationConfig.class));
    }

    @Test
    @DisplayName("create - 空白 secret 不覆盖")
    void create_blankSecretIgnored() {
        IntegrationConfigDTO dto = new IntegrationConfigDTO();
        dto.setType("dingtalk");
        dto.setName("X");
        dto.setWebhookUrl("https://x");
        dto.setSecret("   ");

        when(mapper.insert(any(IntegrationConfigEntity.class))).thenReturn(1);
        IntegrationConfigEntity created = service.create(dto);
        assertThat(created.getSecret()).isNull();
    }

    @Test
    @DisplayName("update - 模型不存在抛异常")
    void update_notFoundThrows() {
        when(mapper.selectById(99L)).thenReturn(null);
        IntegrationConfigDTO dto = new IntegrationConfigDTO();
        dto.setName("X");

        assertThatThrownBy(() -> service.update(99L, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("update - 同步刷新 sender")
    void update_refreshesSender() {
        when(mapper.selectById(1L)).thenReturn(sample);
        when(mapper.updateById(any(IntegrationConfigEntity.class))).thenReturn(1);

        IntegrationConfigDTO dto = new IntegrationConfigDTO();
        dto.setName("Updated");
        service.update(1L, dto);

        verify(integrationService).saveConfig(any(IntegrationConfig.class));
    }

    @Test
    @DisplayName("delete - 同步清理旧 sender")
    void delete_cleansOldSender() {
        when(mapper.selectById(1L)).thenReturn(sample);
        when(mapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(integrationService).deleteConfig("dingtalk");
    }

    @Test
    @DisplayName("testConnection - 集成不存在")
    void testConnection_notFound() {
        when(mapper.selectById(99L)).thenReturn(null);
        var result = service.testConnection(99L);
        assertThat(result).containsEntry("success", false);
    }

    @Test
    @DisplayName("testConnection - 调用旧 send")
    void testConnection_callsSend() {
        when(mapper.selectById(1L)).thenReturn(sample);
        when(integrationService.send(eq("dingtalk"), anyString()))
                .thenReturn(IntegrationSender.SendResult.success("msg-id", 100L));

        var result = service.testConnection(1L);
        assertThat(result).containsEntry("success", true);

        verify(integrationService).saveConfig(any(IntegrationConfig.class));
    }
}
