package com.mstcc.likesms.repositories;

import com.mstcc.likesms.entities.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    
    /**
     * Find all likes for a specific post
     */
    List<Like> findByPostId(Long postId);
    
    /**
     * Find all likes by a specific user
     */
    List<Like> findByUserId(Long userId);
    
    /**
     * Find all likes for a specific comment
     */
    List<Like> findByCommentId(Long commentId);
}