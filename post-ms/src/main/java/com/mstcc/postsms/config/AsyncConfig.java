package com.mstcc.postsms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution
 * Configures thread pool for handling parallel service calls
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    private static final int CORE_POOL_SIZE = 20;
    private static final int MAX_POOL_SIZE = 100;
    private static final int QUEUE_CAPACITY = 500;
    private static final int AWAIT_TERMINATION_SECONDS = 60;

    public AsyncConfig() {
        logger.info("========================================");
        logger.info(" AsyncConfig initialized");
        logger.info("========================================");
    }

    /**
     * Creates and configures the main task executor for async operations
     * @return configured thread pool executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        logger.info("========================================");
        logger.info("🔧 Creating taskExecutor bean...");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Pool sizing
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        
        // Thread naming
        executor.setThreadNamePrefix("PostAsync-");
        
        // Shutdown behavior
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        
        // Rejection policy
        executor.setRejectedExecutionHandler(new CustomRejectionHandler());
        
        executor.initialize();
        
        logger.info("TaskExecutor configured:");
        logger.info("   - Core pool size: {}", CORE_POOL_SIZE);
        logger.info("   - Max pool size: {}", MAX_POOL_SIZE);
        logger.info("   - Queue capacity: {}", QUEUE_CAPACITY);
        logger.info("   - Thread prefix: PostAsync-");
        logger.info("   - Await termination: {}s", AWAIT_TERMINATION_SECONDS);
        logger.info("========================================");
        
        return executor;
    }

    /**
     * Rejection handler that aborts immediately when the pool and queue are both full.
     *
     * <p>CallerRunsPolicy was the previous strategy, but it is unsafe under spike load:
     * it runs Feign calls synchronously on the Tomcat request thread, and combined with
     * Resilience4j retry (up to 3 attempts × 4 s readTimeout) it can block the caller
     * thread for up to ~13 s per upstream call — and updatePostContentByUser made two
     * sequential calls, causing P95 latencies of 2+ minutes.
     *
     * <p>AbortPolicy throws {@link java.util.concurrent.RejectedExecutionException}, which
     * Spring's {@code @Async} proxy catches and propagates as an exceptionally-completed
     * {@link java.util.concurrent.CompletableFuture}. PostServiceImpl's catch blocks then
     * return partial data (null user / empty comments) immediately, keeping P95 bounded.
     */
    private static class CustomRejectionHandler implements RejectedExecutionHandler {
        private static final Logger log = LoggerFactory.getLogger(CustomRejectionHandler.class);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Async task rejected — executor at capacity. Active: {}, Pool: {}, Queue: {}. "
                    + "Returning partial enrichment data instead of blocking caller thread.",
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size());
            throw new java.util.concurrent.RejectedExecutionException(
                    "PostAsync pool saturated — failing fast to protect Tomcat threads");
        }
    }
}