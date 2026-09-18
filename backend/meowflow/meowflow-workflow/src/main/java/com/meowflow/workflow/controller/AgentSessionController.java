package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.infra.agent.AgentMessage;
import com.meowflow.infra.agent.AgentSessionService;
import com.meowflow.infra.chat.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 会话管理 API
 */
@Tag(name = "Agent 会话")
@RestController
@RequestMapping("/api/agent/session")
@RequiredArgsConstructor
public class AgentSessionController {

    private final AgentSessionService sessionService;

    @Operation(summary = "创建会话")
    @PostMapping("/create")
    public Result<String> createSession(@RequestParam Long userId,
                                       @RequestParam(required = false) String agentNodeId) {
        String sessionId = sessionService.createSession(userId, agentNodeId);
        return Result.success(sessionId);
    }

    @Operation(summary = "获取会话历史")
    @GetMapping("/{sessionId}/history")
    public Result<List<Message>> getHistory(@PathVariable String sessionId,
                                           @RequestParam(defaultValue = "10") Integer maxRounds) {
        List<Message> history = sessionService.getSessionHistory(sessionId, maxRounds);
        return Result.success(history);
    }

    @Operation(summary = "清空会话历史")
    @DeleteMapping("/{sessionId}/clear")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        sessionService.clearSession(sessionId);
        return Result.success();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success();
    }
}
