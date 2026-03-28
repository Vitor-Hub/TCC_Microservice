package com.mstcc.likesms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Like microservice using Caffeine
 * Provides high-performance caching with TTL and size limits
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "likes",      // Cache for getLikeById()
            "postLikes",  // Cache for findLikesByPostId()
            "userLikes"   // Cache for findLikesByUserId()
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats());

        return cacheManager;
    }
}
