package com.meowflow.workflow.trigger;

import com.meowflow.common.result.Result;
import com.meowflow.common.util.JsonUtils;
import com.meowflow.common.redis.RedisService;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.definition.WorkflowDefinition;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.entity.WorkflowVersion;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import com.meowflow.workflow.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用插件事件触发入口。
 */
@Slf4j
@RestController
@RequestMapping("/api/plugin")
@RequiredArgsConstructor
public class PluginTriggerController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository versionRepository;
    private final ExecutionService executionService;
    private final PluginRegistryService pluginRegistryService;
    private final RedisService redisService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> registration) {
        Object pluginId = registration.get("pluginId");
        if (pluginId == null || pluginId.toString().isBlank()) {
            return Result.error(400, "pluginId 必填");
        }
        Object status = registration.get("status");
        if (status != null && !PluginRegistryService.isValidStatus(status.toString())) {
            return Result.error(400, "不支持的插件状态: " + status);
        }
        return Result.success(pluginRegistryService.register(registration));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> listPlugins() {
        return Result.success(pluginRegistryService.list());
    }

    @GetMapping("/{pluginId}")
    public Result<Map<String, Object>> getPlugin(@PathVariable String pluginId) {
        Map<String, Object> plugin = pluginRegistryService.get(pluginId);
        if (plugin == null) {
            return Result.error(404, "插件不存在");
        }
        return Result.success(plugin);
    }

    @PutMapping("/{pluginId}/status")
    public Result<Map<String, Object>> updateStatus(
            @PathVariable String pluginId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) String status) {
        Object statusValue = body != null ? body.get("status") : status;
        if (statusValue == null || statusValue.toString().isBlank()) {
            return Result.error(400, "status 必填");
        }
        if (!PluginRegistryService.isValidStatus(statusValue.toString())) {
            return Result.error(400, "不支持的插件状态: " + statusValue);
        }
        Map<String, Object> plugin = pluginRegistryService.updateStatus(pluginId, statusValue.toString());
        if (plugin == null) {
            return Result.error(404, "插件不存在");
        }
        return Result.success(plugin);
    }

    @DeleteMapping("/{pluginId}")
    public Result<Void> deletePlugin(@PathVariable String pluginId) {
        if (!pluginRegistryService.delete(pluginId)) {
            return Result.error(404, "插件不存在");
        }
        return Result.success();
    }

    @PostMapping("/trigger/{pluginId}")
    public Result<Map<String, Object>> trigger(@PathVariable String pluginId,
                                               @RequestBody(required = false) String rawBody,
                                               @RequestHeader(value = "X-Plugin-Signature", required = false) String signature,
                                               @RequestHeader(value = "X-Plugin-Event-Id", required = false) String eventId,
                                               @RequestHeader(value = "X-Plugin-Timestamp", required = false) String timestamp) {
        Map<String, Object> event;
        try {
            event = rawBody == null || rawBody.isBlank()
                    ? new HashMap<>()
                    : JsonUtils.fromJsonToMap(rawBody);
            if (event == null) {
                event = new HashMap<>();
            }
        } catch (Exception e) {
            return Result.error(400, "插件事件体不是合法 JSON");
        }

        Map<String, Object> registration = pluginRegistryService.get(pluginId);
        if (registration != null && !isActive(registration)) {
            return Result.error(403, "插件未启用");
        }

        if (registration != null) {
            Object secret = registration.get("secret");
            if (secret != null && !secret.toString().isBlank()) {
                String body = rawBody == null ? "" : rawBody;
                String signedPayload = timestamp != null ? body + "." + timestamp : body;
                if (signature == null || !WebhookSignatureVerifier.verify(signedPayload, signature, secret.toString())) {
                    return Result.error(401, "插件签名验证失败");
                }
            }
        }

        if (eventId != null && !eventId.isBlank()) {
            if (!Boolean.TRUE.equals(redisService.tryLock("plugin:dedup:" + eventId, 300))) {
                return Result.error(409, "事件已处理");
            }
        }

        if (registration != null) {
            Object workflowId = registration.get("workflowId");
            if (workflowId != null) {
                ExecutionRequest request = new ExecutionRequest();
                request.setWorkflowId(Long.valueOf(workflowId.toString()));
                request.setTriggerType("plugin");
                request.setInput(event);
                request.setAsync(true);
                executionService.execute(request, 0L);
                return Result.success(Map.of("pluginId", pluginId, "workflowId", workflowId));
            }
        }

        List<Workflow> workflows = workflowRepository.findAllRunning();
        List<Long> triggered = new ArrayList<>();

        for (Workflow workflow : workflows) {
            WorkflowVersion version = versionRepository
                    .findByWorkflowIdAndVersion(workflow.getId(), workflow.getCurrentVersion())
                    .orElse(null);
            if (version == null || version.getDefinition() == null) continue;

            WorkflowDefinition definition;
            try {
                definition = JsonUtils.fromJson(version.getDefinition(), WorkflowDefinition.class);
            } catch (Exception e) {
                log.warn("解析工作流定义失败: workflowId={}", workflow.getId());
                continue;
            }

            boolean matched = definition.getNodes() != null && definition.getNodes().stream()
                    .filter(n -> n.getType() == NodeType.TRIGGER_PLUGIN)
                    .anyMatch(n -> pluginId.equals(readPluginId(n)));
            if (!matched) continue;

            ExecutionRequest request = new ExecutionRequest();
            request.setWorkflowId(workflow.getId());
            request.setTriggerType("plugin");
            request.setInput(event);
            request.setAsync(true);
            executionService.execute(request, 0L);
            triggered.add(workflow.getId());
        }

        return Result.success(Map.of("pluginId", pluginId, "triggeredWorkflowIds", triggered));
    }

    private boolean isActive(Map<String, Object> registration) {
        Object status = registration.get("status");
        return status == null || "active".equalsIgnoreCase(status.toString().trim());
    }

    private Object readPluginId(NodeDefinition node) {
        if (node.getData() == null) return null;
        Object nested = node.getData().get("config");
        if (nested instanceof Map && ((Map<?, ?>) nested).containsKey("pluginId")) {
            return ((Map<?, ?>) nested).get("pluginId");
        }
        return node.getData().get("pluginId");
    }
}
