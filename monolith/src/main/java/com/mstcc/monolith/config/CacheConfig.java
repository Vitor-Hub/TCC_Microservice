package com.mstcc.monolith.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration for the monolith.
 *
 * <p>Each domain's cache regions are registered with exactly the same size
 * limit and TTLs as the corresponding microservice's {@code CacheConfig}
 * (user-ms, post-ms, comment-ms, like-ms, friendship-ms). Using identical
 * per-domain settings eliminates cache policy as a variable when comparing
 * latency and throughput between architectures.
 *
 * <p>All domain cache regions live in a single {@link CaffeineCacheManager}
 * because, in a monolith, all domains share the same JVM heap — there is no
 * benefit in maintaining separate cache managers.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Returns a {@link CacheManager} with per-domain cache regions whose
     * size limits and TTLs mirror each microservice's configuration.
     *
     * @return configured cache manager covering all domain cache names
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // User domain — mirrors user-ms: 10k entries, 30m write TTL, 15m access TTL
        registerCaches(cacheManager, 10_000, 30, 15,
            "users", "usersByUsername", "userExists", "allUsers");

        // Post domain — mirrors post-ms: 5k entries, 15m write TTL, 10m access TTL
        registerCaches(cacheManager, 5_000, 15, 10,
            "posts", "allPosts", "userPosts");

        // Comment domain — mirrors comment-ms: 10k entries, 15m write TTL, 10m access TTL
        registerCaches(cacheManager, 10_000, 15, 10,
            "comments", "postComments", "userComments");

        // Like domain — mirrors like-ms: 5k entries, 15m write TTL, 10m access TTL
        registerCaches(cacheManager, 5_000, 15, 10,
            "likes", "postLikes", "userLikes");

        // Friendship domain — mirrors friendship-ms: 5k entries, 20m write TTL, 10m access TTL
        registerCaches(cacheManager, 5_000, 20, 10,
            "friendships", "userFriendships");

        return cacheManager;
    }

    /**
     * Registers a group of cache regions sharing the same Caffeine policy.
     *
     * @param cacheManager        target cache manager
     * @param maximumSize         maximum number of entries per cache region
     * @param writeTtlMinutes     expire-after-write TTL in minutes
     * @param accessTtlMinutes    expire-after-access TTL in minutes
     * @param cacheNames          cache region names to register
     */
    private void registerCaches(CaffeineCacheManager cacheManager,
                                long maximumSize,
                                long writeTtlMinutes,
                                long accessTtlMinutes,
                                String... cacheNames) {
        for (String cacheName : cacheNames) {
            cacheManager.registerCustomCache(cacheName, Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(writeTtlMinutes, TimeUnit.MINUTES)
                .expireAfterAccess(accessTtlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build());
        }
    }
}
