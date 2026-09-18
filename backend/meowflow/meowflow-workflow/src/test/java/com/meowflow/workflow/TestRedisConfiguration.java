package com.meowflow.workflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test configuration for mocking Redis in unit tests
 */
@TestConfiguration
public class TestRedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Mock Redis template for isolated unit tests
     */
    public static class MockRedisTemplate extends RedisTemplate<String, Object> {

        private final Map<String, Object> store = new ConcurrentHashMap<>();
        private final Map<String, Map<Object, Object>> hashStore = new ConcurrentHashMap<>();

        @Override
        public void afterPropertiesSet() {
            // Skip actual Redis connection
        }

        public Map<String, Object> getStore() {
            return store;
        }

        public Map<String, Map<Object, Object>> getHashStore() {
            return hashStore;
        }
    }
}
