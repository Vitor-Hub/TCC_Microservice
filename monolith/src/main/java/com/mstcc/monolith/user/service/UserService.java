package com.mstcc.monolith.user.service;

import com.mstcc.monolith.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mstcc.monolith.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for the User domain.
 *
 * <p>In the microservices implementation this class is the {@code UserServiceImpl}
 * inside {@code user-ms}. In the monolith it is a plain Spring {@code @Service}
 * that other domain services can inject directly, eliminating the Feign HTTP hop
 * and the associated serialisation/deserialisation overhead.
 *
 * <p>Cache names and TTLs match the microservices configuration so that the
 * caching behaviour is not a variable when comparing architectures.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    /**
     * Constructs the service with its required repository dependency.
     *
     * @param userRepository JPA repository for User persistence
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns all registered users.
     *
     * @return unordered list of all users
     */
    @Cacheable(value = "allUsers", key = "'all'")
    public List<User> findAllUsers() {
        logger.info("Fetching all users from database");
        return userRepository.findAll();
    }

    /**
     * Looks up a user by primary key.
     *
     * @param id user ID
     * @return optional containing the user if found
     */
    @Cacheable(value = "users", key = "#id")
    public Optional<User> findUserById(Long id) {
        logger.info("Fetching user by id: {} (cache miss or first call)", id);
        return userRepository.findById(id);
    }

    /**
     * Looks up a user by username.
     *
     * @param username the exact username to search for
     * @return optional containing the user if found
     */
    @Cacheable(value = "usersByUsername", key = "#username")
    public Optional<User> findByUsername(String username) {
        logger.info("Fetching user by username: {} (cache miss or first call)", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Checks whether a user exists by ID.
     * Used by other domain services for lightweight cross-domain validation —
     * replacing what was a Feign HTTP call in the microservices implementation.
     *
     * @param id user ID
     * @return true if the user exists
     */
    @Cacheable(value = "userExists", key = "#id")
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * Persists a new user after validating username uniqueness.
     *
     * @param user user entity to save
     * @return the saved user with generated ID
     * @throws IllegalArgumentException if the username is already taken
     */
    @Transactional
    @CacheEvict(value = {"users", "usersByUsername", "allUsers", "userExists"}, allEntries = true)
    public User saveUser(User user) {
        logger.info("Creating new user: {}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.error("Username already exists: {}", user.getUsername());
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        User savedUser = userRepository.save(user);
        logger.info("User created successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * Updates an existing user's fields.
     *
     * @param id          ID of the user to update
     * @param userDetails new field values
     * @return optional containing the updated user, or empty if not found
     * @throws IllegalArgumentException if the new username conflicts with another user
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "usersByUsername", allEntries = true),
        @CacheEvict(value = "allUsers", allEntries = true),
        @CacheEvict(value = "userExists", key = "#id")
    })
    public Optional<User> updateUser(Long id, User userDetails) {
        logger.info("Updating user: id={}", id);

        return userRepository.findById(id).map(user -> {
            if (!user.getUsername().equals(userDetails.getUsername())) {
                Optional<User> existing = userRepository.findByUsername(userDetails.getUsername());
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    logger.error("Username already taken: {}", userDetails.getUsername());
                    throw new IllegalArgumentException("Username already exists: " + userDetails.getUsername());
                }
            }

            user.setUsername(userDetails.getUsername());
            user.setEmail(userDetails.getEmail());
            user.setPassword(userDetails.getPassword());

            User updated = userRepository.save(user);
            logger.info("User updated successfully: id={}", id);
            return updated;
        });
    }

    /**
     * Deletes a user by ID.
     *
     * @param id user ID
     * @throws IllegalArgumentException if no user exists with that ID
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "usersByUsername", allEntries = true),
        @CacheEvict(value = "allUsers", allEntries = true),
        @CacheEvict(value = "userExists", key = "#id")
    })
    public void deleteUser(Long id) {
        logger.info("Deleting user: id={}", id);

        if (!userRepository.existsById(id)) {
            logger.warn("User not found for deletion: id={}", id);
            throw new IllegalArgumentException("User not found: " + id);
        }

        userRepository.deleteById(id);
        logger.info("User deleted successfully: id={}", id);
    }
}
