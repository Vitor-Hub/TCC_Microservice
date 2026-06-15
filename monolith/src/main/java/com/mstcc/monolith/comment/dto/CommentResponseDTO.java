package com.mstcc.monolith.comment.dto;

import com.mstcc.monolith.comment.entity.Comment;

import java.time.LocalDateTime;

/**
 * Read-only projection of {@link Comment} returned by the API.
 *
 * @param id        unique identifier
 * @param postId    ID of the post this comment belongs to
 * @param userId    ID of the comment's author
 * @param content   comment text
 * @param createdAt creation timestamp
 */
public record CommentResponseDTO(
        Long id,
        Long postId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
    /**
     * Creates a response DTO from a {@link Comment} entity.
     *
     * @param comment source entity
     * @return populated DTO
     */
    public static CommentResponseDTO from(Comment comment) {
        return new CommentResponseDTO(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
