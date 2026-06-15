package com.mstcc.friendshipms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Friendship microservice using Caffeine
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
            "friendships",      // Cache for getFriendshipById()
            "userFriendships"   // Cache for findFriendshipsByUserId()
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5000)                     // Max 5k entries
            .expireAfterWrite(20, TimeUnit.MINUTES) // TTL 20 minutes
            .expireAfterAccess(10, TimeUnit.MINUTES) // Evict if not accessed for 10 min
            .recordStats());                        // Enable statistics for monitoring
        
        return cacheManager;
    }
}