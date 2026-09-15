package com.quanzhou.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 聊天模块专用线程池 — 用于自动回复等异步任务
 *
 * ✅ 修复7：使用 Spring 内置 ThreadPoolTaskExecutor 替代 new Thread() 手动创建线程
 */
@Configuration
public class ChatThreadPoolConfig {

    @Bean(name = "chatTaskExecutor")
    public ThreadPoolTaskExecutor chatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                // 核心线程数
        executor.setMaxPoolSize(8);                 // 最大线程数
        executor.setQueueCapacity(50);              // 缓冲队列
        executor.setKeepAliveSeconds(60);           // 空闲线程存活时间
        executor.setThreadNamePrefix("chat-auto-reply-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略：调用者线程执行
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
