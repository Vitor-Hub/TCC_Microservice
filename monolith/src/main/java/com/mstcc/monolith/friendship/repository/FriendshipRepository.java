package com.mstcc.monolith.friendship.repository;

import com.mstcc.monolith.friendship.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Friendship} entities.
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Finds all friendships where the user is either userId1 or userId2.
     *
     * @param userId1 first user ID (pass the same value as userId2 to match either column)
     * @param userId2 second user ID
     * @return list of matching friendships
     */
    List<Friendship> findByUserId1OrUserId2(Long userId1, Long userId2);

    /**
     * Checks whether a friendship already exists for a given normalised user pair.
     * IDs must be normalised (userId1 &lt; userId2) before calling.
     *
     * @param userId1 the smaller user ID
     * @param userId2 the larger user ID
     * @return {@code true} if the row exists
     */
    boolean existsByUserId1AndUserId2(Long userId1, Long userId2);

    /**
     * Finds the friendship row for a given normalised user pair.
     * IDs must be normalised (userId1 &lt; userId2) before calling.
     *
     * @param userId1 the smaller user ID
     * @param userId2 the larger user ID
     * @return optional containing the friendship if found
     */
    Optional<Friendship> findByUserId1AndUserId2(Long userId1, Long userId2);
}
