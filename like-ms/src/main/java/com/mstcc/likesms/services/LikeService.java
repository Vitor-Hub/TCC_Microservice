package com.mstcc.likesms.services;

import com.mstcc.likesms.dto.CommentDTO;
import com.mstcc.likesms.dto.PostDTO;
import com.mstcc.likesms.dto.UserDTO;
import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.repositories.LikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for managing Like entities with async validation
 * and parallel processing capabilities
 */
@Service
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final LikeRepository likeRepository;
    private final LikeAsyncHelper asyncHelper;

    public LikeService(LikeRepository likeRepository,
                       LikeAsyncHelper asyncHelper) {
        this.likeRepository = likeRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * Creates a like with parallel validation of user, post, and optional comment
     * @param like the like entity to create
     * @return the saved like entity
     * @throws RuntimeException if validation fails
     */
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Like createAndValidateLike(Like like) {
        long startTime = System.currentTimeMillis();
        logger.info("Creating like - userId: {}, postId: {}, commentId: {}", 
                   like.getUserId(), like.getPostId(), like.getCommentId());
        
        try {
            // Parallel validation of user and post (mandatory)
            CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(like.getUserId());
            CompletableFuture<PostDTO> postFuture = asyncHelper.getPostAsync(like.getPostId());
            
            // Wait for mandatory validations with timeout
            CompletableFuture.allOf(userFuture, postFuture)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Optional comment validation (non-blocking failure)
            if (like.getCommentId() != null) {
                asyncHelper.getCommentAsync(like.getCommentId())
                    .exceptionally(ex -> {
                        logger.warn("Comment validation failed for commentId: {}, proceeding anyway", 
                                   like.getCommentId());
                        return null;
                    })
                    .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            
            Like savedLike = likeRepository.save(like);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info(" Like created successfully in {}ms - likeId: {}", duration, savedLike.getId());
            
            return savedLike;
            
        } catch (TimeoutException e) {
            logger.error("  Validation timeout after {}s", VALIDATION_TIMEOUT_SECONDS);
            throw new RuntimeException("Validation timeout: external services took too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("  Validation interrupted");
            throw new RuntimeException("Validation interrupted", e);
        } catch (ExecutionException e) {
            logger.error("  Validation failed: {}", e.getCause().getMessage());
            throw new RuntimeException("Validation failed: " + e.getCause().getMessage(), e);
        }
    }

    /**
     * Updates a like with parallel validation
     */
    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public Optional<Like> updateAndValidateLike(Long id, Like likeDetails) {
        logger.info("Updating like - id: {}", id);
        
        return likeRepository.findById(id).map(like -> {
            long startTime = System.currentTimeMillis();
            
            try {
                CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(likeDetails.getUserId());
                CompletableFuture<PostDTO> postFuture = asyncHelper.getPostAsync(likeDetails.getPostId());

                CompletableFuture.allOf(userFuture, postFuture)
                    .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                like.setUserId(likeDetails.getUserId());
                like.setPostId(likeDetails.getPostId());
                like.setCommentId(likeDetails.getCommentId());
                
                Like updated = likeRepository.save(like);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info(" Like updated successfully in {}ms", duration);
                
                return updated;
                
            } catch (Exception e) {
                logger.error("  Failed to update like: {}", e.getMessage());
                throw new RuntimeException("Failed to update like: " + e.getMessage(), e);
            }
        });
    }

    @Cacheable(value = "likes", key = "#id")
    public Optional<Like> getLikeById(Long id) {
        return likeRepository.findById(id);
    }

    @Cacheable(value = "postLikes", key = "#postId")
    public List<Like> findLikesByPostId(Long postId) {
        return likeRepository.findByPostId(postId);
    }

    @Cacheable(value = "userLikes", key = "#userId")
    public List<Like> findLikesByUserId(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    public List<Like> findLikesByCommentId(Long commentId) {
        return likeRepository.findByCommentId(commentId);
    }

    @CacheEvict(value = {"likes", "postLikes", "userLikes"}, allEntries = true)
    public void deleteLike(Long id) {
        logger.info("Deleting like - id: {}", id);
        likeRepository.deleteById(id);
    }

    public List<Like> findAllLikes() {
        return likeRepository.findAll();
    }
}