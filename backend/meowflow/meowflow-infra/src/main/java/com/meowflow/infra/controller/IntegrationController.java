package com.meowflow.infra.controller;

import com.meowflow.common.result.Result;
import com.meowflow.infra.dto.IntegrationConfigDTO;
import com.meowflow.infra.entity.IntegrationConfigEntity;
import com.meowflow.infra.integration.IntegrationConfig;
import com.meowflow.infra.integration.IntegrationSender;
import com.meowflow.infra.service.IntegrationConfigService;
import com.meowflow.infra.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 集成配置控制器
 *
 * <p>提供基于数据库的 CRUD + 发送测试能力。
 * 兼容旧版 {@code /configs} 端点以便平滑迁移。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/integration")
@Tag(name = "集成配置", description = "第三方集成配置管理接口")
public class IntegrationController {

    private final IntegrationConfigService integrationConfigService;
    private final IntegrationService integrationService;

    public IntegrationController(IntegrationConfigService integrationConfigService,
                                 IntegrationService integrationService) {
        this.integrationConfigService = integrationConfigService;
        this.integrationService = integrationService;
    }

    // ---------- 新版基于数据库的接口 ----------

    @GetMapping("/enabled")
    @Operation(summary = "获取启用的集成列表", description = "供工作流节点下拉框使用")
    public Result<List<IntegrationConfigEntity>> listEnabled() {
        return Result.success(integrationConfigService.listEnabled());
    }

    @GetMapping
    @Operation(summary = "列出所有集成", description = "系统设置页面使用,包含已禁用")
    public Result<List<IntegrationConfigEntity>> listAll() {
        return Result.success(integrationConfigService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取集成详情", description = "敏感字段以加密形式返回")
    public Result<IntegrationConfigEntity> getOne(@PathVariable Long id) {
        return Result.success(integrationConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建集成配置")
    public Result<IntegrationConfigEntity> create(@RequestBody IntegrationConfigDTO dto) {
        return Result.success(integrationConfigService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新集成配置")
    public Result<IntegrationConfigEntity> update(@PathVariable Long id, @RequestBody IntegrationConfigDTO dto) {
        return Result.success(integrationConfigService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除集成配置")
    public Result<Void> delete(@PathVariable Long id) {
        integrationConfigService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试连接", description = "向目标集成发送一条测试消息")
    public Result<Map<String, Object>> testConnection(@PathVariable Long id) {
        return Result.success(integrationConfigService.testConnection(id));
    }

    // ---------- 兼容旧版接口(内部使用) ----------

    @PostMapping("/configs")
    @Operation(summary = "[兼容] 保存集成配置", description = "旧版基于内存的保存接口,建议迁移到新版")
    public Result<Void> saveConfig(@RequestBody IntegrationConfigRequest request) {
        IntegrationConfig config = new IntegrationConfig();
        config.setType(request.getType());
        config.setName(request.getName());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setEnabled(request.isEnabled());
        integrationService.saveConfig(config);
        return Result.success();
    }

    @GetMapping("/configs/{type}")
    @Operation(summary = "[兼容] 获取集成配置")
    public Result<IntegrationConfig> getConfig(@PathVariable String type) {
        return Result.success(integrationService.getConfig(type));
    }

    @GetMapping("/configs")
    @Operation(summary = "[兼容] 列出集成配置")
    public Result<List<IntegrationConfig>> listConfigs() {
        return Result.success(integrationService.listConfigs());
    }

    @DeleteMapping("/configs/{type}")
    @Operation(summary = "[兼容] 删除集成配置")
    public Result<Void> deleteConfig(@PathVariable String type) {
        integrationService.deleteConfig(type);
        return Result.success();
    }

    @PostMapping("/send")
    @Operation(summary = "发送通知", description = "通过指定渠道发送通知")
    public Result<IntegrationSender.SendResult> send(@RequestBody SendRequest request) {
        IntegrationSender.SendResult result = integrationService.send(request.getType(), request.getMessage());
        return Result.success(result);
    }

    @PostMapping("/send/async")
    @Operation(summary = "异步发送通知", description = "异步发送通知,回调通知结果")
    public Result<Void> sendAsync(@RequestBody SendRequest request) {
        integrationService.sendAsync(request.getType(), request.getMessage(), new IntegrationSender.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                log.info("Async send success: {} to {}", messageId, request.getType());
            }

            @Override
            public void onFailure(String errorMessage) {
                log.error("Async send failed: {} to {} - {}", errorMessage, request.getType());
            }
        });
        return Result.success();
    }

    @PostMapping("/send/with-retry")
    @Operation(summary = "重试发送通知", description = "带重试的发送通知")
    public Result<Void> sendWithRetry(@RequestBody SendWithRetryRequest request) {
        integrationService.sendWithRetry(request.getType(), request.getMessage(), request.getMaxRetries());
        return Result.success();
    }

    @PostMapping("/configs/dingtalk")
    @Operation(summary = "[兼容] 创建钉钉配置")
    public Result<Void> createDingtalkConfig(@RequestBody DingtalkConfigRequest request) {
        IntegrationConfig config = integrationService.createDingtalkConfig(
                request.getWebhookUrl(), request.getSecret());
        integrationService.saveConfig(config);
        return Result.success();
    }

    @PostMapping("/configs/feishu")
    @Operation(summary = "[兼容] 创建飞书配置")
    public Result<Void> createFeishuConfig(@RequestBody FeishuConfigRequest request) {
        IntegrationConfig config = integrationService.createFeishuConfig(request.getWebhookUrl());
        integrationService.saveConfig(config);
        return Result.success();
    }

    @PostMapping("/configs/wxwork")
    @Operation(summary = "[兼容] 创建企微配置")
    public Result<Void> createWxworkConfig(@RequestBody WxworkConfigRequest request) {
        IntegrationConfig config = integrationService.createWxworkConfig(request.getWebhookUrl());
        integrationService.saveConfig(config);
        return Result.success();
    }

    // ---------- DTO ----------

    @Data
    public static class IntegrationConfigRequest {
        @Parameter(description = "集成类型: dingtalk/feishu/wxwork/email/sms")
        private String type;

        @Parameter(description = "配置名称")
        private String name;

        @Parameter(description = "Webhook 地址")
        private String webhookUrl;

        @Parameter(description = "密钥")
        private String secret;

        @Parameter(description = "是否启用")
        private boolean enabled = true;
    }

    @Data
    public static class SendRequest {
        @Parameter(description = "集成类型")
        private String type;

        @Parameter(description = "消息内容")
        private String message;
    }

    @Data
    public static class SendWithRetryRequest {
        @Parameter(description = "集成类型")
        private String type;

        @Parameter(description = "消息内容")
        private String message;

        @Parameter(description = "最大重试次数")
        private int maxRetries = 3;
    }

    @Data
    public static class DingtalkConfigRequest {
        @Parameter(description = "Webhook 地址")
        private String webhookUrl;

        @Parameter(description = "加签密钥")
        private String secret;
    }

    @Data
    public static class FeishuConfigRequest {
        @Parameter(description = "Webhook 地址")
        private String webhookUrl;
    }

    @Data
    public static class WxworkConfigRequest {
        @Parameter(description = "Webhook 地址")
        private String webhookUrl;
    }
}
