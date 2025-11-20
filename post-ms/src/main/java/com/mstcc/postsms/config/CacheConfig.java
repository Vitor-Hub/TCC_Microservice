package com.mstcc.postsms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine
 * Optimizes read operations for posts
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with TTL and size limits
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("posts");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache builder configuration
     * - Maximum 1000 entries per cache
     * - TTL of 5 minutes
     * - Automatic cleanup of expired entries
     */
    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }
}