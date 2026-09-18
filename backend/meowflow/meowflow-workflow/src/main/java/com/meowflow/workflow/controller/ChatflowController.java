package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.event.RunEvent;
import com.meowflow.workflow.event.RunEventSink;
import com.meowflow.workflow.service.ChatflowSessionStore;
import com.meowflow.workflow.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatflow")
@RequiredArgsConstructor
public class ChatflowController {

    private final ExecutionService executionService;
    private final ChatflowSessionStore sessionStore;
    private final RunEventSink runEventSink;

    @PostMapping("/{workflowId}/chat")
    public Result<Map<String, Object>> chat(@PathVariable Long workflowId,
                                            @RequestBody Map<String, Object> body) {
        String conversationId = body.get("conversationId") != null
                ? body.get("conversationId").toString()
                : UUID.randomUUID().toString();
        String message = body.get("message") != null ? body.get("message").toString() : "";
        Map<String, Object> inputs = body.get("inputs") instanceof Map
                ? (Map<String, Object>) body.get("inputs")
                : new HashMap<>();

        sessionStore.append(conversationId, "user", message);
        Map<String, Object> requestInput = new HashMap<>(inputs);
        requestInput.put("message", message);
        requestInput.put("conversationId", conversationId);
        requestInput.put("history", sessionStore.history(conversationId));

        ExecutionRequest request = new ExecutionRequest();
        request.setWorkflowId(workflowId);
        request.setTriggerType("chatflow");
        request.setInput(requestInput);
        request.setAsync(false);
        ExecutionResponse response = executionService.execute(request, 0L);

        String answer = extractAnswer(response.getOutput());
        sessionStore.append(conversationId, "assistant", answer);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId);
        result.put("answer", answer);
        result.put("executionId", response.getExecutionId());
        result.put("history", sessionStore.history(conversationId));
        return Result.success(result);
    }

    @PostMapping(value = "/{workflowId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long workflowId,
                             @RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        String conversationId = body.get("conversationId") != null
                ? body.get("conversationId").toString()
                : UUID.randomUUID().toString();
        String message = body.get("message") != null ? body.get("message").toString() : "";
        Map<String, Object> inputs = body.get("inputs") instanceof Map
                ? (Map<String, Object>) body.get("inputs")
                : new HashMap<>();

        sessionStore.append(conversationId, "user", message);
        Map<String, Object> requestInput = new HashMap<>(inputs);
        requestInput.put("message", message);
        requestInput.put("conversationId", conversationId);
        requestInput.put("history", sessionStore.history(conversationId));

        ExecutionRequest request = new ExecutionRequest();
        request.setWorkflowId(workflowId);
        request.setTriggerType("chatflow");
        request.setInput(requestInput);
        request.setAsync(true);
        ExecutionResponse response;
        try {
            response = executionService.execute(request, 0L);
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        Long executionId = response.getExecutionId();
        Thread worker = new Thread(() -> relayStream(emitter, executionId, conversationId));
        worker.setDaemon(true);
        worker.start();
        return emitter;
    }

    private void relayStream(SseEmitter emitter, Long executionId, String conversationId) {
        String cursor = "0-0";
        String answer = "";
        try {
            emitter.send(SseEmitter.event()
                    .name("conversation")
                    .data(Map.of("conversationId", conversationId)));

            boolean finished = false;
            while (!finished) {
                RunEventSink.EventPage page = runEventSink.fetchEvents(executionId, cursor);
                for (RunEvent event : page.events()) {
                    switch (event.getEvent()) {
                        case "node_stream_delta" -> {
                            String delta = String.valueOf(event.getData().getOrDefault("delta", ""));
                            answer += delta;
                            emitter.send(SseEmitter.event().name("answer").data(delta));
                        }
                        case "execution_succeeded" -> {
                            finished = true;
                            Object output = event.getData().get("output");
                            if (output instanceof Map<?, ?> map) {
                                answer = extractAnswer((Map<String, Object>) map);
                            }
                        }
                        case "execution_failed", "execution_cancelled" -> {
                            finished = true;
                            emitter.send(SseEmitter.event().name("error")
                                    .data(event.getData().getOrDefault("error", "执行失败")));
                        }
                        default -> {
                        }
                    }
                }
                if (page.nextCursor() != null) {
                    cursor = page.nextCursor();
                }
                if (page.events().isEmpty()) {
                    Thread.sleep(150);
                }
            }

            if (answer.isBlank()) {
                ExecutionResponse latest = executionService.getById(executionId);
                answer = extractAnswer(latest.getOutput());
            }
            sessionStore.append(conversationId, "assistant", answer);
            emitter.send(SseEmitter.event().name("done").data(Map.of(
                    "executionId", executionId,
                    "answer", answer
            )));
            emitter.complete();
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(e);
        }
    }

    private String extractAnswer(Map<String, Object> output) {
        if (output == null) return "";
        if (output.get("answer") != null) return output.get("answer").toString();
        if (output.get("text") != null) return output.get("text").toString();
        if (output.get("output") != null) return output.get("output").toString();
        return "";
    }
}
