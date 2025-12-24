package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.feignclients.UserFeignClient;
import com.mstcc.friendshipms.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Helper component for async operations with external services
 * Provides non-blocking calls to User service
 */
@Component
public class FriendshipAsyncHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendshipAsyncHelper.class);
    
    private final UserFeignClient userFeignClient;

    public FriendshipAsyncHelper(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    /**
     * Fetches user data asynchronously
     * @param userId the user ID
     * @return CompletableFuture containing user data
     */
    @Async("taskExecutor")
    public CompletableFuture<UserDTO> getUserAsync(Long userId) {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        logger.info("[{}] START - Fetching user async: userId={}", threadName, userId);
        
        try {
            ResponseEntity<UserDTO> response = userFeignClient.getUserById(userId);
            UserDTO user = response.getBody();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] SUCCESS - User fetched in {}ms: userId={}", 
                       threadName, duration, userId);
            
            return CompletableFuture.completedFuture(user);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}] FAILED - User fetch failed in {}ms: userId={}, error={}", 
                        threadName, duration, userId, e.getMessage());
            
            CompletableFuture<UserDTO> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                new RuntimeException("User not found: " + userId, e)
            );
            return failedFuture;
        }
    }
}