package com.mstcc.userms.services;

import com.mstcc.userms.entities.User;

import java.util.List;
import java.util.Optional;

/**
 * Contract for user management operations.
 * Controllers depend on this abstraction, not on the concrete implementation (DIP).
 * New implementations (e.g. read replicas, external IdP) can be introduced
 * without modifying any controller or caller (OCP).
 */
public interface UserService {

    /**
     * Returns all registered users.
     * @return unordered list of all users
     */
    List<User> findAllUsers();

    /**
     * Looks up a user by primary key.
     * @param id user ID
     * @return optional containing the user if found
     */
    Optional<User> findUserById(Long id);

    /**
     * Looks up a user by username.
     * @param username the exact username to search for
     * @return optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Persists a new user after validating username uniqueness.
     * @param user user entity to save
     * @return the saved user with generated ID
     * @throws IllegalArgumentException if the username is already taken
     */
    User saveUser(User user);

    /**
     * Updates an existing user's fields.
     * @param id          ID of the user to update
     * @param userDetails new field values
     * @return optional containing the updated user, or empty if not found
     * @throws IllegalArgumentException if the new username conflicts with another user
     */
    Optional<User> updateUser(Long id, User userDetails);

    /**
     * Deletes a user by ID.
     * @param id user ID
     * @throws IllegalArgumentException if no user exists with that ID
     */
    void deleteUser(Long id);

    /**
     * Checks whether a user exists by ID.
     * Used by other services for lightweight cross-service validation.
     * @param id user ID
     * @return true if the user exists
     */
    boolean existsById(Long id);
}
