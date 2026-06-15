package com.mstcc.monolith.comment.controller;

import com.mstcc.monolith.comment.entity.Comment;
import com.mstcc.monolith.comment.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Comment operations.
 *
 * <p>Path prefix {@code /comment-ms/api/comments} mirrors the API Gateway route
 * so that K6 scripts need no changes when switching between stacks.
 */
@RestController
@RequestMapping("/comment-ms/api/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param commentService the comment business logic service
     */
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Retrieves all comments.
     *
     * @return 200 with list of all comments
     */
    @GetMapping
    public ResponseEntity<List<Comment>> getAllComments() {
        logger.info("GET /comment-ms/api/comments");
        return ResponseEntity.ok(commentService.findAllComments());
    }

    /**
     * Retrieves a comment by ID.
     *
     * @param id comment ID
     * @return 200 with the comment, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Comment> getCommentById(@PathVariable Long id) {
        logger.info("GET /comment-ms/api/comments/{}", id);
        return commentService.getCommentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all comments for a given post.
     *
     * @param postId post ID
     * @return 200 with list of comments (may be empty)
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        logger.info("GET /comment-ms/api/comments/post/{}", postId);
        return ResponseEntity.ok(commentService.findCommentsByPostId(postId));
    }

    /**
     * Retrieves all comments by a given user.
     *
     * @param userId user ID
     * @return 200 with list of comments (may be empty)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Comment>> getCommentsByUserId(@PathVariable Long userId) {
        logger.info("GET /comment-ms/api/comments/user/{}", userId);
        return ResponseEntity.ok(commentService.findCommentsByUserId(userId));
    }

    /**
     * Creates a new comment.
     *
     * @param comment comment data
     * @return 201 with the created comment
     */
    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
        logger.info("POST /comment-ms/api/comments - userId: {}, postId: {}",
                comment.getUserId(), comment.getPostId());
        Comment saved = commentService.createAndValidateComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates an existing comment.
     *
     * @param id             comment ID
     * @param commentDetails updated comment data
     * @return 200 with updated comment, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long id,
            @RequestBody Comment commentDetails) {
        logger.info("PUT /comment-ms/api/comments/{}", id);
        return commentService.updateAndValidateComment(id, commentDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a comment.
     *
     * @param id comment ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        logger.info("DELETE /comment-ms/api/comments/{}", id);
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
