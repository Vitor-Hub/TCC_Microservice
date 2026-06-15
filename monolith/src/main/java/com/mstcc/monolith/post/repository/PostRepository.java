package com.mstcc.monolith.post.repository;

import com.mstcc.monolith.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Post} entities.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Returns all posts for a given user, ordered by the repository's default sort.
     *
     * @param userId user identifier
     * @return list of the user's posts
     */
    List<Post> findByUserId(Long userId);

    /**
     * Returns a paginated view of all posts for the recent feed.
     * Delegates ordering to the {@link Pageable} argument (typically descending createdAt).
     *
     * @param pageable pagination and sort instructions
     * @return one page of posts
     */
    Page<Post> findAll(Pageable pageable);
}
