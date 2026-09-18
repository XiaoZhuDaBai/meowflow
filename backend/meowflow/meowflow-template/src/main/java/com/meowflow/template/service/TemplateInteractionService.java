package com.meowflow.template.service;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.template.dto.TemplateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateInteractionService {

    private static final String LIKE_USERS = "wf:tpl:like:users:";
    private static final String LIKE_COUNT = "wf:tpl:like:count:";
    private static final String FAVORITE_USERS = "wf:tpl:favorite:users:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Set<String>> memorySets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> memoryCounts = new ConcurrentHashMap<>();

    public void enrich(TemplateDTO dto, String userId) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        String templateId = String.valueOf(dto.getId());
        dto.setLikes(getLikeCount(templateId));
        if (userId != null && !userId.isBlank()) {
            dto.setIsLiked(isLiked(templateId, userId));
            dto.setIsFavorited(isFavorited(templateId, userId));
        } else {
            dto.setIsLiked(false);
            dto.setIsFavorited(false);
        }
    }

    public void like(String templateId, String userId) {
        if (addToSet(LIKE_USERS + templateId, userId)) {
            increment(LIKE_COUNT + templateId);
        }
    }

    public void unlike(String templateId, String userId) {
        if (removeFromSet(LIKE_USERS + templateId, userId)) {
            decrement(LIKE_COUNT + templateId);
        }
    }

    public void favorite(String templateId, String userId) {
        addToSet(FAVORITE_USERS + templateId, userId);
    }

    public void unfavorite(String templateId, String userId) {
        removeFromSet(FAVORITE_USERS + templateId, userId);
    }

    public long getLikeCount(String templateId) {
        long redisCount = 0L;
        try {
            String value = redisTemplate.opsForValue().get(LIKE_COUNT + templateId);
            if (value != null) {
                redisCount = Long.parseLong(value);
            }
        } catch (Exception e) {
            log.debug("Read like count from Redis failed, using memory fallback: {}", e.getMessage());
        }
        long memoryCount = memoryCount(LIKE_COUNT + templateId).get();
        return Math.max(0L, Math.max(redisCount, memoryCount));
    }

    public boolean isLiked(String templateId, String userId) {
        return isMember(LIKE_USERS + templateId, userId);
    }

    public boolean isFavorited(String templateId, String userId) {
        return isMember(FAVORITE_USERS + templateId, userId);
    }

    public Set<String> getFavoriteTemplateIds(String userId) {
        String key = "wf:tpl:favorite:ids:" + userId;
        try {
            Set<String> values = redisTemplate.opsForSet().members(key);
            return values == null ? Collections.emptySet() : values;
        } catch (Exception e) {
            log.debug("Read favorites from Redis failed, using memory fallback: {}", e.getMessage());
            return memorySets.getOrDefault(key, Collections.emptySet());
        }
    }

    private boolean addToSet(String key, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            Long added = redisTemplate.opsForSet().add(key, userId);
            if (added != null && added > 0) {
                if (key.startsWith(FAVORITE_USERS)) {
                    String owner = key.substring(FAVORITE_USERS.length());
                    redisTemplate.opsForSet().add("wf:tpl:favorite:ids:" + userId, owner);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("Redis set add failed, using memory fallback: {}", e.getMessage());
            boolean added = memorySet(key).add(userId);
            if (added && key.startsWith(FAVORITE_USERS)) {
                memorySet("wf:tpl:favorite:ids:" + userId).add(key.substring(FAVORITE_USERS.length()));
            }
            return added;
        }
    }

    private boolean removeFromSet(String key, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            Long removed = redisTemplate.opsForSet().remove(key, userId);
            if (removed != null && removed > 0) {
                if (key.startsWith(FAVORITE_USERS)) {
                    String owner = key.substring(FAVORITE_USERS.length());
                    redisTemplate.opsForSet().remove("wf:tpl:favorite:ids:" + userId, owner);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("Redis set remove failed, using memory fallback: {}", e.getMessage());
            boolean removed = memorySet(key).remove(userId);
            if (removed && key.startsWith(FAVORITE_USERS)) {
                memorySet("wf:tpl:favorite:ids:" + userId).remove(key.substring(FAVORITE_USERS.length()));
            }
            return removed;
        }
    }

    private boolean isMember(String key, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            Boolean member = redisTemplate.opsForSet().isMember(key, userId);
            return Boolean.TRUE.equals(member) || memorySet(key).contains(userId);
        } catch (Exception e) {
            return memorySet(key).contains(userId);
        }
    }

    private void increment(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            memoryCount(key).incrementAndGet();
        }
    }

    private void decrement(String key) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key);
            if (value != null && value < 0) {
                redisTemplate.opsForValue().set(key, "0");
            }
        } catch (Exception e) {
            AtomicLong count = memoryCount(key);
            count.updateAndGet(current -> Math.max(0L, current - 1));
        }
    }

    private Set<String> memorySet(String key) {
        return memorySets.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet());
    }

    private AtomicLong memoryCount(String key) {
        return memoryCounts.computeIfAbsent(key, ignored -> new AtomicLong());
    }

    public String currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId != null ? String.valueOf(userId) : "anonymous";
    }
}
