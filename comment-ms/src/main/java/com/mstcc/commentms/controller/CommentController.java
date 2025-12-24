package com.mstcc.commentms.controller;

import com.mstcc.commentms.entities.Comment;
import com.mstcc.commentms.services.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Comment operations
 * Provides CRUD endpoints for comment management
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Retrieves all comments
     * @return list of all comments
     */
    @GetMapping
    public ResponseEntity<List<Comment>> getAllComments() {
        logger.info("GET /api/comments - Fetching all comments");
        List<Comment> comments = commentService.findAllComments();
        logger.info("Returning {} comments", comments.size());
        return ResponseEntity.ok(comments);
    }

    /**
     * Retrieves a comment by ID
     * @param id comment ID
     * @return comment if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<Comment> getCommentById(@PathVariable Long id) {
        logger.info("GET /api/comments/{} - Fetching comment by id", id);
        return commentService.getCommentById(id)
                .map(comment -> {
                    logger.info("Comment found: id={}", id);
                    return ResponseEntity.ok(comment);
                })
                .orElseGet(() -> {
                    logger.warn("Comment not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Retrieves all comments for a post
     * @param postId post ID
     * @return list of comments for the post
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getCommentsByPostId(@PathVariable Long postId) {
        logger.info("GET /api/comments/post/{} - Fetching comments by post", postId);
        
        try {
            List<Comment> comments = commentService.findCommentsByPostId(postId);
            logger.info("Returning {} comments for postId: {}", comments.size(), postId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            logger.error("Error fetching comments for postId: {}", postId);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Retrieves all comments by a user
     * @param userId user ID
     * @return list of user's comments
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getCommentsByUserId(@PathVariable Long userId) {
        logger.info("GET /api/comments/user/{} - Fetching comments by user", userId);
        
        try {
            List<Comment> comments = commentService.findCommentsByUserId(userId);
            logger.info("Returning {} comments for userId: {}", comments.size(), userId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            logger.error("Error fetching comments for userId: {}", userId);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Creates a new comment
     * @param comment comment data
     * @return created comment with 201 status
     */
    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody Comment comment) {
        logger.info("POST /api/comments - Creating new comment");
        
        try {
            Comment savedComment = commentService.createAndValidateComment(comment);
            logger.info("Comment created: id={}", savedComment.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error creating comment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates an existing comment
     * @param id comment ID
     * @param commentDetails updated comment data
     * @return updated comment or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long id, 
            @RequestBody Comment commentDetails) {
        logger.info("PUT /api/comments/{} - Updating comment", id);
        
        try {
            return commentService.updateAndValidateComment(id, commentDetails)
                    .map(comment -> {
                        logger.info("Comment updated: id={}", id);
                        return ResponseEntity.ok(comment);
                    })
                    .orElseGet(() -> {
                        logger.warn("Comment not found for update: id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (RuntimeException e) {
            logger.error("Error updating comment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a comment
     * @param id comment ID
     * @return 204 if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        logger.info("DELETE /api/comments/{} - Deleting comment", id);
        
        try {
            commentService.deleteComment(id);
            logger.info("Comment deleted: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("{}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}