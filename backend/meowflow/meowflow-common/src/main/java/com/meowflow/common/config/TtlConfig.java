package com.meowflow.common.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TTL (TransmittableThreadLocal) 上下文透传配置
 * <p>
 * Alibaba TransmittableThreadLocal 是阿里巴巴开源的库，用于在异步执行时
 * 传递 ThreadLocal 值到子线程，解决父子线程上下文传递问题。
 *
 * <p>配置说明：
 * <ul>
 *   <li>自动为所有线程池 Bean 包装 TTL 版本</li>
 *   <li>确保 UserContext、TraceContext 等能够在异步线程中正确访问</li>
 *   <li>与 ThreadPoolConfig 配合使用，所有线程池已自动 TTL 包装</li>
 * </ul>
 *
 * @see ThreadPoolConfig
 */
@Slf4j
@Configuration
public class TtlConfig {

    /**
     * TTL Bean 后处理器
     * <p>
     * 自动为所有 Executor 类型的 Bean 包装 TTL 版本，
     * 确保在提交到线程池的任务中能够访问父线程的 TransmittableThreadLocal 值。
     */
    @Bean
    public static BeanPostProcessor ttlBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof Executor) {
                    // 检查是否已经是 TTL 包装的线程池
                    String className = bean.getClass().getName();
                    if (className.contains("com.alibaba.ttl")) {
                        log.debug("Bean '{}' 已是 TTL 包装的线程池，跳过处理", beanName);
                        return bean;
                    }

                    // 如果是 ThreadPoolExecutor，包装为 TTL 版本
                    if (bean instanceof ThreadPoolExecutor) {
                        Executor ttlExecutor = TtlExecutors.getTtlExecutor((Executor) bean);
                        log.info("Bean '{}' 已自动 TTL 包装", beanName);
                        return ttlExecutor;
                    }
                }
                return bean;
            }
        };
    }
}
