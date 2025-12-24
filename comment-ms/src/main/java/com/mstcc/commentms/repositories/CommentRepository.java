package com.mstcc.commentms.repositories;

import com.mstcc.commentms.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Find all comments for a specific post
     */
    List<Comment> findByPostId(Long postId);
    
    /**
     * Find all comments by a specific user
     */
    List<Comment> findByUserId(Long userId);
}