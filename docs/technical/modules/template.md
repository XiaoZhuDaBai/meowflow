# MeowFlow Template - 模板服务模块设计文档

> 本文档详细描述喵流平台的模板服务模块（MeowFlow-template）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          MeowFlow-template 模块定位                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-template 是模板服务模块，负责：                                   │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 模板管理                                                    │   │
│  │      • 模板创建、编辑、发布                                       │   │
│  │      • 模板分类、标签                                            │   │
│  │      • 模板审核、上下架                                           │   │
│  │                                                                   │   │
│  │   2. 模板市场                                                    │   │
│  │      • 模板浏览、搜索                                            │   │
│  │      • 模板评分、评论                                            │   │
│  │      • 一键使用、复制修改                                        │   │
│  │                                                                   │   │
│  │   3. 模板推荐                                                    │   │
│  │      • 个性化推荐                                                │   │
│  │      • 热门模板                                                  │   │
│  │      • 最新模板                                                  │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 模板标准化                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 统一模板结构定义                                                   │
│  • 模板元数据规范                                                     │
│  • 模板版本管理                                                       │
│                                                                          │
│  2. 社区化运营                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 模板审核机制保障质量                                               │
│  • 用户评分形成口碑                                                   │
│  • 模板分类便于发现                                                   │
│                                                                          │
│  3. 零门槛使用                                                        │
│  ───────────────────────────────────────────────────────────────────────│
│  • 一键使用快速上手                                                   │
│  • 复制后自定义修改                                                   │
│  • 模板预览降低风险                                                   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
MeowFlow-template/
├── pom.xml
└── src/main/java/com/meowflow/template/
    │
    ├── controller/                      # 控制器层
    │   ├── TemplateController.java   # 模板管理
    │   ├── TemplateReviewController.java     # 审核管理
    │   ├── TemplateRatingController.java     # 评分管理
    │   ├── TemplateCategoryController.java   # 分类管理
    │   └── TemplateSearchController.java     # 搜索管理
    │
    ├── service/                         # 业务逻辑层
    │   ├── TemplateService.java         # 模板服务
    │   ├── TemplateSearchService.java    # 模板搜索服务
    │   ├── TemplateReviewService.java    # 模板审核服务
    │   ├── TemplateRatingService.java    # 模板评分服务
    │   └── TemplateCategoryService.java  # 分类标签服务
    │
    ├── repository/                     # 数据访问层
    │   ├── TemplateRepository.java
    │   ├── TemplateReviewRepository.java
    │   ├── TemplateRatingRepository.java
    │   ├── TemplateCategoryRepository.java
    │   └── TemplateTagRepository.java
    │
    ├── entity/                        # 实体类
    │   ├── Template.java
    │   ├── TemplateCategory.java
    │   ├── TemplateTag.java
    │   ├── TemplateReview.java
    │   ├── TemplateReviewRecord.java
    │   ├── TemplateRating.java
    │   └── TemplateRatingHelpful.java
    │
    ├── dto/                          # 数据传输对象
    │   ├── TemplateDTO.java
    │   ├── TemplateCreateRequest.java
    │   ├── TemplateUpdateRequest.java
    │   ├── TemplateSearchRequest.java
    │   ├── TemplateReviewRequest.java
    │   ├── TemplateRatingRequest.java
    │   ├── TemplateRatingDTO.java
    │   ├── RatingStatistics.java
    │   ├── CategoryCreateRequest.java
    │   ├── TemplateCategoryDTO.java
    │   ├── TagCreateRequest.java
    │   └── TemplateTagDTO.java
    │
    └── config/                        # 配置类
        └── TemplateConfig.java
```

---

## 三、模板管理

### 3.1 模板实体

```java
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_tpl_template")
public class Template implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;                      // 使用雪花ID

    private String name;                   // 模板名称
    private String description;            // 模板描述
    private String workflowJson;            // 工作流JSON定义
    private String workflowGraph;           // 工作流图形定义
    private String categoryId;              // 分类ID
    private String coverImage;              // 封面图
    private String previewImages;           // 预览图列表
    private String tags;                   // 标签ID列表（逗号分隔）

    private Integer useCount = 0;           // 使用次数
    private Double rating = 0.0;            // 平均评分
    private Integer reviewCount = 0;        // 评价数量

    private String status;                  // 状态：active/deleted
    private String reviewStatus;            // 审核状态：pending/approved/rejected
    private String reviewComment;           // 审核意见
    private String reviewBy;                // 审核人
    private LocalDateTime reviewTime;       // 审核时间

    private String createBy;                // 创建者
    private LocalDateTime createTime;       // 创建时间
    private String updateBy;                // 更新者
    private LocalDateTime updateTime;      // 更新时间

    private String isPublic;               // 是否公开：Y/N
    private String isFeatured;             // 是否精选：Y/N
    private Integer version;               // 版本号
    private String remark;                 // 备注
}
```

### 3.2 模板服务

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateTagRepository tagRepository;
    private final TemplateRatingRepository ratingRepository;
    private final IdGenerator idGenerator;    // 使用雪花ID生成器

    /**
     * 创建模板 - 使用 IdGenerator 生成雪花ID
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO createTemplate(TemplateCreateRequest request) {
        Template template = new Template();
        template.setId(idGenerator.nextIdStr());  // 雪花ID
        // ... 设置其他字段
        templateRepository.insert(template);
        return convertToDTO(template);
    }

    /**
     * 更新模板 - 包含权限检查
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDTO updateTemplate(String id, TemplateUpdateRequest request) {
        Template template = templateRepository.findById(id);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        checkTemplateOwner(template);  // 权限检查

        // ... 更新逻辑
        return convertToDTO(template);
    }

    /**
     * 删除模板 - 包含权限检查
     */
    public void deleteTemplate(String id) {
        Template template = templateRepository.findById(id);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        checkTemplateOwner(template);  // 权限检查

        template.setStatus("deleted");
        templateRepository.updateById(template);
    }

    /**
     * 权限检查 - 所有者或管理员可操作
     */
    private void checkTemplateOwner(Template template) {
        String currentUserId = getCurrentUserId();
        if (!template.getCreateBy().equals(currentUserId) && !UserContextHolder.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作此模板");
        }
    }

    /**
     * 复制模板
     */
    public TemplateDTO copyTemplate(String id) {
        Template original = templateRepository.findById(id);
        Template copy = new Template();
        copy.setId(idGenerator.nextIdStr());  // 雪花ID
        copy.setName(original.getName() + " (副本)");
        // ... 复制其他字段
        templateRepository.insert(copy);
        return convertToDTO(copy);
    }
}
```

---

## 四、模板审核

### 4.1 审核服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          模板审核服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TemplateReviewService 模板审核服务                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  @Transactional                                                 │    │
│  │  public class TemplateReviewService {                            │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateRepository templateRepository;                  │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateReviewRepository reviewRepository;            │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 审核模板                                                  │    │
│  │       */                                                        │    │
│  │      public void reviewTemplate(String templateId,                │    │
│  │                                ReviewRequest request) {           │    │
│  │          Template template = templateRepository.findById(templateId) │    │
│  │              .orElseThrow(() -> new TemplateNotFoundException(templateId));│   │
│  │                                                                   │    │
│  │          // 检查状态                                              │    │
│  │          if (!"pending".equals(template.getStatus())) {         │    │
│  │              throw new BizException(ResultCode.TEMPLATE_NOT_PENDING);│    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 创建审核记录                                          │    │
│  │          TemplateReview review = new TemplateReview();            │    │
│  │          review.setTemplateId(templateId);                        │    │
│  │          review.setReviewerId(userContextHolder.getUserId());    │    │
│  │          review.setStatus(request.getStatus());                  │    │
│  │          review.setComment(request.getComment());                 │    │
│  │          reviewRepository.save(review);                          │    │
│  │                                                                   │    │
│  │          // 更新模板状态                                          │    │
│  │          template.setStatus(request.getStatus());                │    │
│  │          templateRepository.save(template);                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取待审核列表                                            │    │
│  │       */                                                        │    │
│  │      public Page<Template> getPendingTemplates(Pageable pageable) {│   │
│  │          return templateRepository.findByStatus("pending", pageable);│   │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取审核历史                                              │    │
│  │       */                                                        │    │
│  │      public List<TemplateReview> getReviewHistory(String templateId) {│  │
│  │          return reviewRepository.findByTemplateIdOrderByCreateTimeDesc(templateId);│  │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、模板搜索

### 5.1 搜索服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          模板搜索服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TemplateSearchService 模板搜索服务                                │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class TemplateSearchService {                             │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateRepository templateRepository;                  │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 搜索模板                                                  │    │
│  │       */                                                        │    │
│  │      public Page<Template> searchTemplates(TemplateSearchRequest request) {│  │
│  │          // 构建查询条件                                          │    │
│  │          BooleanBuilder conditions = new BooleanBuilder();         │    │
│  │                                                                   │    │
│  │          // 状态筛选                                              │    │
│  │          conditions.and(QTemplate.template.status.eq("approved"));│    │
│  │                                                                   │    │
│  │          // 分类筛选                                              │    │
│  │          if (StringUtils.isNotBlank(request.getCategory())) {    │    │
│  │              conditions.and(QTemplate.template.category.eq(request.getCategory()));│  │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 关键词搜索                                            │    │
│  │          if (StringUtils.isNotBlank(request.getKeyword())) {    │    │
│  │              String keyword = "%" + request.getKeyword() + "%";  │    │
│  │              conditions.and(                                     │    │
│  │                  QTemplate.template.name.like(keyword)          │    │
│  │                      .or(QTemplate.template.description.like(keyword))│   │
│  │                      .or(QTemplate.template.tags.stringValue().like(keyword))│  │
│  │              );                                                  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 排序                                                  │    │
│  │          PageRequest pageRequest = PageRequest.of(                 │    │
│  │              request.getPage(), request.getSize());             │    │
│  │                                                                   │    │
│  │          return templateRepository.findAll(conditions, pageRequest);│    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取热门模板                                              │    │
│  │       */                                                        │    │
│  │      public List<Template> getHotTemplates(int limit) {            │    │
│  │          return templateRepository.findTopByStatusOrderByUseCountDesc(│  │
│  │              "approved", PageRequest.of(0, limit)                │    │
│  │          ).getContent();                                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取最新模板                                              │    │
│  │       */                                                        │    │
│  │      public List<Template> getLatestTemplates(int limit) {        │    │
│  │          return templateRepository.findTopByStatusOrderByCreateTimeDesc(│ │
│  │              "approved", PageRequest.of(0, limit)                │    │
│  │          ).getContent();                                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取推荐模板                                              │    │
│  │       */                                                        │    │
│  │      public List<Template> getRecommendedTemplates(String userId, int limit) {│  │
│  │          // 简化实现：基于评分和使用的综合推荐                    │    │
│  │          return templateRepository.findRecommendedTemplates(       │    │
│  │              "approved", limit                                   │    │
│  │          );                                                      │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、模板评价

### 6.1 评价服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          模板评价服务                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TemplateReviewService 模板评价服务                              │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  @Transactional                                                 │    │
│  │  public class TemplateReviewService {                            │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateRepository templateRepository;                  │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateReviewRepository reviewRepository;            │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 评价模板                                                  │    │
│  │       */                                                        │    │
│  │      public void reviewTemplate(String templateId,                │    │
│  │                                TemplateReviewRequest request) {   │    │
│  │          Template template = templateRepository.findById(templateId) │    │
│  │              .orElseThrow(() -> new TemplateNotFoundException(templateId));│   │
│  │                                                                   │    │
│  │          // 检查是否已评价                                        │    │
│  │          if (reviewRepository.existsByTemplateIdAndUserId(       │    │
│  │                  templateId, userContextHolder.getUserId())) {  │    │
│  │              throw new BizException(ResultCode.ALREADY_REVIEWED);│    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 创建评价                                              │    │
│  │          TemplateReview review = new TemplateReview();            │    │
│  │          review.setTemplateId(templateId);                        │    │
│  │          review.setUserId(userContextHolder.getUserId());        │    │
│  │          review.setRating(request.getRating());                  │    │
│  │          review.setContent(request.getContent());                 │    │
│  │          review.setAnonymous(request.getAnonymous());            │    │
│  │          reviewRepository.save(review);                          │    │
│  │                                                                   │    │
│  │          // 更新模板评分                                          │    │
│  │          updateTemplateRating(template);                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取评价列表                                              │    │
│  │       */                                                        │    │
│  │      public Page<TemplateReview> getReviews(String templateId,   │    │
│  │                                              Pageable pageable) {  │    │
│  │          return reviewRepository.findByTemplateIdOrderByCreateTimeDesc(│ │
│  │              templateId, pageable);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 更新模板评分                                              │    │
│  │       */                                                        │    │
│  │      private void updateTemplateRating(Template template) {       │    │
│  │          // 计算平均评分                                          │    │
│  │          Double avgRating = reviewRepository.getAverageRating(template.getId());│  │
│  │          Long reviewCount = reviewRepository.countByTemplateId(template.getId());│  │
│  │                                                                   │    │
│  │          template.setRating(avgRating != null ?                 │    │
│  │              BigDecimal.valueOf(avgRating) : BigDecimal.ZERO);   │    │
│  │          template.setReviewCount(reviewCount != null ?           │    │
│  │              reviewCount.intValue() : 0);                       │    │
│  │                                                                   │    │
│  │          templateRepository.save(template);                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、API 接口

### 7.1 模板接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          模板 API                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  TemplateController 模板接口                                      │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/templates")                           │    │
│  │  @Api(tags = "模板管理")                                         │    │
│  │  public class TemplateController {                               │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private TemplateService templateService;                     │    │
│  │                                                                   │    │
│  │      @PostMapping                                                │    │
│  │      @ApiOperation("创建模板")                                   │    │
│  │      public Result<TemplateDTO> create(                         │    │
│  │          @RequestBody @Valid TemplateCreateRequest request) {   │    │
│  │          Template template = templateService.createTemplate(request);│  │
│  │          return Result.success(toDTO(template));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/{id}")                                        │    │
│  │      @ApiOperation("更新模板")                                   │    │
│  │      public Result<TemplateDTO> update(                         │    │
│  │          @PathVariable String id,                                │    │
│  │          @RequestBody TemplateUpdateRequest request) {           │    │
│  │          Template template = templateService.updateTemplate(id, request);│ │
│  │          return Result.success(toDTO(template));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/submit")                               │    │
│  │      @ApiOperation("提交审核")                                   │    │
│  │      public Result<Void> submit(@PathVariable String id) {        │    │
│  │          templateService.submitForReview(id);                    │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/use")                                  │    │
│  │      @ApiOperation("使用模板创建工作流")                         │    │
│  │      public Result<WorkflowDTO> useTemplate(                     │    │
│  │          @PathVariable String id,                                │    │
│  │          @RequestParam String workflowName) {                    │    │
│  │          Workflow workflow = templateService.useTemplate(id, workflowName);│  │
│  │          return Result.success(toWorkflowDTO(workflow));         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/{id}/copy")                                  │    │
│  │      @ApiOperation("复制模板")                                   │    │
│  │      public Result<TemplateDTO> copy(@PathVariable String id) {  │    │
│  │          Template template = templateService.copyTemplate(id);   │    │
│  │          return Result.success(toDTO(template));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{id}")                                        │    │
│  │      @ApiOperation("获取模板详情")                                │    │
│  │      public Result<TemplateDTO> getById(@PathVariable String id) {│ │
│  │          Template template = templateService.getById(id);        │    │
│  │          return Result.success(toDTO(template));                  │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping                                                │    │
│  │      @ApiOperation("搜索模板")                                   │    │
│  │      public Result<Page<TemplateDTO>> search(                    │    │
│  │          @ModelAttribute TemplateSearchRequest request) {       │    │
│  │          Page<Template> page = templateSearchService             │    │
│  │              .searchTemplates(request);                          │    │
│  │          return Result.success(page.map(this::toDTO));           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/hot")                                        │    │
│  │      @ApiOperation("获取热门模板")                               │    │
│  │      public Result<List<TemplateDTO>> getHot(                   │    │
│  │          @RequestParam(defaultValue = "10") int limit) {        │    │
│  │          List<Template> templates = templateSearchService         │    │
│  │              .getHotTemplates(limit);                           │    │
│  │          return Result.success(templates.stream().map(this::toDTO)│    │
│  │              .collect(Collectors.toList()));                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/latest")                                      │    │
│  │      @ApiOperation("获取最新模板")                               │    │
│  │      public Result<List<TemplateDTO>> getLatest(                 │    │
│  │          @RequestParam(defaultValue = "10") int limit) {        │    │
│  │          List<Template> templates = templateSearchService         │    │
│  │              .getLatestTemplates(limit);                        │    │
│  │          return Result.success(templates.stream().map(this::toDTO)│    │
│  │              .collect(Collectors.toList()));                     │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 八、依赖说明

### 8.1 模块依赖

Template 模块依赖以下模块：

| 依赖模块 | 版本 | 用途 |
|----------|------|------|
| `meowflow-common` | ${project.version} | 通用工具、上下文、异常处理 |
| `meowflow-infra` | ${project.version} | 搜索服务、AI 能力、集成服务 |

### 8.2 核心依赖说明

- **IdGenerator**：雪花 ID 生成器，用于生成全局唯一 ID
- **UserContextHolder**：用户上下文，获取当前登录用户信息
- **BizException**：业务异常，统一异常处理
- **ResultCode**：结果码定义，规范错误码

---

## 九、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-07-10 | 初始版本 |
| v1.1 | 2026-07-15 | 完善模块结构，添加 IdGenerator、权限检查、Infra 依赖 |

---

**文档版本：v1.1**
**最后更新：2026-07-15**
