package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.workflow.service.HumanTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/execution")
@RequiredArgsConstructor
public class HumanInputController {

    private final HumanTaskService humanTaskService;

    @PostMapping("/{executionId}/human-input/{nodeId}")
    public Result<Void> submit(@PathVariable Long executionId,
                               @PathVariable String nodeId,
                               @RequestBody Map<String, Object> input) {
        boolean completed = humanTaskService.complete(executionId, nodeId, input);
        return completed ? Result.success() : Result.error(404, "等待中的人工输入任务不存在");
    }
}
