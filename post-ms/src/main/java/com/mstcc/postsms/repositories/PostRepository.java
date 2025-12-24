package com.mstcc.postsms.repositories;

import com.mstcc.postsms.entities.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * Find all posts by a specific user
     */
    List<Post> findByUserId(Long userId);
}