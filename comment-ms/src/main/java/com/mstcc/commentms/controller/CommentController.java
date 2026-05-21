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
 * REST controller for Comment operations.
 *
 * <p>Applying SRP: this controller is responsible only for routing and delegating to
 * {@link CommentService}. All exception formatting is handled by
 * {@code GlobalExceptionHandler} — no try-catch blocks here.
 *
 * <p>Applying DIP: the controller depends on the {@link CommentService} abstraction,
 * not on the concrete {@code CommentServiceImpl}.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param commentService the comment business logic abstraction
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
        logger.info("GET /api/comments - Fetching all comments");
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
        logger.info("GET /api/comments/{} - Fetching comment by id", id);
        return commentService.getCommentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all comments for a given post.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param postId post ID
     * @return 200 with list of comments (may be empty)
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        logger.info("GET /api/comments/post/{} - Fetching comments by post", postId);
        return ResponseEntity.ok(commentService.findCommentsByPostId(postId));
    }

    /**
     * Retrieves all comments by a given user.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param userId user ID
     * @return 200 with list of comments (may be empty)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Comment>> getCommentsByUserId(@PathVariable Long userId) {
        logger.info("GET /api/comments/user/{} - Fetching comments by user", userId);
        return ResponseEntity.ok(commentService.findCommentsByUserId(userId));
    }

    /**
     * Creates a new comment.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param comment comment data
     * @return 201 with the created comment
     */
    @PostMapping
    public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
        logger.info("POST /api/comments - Creating new comment for userId: {}, postId: {}",
                comment.getUserId(), comment.getPostId());
        Comment savedComment = commentService.createAndValidateComment(comment);
        logger.info("Comment created: id={}", savedComment.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    /**
     * Updates an existing comment.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param id             comment ID
     * @param commentDetails updated comment data
     * @return 200 with the updated comment, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long id,
            @RequestBody Comment commentDetails) {
        logger.info("PUT /api/comments/{} - Updating comment", id);
        return commentService.updateAndValidateComment(id, commentDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a comment.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param id comment ID
     * @return 204 No Content, or 400 if not found (via GlobalExceptionHandler)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        logger.info("DELETE /api/comments/{} - Deleting comment", id);
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
