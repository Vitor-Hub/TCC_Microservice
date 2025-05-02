package com.mstcc.friendshipms.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Setter
@Getter
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id1")
    private Long userId1;

    @Column(name = "user_id2")
    private Long userId2;

    private String status; // for example, 'pending', 'accepted', 'declined'

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

}