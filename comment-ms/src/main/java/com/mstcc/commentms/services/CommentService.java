package com.mstcc.commentms.services;

import com.mstcc.commentms.entities.Comment;

import java.util.List;
import java.util.Optional;

/**
 * Contract for comment management operations.
 * Controllers depend on this abstraction, not on the concrete implementation (DIP).
 * New implementations can be introduced without modifying callers (OCP).
 */
public interface CommentService {

    /**
     * Creates a new comment with parallel validation of user and post existence.
     * @param comment comment entity to persist
     * @return the saved comment
     * @throws RuntimeException if user or post validation fails or times out
     */
    Comment createAndValidateComment(Comment comment);

    /**
     * Returns a comment by ID.
     * @param id comment ID
     * @return optional containing the comment if found
     */
    Optional<Comment> getCommentById(Long id);

    /**
     * Returns all comments for a given post.
     * @param postId post ID
     * @return list of comments, possibly empty
     */
    List<Comment> findCommentsByPostId(Long postId);

    /**
     * Returns all comments authored by a given user.
     * @param userId user ID
     * @return list of comments, possibly empty
     */
    List<Comment> findCommentsByUserId(Long userId);

    /**
     * Updates an existing comment with re-validation of user and post.
     * @param id             comment ID to update
     * @param commentDetails new field values
     * @return optional containing the updated comment, or empty if not found
     */
    Optional<Comment> updateAndValidateComment(Long id, Comment commentDetails);

    /**
     * Deletes a comment by ID.
     * @param id comment ID
     * @throws IllegalArgumentException if the comment does not exist
     */
    void deleteComment(Long id);

    /**
     * Returns all comments (no filter).
     * @return list of all comments
     */
    List<Comment> findAllComments();
}
