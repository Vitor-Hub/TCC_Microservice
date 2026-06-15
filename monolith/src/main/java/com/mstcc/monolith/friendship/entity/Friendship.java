package com.mstcc.monolith.friendship.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a friendship connection between two users.
 *
 * <p>ID normalisation invariant: {@code userId1} is always the smaller of the two IDs,
 * ensuring that (A,B) and (B,A) resolve to the same database row and preventing
 * duplicate friendship entries with swapped user IDs. This invariant is enforced
 * in {@link com.mstcc.monolith.friendship.service.FriendshipService} before every
 * persist operation.
 */
@Entity
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id1", "user_id2"}),
    indexes = {
        @Index(name = "idx_friendships_user_id2", columnList = "user_id2")
    }
)
@Getter
@Setter
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id1")
    private Long userId1;

    @Column(name = "user_id2")
    private Long userId2;

    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Sets {@code createdAt} to the current time before the first persist. */
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
