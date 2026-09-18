package com.meowflow.workflow.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.config.WorkflowProperties;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.entity.Workflow;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Tag(name = "Webhook 触发")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final String REGISTRY_PREFIX = "wf:webhook:registration:";
    private static final Duration REGISTRY_TTL = Duration.ofDays(30);

    private final ExecutionService executionService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowProperties workflowProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, WebhookRegistrationRecord> registrationCache = new ConcurrentHashMap<>();

    @Operation(summary = "注册 Webhook 触发器")
    @PostMapping("/register")
    public Result<WebhookRegistrationResponse> register(@RequestBody WebhookRegistrationRequest request) {
        if (request == null || request.getWorkflowId() == null
                || request.getPath() == null || request.getPath().isEmpty()) {
            return Result.error(400, "workflowId 与 path 必填");
        }
        Workflow wf = workflowRepository.selectById(request.getWorkflowId());
        if (wf == null) {
            return Result.error(404, "工作流不存在");
        }

        String workflowCode = wf.getCode() != null && !wf.getCode().isEmpty()
                ? wf.getCode()
                : "wf_" + wf.getId();
        String method = normalizeMethod(request.getMethod());
        if (method == null) {
            return Result.error(400, "method 仅支持 GET / POST / PUT");
        }

        String path = normalizePath(request.getPath());
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        WebhookRegistrationRecord record = new WebhookRegistrationRecord(
                request.getWorkflowId(), workflowCode, path, method, request.getSecret(), token);
        registrationCache.put(registryKey(workflowCode, path), record);
        persistRegistration(workflowCode, path, record);

        WebhookRegistrationResponse resp = new WebhookRegistrationResponse();
        resp.setWorkflowCode(workflowCode);
        resp.setPath(path);
        resp.setMethod(method);
        resp.setSecret(request.getSecret());
        resp.setToken(token);
        resp.setWebhookUrl(buildGatewayBaseUrl() + "/workflow/api/webhook/" + workflowCode + path);
        log.info("Webhook 注册成功: workflowId={}, workflowCode={}, path={}, method={}",
                request.getWorkflowId(), workflowCode, path, method);
        return Result.success(resp);
    }

    @Operation(summary = "触发工作流（支持固定路径和自定义子路径）")
    @RequestMapping(value = {"/{workflowCode}", "/{workflowCode}/**"}, method = {
            RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT
    })
    public Result<ExecutionResponse> trigger(
            @PathVariable String workflowCode,
            @RequestBody(required = false) Map<String, Object> requestBody,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestParam(required = false) String secret,
            HttpServletRequest request) {

        String routePath = resolveRoutePath(request, workflowCode);
        WebhookRegistrationRecord registration = findRegistration(workflowCode, routePath);
        if (registration != null && !registration.getMethod().equalsIgnoreCase(request.getMethod())) {
            return Result.error(405, "该 Webhook 仅支持 " + registration.getMethod());
        }

        Map<String, Object> payload = requestBody != null
                ? requestBody
                : copyQueryParameters(request);
        String verificationSecret = registration != null && registration.getSecret() != null
                ? registration.getSecret()
                : secret;

        log.info("Webhook received: workflowCode={}, path={}, method={}, hasPayload={}",
                workflowCode, routePath, request.getMethod(), !payload.isEmpty());

        if (verificationSecret != null && !verificationSecret.isBlank()) {
            if (signature == null || signature.isBlank()) {
                return Result.error(401, "缺少 X-Webhook-Signature 签名");
            }
            String canonicalPayload = toSignaturePayload(payload);
            if (!WebhookSignatureVerifier.verify(canonicalPayload, signature, verificationSecret)) {
                log.warn("Webhook signature verification failed: workflowCode={}, path={}", workflowCode, routePath);
                return Result.error(401, "签名验证失败");
            }
        }

        Workflow workflow = resolveWorkflow(workflowCode);
        if (workflow == null) {
            return Result.error(404, "工作流不存在或未发布: " + workflowCode);
        }

        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setWorkflowId(workflow.getId());
        executionRequest.setTriggerType("webhook");
        executionRequest.setInput(payload);
        executionRequest.setAsync(true);

        Long userId = UserContextHolder.getUserId();
        ExecutionResponse response = executionService.execute(executionRequest, userId != null ? userId : 0L);
        return Result.success(response);
    }

    @Operation(summary = "Webhook 回调（用于接收外部响应）")
    @PostMapping("/callback/{executionId}")
    public Result<Void> callback(
            @PathVariable Long executionId,
            @RequestBody Map<String, Object> response) {
        log.info("Webhook callback received: executionId={}", executionId);
        return Result.success();
    }

    private Workflow resolveWorkflow(String workflowCode) {
        if (workflowCode == null || workflowCode.isBlank()) {
            return null;
        }
        Workflow byCode = workflowRepository.selectByCode(workflowCode);
        if (byCode != null) {
            return byCode;
        }
        if (workflowCode.startsWith("wf_")) {
            try {
                Long id = Long.parseLong(workflowCode.substring(3));
                return workflowRepository.selectById(id);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return null;
    }

    private String resolveRoutePath(HttpServletRequest request, String workflowCode) {
        String uri = request.getRequestURI();
        String marker = "/api/webhook/" + workflowCode;
        int index = uri.indexOf(marker);
        if (index < 0) {
            return "/";
        }
        return normalizePath(uri.substring(index + marker.length()));
    }

    private Map<String, Object> copyQueryParameters(HttpServletRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values == null || values.length == 0) {
                payload.put(key, null);
            } else if (values.length == 1) {
                payload.put(key, values[0]);
            } else {
                payload.put(key, values);
            }
        });
        return payload;
    }

    private String toSignaturePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }

    private void persistRegistration(String workflowCode, String path, WebhookRegistrationRecord record) {
        try {
            stringRedisTemplate.opsForValue().set(
                    REGISTRY_PREFIX + registryKey(workflowCode, path),
                    objectMapper.writeValueAsString(record),
                    REGISTRY_TTL);
        } catch (Exception e) {
            log.warn("Webhook registration persisted in memory only: code={}, path={}, reason={}",
                    workflowCode, path, e.getMessage());
        }
    }

    private WebhookRegistrationRecord findRegistration(String workflowCode, String path) {
        String key = registryKey(workflowCode, path);
        WebhookRegistrationRecord cached = registrationCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(REGISTRY_PREFIX + key);
            if (json == null || json.isBlank()) {
                return null;
            }
            WebhookRegistrationRecord record = objectMapper.readValue(json, WebhookRegistrationRecord.class);
            registrationCache.put(key, record);
            return record;
        } catch (Exception e) {
            log.warn("Failed to load webhook registration: code={}, path={}, reason={}",
                    workflowCode, path, e.getMessage());
            return null;
        }
    }

    private String registryKey(String workflowCode, String path) {
        return workflowCode + ":" + normalizePath(path);
    }

    private String buildGatewayBaseUrl() {
        return workflowProperties.getGateway() != null
                && workflowProperties.getGateway().getBaseUrl() != null
                && !workflowProperties.getGateway().getBaseUrl().isEmpty()
                ? workflowProperties.getGateway().getBaseUrl()
                : "http://localhost:8080";
    }

    private String normalizeMethod(String method) {
        String normalized = method == null || method.isBlank() ? "POST" : method.trim().toUpperCase();
        return switch (normalized) {
            case "GET", "POST", "PUT" -> normalized;
            default -> null;
        };
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Data
    public static class WebhookRegistrationRequest {
        private Long workflowId;
        private String path;
        private String method;
        private String secret;
    }

    @Data
    public static class WebhookRegistrationResponse {
        private String workflowCode;
        private String path;
        private String method;
        private String secret;
        private String token;
        private String webhookUrl;
    }

    @Data
    public static class WebhookRegistrationRecord {
        private Long workflowId;
        private String workflowCode;
        private String path;
        private String method;
        private String secret;
        private String token;

        public WebhookRegistrationRecord() {
        }

        public WebhookRegistrationRecord(Long workflowId, String workflowCode, String path,
                                         String method, String secret, String token) {
            this.workflowId = workflowId;
            this.workflowCode = workflowCode;
            this.path = path;
            this.method = method;
            this.secret = secret;
            this.token = token;
        }
    }
}
