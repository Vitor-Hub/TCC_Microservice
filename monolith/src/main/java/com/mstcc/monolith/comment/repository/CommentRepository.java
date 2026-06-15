package com.mstcc.monolith.comment.repository;

import com.mstcc.monolith.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Comment} entities.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Finds all comments for a specific post.
     *
     * @param postId post identifier
     * @return list of comments (may be empty)
     */
    List<Comment> findByPostId(Long postId);

    /**
     * Finds all comments authored by a specific user.
     *
     * @param userId user identifier
     * @return list of comments (may be empty)
     */
    List<Comment> findByUserId(Long userId);
}
