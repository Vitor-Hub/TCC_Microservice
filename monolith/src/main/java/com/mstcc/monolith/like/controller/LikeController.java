package com.mstcc.monolith.like.controller;

import com.mstcc.monolith.like.entity.Like;
import com.mstcc.monolith.like.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Like operations.
 *
 * <p>Path prefix {@code /like-ms/api/likes} mirrors the API Gateway route
 * so that K6 scripts need no changes when switching between stacks.
 */
@RestController
@RequestMapping("/like-ms/api/likes")
public class LikeController {

    private static final Logger logger = LoggerFactory.getLogger(LikeController.class);

    private final LikeService likeService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param likeService the like business logic service
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
        logger.info("GET /like-ms/api/likes");
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
        logger.info("GET /like-ms/api/likes/{}", id);
        return likeService.getLikeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new like.
     *
     * @param like like data
     * @return 201 with the created like
     */
    @PostMapping
    public ResponseEntity<Like> createLike(@RequestBody Like like) {
        logger.info("POST /like-ms/api/likes - userId: {}", like.getUserId());
        Like saved = likeService.createAndValidateLike(like);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates an existing like.
     *
     * @param id          like ID
     * @param likeDetails updated like data
     * @return 200 with the updated like, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Like> updateLike(@PathVariable Long id, @RequestBody Like likeDetails) {
        logger.info("PUT /like-ms/api/likes/{}", id);
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
        logger.info("DELETE /like-ms/api/likes/{}", id);
        likeService.deleteLike(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all likes for a given post.
     *
     * @param postId post ID
     * @return 200 with list of likes
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Like>> getLikesByPostId(@PathVariable Long postId) {
        logger.info("GET /like-ms/api/likes/post/{}", postId);
        return ResponseEntity.ok(likeService.findLikesByPostId(postId));
    }

    /**
     * Retrieves all likes made by a given user.
     *
     * @param userId user ID
     * @return 200 with list of likes
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Like>> getLikesByUserId(@PathVariable Long userId) {
        logger.info("GET /like-ms/api/likes/user/{}", userId);
        return ResponseEntity.ok(likeService.findLikesByUserId(userId));
    }

    /**
     * Retrieves all likes for a given comment.
     *
     * @param commentId comment ID
     * @return 200 with list of likes
     */
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<List<Like>> getLikesByCommentId(@PathVariable Long commentId) {
        logger.info("GET /like-ms/api/likes/comment/{}", commentId);
        return ResponseEntity.ok(likeService.findLikesByCommentId(commentId));
    }
}
