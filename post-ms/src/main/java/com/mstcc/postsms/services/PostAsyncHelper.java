package com.mstcc.postsms.services;

import com.mstcc.postsms.feignclients.CommentFeignClient;
import com.mstcc.postsms.feignclients.UserFeignClient;
import com.mstcc.postsms.dto.CommentDTO;
import com.mstcc.postsms.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Helper component for async operations with external services
 * Provides non-blocking calls to User and Comment services
 */
@Component
public class PostAsyncHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(PostAsyncHelper.class);
    
    private final UserFeignClient userFeignClient;
    private final CommentFeignClient commentFeignClient;

    public PostAsyncHelper(UserFeignClient userFeignClient, 
                          CommentFeignClient commentFeignClient) {
        this.userFeignClient = userFeignClient;
        this.commentFeignClient = commentFeignClient;
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
     * Fetches comments for a post asynchronously
     * @param postId the post ID
     * @return CompletableFuture containing list of comments
     */
    @Async("taskExecutor")
    public CompletableFuture<List<CommentDTO>> getCommentsAsync(Long postId) {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        
        logger.info("[{}] START - Fetching comments async: postId={}", threadName, postId);
        
        try {
            ResponseEntity<List<CommentDTO>> response = commentFeignClient.getCommentsByPostId(postId);
            List<CommentDTO> comments = response.getBody();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] SUCCESS - Comments fetched in {}ms: postId={}, count={}", 
                       threadName, duration, postId, comments != null ? comments.size() : 0);
            
            return CompletableFuture.completedFuture(comments != null ? comments : List.of());
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn(" [{}] WARN - Comments fetch failed in {}ms: postId={}, returning empty list", 
                       threadName, duration, postId);
            
            // Return empty list instead of failing - comments are optional
            return CompletableFuture.completedFuture(List.of());
        }
    }
}