package com.mstcc.commentms.services;

import com.mstcc.commentms.feignclients.PostFeignClient;
import com.mstcc.commentms.feignclients.UserFeignClient;
import com.mstcc.commentms.dto.PostDTO;
import com.mstcc.commentms.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Helper component for async operations with external services
 * Provides non-blocking calls to User and Post services
 */
@Component
public class CommentAsyncHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(CommentAsyncHelper.class);
    
    private final UserFeignClient userFeignClient;
    private final PostFeignClient postFeignClient;

    public CommentAsyncHelper(UserFeignClient userFeignClient,
                             PostFeignClient postFeignClient) {
        this.userFeignClient = userFeignClient;
        this.postFeignClient = postFeignClient;
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

    /**
     * Fetches post data asynchronously
     * @param postId the post ID
     * @return CompletableFuture containing post data
     */
    @Async("taskExecutor")
    public CompletableFuture<PostDTO> getPostAsync(Long postId) {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        logger.info("[{}] START - Fetching post async: postId={}", threadName, postId);
        
        try {
            ResponseEntity<PostDTO> response = postFeignClient.getPostById(postId);
            PostDTO post = response.getBody();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] SUCCESS - Post fetched in {}ms: postId={}", 
                       threadName, duration, postId);
            
            return CompletableFuture.completedFuture(post);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}] FAILED - Post fetch failed in {}ms: postId={}, error={}", 
                        threadName, duration, postId, e.getMessage());
            
            CompletableFuture<PostDTO> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                new RuntimeException("Post not found: " + postId, e)
            );
            return failedFuture;
        }
    }
}