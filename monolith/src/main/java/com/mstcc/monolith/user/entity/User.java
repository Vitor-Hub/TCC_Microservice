package com.mstcc.monolith.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity representing a registered user.
 *
 * <p>The table name and column names are kept identical to the {@code user-ms}
 * microservice to ensure that schema migration between architectures (if ever
 * needed) requires no DDL changes.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Sets {@code createdAt} to the current time before the first persist. */
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
