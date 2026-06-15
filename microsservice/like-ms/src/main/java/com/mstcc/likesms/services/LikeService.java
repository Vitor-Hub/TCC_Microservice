package com.mstcc.likesms.services;

import com.mstcc.likesms.entities.Like;

import java.util.List;
import java.util.Optional;

/**
 * Contract for Like business operations.
 *
 * <p>Applying DIP: high-level modules ({@code LikeController}) depend on this abstraction,
 * not on a concrete implementation. The concrete class ({@code LikeServiceImpl}) is wired
 * by Spring at runtime, keeping controller and implementation independently changeable.
 */
public interface LikeService {

    /**
     * Creates a like after parallel validation of the referenced user, post, and/or comment.
     *
     * @param like the like entity to persist
     * @return the saved like with a generated ID
     * @throws com.mstcc.likesms.exceptions.LikeValidationException if upstream validation fails
     * @throws IllegalArgumentException if neither postId nor commentId is set
     */
    Like createAndValidateLike(Like like);

    /**
     * Updates an existing like after re-validating the referenced user and post.
     *
     * @param id          ID of the like to update
     * @param likeDetails updated field values
     * @return optional containing the updated like, or empty if not found
     * @throws com.mstcc.likesms.exceptions.LikeValidationException if upstream validation fails
     */
    Optional<Like> updateAndValidateLike(Long id, Like likeDetails);

    /**
     * Retrieves a like by its ID.
     *
     * @param id like ID
     * @return optional containing the like, or empty if not found
     */
    Optional<Like> getLikeById(Long id);

    /**
     * Retrieves all likes for a given post.
     *
     * @param postId post ID
     * @return list of likes (may be empty)
     */
    List<Like> findLikesByPostId(Long postId);

    /**
     * Retrieves all likes made by a given user.
     *
     * @param userId user ID
     * @return list of likes (may be empty)
     */
    List<Like> findLikesByUserId(Long userId);

    /**
     * Retrieves all likes for a given comment.
     *
     * @param commentId comment ID
     * @return list of likes (may be empty)
     */
    List<Like> findLikesByCommentId(Long commentId);

    /**
     * Deletes a like by its ID.
     *
     * @param id like ID
     */
    void deleteLike(Long id);

    /**
     * Retrieves all likes without any filter.
     *
     * @return list of all likes
     */
    List<Like> findAllLikes();
}
