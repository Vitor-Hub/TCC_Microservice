package com.mstcc.likesms.controllers;

import com.mstcc.likesms.entities.Like;
import com.mstcc.likesms.services.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Like operations.
 *
 * <p>Applying SRP: this controller is responsible only for routing and delegating to
 * {@link LikeService}. All exception formatting is handled by
 * {@code GlobalExceptionHandler} — no try-catch blocks here.
 *
 * <p>Applying DIP: the controller depends on the {@link LikeService} abstraction,
 * not on the concrete {@code LikeServiceImpl}.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private static final Logger logger = LoggerFactory.getLogger(LikeController.class);

    private final LikeService likeService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param likeService the like business logic abstraction
     */
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * Retrieves all likes.
     *
     * @return 200 with list of all likes
     */
    @GetMapping
    public ResponseEntity<List<Like>> getAllLikes() {
        logger.info("GET /api/likes - Fetching all likes");
        return ResponseEntity.ok(likeService.findAllLikes());
    }

    /**
     * Retrieves a like by ID.
     *
     * @param id like ID
     * @return 200 with the like, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Like> getLikeById(@PathVariable Long id) {
        logger.info("GET /api/likes/{} - Fetching like by id", id);
        return likeService.getLikeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new like.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param like like data
     * @return 201 with the created like
     */
    @PostMapping
    public ResponseEntity<Like> createLike(@RequestBody Like like) {
        logger.info("POST /api/likes - Creating new like for userId: {}", like.getUserId());
        Like savedLike = likeService.createAndValidateLike(like);
        logger.info("Like created: id={}", savedLike.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLike);
    }

    /**
     * Updates an existing like.
     * Exceptions are propagated to {@code GlobalExceptionHandler}.
     *
     * @param id          like ID
     * @param likeDetails updated like data
     * @return 200 with the updated like, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Like> updateLike(@PathVariable Long id, @RequestBody Like likeDetails) {
        logger.info("PUT /api/likes/{} - Updating like", id);
        return likeService.updateAndValidateLike(id, likeDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a like.
     *
     * @param id like ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id) {
        logger.info("DELETE /api/likes/{} - Deleting like", id);
        likeService.deleteLike(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all likes for a given post.
     *
     * @param postId post ID
     * @return 200 with list of likes for the post
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Like>> getLikesByPostId(@PathVariable Long postId) {
        logger.info("GET /api/likes/post/{} - Fetching likes by post", postId);
        return ResponseEntity.ok(likeService.findLikesByPostId(postId));
    }

    /**
     * Retrieves all likes made by a given user.
     *
     * @param userId user ID
     * @return 200 with list of likes by the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Like>> getLikesByUserId(@PathVariable Long userId) {
        logger.info("GET /api/likes/user/{} - Fetching likes by user", userId);
        return ResponseEntity.ok(likeService.findLikesByUserId(userId));
    }

    /**
     * Retrieves all likes for a given comment.
     *
     * @param commentId comment ID
     * @return 200 with list of likes for the comment
     */
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<List<Like>> getLikesByCommentId(@PathVariable Long commentId) {
        logger.info("GET /api/likes/comment/{} - Fetching likes by comment", commentId);
        return ResponseEntity.ok(likeService.findLikesByCommentId(commentId));
    }
}
