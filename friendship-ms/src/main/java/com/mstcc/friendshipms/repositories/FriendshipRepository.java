package com.mstcc.friendshipms.repositories;

import com.mstcc.friendshipms.entities.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    /**
     * Find all friendships where the user is either userId1 or userId2
     * This returns all friendships involving the specified user
     */
    List<Friendship> findByUserId1OrUserId2(Long userId1, Long userId2);
}