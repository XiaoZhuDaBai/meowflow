package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meowflow.common.exception.BizException;
import com.meowflow.infra.dto.AIModelDTO;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.persistence.mapper.AIModelMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIModelServiceTest {

    @Mock
    private AIModelMapper mapper;

    @InjectMocks
    private AIModelService service;

    private AIModelEntity sample;

    @BeforeEach
    void setUp() {
        sample = new AIModelEntity();
        sample.setId(1L);
        sample.setName("GPT-4o");
        sample.setProvider("openai");
        sample.setModelKey("gpt-4o");
        sample.setEnabled(true);
        sample.setIsDefault(false);
        sample.setApiKey("encrypted-key");
    }

    @Test
    @DisplayName("listAll - 按 sortOrder 排序")
    void listAll_returnsAllModels() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sample));
        List<AIModelEntity> result = service.listAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("GPT-4o");
    }

    @Test
    @DisplayName("listEnabled - 仅返回启用项")
    void listEnabled_filtersDisabled() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(sample));
        List<AIModelEntity> result = service.listEnabled();
        assertThat(result).isNotEmpty();
        verify(mapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("getById - null 时返回 null")
    void getById_nullReturnsNull() {
        assertThat(service.getById(null)).isNull();
    }

    @Test
    @DisplayName("create - 默认值补全")
    void create_fillsDefaults() {
        AIModelDTO dto = new AIModelDTO();
        dto.setName("Test");
        dto.setProvider("openai");
        dto.setModelKey("gpt-test");
        dto.setApiKey("sk-test");

        when(mapper.insert(any(AIModelEntity.class))).thenReturn(1);
        AIModelEntity created = service.create(dto);

        assertThat(created.getEnabled()).isTrue();
        assertThat(created.getIsDefault()).isFalse();
        assertThat(created.getSortOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("create - 空白 apiKey 不覆盖")
    void create_blankApiKeyNotSet() {
        AIModelDTO dto = new AIModelDTO();
        dto.setName("Test");
        dto.setProvider("openai");
        dto.setModelKey("gpt-test");
        dto.setApiKey("   ");  // blank

        when(mapper.insert(any(AIModelEntity.class))).thenReturn(1);
        AIModelEntity created = service.create(dto);

        assertThat(created.getApiKey()).isNull();
    }

    @Test
    @DisplayName("update - 模型不存在时抛 BizException")
    void update_notFoundThrows() {
        when(mapper.selectById(anyLong())).thenReturn(null);
        AIModelDTO dto = new AIModelDTO();
        dto.setName("New");

        assertThatThrownBy(() -> service.update(99L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("update - 更新字段")
    void update_succeeds() {
        when(mapper.selectById(1L)).thenReturn(sample);
        when(mapper.updateById(any(AIModelEntity.class))).thenReturn(1);

        AIModelDTO dto = new AIModelDTO();
        dto.setName("GPT-4o Updated");
        AIModelEntity result = service.update(1L, dto);

        assertThat(result.getName()).isEqualTo("GPT-4o Updated");
    }

    @Test
    @DisplayName("setDefault - 重置所有标记再设置新默认")
    void setDefault_resetsAndSets() {
        when(mapper.selectById(1L)).thenReturn(sample);
        when(mapper.update(any(), any())).thenReturn(1);
        when(mapper.updateById(any(AIModelEntity.class))).thenReturn(1);

        service.setDefault(1L);

        ArgumentCaptor<AIModelEntity> captor = ArgumentCaptor.forClass(AIModelEntity.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isTrue();
        assertThat(captor.getValue().getEnabled()).isTrue();
    }

    @Test
    @DisplayName("delete - 直接按 ID 删除")
    void delete_byId() {
        when(mapper.deleteById(1L)).thenReturn(1);
        service.delete(1L);
        verify(mapper).deleteById(1L);
    }

    @Test
    @DisplayName("testConnection - 模型不存在返回失败")
    void testConnection_notFound() {
        when(mapper.selectById(99L)).thenReturn(null);
        var result = service.testConnection(99L);
        assertThat(result).containsEntry("success", false);
        assertThat(result).containsEntry("message", "模型不存在");
    }

    @Test
    @DisplayName("testConnection - 未配置 API Key 返回失败")
    void testConnection_noApiKey() {
        sample.setApiKey(null);
        when(mapper.selectById(1L)).thenReturn(sample);
        var result = service.testConnection(1L);
        assertThat(result).containsEntry("success", false);
    }

    @Test
    @DisplayName("testConnection - 完整配置返回成功")
    void testConnection_success() {
        when(mapper.selectById(1L)).thenReturn(sample);
        var result = service.testConnection(1L);
        assertThat(result).containsEntry("success", true);
    }
}
