package com.meowflow.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.user.dto.AuditLogDTO;
import com.meowflow.user.dto.AuditLogQuery;
import com.meowflow.user.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "审计日志", description = "审计日志查询接口")
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志", description = "支持用户名、模块、状态、时间范围筛选")
    @GetMapping
    public Result<IPage<AuditLogDTO>> pageQuery(AuditLogQuery query) {
        IPage<AuditLogDTO> page = auditLogService.pageQuery(query);
        return Result.success(page);
    }

    @Operation(summary = "获取审计日志详情", description = "根据ID获取审计日志详细信息")
    @GetMapping("/{id}")
    public Result<AuditLogDTO> getById(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        AuditLogDTO log = auditLogService.getById(id);
        return Result.success(log);
    }

    @Operation(summary = "获取用户操作日志", description = "获取指定用户的操作日志列表")
    @GetMapping("/user/{username}")
    public Result<?> getByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        return Result.success(auditLogService.getByUsername(username));
    }
}
