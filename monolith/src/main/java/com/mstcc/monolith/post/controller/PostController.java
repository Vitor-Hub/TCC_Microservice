package com.mstcc.monolith.post.controller;

import com.mstcc.monolith.exception.ErrorResponse;
import com.mstcc.monolith.post.dto.PostDTO;
import com.mstcc.monolith.post.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Post operations.
 *
 * <p>Path prefix {@code /post-ms/api/posts} mirrors the API Gateway route
 * so that K6 scripts need no changes when switching from the microservices
 * stack to the monolith.
 */
@RestController
@RequestMapping("/post-ms/api/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param postService the post business logic service
     */
    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Retrieves recent posts up to the given limit.
     *
     * @param limit maximum number of posts to return (defaults to 20, capped at 100)
     * @return list of recent posts or 400 if limit is invalid
     */
    @GetMapping
    public ResponseEntity<?> getAllPosts(@RequestParam(defaultValue = "20") int limit) {
        if (limit <= 0) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(400, "Bad Request", "limit must be greater than 0"));
        }
        List<PostDTO> posts = postService.getRecentPosts(Math.min(limit, 100));
        return ResponseEntity.ok(posts);
    }

    /**
     * Retrieves a post by ID with enrichment.
     *
     * @param id post ID
     * @return 200 with post DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        logger.info("GET /post-ms/api/posts/{}", id);
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all posts by a user.
     *
     * @param userId user ID
     * @return list of user's posts
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable Long userId) {
        logger.info("GET /post-ms/api/posts/user/{}", userId);
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    /**
     * Creates a new post.
     *
     * @param postDto post data
     * @return 201 with created post
     */
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDto) {
        logger.info("POST /post-ms/api/posts");
        PostDTO saved = postService.createPost(postDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Updates post content for a specific user.
     *
     * @param userId  user ID (ownership check)
     * @param postId  post ID
     * @param postDto updated post data
     * @return 200 with updated post, or 404 if not found or not owner
     */
    @PutMapping("/user/{userId}/posts/{postId}")
    public ResponseEntity<PostDTO> updatePostContent(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestBody PostDTO postDto) {
        logger.info("PUT /post-ms/api/posts/user/{}/posts/{}", userId, postId);
        return postService.updatePostContentByUser(userId, postId, postDto.getContent())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Deletes a post.
     *
     * @param id post ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        logger.info("DELETE /post-ms/api/posts/{}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lightweight existence check for cross-domain validation.
     *
     * @param id post ID
     * @return 200 with boolean
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> postExists(@PathVariable Long id) {
        return ResponseEntity.ok(postService.existsById(id));
    }
}
