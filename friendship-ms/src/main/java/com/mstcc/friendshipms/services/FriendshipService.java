package com.mstcc.friendshipms.services;

import com.mstcc.friendshipms.entities.Friendship;

import java.util.List;
import java.util.Optional;

/**
 * Contract for Friendship business operations.
 *
 * <p>Applying DIP: high-level modules ({@code FriendshipController}) depend on this abstraction,
 * not on a concrete implementation. The concrete class ({@code FriendshipServiceImpl}) is wired
 * by Spring at runtime, keeping controller and implementation independently changeable.
 */
public interface FriendshipService {

    /**
     * Creates a friendship after parallel validation of both users.
     * Normalises user ID order so that (A,B) and (B,A) map to the same row.
     *
     * @param friendship the friendship entity to persist
     * @return the saved friendship with a generated ID
     * @throws IllegalArgumentException if both user IDs are equal (self-friendship)
     * @throws com.mstcc.friendshipms.exception.FriendshipValidationException if upstream validation fails
     */
    Friendship createAndValidateFriendship(Friendship friendship);

    /**
     * Retrieves a friendship by its ID.
     *
     * @param id friendship ID
     * @return optional containing the friendship, or empty if not found
     */
    Optional<Friendship> getFriendshipById(Long id);

    /**
     * Retrieves all friendships involving a given user.
     *
     * @param userId user ID
     * @return list of friendships (may be empty)
     * @throws com.mstcc.friendshipms.exception.FriendshipValidationException if the user does not exist
     */
    List<Friendship> findFriendshipsByUserId(Long userId);

    /**
     * Updates an existing friendship after re-validating both users.
     *
     * @param id                friendship ID
     * @param friendshipDetails updated field values
     * @return optional containing the updated friendship, or empty if not found
     * @throws IllegalArgumentException if both user IDs are equal
     * @throws com.mstcc.friendshipms.exception.FriendshipValidationException if upstream validation fails
     */
    Optional<Friendship> updateAndValidateFriendship(Long id, Friendship friendshipDetails);

    /**
     * Deletes a friendship by its ID.
     *
     * @param id friendship ID
     * @throws IllegalArgumentException if no friendship with the given ID exists
     */
    void deleteFriendship(Long id);

    /**
     * Retrieves all friendships without any filter.
     *
     * @return list of all friendships
     */
    List<Friendship> findAllFriendships();
}
