package com.mstcc.commentms.services;

import com.mstcc.commentms.dto.PostDTO;
import com.mstcc.commentms.dto.UserDTO;
import com.mstcc.commentms.entities.Comment;
import com.mstcc.commentms.repositories.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for managing Comment entities with async validation
 * and parallel processing capabilities
 */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final CommentRepository commentRepository;
    private final CommentAsyncHelper asyncHelper;

    public CommentService(CommentRepository commentRepository,
                          CommentAsyncHelper asyncHelper) {
        this.commentRepository = commentRepository;
        this.asyncHelper = asyncHelper;
    }

    /**
     * Creates a comment with parallel validation of user and post
     * @param comment the comment entity to create
     * @return the saved comment entity
     * @throws RuntimeException if validation fails
     */
    @Transactional
    @CacheEvict(value = {"comments", "postComments", "userComments"}, allEntries = true)
    public Comment createAndValidateComment(Comment comment) {
        long startTime = System.currentTimeMillis();
        logger.info("Creating comment - userId: {}, postId: {}", 
                   comment.getUserId(), comment.getPostId());
        
        try {
            // Parallel validation of user and post
            CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(comment.getUserId());
            CompletableFuture<PostDTO> postFuture = asyncHelper.getPostAsync(comment.getPostId());
            
            // Wait for both validations with timeout
            CompletableFuture.allOf(userFuture, postFuture)
                .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            comment.setCreatedAt(LocalDateTime.now());
            Comment savedComment = commentRepository.save(comment);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Comment created successfully in {}ms - commentId: {}", 
                       duration, savedComment.getId());
            
            return savedComment;
            
        } catch (TimeoutException e) {
            logger.error("Validation timeout after {}s", VALIDATION_TIMEOUT_SECONDS);
            throw new RuntimeException("Validation timeout: external services took too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Validation interrupted");
            throw new RuntimeException("Validation interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Validation failed: {}", e.getCause().getMessage());
            throw new RuntimeException("Validation failed: " + e.getCause().getMessage(), e);
        }
    }

    /**
     * Retrieves a comment by ID
     * @param id comment ID
     * @return optional containing comment if found
     */
    @Cacheable(value = "comments", key = "#id")
    public Optional<Comment> getCommentById(Long id) {
        logger.info("Fetching comment by id: {}", id);
        return commentRepository.findById(id);
    }

    /**
     * Retrieves all comments for a post
     * @param postId post ID
     * @return list of comments for the post
     */
    @Cacheable(value = "postComments", key = "#postId")
    public List<Comment> findCommentsByPostId(Long postId) {
        logger.info("Fetching comments for postId: {}", postId);
        long startTime = System.currentTimeMillis();
        
        List<Comment> comments = commentRepository.findByPostId(postId);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Comments fetched in {}ms - postId: {}, count: {}", 
                   duration, postId, comments.size());
        
        return comments;
    }

    /**
     * Retrieves all comments by a user
     * @param userId user ID
     * @return list of user's comments
     */
    @Cacheable(value = "userComments", key = "#userId")
    public List<Comment> findCommentsByUserId(Long userId) {
        logger.info("Fetching comments for userId: {}", userId);
        long startTime = System.currentTimeMillis();
        
        List<Comment> comments = commentRepository.findByUserId(userId);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Comments fetched in {}ms - userId: {}, count: {}", 
                   duration, userId, comments.size());
        
        return comments;
    }

    /**
     * Updates a comment with parallel validation
     * @param id comment ID
     * @param commentDetails updated comment details
     * @return optional containing updated comment
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "comments", key = "#id"),
        @CacheEvict(value = "postComments", allEntries = true),
        @CacheEvict(value = "userComments", allEntries = true)
    })
    public Optional<Comment> updateAndValidateComment(Long id, Comment commentDetails) {
        logger.info("Updating comment - id: {}", id);
        
        return commentRepository.findById(id).map(comment -> {
            long startTime = System.currentTimeMillis();
            
            try {
                CompletableFuture<UserDTO> userFuture = asyncHelper.getUserAsync(commentDetails.getUserId());
                CompletableFuture<PostDTO> postFuture = asyncHelper.getPostAsync(commentDetails.getPostId());

                CompletableFuture.allOf(userFuture, postFuture)
                    .get(VALIDATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                comment.setContent(commentDetails.getContent());
                comment.setUserId(commentDetails.getUserId());
                comment.setPostId(commentDetails.getPostId());
                
                Comment updated = commentRepository.save(comment);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Comment updated successfully in {}ms", duration);
                
                return updated;
                
            } catch (Exception e) {
                logger.error("Failed to update comment: {}", e.getMessage());
                throw new RuntimeException("Failed to update comment: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Deletes a comment
     * @param id comment ID
     */
    @Transactional
    @CacheEvict(value = {"comments", "postComments", "userComments"}, allEntries = true)
    public void deleteComment(Long id) {
        logger.info("Deleting comment - id: {}", id);
        
        if (!commentRepository.existsById(id)) {
            logger.warn("Comment not found for deletion - id: {}", id);
            throw new IllegalArgumentException("Comment not found: " + id);
        }
        
        commentRepository.deleteById(id);
        logger.info("Comment deleted successfully - id: {}", id);
    }

    /**
     * Retrieves all comments
     * @return list of all comments
     */
    public List<Comment> findAllComments() {
        logger.info("Fetching all comments");
        List<Comment> comments = commentRepository.findAll();
        logger.info("Returning {} comments", comments.size());
        return comments;
    }
}