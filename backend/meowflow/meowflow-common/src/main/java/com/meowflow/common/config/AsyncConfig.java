package com.meowflow.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * <p>
 * 配置全局异步任务执行器，支持 TTL 上下文透传。
 * 所有通过 @Async 注解的异步方法都会使用此执行器。
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 默认异步执行器
     * 使用工作流执行线程池，保持与业务线程池的一致性
     */
    @Override
    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("AsyncConfig 初始化完成: coreSize=10, maxSize=20, queueCapacity=500");
        return executor;
    }

    /**
     * 异步方法未捕获异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncUncaughtExceptionHandler();
    }

    /**
     * 自定义异步异常处理器
     */
    public static class CustomAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("异步任务执行异常 - 方法: {}#{}, 参数: {}, 异常: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    params != null ? params.length : 0,
                    ex.getMessage(),
                    ex);
        }
    }
}
