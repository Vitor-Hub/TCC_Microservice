package com.mstcc.postsms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Post microservice using Caffeine
 * Provides high-performance caching with TTL and size limits
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures Caffeine cache manager with optimized settings
     * @return configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "posts",      // Cache for getPostById()
            "allPosts",   // Cache for getAllPosts()
            "userPosts"   // Cache for getPostsByUser()
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5000)                     // Max 5k entries (posts are larger than users)
            .expireAfterWrite(15, TimeUnit.MINUTES) // TTL 15 minutes
            .expireAfterAccess(10, TimeUnit.MINUTES) // Evict if not accessed for 10 min
            .recordStats());                        // Enable statistics for monitoring
        
        return cacheManager;
    }
}