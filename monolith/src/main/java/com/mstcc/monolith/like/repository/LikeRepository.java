package com.mstcc.monolith.like.repository;

import com.mstcc.monolith.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Like} entities.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * Finds all likes for a specific post.
     *
     * @param postId post identifier
     * @return list of likes (may be empty)
     */
    List<Like> findByPostId(Long postId);

    /**
     * Finds all likes made by a specific user.
     *
     * @param userId user identifier
     * @return list of likes (may be empty)
     */
    List<Like> findByUserId(Long userId);

    /**
     * Finds all likes for a specific comment.
     *
     * @param commentId comment identifier
     * @return list of likes (may be empty)
     */
    List<Like> findByCommentId(Long commentId);
}
