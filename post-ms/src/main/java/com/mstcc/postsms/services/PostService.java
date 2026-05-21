package com.mstcc.postsms.services;

import com.mstcc.postsms.dto.PostDTO;

import java.util.List;
import java.util.Optional;

/**
 * Contract for post management operations.
 * Controllers depend on this abstraction, not on the concrete implementation (DIP).
 * New implementations can be introduced without modifying callers (OCP).
 */
public interface PostService {

    /**
     * Returns recent posts up to the given limit, with user and comments populated.
     * @param limit maximum number of posts (capped at 100)
     * @return list of recent post DTOs
     */
    List<PostDTO> getRecentPosts(int limit);

    /**
     * Returns a post by ID with user and comments populated.
     * @param id post ID
     * @return optional containing the post DTO if found
     */
    Optional<PostDTO> getPostById(Long id);

    /**
     * Returns all posts by a user.
     * @param userId user ID
     * @return list of that user's post DTOs
     */
    List<PostDTO> getPostsByUser(Long userId);

    /**
     * Creates a new post.
     * @param postDTO post data (must have user.id set)
     * @return the created post DTO
     */
    PostDTO createPost(PostDTO postDTO);

    /**
     * Updates a post's content.
     * @param userId  owning user ID (for authorization check)
     * @param postId  post to update
     * @param content new content string
     * @return optional containing updated post DTO, or empty if not found or not owner
     */
    Optional<PostDTO> updatePostContentByUser(Long userId, Long postId, String content);

    /**
     * Deletes a post.
     * @param id post ID
     * @throws IllegalArgumentException if no post exists with that ID
     */
    void deletePost(Long id);

    /**
     * Lightweight existence check — does not trigger Feign calls.
     * Used by comment-ms for cross-service validation.
     * @param id post ID
     * @return true if the post exists
     */
    boolean existsById(Long id);
}
