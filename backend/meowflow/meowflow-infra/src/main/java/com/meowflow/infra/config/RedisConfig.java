package com.meowflow.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.infra.cache.RedisCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Infra 模块 Redis 配置
 *
 * <p>使用 {@link ConditionalOnMissingBean} 确保不与 common 模块的同名 Bean 冲突，
 * 优先复用 common 的 Bean 定义。
 *
 * <p>{@code RedisCacheManager} 走 auto-config 注册，是为了不依赖各业务模块的 ComponentScan
 * 范围（template 模块只扫 {@code com.meowflow.template, com.meowflow.common}）。
 */
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
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
     * RedisCacheManager 作为基础设施 Bean 注册，对所有依赖 meowflow-infra 的模块可见。
     * <p>注意：这里把 {@code RedisCacheManager} 上的 {@code @Component} 也保留，
     * {@link ConditionalOnMissingBean} 保证只产生一个实例。
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public RedisCacheManager redisCacheManager(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisCacheManager(stringRedisTemplate, objectMapper);
    }
}