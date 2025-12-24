package com.mstcc.postsms.controllers;

import com.mstcc.postsms.dto.PostDTO;
import com.mstcc.postsms.services.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Post operations
 * Provides CRUD endpoints for post management
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
     * Retrieves all posts
     * @return list of all posts
     */
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts(
        @RequestParam(defaultValue = "20") int limit) {
        List<PostDTO> posts = postService.getRecentPosts(Math.min(limit, 100));
        return ResponseEntity.ok(posts);
    }

    /**
     * Retrieves a post by ID
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
     * Retrieves all posts by a user
     * @param userId user ID
     * @return list of user's posts
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUser(@PathVariable Long userId) {
        logger.info("GET /api/posts/user/{} - Fetching posts by user", userId);
        
        try {
            List<PostDTO> postDTOs = postService.getPostsByUser(userId);
            logger.info("Returning {} posts for userId: {}", postDTOs.size(), userId);
            return ResponseEntity.ok(postDTOs);
        } catch (RuntimeException e) {
            logger.error("Error fetching posts for userId: {}", userId);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Creates a new post
     * @param postDto post data
     * @return created post with 201 status
     */
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody PostDTO postDto) {
        logger.info("POST /api/posts - Creating new post");
        
        try {
            PostDTO savedPostDto = postService.createPost(postDto);
            logger.info("Post created: id={}", savedPostDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPostDto);
        } catch (RuntimeException e) {
            logger.error("Error creating post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates post content
     * @param userId user ID (for authorization)
     * @param postId post ID
     * @param postDto updated post data
     * @return updated post or 404 if not found
     */
    @PutMapping("/user/{userId}/posts/{postId}")
    public ResponseEntity<?> updatePostContent(
            @PathVariable Long userId, 
            @PathVariable Long postId, 
            @RequestBody PostDTO postDto) {
        logger.info("PUT /api/posts/user/{}/posts/{} - Updating post", userId, postId);
        
        try {
            return postService.updatePostContentByUser(userId, postId, postDto.getContent())
                    .map(post -> {
                        logger.info("Post updated: id={}", postId);
                        return ResponseEntity.ok(post);
                    })
                    .orElseGet(() -> {
                        logger.warn("Post not found or unauthorized: postId={}, userId={}", postId, userId);
                        return ResponseEntity.notFound().build();
                    });
        } catch (RuntimeException e) {
            logger.error("Error updating post: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a post
     * @param id post ID
     * @return 204 if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        logger.info("DELETE /api/posts/{} - Deleting post", id);
        
        try {
            postService.deletePost(id);
            logger.info("Post deleted: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("{}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}