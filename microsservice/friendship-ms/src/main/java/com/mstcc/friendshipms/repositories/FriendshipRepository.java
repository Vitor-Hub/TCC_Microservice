package com.mstcc.friendshipms.repositories;

import com.mstcc.friendshipms.entities.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Find all friendships where the user is either userId1 or userId2.
     * Returns all friendships involving the specified user.
     *
     * @param userId1 first user ID
     * @param userId2 second user ID (pass the same value as userId1 to match either column)
     * @return list of matching friendships
     */
    List<Friendship> findByUserId1OrUserId2(Long userId1, Long userId2);

    /**
     * Checks whether a friendship already exists between two users.
     * IDs must be normalised (userId1 &lt; userId2) before calling.
     *
     * @param userId1 the smaller user ID
     * @param userId2 the larger user ID
     * @return {@code true} if the friendship row already exists
     */
    boolean existsByUserId1AndUserId2(Long userId1, Long userId2);

    /**
     * Finds the friendship row for a given normalised user pair.
     * IDs must be normalised (userId1 &lt; userId2) before calling.
     *
     * @param userId1 the smaller user ID
     * @param userId2 the larger user ID
     * @return an {@link Optional} containing the friendship if found, or empty
     */
    Optional<Friendship> findByUserId1AndUserId2(Long userId1, Long userId2);
}