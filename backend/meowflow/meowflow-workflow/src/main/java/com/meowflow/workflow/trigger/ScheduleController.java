package com.meowflow.workflow.trigger;

import com.meowflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "定时调度")
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final TriggerManager triggerManager;

    @Operation(summary = "创建定时任务")
    @PostMapping("/workflow/{workflowId}/node/{nodeId}")
    public Result<Void> schedule(
            @PathVariable Long workflowId,
            @PathVariable String nodeId,
            @RequestParam String cronExpression) {

        log.info("Creating scheduled task: workflowId={}, nodeId={}, cron={}",
                workflowId, nodeId, cronExpression);

        triggerManager.scheduleCron(workflowId, nodeId, cronExpression);
        return Result.success();
    }

    @Operation(summary = "删除定时任务")
    @DeleteMapping("/workflow/{workflowId}/node/{nodeId}")
    public Result<Void> cancel(
            @PathVariable Long workflowId,
            @PathVariable String nodeId) {

        log.info("Cancelling scheduled task: workflowId={}, nodeId={}", workflowId, nodeId);

        triggerManager.cancelScheduledTask(workflowId, nodeId);
        return Result.success();
    }

    @Operation(summary = "查询定时任务状态")
    @GetMapping("/workflow/{workflowId}/node/{nodeId}/status")
    public Result<Boolean> status(
            @PathVariable Long workflowId,
            @PathVariable String nodeId) {

        boolean scheduled = triggerManager.isScheduled(workflowId, nodeId);
        return Result.success(scheduled);
    }

    @Operation(summary = "取消工作流所有定时任务")
    @DeleteMapping("/workflow/{workflowId}")
    public Result<Void> cancelAll(@PathVariable Long workflowId) {

        log.info("Cancelling all scheduled tasks: workflowId={}", workflowId);

        triggerManager.cancelAllTasks(workflowId);
        return Result.success();
    }
}
