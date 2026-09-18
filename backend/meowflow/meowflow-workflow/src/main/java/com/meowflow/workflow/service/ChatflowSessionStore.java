package com.meowflow.workflow.service;

import com.meowflow.common.redis.RedisService;
import com.meowflow.common.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chatflow 会话 Redis 存储。
 */
@Component
public class ChatflowSessionStore {

    private final RedisService redisService;

    public ChatflowSessionStore(RedisService redisService) {
        this.redisService = redisService;
    }

    public Map<String, Object> getOrCreate(String conversationId) {
        String key = sessionKey(conversationId);
        Map<String, Object> session = redisService.get(key);
        if (session == null) {
            session = new HashMap<>();
            session.put("history", new ArrayList<Map<String, String>>());
            redisService.set(key, session);
        }
        return session;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> history(String conversationId) {
        Map<String, Object> session = getOrCreate(conversationId);
        Object raw = session.get("history");
        if (raw instanceof List<?> list) {
            return (List<Map<String, String>>) list;
        }
        return new ArrayList<>();
    }

    public void append(String conversationId, String role, String content) {
        Map<String, Object> session = getOrCreate(conversationId);
        List<Map<String, String>> history = history(conversationId);
        history.add(Map.of("role", role, "content", content));
        session.put("history", history);
        redisService.set(sessionKey(conversationId), session);
    }

    private String sessionKey(String conversationId) {
        return "chatflow:session:" + conversationId;
    }
}
