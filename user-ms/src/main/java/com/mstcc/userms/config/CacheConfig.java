package com.mstcc.userms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for User microservice using Caffeine
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
            "users",              // Cache for getUserById()
            "usersByUsername",    // Cache for findByUsername()
            "userExists"          // Cache for existsById()
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)                    // Max 10k entries
            .expireAfterWrite(30, TimeUnit.MINUTES) // TTL 30 minutes
            .expireAfterAccess(15, TimeUnit.MINUTES) // Evict if not accessed for 15 min
            .recordStats());                        // Enable statistics for monitoring
        
        return cacheManager;
    }
}