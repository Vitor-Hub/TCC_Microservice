package com.mstcc.monolith.comment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a comment on a post.
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_comments_post_id", columnList = "post_id"),
        @Index(name = "idx_comments_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Sets {@code createdAt} to the current time before the first persist. */
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
