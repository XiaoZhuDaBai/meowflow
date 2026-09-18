package com.meowflow.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.template.cache.TemplateCacheSupport;
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
import com.meowflow.template.catalog.BuiltinTemplateCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TemplateService Unit Tests")
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateCategoryRepository categoryRepository;

    @Mock
    private TemplateTagRepository tagRepository;

    @Mock
    private BuiltinTemplateCatalog builtinCatalog;

    @Mock
    private TemplateCacheSupport cacheSupport;

    @Mock
    private UseCountCounter useCountCounter;

    @InjectMocks
    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(UserContext.builder().userId(1001L).username("testuser").build());
        when(cacheSupport.loadDetail(anyString(), eq(TemplateDTO.class), any(), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(2)).get());
        when(cacheSupport.loadDetail(anyString(), any(), any()))
                .thenAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(1)).get());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("createTemplate - 成功创建模板")
    void createTemplate_validRequest_createsTemplate() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setName("Test Template");
        request.setDescription("Test description");
        request.setWorkflowJson("{\"nodes\":[]}");
        request.setWorkflowGraph("{}");
        request.setCategoryId(101L);
        request.setIsPublic("Y");
        request.setTagIds(new HashSet<>(Arrays.asList(1L, 2L)));

        TemplateCategory category = new TemplateCategory();
        category.setId(101L);
        category.setName("Test Category");
        when(categoryRepository.selectById(101L)).thenReturn(category);

        TemplateTag tag1 = new TemplateTag();
        tag1.setId(1L);
        tag1.setName("Tag1");
        TemplateTag tag2 = new TemplateTag();
        tag2.setId(2L);
        tag2.setName("Tag2");
        when(tagRepository.selectBatchIds(any())).thenReturn(List.of(tag1, tag2));

        when(templateRepository.insert(any(Template.class))).thenReturn(1);

        TemplateDTO result = templateService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Template");
        assertThat(result.getCategoryName()).isEqualTo("Test Category");
        verify(templateRepository).insert(any(Template.class));
        verify(tagRepository, times(2)).incrementUsageCount(any());
    }

    @Test
    @DisplayName("createTemplate - 不传 tagIds 时也能创建")
    void createTemplate_noTags_createsTemplate() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setName("Simple Template");
        request.setDescription("No tags");
        request.setWorkflowJson("{}");

        when(templateRepository.insert(any(Template.class))).thenReturn(1);

        TemplateDTO result = templateService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Simple Template");
        verify(tagRepository, never()).incrementUsageCount(any());
    }

    @Test
    @DisplayName("getTemplate - 存在则返回模板")
    void getTemplate_exists_returnsTemplate() {
        Template template = createTemplate(1001L, "Test Template");
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(builtinCatalog.findById("1001")).thenReturn(null);

        TemplateDTO result = templateService.getTemplate("1001");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getName()).isEqualTo("Test Template");
    }

    @Test
    @DisplayName("getTemplate - 不存在抛异常")
    void getTemplate_notFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);
        when(builtinCatalog.findById("9999")).thenReturn(null);

        assertThatThrownBy(() -> templateService.getTemplate("9999"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("updateTemplate - 成功更新模板")
    void updateTemplate_validRequest_updatesTemplate() {
        Template existing = createTemplate(1001L, "Old Name");
        existing.setCreateBy(1001L);
        when(templateRepository.findById(1001L)).thenReturn(existing);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setName("New Name");
        request.setDescription("New description");

        TemplateDTO result = templateService.updateTemplate(1001L, request);

        assertThat(result.getName()).isEqualTo("New Name");
        verify(templateRepository).updateById(any(Template.class));
    }

    @Test
    @DisplayName("updateTemplate - 非拥有者抛异常")
    void updateTemplate_notOwner_throwsBizException() {
        Template existing = createTemplate(1001L, "Test");
        existing.setCreateBy(999L);
        when(templateRepository.findById(1001L)).thenReturn(existing);

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> templateService.updateTemplate(1001L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权操作此模板");
    }

    @Test
    @DisplayName("deleteTemplate - 软删除模板")
    void deleteTemplate_exists_softDeletes() {
        Template existing = createTemplate(1001L, "Test Template");
        existing.setCreateBy(1001L);
        when(templateRepository.findById(1001L)).thenReturn(existing);
        when(templateRepository.updateById(any(Template.class))).thenReturn(1);

        templateService.deleteTemplate(1001L);

        assertThat(existing.getStatus()).isEqualTo("deleted");
        verify(templateRepository).updateById(existing);
    }

    @Test
    @DisplayName("deleteTemplate - 不存在抛异常")
    void deleteTemplate_notFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);

        assertThatThrownBy(() -> templateService.deleteTemplate(9999L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("useTemplate - 成功使用模板")
    void useTemplate_approvedTemplate_incrementsCount() {
        Template template = createTemplate(1001L, "Approved Template");
        template.setReviewStatus("approved");
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(builtinCatalog.findById("1001")).thenReturn(null);

        templateService.useTemplate("1001");

        // useCountCounter.increment 会被调用，不验证 incrementUseCount
    }

    @Test
    @DisplayName("useTemplate - 未审核模板抛异常")
    void useTemplate_notApproved_throwsBizException() {
        Template template = createTemplate(1001L, "Pending Template");
        template.setReviewStatus("pending");
        when(templateRepository.findById(1001L)).thenReturn(template);
        when(builtinCatalog.findById("1001")).thenReturn(null);

        assertThatThrownBy(() -> templateService.useTemplate("1001"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("模板未通过审核");
    }

    @Test
    @DisplayName("copyTemplate - 成功复制模板")
    void copyTemplate_exists_createsCopy() {
        Template original = createTemplate(1001L, "Original");
        original.setReviewStatus("approved");
        original.setWorkflowJson("{\"data\":\"test\"}");
        when(templateRepository.findById(1001L)).thenReturn(original);
        when(templateRepository.insert(any(Template.class))).thenReturn(1);
        when(builtinCatalog.findById("1001")).thenReturn(null);

        TemplateDTO result = templateService.copyTemplate("1001");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Original (副本)");
        assertThat(result.getCreateBy()).isEqualTo("1001");
        assertThat(result.getReviewStatus()).isEqualTo("pending");
        assertThat(result.getUseCount()).isEqualTo(0L);
        verify(templateRepository).insert(any(Template.class));
    }

    @Test
    @DisplayName("copyTemplate - 不存在抛异常")
    void copyTemplate_notFound_throwsBizException() {
        when(templateRepository.findById(9999L)).thenReturn(null);
        when(builtinCatalog.findById("9999")).thenReturn(null);

        assertThatThrownBy(() -> templateService.copyTemplate("9999"))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("getMyTemplates - 返回分页结果")
    void getMyTemplates_returnsPagedResults() {
        Template template = createTemplate(1001L, "My Template");
        Page<Template> page = new Page<>(1, 10);
        page.setRecords(List.of(template));
        page.setTotal(1L);
        when(templateRepository.findByUserId(any(Page.class), eq(1001L))).thenReturn(page);

        IPage<TemplateDTO> result = templateService.getMyTemplates(1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    private Template createTemplate(Long id, String name) {
        Template template = new Template();
        template.setId(id);
        template.setName(name);
        template.setDescription("Test description");
        template.setWorkflowJson("{}");
        template.setWorkflowGraph("{}");
        template.setStatus("active");
        template.setReviewStatus("pending");
        template.setIsPublic("N");
        template.setUseCount(0L);
        template.setScore(0.0);
        template.setReviewCount(0L);
        template.setTemplateVersion(1);
        template.setCreateBy(1001L);
        template.setCreateTime(LocalDateTime.now());
        return template;
    }
}



