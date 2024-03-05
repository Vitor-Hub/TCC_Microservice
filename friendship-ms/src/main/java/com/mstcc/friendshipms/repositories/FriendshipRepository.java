package com.mstcc.friendshipms.repositories;

import com.mstcc.friendshipms.entities.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUserId1OrUserId2(Long userId1, Long userId2);
}