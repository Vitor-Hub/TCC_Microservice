package com.mstcc.monolith.like.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a like on a post or a comment.
 */
@Entity
@Table(
    name = "likes",
    indexes = {
        @Index(name = "idx_likes_post_id",    columnList = "post_id"),
        @Index(name = "idx_likes_user_id",    columnList = "user_id"),
        @Index(name = "idx_likes_comment_id", columnList = "comment_id")
    }
)
@Getter
@Setter
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Sets {@code createdAt} to the current time before the first persist. */
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
