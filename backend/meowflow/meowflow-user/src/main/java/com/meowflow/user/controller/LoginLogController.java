package com.meowflow.user.controller;

import com.meowflow.common.result.Result;
import com.meowflow.user.dto.LoginLogDTO;
import com.meowflow.user.dto.LoginLogQuery;
import com.meowflow.user.service.LoginLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "登录日志", description = "登录日志查询接口")
@RestController
@RequestMapping("/api/v1/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志", description = "支持用户名、登录状态、时间范围筛选")
    @GetMapping
    public Result<IPage<LoginLogDTO>> pageQuery(LoginLogQuery query) {
        IPage<LoginLogDTO> page = loginLogService.pageQuery(query);
        return Result.success(page);
    }

    @Operation(summary = "获取用户登录日志", description = "获取指定用户的登录日志列表")
    @GetMapping("/user/{username}")
    public Result<List<LoginLogDTO>> getByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        List<LoginLogDTO> logs = loginLogService.getLogsByUsername(username);
        return Result.success(logs);
    }

    @Operation(summary = "获取最近登录日志", description = "获取最近的登录成功日志")
    @GetMapping("/recent")
    public Result<List<LoginLogDTO>> getRecentLogs(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit) {
        List<LoginLogDTO> logs = loginLogService.getRecentLogs(limit);
        return Result.success(logs);
    }

    @Operation(summary = "统计登录失败次数", description = "统计用户在指定小时内的登录失败次数")
    @GetMapping("/fail-count")
    public Result<Integer> countFailedLogins(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "小时数") @RequestParam(defaultValue = "1") int hours) {
        int count = loginLogService.countFailedLogins(username, hours);
        return Result.success(count);
    }

    @Operation(summary = "清空登录日志", description = "清空所有登录日志记录（仅管理员可操作）")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        loginLogService.clean();
        return Result.success();
    }
}
