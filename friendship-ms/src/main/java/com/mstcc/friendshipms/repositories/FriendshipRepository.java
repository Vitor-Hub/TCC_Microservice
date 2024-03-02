package com.mstcc.friendshipms.repositories;

import com.mstcc.friendshipms.entities.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // You can add custom query methods if needed
}