package com.meowflow.workflow.trigger;

import com.meowflow.common.redis.RedisService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件注册与生命周期管理。
 */
@Component
public class PluginRegistryService {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_DISABLED = "disabled";

    private final RedisService redisService;

    public PluginRegistryService(RedisService redisService) {
        this.redisService = redisService;
    }

    public Map<String, Object> register(Map<String, Object> manifest) {
        Object rawPluginId = manifest.get("pluginId");
        String pluginId = rawPluginId == null ? null : String.valueOf(rawPluginId);
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId 不能为空");
        }
        Map<String, Object> existing = get(pluginId);
        Map<String, Object> saved = new HashMap<>(manifest);
        if (existing != null && existing.get("createdAt") != null) {
            saved.put("createdAt", existing.get("createdAt"));
        } else {
            saved.put("createdAt", LocalDateTime.now().toString());
        }
        saved.put("updatedAt", LocalDateTime.now().toString());
        if (saved.get("status") != null && !isValidStatus(saved.get("status").toString())) {
            throw new IllegalArgumentException("不支持的插件状态: " + saved.get("status"));
        }
        saved.put("status", normalizeStatus(saved.get("status")));
        redisService.set(key(pluginId), saved);
        addToIndex(pluginId);
        return saved;
    }

    public Map<String, Object> get(String pluginId) {
        return redisService.get(key(pluginId));
    }

    public Map<String, Object> updateStatus(String pluginId, String status) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("不支持的插件状态: " + status);
        }
        Map<String, Object> manifest = get(pluginId);
        if (manifest == null) return null;
        manifest.put("status", normalizeStatus(status));
        manifest.put("updatedAt", LocalDateTime.now().toString());
        redisService.set(key(pluginId), manifest);
        return manifest;
    }

    public boolean delete(String pluginId) {
        Boolean deleted = redisService.delete(key(pluginId));
        removeFromIndex(pluginId);
        return Boolean.TRUE.equals(deleted);
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        Object raw = redisService.get(INDEX_KEY);
        if (raw instanceof List<?> ids) {
            for (Object id : ids) {
                Map<String, Object> manifest = get(String.valueOf(id));
                if (manifest != null) result.add(manifest);
            }
        }
        return result;
    }

    private void addToIndex(String pluginId) {
        Object raw = redisService.get(INDEX_KEY);
        List<String> ids = raw instanceof List ? new ArrayList<>((List<String>) raw) : new ArrayList<>();
        if (!ids.contains(pluginId)) ids.add(pluginId);
        redisService.set(INDEX_KEY, ids);
    }

    private void removeFromIndex(String pluginId) {
        Object raw = redisService.get(INDEX_KEY);
        if (!(raw instanceof List)) return;
        List<String> ids = new ArrayList<>((List<String>) raw);
        if (ids.remove(pluginId)) {
            redisService.set(INDEX_KEY, ids);
        }
    }

    public static boolean isValidStatus(String status) {
        String normalized = normalizeStatus(status);
        return STATUS_ACTIVE.equals(normalized)
                || STATUS_INACTIVE.equals(normalized)
                || STATUS_DISABLED.equals(normalized);
    }

    private static String normalizeStatus(Object status) {
        if (status == null || status.toString().isBlank()) {
            return STATUS_ACTIVE;
        }
        return status.toString().trim().toLowerCase();
    }

    private String key(String pluginId) {
        return "plugin:registry:" + pluginId;
    }

    private static final String INDEX_KEY = "plugin:registry:index";
}
