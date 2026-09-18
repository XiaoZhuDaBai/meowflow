package com.meowflow.monitor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.monitor.dto.AlertRecordDTO;
import com.meowflow.monitor.dto.AlertRuleDTO;
import com.meowflow.monitor.dto.AlertSilenceCreateDTO;
import com.meowflow.monitor.dto.AlertSilenceDTO;
import com.meowflow.monitor.entity.AlertRule;
import com.meowflow.monitor.entity.AlertSilence;
import com.meowflow.monitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "告警管理", description = "告警规则和告警记录相关接口")
@RestController
@RequestMapping("/api/monitor/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "创建告警规则", description = "创建新的告警规则")
    @PostMapping("/rule")
    public Result<AlertRule> createRule(@Valid @RequestBody AlertRuleDTO dto) {
        return Result.success(alertService.createRule(dto));
    }

    @Operation(summary = "更新告警规则", description = "更新已有告警规则")
    @PutMapping("/rule/{id}")
    public Result<AlertRule> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @Valid @RequestBody AlertRuleDTO dto) {
        return Result.success(alertService.updateRule(id, dto));
    }

    @Operation(summary = "删除告警规则", description = "删除告警规则")
    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        alertService.deleteRule(id);
        return Result.success();
    }

    @Operation(summary = "获取告警规则", description = "根据ID获取告警规则详情")
    @GetMapping("/rule/{id}")
    public Result<AlertRule> getRule(@Parameter(description = "规则ID") @PathVariable Long id) {
        return Result.success(alertService.getRule(id));
    }

    @Operation(summary = "获取所有告警规则", description = "分页获取所有告警规则")
    @GetMapping("/rules")
    public Result<IPage<AlertRule>> getAllRules(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(alertService.getAllRules(pageNum, pageSize));
    }

    @Operation(summary = "评估告警规则", description = "评估指定规则是否触发告警")
    @PostMapping("/rule/{id}/evaluate")
    public Result<Void> evaluateRule(
            @Parameter(description = "规则ID") @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        AlertRule rule = alertService.getRule(id);
        if (rule != null && request.containsKey("value")) {
            alertService.evaluateRule(rule, request.get("value"));
        }
        return Result.success();
    }

    @Operation(summary = "查询告警记录", description = "根据状态查询告警记录")
    @GetMapping("/records")
    public Result<IPage<AlertRecordDTO>> getAlerts(
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(alertService.getAlerts(status, pageNum, pageSize));
    }

    @Operation(summary = "获取触发中的告警", description = "获取所有触发中的告警")
    @GetMapping("/firing")
    public Result<List<AlertRecordDTO>> getFiringAlerts() {
        return Result.success(alertService.getFiringAlerts());
    }

    @Operation(summary = "获取告警统计", description = "获取告警统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Integer>> getAlertStats() {
        return Result.success(Map.of(
                "firing", alertService.getFiringAlertCount()
        ));
    }

    @Operation(summary = "解决告警", description = "解决一个告警")
    @PostMapping("/{alertId}/resolve")
    public Result<Void> resolveAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId,
            @RequestBody(required = false) Map<String, String> request) {
        String comment = request != null ? request.get("comment") : null;
        alertService.resolveAlert(alertId, comment);
        return Result.success();
    }

    @Operation(summary = "创建告警沉默", description = "创建告警沉默规则")
    @PostMapping("/silence")
    public Result<AlertSilence> createSilence(@Valid @RequestBody AlertSilenceCreateDTO dto) {
        return Result.success(alertService.createSilence(dto));
    }

    @Operation(summary = "删除告警沉默", description = "删除告警沉默规则")
    @DeleteMapping("/silence/{id}")
    public Result<Void> deleteSilence(@Parameter(description = "沉默ID") @PathVariable Long id) {
        alertService.deleteSilence(id);
        return Result.success();
    }

    @Operation(summary = "获取告警沉默列表", description = "获取告警沉默规则列表")
    @GetMapping("/silences")
    public Result<IPage<AlertSilence>> getSilences(
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(alertService.getSilences(status, pageNum, pageSize));
    }
}
