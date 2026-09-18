package com.meowflow.infra.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.result.Result;
import com.meowflow.infra.dto.AIModelDTO;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.service.AIModelConnectionTester;
import com.meowflow.infra.service.AIModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 模型配置控制器
 */
@RestController
@RequestMapping("/api/infra/model")
@RequiredArgsConstructor
@Tag(name = "AI 模型配置", description = "AI 模型配置管理接口")
public class AIModelController {

    private final AIModelService service;
    private final AIModelConnectionTester connectionTester;

    /**
     * 获取启用的模型列表(工作流节点使用)
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取启用的模型列表", description = "供工作流节点下拉框使用")
    public Result<List<AIModelEntity>> listEnabled() {
        return Result.success(service.listEnabled());
    }

    /**
     * 列出所有模型(管理用)
     */
    @GetMapping
    @Operation(summary = "列出所有模型", description = "系统设置页面使用,包含已禁用")
    public Result<List<AIModelEntity>> listAll() {
        return Result.success(service.listAll());
    }

    /**
     * 获取默认模型
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认模型", description = "返回 isDefault=true 且启用中的模型")
    public Result<AIModelEntity> getDefault() {
        return Result.success(service.getDefault());
    }

    /**
     * 分页查询(管理后台)
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<AIModelEntity>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(service.page(current, size, keyword));
    }

    /**
     * 按 ID 获取详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模型详情", description = "API Key 字段为加密,列表接口统一打码返回")
    public Result<AIModelEntity> getOne(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建模型")
    public Result<AIModelEntity> create(@Valid @RequestBody AIModelDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型")
    public Result<AIModelEntity> update(@PathVariable Long id, @Valid @RequestBody AIModelDTO dto) {
        return Result.success(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        return Result.success(connectionTester.test(id));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认", description = "将指定模型设为默认,并自动取消其它默认标记")
    public Result<Void> setDefault(@PathVariable Long id) {
        service.setDefault(id);
        return Result.success();
    }
}
