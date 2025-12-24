package com.mstcc.userms.services;

import com.mstcc.userms.entities.User;
import com.mstcc.userms.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing User entities with caching support
 * This is a critical service called by all other microservices
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves all users without caching (not recommended for large datasets)
     */
    public List<User> findAllUsers() {
        logger.info("Fetching all users from database");
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by ID with caching
     */
    @Cacheable(
        value = "users",
        key = "#id",
        unless = "#result == null"
    )
    public Optional<User> findUserById(Long id) {
        logger.info("Fetching user by id: {} (cache miss or first call)", id);
        return userRepository.findById(id);
    }

    /**
     * Retrieves a user by username with caching
     */
    @Cacheable(
        value = "usersByUsername",
        key = "#username",
        unless = "#result == null"
    )
    public Optional<User> findByUsername(String username) {
        logger.info("Fetching user by username: {} (cache miss or first call)", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Creates a new user and evicts cache
     */
    @Transactional
    @CacheEvict(value = {"users", "usersByUsername"}, allEntries = true)
    public User saveUser(User user) {
        logger.info("Creating new user: {}", user.getUsername());

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.error("Username already exists: {}", user.getUsername());
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        User savedUser = userRepository.save(user);

        logger.info("User created successfully: id={}, username={}",
                     savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    /**
     * Updates an existing user and evicts cache
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "usersByUsername", allEntries = true)
    })
    public Optional<User> updateUser(Long id, User userDetails) {
        logger.info("Updating user: id={}", id);

        return userRepository.findById(id).map(user -> {

            if (!user.getUsername().equals(userDetails.getUsername())) {
                Optional<User> existingUser = userRepository.findByUsername(userDetails.getUsername());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
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
     * Deletes a user and evicts cache
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#id"),
        @CacheEvict(value = "usersByUsername", allEntries = true)
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

    /**
     * Checks if a user exists (cached)
     */
    @Cacheable(value = "userExists", key = "#id")
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}
