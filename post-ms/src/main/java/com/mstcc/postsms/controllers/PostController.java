package com.mstcc.postsms.controllers;

import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.services.PostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Post operations.
 * Provides CRUD endpoints for post management.
 * <p>
 * SRP: handles only HTTP routing and delegation — error formatting is centralised
 * in {@link com.mstcc.postsms.exception.GlobalExceptionHandler}.
 * DIP: depends on {@link PostService} interface, not on a concrete implementation.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Retrieves recent posts up to the given limit.
     * @param limit maximum number of posts to return (defaults to 20, capped at 100)
     * @return list of recent posts
     */
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts(
            @RequestParam(defaultValue = "20") int limit) {
        List<PostDTO> posts = postService.getRecentPosts(Math.min(limit, 100));
        return ResponseEntity.ok(posts);
    }

    /**
     * Retrieves a post by ID.
     * @param id post ID
     * @return post if found, 404 otherwise
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        logger.info("GET /api/posts/{} - Fetching post by id", id);
        return postService.getPostById(id)
                .map(post -> {
                    logger.info("Post found: id={}", id);
                    return ResponseEntity.ok(post);
                })
                .orElseGet(() -> {
                    logger.warn("Post not found: id={}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Retrieves all posts by a user.
     * @param userId user ID
     * @return list of user's posts
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable Long userId) {
        logger.info("GET /api/posts/user/{} - Fetching posts by user", userId);
        List<PostDTO> postDTOs = postService.getPostsByUser(userId);
        logger.info("Returning {} posts for userId: {}", postDTOs.size(), userId);
        return ResponseEntity.ok(postDTOs);
    }

    /**
     * Creates a new post.
     * @param postDto post data (validated)
     * @return created post with 201 status
     */
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostDTO postDto) {
        logger.info("POST /api/posts - Creating new post");
        PostDTO savedPostDto = postService.createPost(postDto);
        logger.info("Post created: id={}", savedPostDto.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPostDto);
    }

    /**
     * Updates post content.
     * @param userId user ID (for authorization)
     * @param postId post ID
     * @param postDto updated post data
     * @return updated post or 404 if not found or not owner
     */
    @PutMapping("/user/{userId}/posts/{postId}")
    public ResponseEntity<PostDTO> updatePostContent(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestBody PostDTO postDto) {
        logger.info("PUT /api/posts/user/{}/posts/{} - Updating post", userId, postId);
        return postService.updatePostContentByUser(userId, postId, postDto.getContent())
                .map(post -> {
                    logger.info("Post updated: id={}", postId);
                    return ResponseEntity.ok(post);
                })
                .orElseGet(() -> {
                    logger.warn("Post not found or unauthorized: postId={}, userId={}", postId, userId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Deletes a post.
     * @param id post ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        logger.info("DELETE /api/posts/{} - Deleting post", id);
        postService.deletePost(id);
        logger.info("Post deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks if a post exists by ID without triggering Feign calls.
     * Lightweight endpoint used by other microservices for existence validation.
     * @param id post ID
     * @return 200 with true if the post exists, false otherwise
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> postExists(@PathVariable Long id) {
        logger.info("GET /api/posts/{}/exists - Checking post existence", id);
        boolean exists = postService.existsById(id);
        return ResponseEntity.ok(exists);
    }
}
