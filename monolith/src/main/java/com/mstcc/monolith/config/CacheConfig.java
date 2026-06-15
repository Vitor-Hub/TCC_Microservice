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
 * <p>The TTL, size limit, and cache names mirror the per-service configurations
 * in the microservices implementation. Using identical settings eliminates cache
 * policy as a variable when comparing latency and throughput between architectures.
 *
 * <p>All five domain cache namespaces are registered in a single
 * {@link CaffeineCacheManager} because, in a monolith, all domains share the
 * same JVM heap — there is no benefit in maintaining separate cache managers.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Returns a {@link CacheManager} backed by Caffeine with a 30-minute write TTL,
     * 15-minute access TTL, and a maximum of 10 000 entries per cache region.
     *
     * @return configured cache manager covering all domain cache names
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            // User domain
            "users", "usersByUsername", "userExists", "allUsers",
            // Post domain
            "posts", "allPosts", "userPosts",
            // Comment domain
            "comments", "postComments", "userComments",
            // Like domain
            "likes", "postLikes", "userLikes",
            // Friendship domain
            "friendships", "userFriendships"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .recordStats());

        return cacheManager;
    }
}
