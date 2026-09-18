package com.meowflow.common.idempotent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IdempotentStore Tests")
class IdempotentStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private IdempotentStore store;

    @BeforeEach
    void setUp() {
        store = new IdempotentStore(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("tryLock - 首次设置成功")
    void tryLock_firstTime_succeeds() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        boolean result = store.tryLock("key1", "value1", 60);

        assertThat(result).isTrue();
        verify(valueOps).setIfAbsent(eq("idempotent:key1"), eq("value1"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("tryLock - 重复设置失败")
    void tryLock_duplicate_fails() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        boolean result = store.tryLock("key1", "value1", 60);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryLock - 返回 null 时视为失败")
    void tryLock_nullResult_fails() {
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(null);

        boolean result = store.tryLock("key1", "value1", 60);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("unlock - 值匹配时删除")
    void unlock_matchingValue_deletes() {
        when(valueOps.get("idempotent:key1")).thenReturn("value1");

        store.unlock("key1", "value1");

        verify(redisTemplate).delete("idempotent:key1");
    }

    @Test
    @DisplayName("unlock - 值不匹配时不删除")
    void unlock_mismatchValue_doesNotDelete() {
        when(valueOps.get("idempotent:key1")).thenReturn("different-value");

        store.unlock("key1", "value1");

        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("exists - key 存在返回 true")
    void exists_keyExists_returnsTrue() {
        when(redisTemplate.hasKey("idempotent:key1")).thenReturn(true);

        assertThat(store.exists("key1")).isTrue();
    }

    @Test
    @DisplayName("exists - key 不存在返回 false")
    void exists_keyMissing_returnsFalse() {
        when(redisTemplate.hasKey("idempotent:key1")).thenReturn(false);

        assertThat(store.exists("key1")).isFalse();
    }

    @Test
    @DisplayName("get - 返回存储的值")
    void get_returnsValue() {
        when(valueOps.get("idempotent:key1")).thenReturn("stored-value");

        assertThat(store.get("key1")).isEqualTo("stored-value");
    }

    @Test
    @DisplayName("set - 设置带过期时间")
    void set_setsValueWithExpiry() {
        store.set("key1", "value1", 120);

        verify(valueOps).set(eq("idempotent:key1"), eq("value1"), eq(120L), eq(TimeUnit.SECONDS));
    }
}